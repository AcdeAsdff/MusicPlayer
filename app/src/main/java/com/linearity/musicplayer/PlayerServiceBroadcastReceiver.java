package com.linearity.musicplayer;

import static com.linearity.musicplayer.Consts.LoggerTag;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.PlayerService.intentCloseNotification;
import static com.linearity.musicplayer.PlayerService.intentNext;
import static com.linearity.musicplayer.PlayerService.intentOrder;
import static com.linearity.musicplayer.PlayerService.intentPause;
import static com.linearity.musicplayer.PlayerService.intentPrev;
import static com.linearity.musicplayer.PlayerService.pathToListen2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Stack;

public class PlayerServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null){return;}
        if (intentOrder != null){
            if (action.equals(intentNext.getAction())){
                if (pathToListen2 == null || pathToListen2.length == 0){
                    return;
                }
                instance.NextOnClick();
            }else if (action.equals(intentPrev.getAction())){
                if (pathToListen2 == null || pathToListen2.length == 0){
                    return;
                }
                instance.PrevOnClick();
            }else if(action.equals(intentPause.getAction())){
                if (pathToListen2 == null || pathToListen2.length == 0){
                    return;
                }
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
