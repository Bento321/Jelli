package com.dkanada.gramophone.service.auto;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat.Result;
import androidx.media.utils.MediaConstants;

import com.dkanada.gramophone.App;
import com.dkanada.gramophone.R;
import com.dkanada.gramophone.glide.CustomGlideRequest;
import com.dkanada.gramophone.model.Album;
import com.dkanada.gramophone.model.Artist;
import com.dkanada.gramophone.model.Playlist;
import com.dkanada.gramophone.model.Song;
import com.dkanada.gramophone.util.QueryUtil;

import org.jellyfin.apiclient.interaction.Response;
import org.jellyfin.apiclient.model.dto.BaseItemDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AutoMediaBrowser {
    private final Context context;
    private final Consumer<List<Song>> playCallback;
    private final Consumer<List<Song>> shuffleCallback;

    public AutoMediaBrowser(Context context, Consumer<List<Song>> playCallback, Consumer<List<Song>> shuffleCallback) {
        this.context = context;
        this.playCallback = playCallback;
        this.shuffleCallback = shuffleCallback;
    }

    public static Bundle rootHints() {
        Bundle hints = new Bundle();
        hints.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM);
        hints.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM);
        return hints;
    }

    public void loadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result) {
        if (!isAuthenticated()) {
            result.sendResult(Collections.singletonList(signInItem()));
            return;
        }

        switch (parentId) {
            case MediaId.ROOT:
                result.sendResult(buildRoot());
                return;
            case MediaId.PLAYLISTS:
                result.detach();
                loadPlaylists(result);
                return;
            case MediaId.ALBUMS:
                result.detach();
                loadAlbums(result);
                return;
            case MediaId.ARTISTS:
                result.detach();
                loadArtists(result);
                return;
            case MediaId.SONGS:
                result.detach();
                loadSongs(result, null);
                return;
            case MediaId.FAVORITES:
                result.detach();
                loadFavorites(result);
                return;
        }

        String albumId = MediaId.extractId(parentId, MediaId.PREFIX_ALBUM);
        if (albumId != null) {
            result.detach();
            QueryUtil.getSongsForAlbum(albumId, songs -> result.sendResult(toSongItems(songs)));
            return;
        }

        String playlistId = MediaId.extractId(parentId, MediaId.PREFIX_PLAYLIST);
        if (playlistId != null) {
            result.detach();
            QueryUtil.getSongsForAlbum(playlistId, songs -> result.sendResult(toSongItems(songs)));
            return;
        }

        String artistId = MediaId.extractId(parentId, MediaId.PREFIX_ARTIST);
        if (artistId != null) {
            result.detach();
            QueryUtil.getAlbumsForArtist(artistId, albums -> result.sendResult(toAlbumItems(albums)));
            return;
        }

        result.sendResult(Collections.emptyList());
    }

    public void playFromMediaId(@NonNull String mediaId, Bundle extras) {
        if (!isAuthenticated()) return;

        // Favorite song: queue all favorites starting from the tapped song
        String favSongId = MediaId.extractId(mediaId, MediaId.PREFIX_FAVORITE_SONG);
        if (favSongId != null) {
            final String targetId = favSongId;
            QueryUtil.getFavoriteSongs(songs -> {
                int startIndex = 0;
                for (int i = 0; i < songs.size(); i++) {
                    if (songs.get(i).id.equals(targetId)) {
                        startIndex = i;
                        break;
                    }
                }
                List<Song> ordered = new ArrayList<>();
                ordered.addAll(songs.subList(startIndex, songs.size()));
                ordered.addAll(songs.subList(0, startIndex));
                playCallback.accept(ordered);
            });
            return;
        }

        if (MediaId.SHUFFLE_FAVORITES.equals(mediaId)) {
            QueryUtil.getFavoriteSongs(shuffleCallback::accept);
            return;
        }

        if (MediaId.SHUFFLE_SONGS.equals(mediaId)) {
            QueryUtil.getSongs(new org.jellyfin.apiclient.model.querying.ItemQuery(), shuffleCallback::accept);
            return;
        }

        String songId = MediaId.extractId(mediaId, MediaId.PREFIX_SONG);
        if (songId != null) {
            QueryUtil.getItemById(songId, new Response<BaseItemDto>() {
                @Override
                public void onResponse(BaseItemDto result) {
                    playCallback.accept(Collections.singletonList(new Song(result)));
                }
            });
            return;
        }

        String albumId = MediaId.extractId(mediaId, MediaId.PREFIX_ALBUM);
        if (albumId != null) {
            QueryUtil.getSongsForAlbum(albumId, playCallback::accept);
            return;
        }

        String playlistId = MediaId.extractId(mediaId, MediaId.PREFIX_PLAYLIST);
        if (playlistId != null) {
            QueryUtil.getSongsForAlbum(playlistId, playCallback::accept);
            return;
        }

        if (MediaId.FAVORITES.equals(mediaId)) {
            QueryUtil.getFavoriteSongs(playCallback::accept);
        }
    }

    public void playFromSearch(String query, Bundle extras) {
        if (!isAuthenticated()) return;

        if (query == null || query.trim().isEmpty()) {
            QueryUtil.getFavoriteSongs(playCallback::accept);
            return;
        }

        QueryUtil.searchSongs(query, songs -> {
            if (songs.isEmpty()) return;
            playCallback.accept(songs);
        });
    }

    private boolean isAuthenticated() {
        String userId = App.getApiClient().getCurrentUserId();
        return userId != null && !userId.isEmpty();
    }

    private List<MediaItem> buildRoot() {
        List<MediaItem> items = new ArrayList<>(5);
        items.add(browsable(MediaId.FAVORITES, context.getString(R.string.favorites), null));
        items.add(browsable(MediaId.PLAYLISTS, context.getString(R.string.playlists), null));
        items.add(browsable(MediaId.ALBUMS, context.getString(R.string.albums), null));
        items.add(browsable(MediaId.ARTISTS, context.getString(R.string.artists), null));
        items.add(browsable(MediaId.SONGS, context.getString(R.string.songs), null));
        return items;
    }

    private MediaItem signInItem() {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MediaId.SIGN_IN)
                .setTitle(context.getString(R.string.auto_signin_required))
                .build();
        return new MediaItem(description, 0);
    }

    private void loadPlaylists(Result<List<MediaItem>> result) {
        QueryUtil.getPlaylists((List<Playlist> playlists) -> {
            List<MediaItem> items = new ArrayList<>(playlists.size());
            for (Playlist playlist : playlists) {
                items.add(browsableAndPlayable(
                        MediaId.forPlaylist(playlist.id),
                        playlist.name,
                        null,
                        imageUri(playlist.primary)));
            }
            result.sendResult(items);
        });
    }

    private void loadAlbums(Result<List<MediaItem>> result) {
        QueryUtil.getAlbums(new org.jellyfin.apiclient.model.querying.ItemQuery(), albums ->
                result.sendResult(toAlbumItems(albums)));
    }

    private void loadArtists(Result<List<MediaItem>> result) {
        QueryUtil.getArtists(new org.jellyfin.apiclient.model.querying.ArtistsQuery(), (List<Artist> artists) -> {
            List<MediaItem> items = new ArrayList<>(artists.size());
            for (Artist artist : artists) {
                items.add(browsable(MediaId.forArtist(artist.id), artist.name, imageUri(artist.primary)));
            }
            result.sendResult(items);
        });
    }

    private void loadSongs(Result<List<MediaItem>> result, String parentId) {
        org.jellyfin.apiclient.model.querying.ItemQuery query = new org.jellyfin.apiclient.model.querying.ItemQuery();
        if (parentId != null) query.setParentId(parentId);
        QueryUtil.getSongs(query, songs -> {
            List<MediaItem> items = new ArrayList<>(songs.size() + 1);
            items.add(shufflePlayable(MediaId.SHUFFLE_SONGS, context.getString(R.string.action_shuffle_all)));
            items.addAll(toSongItems(songs));
            result.sendResult(items);
        });
    }

    private void loadFavorites(Result<List<MediaItem>> result) {
        QueryUtil.getFavoriteSongs(songs -> {
            List<MediaItem> items = new ArrayList<>(songs.size() + 1);
            items.add(shufflePlayable(MediaId.SHUFFLE_FAVORITES, context.getString(R.string.action_shuffle_all)));
            for (Song song : songs) {
                items.add(playable(MediaId.forFavoriteSong(song.id), song.title, song.artistName, imageUri(song.primary)));
            }
            result.sendResult(items);
        });
    }

    private List<MediaItem> toAlbumItems(List<Album> albums) {
        List<MediaItem> items = new ArrayList<>(albums.size());
        for (Album album : albums) {
            items.add(browsableAndPlayable(
                    MediaId.forAlbum(album.id),
                    album.title,
                    album.artistName,
                    imageUri(album.primary)));
        }
        return items;
    }

    private List<MediaItem> toSongItems(List<Song> songs) {
        List<MediaItem> items = new ArrayList<>(songs.size());
        for (Song song : songs) {
            items.add(playable(
                    MediaId.forSong(song.id),
                    song.title,
                    song.artistName,
                    imageUri(song.primary)));
        }
        return items;
    }

    private MediaItem shufflePlayable(String mediaId, String title) {
        MediaDescriptionCompat.Builder b = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title);
        return new MediaItem(b.build(), MediaItem.FLAG_PLAYABLE);
    }

    private MediaItem browsable(String mediaId, String title, Uri icon) {
        MediaDescriptionCompat.Builder b = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title);
        if (icon != null) b.setIconUri(icon);
        return new MediaItem(b.build(), MediaItem.FLAG_BROWSABLE);
    }

    private MediaItem browsableAndPlayable(String mediaId, String title, String subtitle, Uri icon) {
        MediaDescriptionCompat.Builder b = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title);
        if (subtitle != null) b.setSubtitle(subtitle);
        if (icon != null) b.setIconUri(icon);
        return new MediaItem(b.build(), MediaItem.FLAG_BROWSABLE | MediaItem.FLAG_PLAYABLE);
    }

    private MediaItem playable(String mediaId, String title, String subtitle, Uri icon) {
        MediaDescriptionCompat.Builder b = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title);
        if (subtitle != null) b.setSubtitle(subtitle);
        if (icon != null) b.setIconUri(icon);
        return new MediaItem(b.build(), MediaItem.FLAG_PLAYABLE);
    }

    private Uri imageUri(String primary) {
        if (primary == null) return null;
        String url = CustomGlideRequest.createUrl(primary);
        return url != null ? Uri.parse(url) : null;
    }
}
