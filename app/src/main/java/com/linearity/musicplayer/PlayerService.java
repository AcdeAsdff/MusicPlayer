package com.linearity.musicplayer;

import static android.app.Notification.FLAG_NO_CLEAR;
import static com.linearity.musicplayer.MainActivity.*;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Random;

public class PlayerService extends Service {
    Random random = new Random();
    public Notification NotificationPlayer;
    public int playOrder;
    public static Intent intentPrev;
    public static PendingIntent pendingIntentPrev;
    public static Intent intentNext;
    public static PendingIntent pendingIntentNext;
    public static Intent intentPause;
    public static PendingIntent pendingIntentPause;
    public static Intent intentOrder;
    public static PendingIntent pendingIntentOrder;
    public RemoteViews notificationLayout;
    public RemoteViews notificationLayoutSmall;
    public NotificationManager nManager;
    public Intent PlayerServiceBroadcastReceiverIntent;
    public PendingIntent PlayerServiceBroadcastReceiverPending;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        playOrder = 0;
        playSong = 0;
        mediaPlayer = new MediaPlayer();
        intentPrev = new Intent("linearity_musicplayer_prev");
        pendingIntentPrev = PendingIntent.getBroadcast(this, 0, intentPrev, PendingIntent.FLAG_IMMUTABLE);
        intentNext = new Intent("linearity_musicplayer_next");
        pendingIntentNext = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_IMMUTABLE);
        intentPause = new Intent("linearity_musicplayer_pause");
        pendingIntentPause = PendingIntent.getBroadcast(this, 0, intentPause, PendingIntent.FLAG_IMMUTABLE);
        intentOrder = new Intent("linearity_musicplayer_order");
        pendingIntentOrder = PendingIntent.getBroadcast(this, 0, intentOrder, PendingIntent.FLAG_IMMUTABLE);

        notificationLayout = new RemoteViews(getPackageName(), R.layout.player_notification);
        notificationLayout.setOnClickPendingIntent(R.id.notification_player_prev, pendingIntentPrev);
        notificationLayout.setOnClickPendingIntent(R.id.notification_player_next, pendingIntentNext);
        notificationLayout.setOnClickPendingIntent(R.id.notification_player_pause, pendingIntentPause);
        notificationLayout.setOnClickPendingIntent(R.id.notification_player_order, pendingIntentOrder);

        notificationLayoutSmall = new RemoteViews(getPackageName(), R.layout.player_notification_small);
        notificationLayoutSmall.setOnClickPendingIntent(R.id.small_notification_player_prev, pendingIntentPrev);
        notificationLayoutSmall.setOnClickPendingIntent(R.id.small_notification_player_next, pendingIntentNext);
        notificationLayoutSmall.setOnClickPendingIntent(R.id.small_notification_player_pause, pendingIntentPause);
        notificationLayoutSmall.setOnClickPendingIntent(R.id.small_notification_player_order, pendingIntentOrder);

        PlayerServiceBroadcastReceiverIntent = new Intent(this,PlayerServiceBroadcastReceiver.class);
        PlayerServiceBroadcastReceiverPending = PendingIntent.getActivity(this, R.string.app_name, PlayerServiceBroadcastReceiverIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationChannel nChannel = new NotificationChannel(playerChannelId, playerChannelId, NotificationManager.IMPORTANCE_LOW);
        nChannel.setDescription(playerChannelId);
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(intentPause.getAction());
        iFilter.addAction(intentPrev.getAction());
        iFilter.addAction(intentNext.getAction());
        iFilter.addAction(intentOrder.getAction());
//        nManager.createNotificationChannel(nChannel);
        nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        registerReceiver(new PlayerServiceBroadcastReceiver(), iFilter);
        NotificationPlayer = new NotificationCompat.Builder(this, playerChannelId)
                .setContentIntent(PlayerServiceBroadcastReceiverPending)
                .setCustomContentView(notificationLayoutSmall)
                .setCustomBigContentView(notificationLayout)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false)
                .build();
        NotificationPlayer.flags = FLAG_NO_CLEAR;
        nManager.notify(R.string.app_name,NotificationPlayer);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
    }


    public void prev_next(int prevOrNext){
        playSong += prevOrNext;
        if (playSong < 0){
            playSong += pathToListen2.length;
        }
        playSong %= pathToListen2.length;
        Play(pathToListen2[playSong]);
    }
    public void Pause(){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }else if (mediaPlayer.getDuration() != -1){
            mediaPlayer.start();
        }
        UpdatePauseStatus();
    }

    public void UpdateNotificationPlayer(){
        NotificationPlayer = new NotificationCompat.Builder(this, playerChannelId)
                .setContentIntent(PlayerServiceBroadcastReceiverPending)
                .setCustomContentView(notificationLayoutSmall)
                .setCustomBigContentView(notificationLayout)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false)
                .build();
        NotificationPlayer.flags = FLAG_NO_CLEAR;
        nManager.notify(R.string.app_name,NotificationPlayer);
    }
    public void UpdatePauseStatus(){
        if (mediaPlayer.isPlaying()){
            if (playerActivityInstance != null){playerActivityInstance.pause_continue.setImageResource(R.drawable._o);}
            notificationLayout.setImageViewResource(R.id.notification_player_pause, R.drawable._o);
            notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_pause, R.drawable._o);
        }else {
            if (playerActivityInstance != null){playerActivityInstance.pause_continue.setImageResource(R.drawable.dv);}
            notificationLayout.setImageViewResource(R.id.notification_player_pause, R.drawable.dv);
            notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_pause, R.drawable.dv);
        }
        UpdateNotificationPlayer();
    }

    public void UpdateOrderStatus(){
        switch (playOrder){
            case 0:{
                if (playerActivityInstance != null){playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_0_7e0508f);}
                notificationLayout.setImageViewResource(R.id.notification_player_order, R.drawable.mode_0_7e0508f);
                notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_order, R.drawable.mode_0_7e0508f);
                mediaPlayer.setLooping(false);
                break;
            }
            case 1:{
                if (playerActivityInstance != null){playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_1_3b4f2c2);}
                notificationLayout.setImageViewResource(R.id.notification_player_order, R.drawable.mode_1_3b4f2c2);
                notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_order, R.drawable.mode_1_3b4f2c2);
                mediaPlayer.setLooping(true);
                break;
            }
            case 2:{
                if (playerActivityInstance != null){playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_2_5fb7b02);}
                notificationLayout.setImageViewResource(R.id.notification_player_order, R.drawable.mode_2_5fb7b02);
                notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_order, R.drawable.mode_2_5fb7b02);
                mediaPlayer.setLooping(false);
                break;
            }
        }
        UpdateNotificationPlayer();
    }

    public void Play(String path){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        isPreparing = true;
        if (playerActivityInstance != null){
            playerActivityInstance.titleTextView.setText("");
            playerActivityInstance.authorTextView.setText("");
        }
        notificationLayout.setTextViewText(R.id.notification_song_author, "");
        notificationLayout.setTextViewText(R.id.notification_song_title, "");
        notificationLayout.setImageViewBitmap(R.id.notification_song_image, null);
        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(path);
            playingSongPath = path;
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isPrevNextClicked = false;
                    isSongItemClicked = false;
                    mediaPlayer.start();
                    isPreparing = false;
                    UpdatePlayerActivityInstance();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (pathToListen2.length > 0){
                        switch (playOrder){
                            case 0:{
                                if (isPrevNextClicked || isSongItemClicked){
                                    isPrevNextClicked = false;
                                    isSongItemClicked = false;
                                    break;
                                }
                                prev_next(1);
                                break;
                            }
                            case 1:{
                                if (isPrevNextClicked || isSongItemClicked){
                                    isPrevNextClicked = false;
                                    isSongItemClicked = false;
                                    break;
                                }
                                if (!mediaPlayer.isLooping()){
                                    Play(playingSongPath);
                                    mediaPlayer.setLooping(true);
                                }
                                break;
                            }
                            case 2:{
                                if (isPrevNextClicked || isSongItemClicked){
                                    isPrevNextClicked = false;
                                    isSongItemClicked = false;
                                    break;
                                }
                                prev_next(random.nextInt(pathToListen2.length));
                                break;
                            }
                        }
                    }
                }
            });
