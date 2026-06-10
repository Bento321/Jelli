package com.dkanada.gramophone.activities.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;

import com.dkanada.gramophone.interfaces.StateListener;
import com.dkanada.gramophone.service.LoginService;
import com.dkanada.gramophone.util.NavigationUtil;

public abstract class AbsMusicContentActivity extends AbsMusicPanelActivity implements StateListener {
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (intent.getAction() == null) return;

            switch(intent.getAction()) {
                case LoginService.STATE_ONLINE:
                    onStateOnline();
                    break;
                case LoginService.STATE_OFFLINE:
                    NavigationUtil.startLogin(context);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(LoginService.STATE_POLLING);
        filter.addAction(LoginService.STATE_ONLINE);
        filter.addAction(LoginService.STATE_OFFLINE);

        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // Always re-verify after the receiver is registered so the broadcast is never missed.
        // getApiClient() is never null, so the old null-check was dead code that always called
        // onStateOnline() directly — bypassing LoginService and leaving no retry path when
        // battery optimization holds back the Volley request.
        startService(new Intent(this, LoginService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-verify every time the activity becomes visible. Devices with aggressive battery
        // optimization can kill or stall Volley threads while the screen is off; a fresh
        // LoginService call here runs after Doze has lifted (screen-on).
        startService(new Intent(this, LoginService.class));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);

        super.onDestroy();
    }

    @Override
    public void onStatePolling() {
    }

    @Override
    public void onStateOffline() {
    }
}
