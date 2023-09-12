package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isPrevNextClicked;
import static com.linearity.musicplayer.MainActivity.pathToListen2;
import static com.linearity.musicplayer.PlayerService.intentNext;
import static com.linearity.musicplayer.PlayerService.intentOrder;
import static com.linearity.musicplayer.PlayerService.intentPause;
import static com.linearity.musicplayer.PlayerService.intentPrev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PlayerServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
//        Log.d("[linearity]","received");
        if (action == null){return;}
        if (intentNext != null && action.equals(intentNext.getAction())){
            instance.NextOnClick();
        }
        if (intentPrev != null && action.equals(intentPrev.getAction())){
            instance.PrevOnClick();
        }
        if(intentPause != null && action.equals(intentPause.getAction())){
            instance.PauseOnClick();
        }
        if(intentOrder != null && action.equals(intentOrder.getAction())){
            instance.ChangeOrderOnClick();
        }
    }
}
