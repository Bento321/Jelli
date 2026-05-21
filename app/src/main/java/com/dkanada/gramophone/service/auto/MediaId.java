package com.dkanada.gramophone.service.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MediaId {
    public static final String ROOT = "gelli/root";
    public static final String PLAYLISTS = "gelli/playlists";
    public static final String ALBUMS = "gelli/albums";
    public static final String ARTISTS = "gelli/artists";
    public static final String SONGS = "gelli/songs";
    public static final String FAVORITES = "gelli/favorites";
    public static final String SIGN_IN = "gelli/signin";

    public static final String PREFIX_PLAYLIST = "gelli/playlist/";
    public static final String PREFIX_ALBUM = "gelli/album/";
    public static final String PREFIX_ARTIST = "gelli/artist/";
    public static final String PREFIX_SONG = "gelli/song/";
    public static final String PREFIX_FAVORITE_SONG = "gelli/favorites/song/";

    public static final String SHUFFLE_FAVORITES = "gelli/shuffle/favorites";
    public static final String SHUFFLE_SONGS = "gelli/shuffle/songs";

    private MediaId() {
    }

    @NonNull
    public static String forPlaylist(@NonNull String id) {
        return PREFIX_PLAYLIST + id;
    }

    @NonNull
    public static String forAlbum(@NonNull String id) {
        return PREFIX_ALBUM + id;
    }

    @NonNull
    public static String forArtist(@NonNull String id) {
        return PREFIX_ARTIST + id;
    }

    @NonNull
    public static String forSong(@NonNull String id) {
        return PREFIX_SONG + id;
    }

    @NonNull
    public static String forFavoriteSong(@NonNull String id) {
        return PREFIX_FAVORITE_SONG + id;
    }

    @Nullable
    public static String extractId(@NonNull String mediaId, @NonNull String prefix) {
        if (!mediaId.startsWith(prefix)) return null;
        String id = mediaId.substring(prefix.length());
        return id.isEmpty() ? null : id;
    }
}
