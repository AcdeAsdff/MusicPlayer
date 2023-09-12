package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.PlayerService.intentCloseNotification;
import static com.linearity.musicplayer.PlayerService.intentNext;
import static com.linearity.musicplayer.PlayerService.intentOrder;
import static com.linearity.musicplayer.PlayerService.intentPause;
import static com.linearity.musicplayer.PlayerService.intentPrev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PlayerServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
//        Log.d("[linearity]","received");
        if (action == null){return;}
        if (intentOrder != null){
            if (action.equals(intentNext.getAction())){
                instance.NextOnClick();
            }else if (action.equals(intentPrev.getAction())){
                instance.PrevOnClick();
            }else if(action.equals(intentPause.getAction())){
                instance.PauseOnClick();
            }else if(action.equals(intentOrder.getAction())){
                instance.ChangeOrderOnClick();
            }
        }
        if (intentCloseNotification != null && action.equals(intentCloseNotification.getAction())){
            instance.switchNotificationState();
        }
    }
}