//            progress_total.setText(mediaPlayer.getDuration());
            UpdatePauseStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void UpdatePlayerActivityInstance() {
        String title = playingSongPath.split("/")[playingSongPath.split("/").length - 1];
        if (playerActivityInstance != null){
            playerActivityInstance.progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
            playerActivityInstance.progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
            playerActivityInstance.progressBar.setMax(mediaPlayer.getDuration());
            playerActivityInstance.titleTextView.setText(title);
        }
        notificationLayout.setTextViewText(R.id.notification_song_title, title);
        notificationLayout.setImageViewBitmap(R.id.notification_song_image, null);
        if (playingSongPath.endsWith(".mp3")) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(playingSongPath);
            String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (author != null) {
                if (playerActivityInstance != null){
                    playerActivityInstance.authorTextView.setText(author);
                }
                notificationLayout.setTextViewText(R.id.notification_song_author, author);
            } else {
                if (playerActivityInstance != null){
                    playerActivityInstance.authorTextView.setText("");
                }
                notificationLayout.setTextViewText(R.id.notification_song_author, "");
            }
            byte[] songImage = mmr.getEmbeddedPicture();
            if (songImage != null){
                notificationLayout.setImageViewBitmap(R.id.notification_song_image,BitmapFactory.decodeByteArray(songImage,0,songImage.length));
            }else {
                notificationLayout.setImageViewBitmap(R.id.notification_song_image,null);
            }
        }
        UpdatePauseStatus();
    }

    public static String getTimeStringFromMills(int mills){
        StringBuilder stringBuilder = new StringBuilder();
        int totalSeconds = mills / 1000;
        int totalMinutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours != 0){
            stringBuilder.append(hours);
            stringBuilder.append(":");
        }
        stringBuilder.append(minutes);
        stringBuilder.append(":");
        if (seconds < 10){
            stringBuilder.append(0);
        }
        stringBuilder.append(seconds);
        return stringBuilder.toString();
    }

    public void NextOnClick(){
        isPrevNextClicked = true;
        if (instance.playOrder == 2) {
            instance.prev_next(instance.random.nextInt(pathToListen2.length));
            return;
        }
        instance.prev_next(1);
    }

    public void PrevOnClick(){
        isPrevNextClicked = true;
        if (instance.playOrder == 2) {
            instance.prev_next(instance.random.nextInt(pathToListen2.length));
            return;
        }
        instance.prev_next(-1);
    }

    public void PauseOnClick(){
        Pause();
    }

    public void ChangeOrderOnClick(){
        instance.playOrder += 1;
        instance.playOrder %= 3;
        instance.UpdateOrderStatus();
    }
}
