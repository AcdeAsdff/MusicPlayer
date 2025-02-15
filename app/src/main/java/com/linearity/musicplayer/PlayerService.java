package com.linearity.musicplayer;

import static android.app.Notification.FLAG_NO_CLEAR;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isPreparing;
import static com.linearity.musicplayer.MainActivity.isPrevNextClicked;
import static com.linearity.musicplayer.MainActivity.isSongItemClicked;
import static com.linearity.musicplayer.MainActivity.mediaPlayer;
import static com.linearity.musicplayer.MainActivity.playSong;
import static com.linearity.musicplayer.MainActivity.playerChannelId;
import static com.linearity.musicplayer.MainActivity.playingSongPath;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.Random;

public class PlayerService extends Service {
    public PlayerActivity playerActivityInstance;
    public static int[] songIndexes;
    public static String[] pathToListen2;
    public static LinkedList<Integer> randomPlayedSongs = new LinkedList<>();
    Random random = new Random();
    public Notification NotificationPlayer;
    public void initNotificationPlayer(){
        NotificationPlayer = new NotificationCompat.Builder(this, playerChannelId)
                .setContentIntent(PlayerServiceBroadcastReceiverPending)
                .setCustomContentView(notificationLayoutSmall)
                .setCustomBigContentView(notificationLayout)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setShowWhen(false)
                .build();
        NotificationPlayer.flags = FLAG_NO_CLEAR;
    }
    public int playOrder;
    public static Intent intentPrev = new Intent("linearity_musicplayer_prev");
    public static PendingIntent pendingIntentPrev;
    public static Intent intentNext = new Intent("linearity_musicplayer_next");
    public static PendingIntent pendingIntentNext;
    public static Intent intentPause = new Intent("linearity_musicplayer_pause");
    public static PendingIntent pendingIntentPause;
    public static Intent intentOrder = new Intent("linearity_musicplayer_order");
    public static PendingIntent pendingIntentOrder;
    public static Intent intentCloseNotification = new Intent("linearity_musicplayer_close_notification");
    public static PendingIntent pendingIntentCloseNotification;
    public RemoteViews notificationLayout;
    public RemoteViews notificationLayoutSmall;
    public NotificationManager nManager;
    public Intent PlayerServiceBroadcastReceiverIntent;
    public PendingIntent PlayerServiceBroadcastReceiverPending;
    public boolean useNotificationPlayer;
    public PlayerServiceBroadcastReceiver playerServiceBroadcastReceiver;
    public IntentFilter iFilter;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        useNotificationPlayer = true;
        instance = this;
        playOrder = 0;
        playSong = 0;
        mediaPlayer = new MediaPlayer();

        setupIntents();

        initNotificationLayouts();

        PlayerServiceBroadcastReceiverIntent = new Intent(this,PlayerActivity.class);
        PlayerServiceBroadcastReceiverPending =
                PendingIntent.getActivity(this,
                        R.string.app_name,
                        PlayerServiceBroadcastReceiverIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        NotificationChannel nChannel = new NotificationChannel(playerChannelId, playerChannelId, NotificationManager.IMPORTANCE_LOW);
        nChannel.setDescription(playerChannelId);

        initIntentFilter();

        nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nManager.createNotificationChannel(nChannel);
        if (playerServiceBroadcastReceiver == null){
            playerServiceRegisterReceiver();
        }else {
            unregisterReceiver(playerServiceBroadcastReceiver);
            playerServiceRegisterReceiver();
        }
        initNotificationPlayer();
        nManager.notify(R.string.app_name,NotificationPlayer);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (useNotificationPlayer){
            switchNotificationState();
        }
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
//        UpdatePauseStatus();
    }

    public void initNotificationLayouts(){
        if (!useNotificationPlayer){return;}
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
        notificationLayoutSmall.setOnClickPendingIntent(R.id.close_notification, pendingIntentCloseNotification);
    }
    public void UpdateNotificationPlayer(){
        if (useNotificationPlayer){initNotificationLayouts();}
        UpdatePauseStatus();
        UpdateOrderStatus();
//        nManager.cancelAll();
        if (!useNotificationPlayer){return;}
        String title = playingSongPath.split("/")[playingSongPath.split("/").length - 1];
        notificationLayout.setTextViewText(R.id.notification_song_title, title);
        notificationLayout.setTextViewText(R.id.notification_song_author, "");
        notificationLayout.setImageViewBitmap(R.id.notification_song_image, null);
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()){
            if (playingSongPath != null && new File(playingSongPath).exists()){
                mmr.setDataSource(playingSongPath);
            }
            byte[] songImage = mmr.getEmbeddedPicture();
            if (songImage != null) {
                notificationLayout.setImageViewBitmap(R.id.notification_song_image, BitmapFactory.decodeByteArray(songImage, 0, songImage.length));
            } else {
                notificationLayout.setImageViewBitmap(R.id.notification_song_image, null);
            }
            initNotificationPlayer();
            nManager.notify(R.string.app_name, NotificationPlayer);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public void UpdatePauseStatus(){
        if (mediaPlayer.isPlaying()){
            if (playerActivityInstance != null){playerActivityInstance.pause_continue.setImageResource(R.drawable._o);}
            if (!useNotificationPlayer){
                notificationLayout.setImageViewResource(R.id.notification_player_pause, R.drawable._o);
                notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_pause, R.drawable._o);
            }
        }else {
            if (playerActivityInstance != null){playerActivityInstance.pause_continue.setImageResource(R.drawable.dv);}
            if (useNotificationPlayer){
                notificationLayout.setImageViewResource(R.id.notification_player_pause, R.drawable.dv);
                notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_pause, R.drawable.dv);
            }
        }
    }

    public void UpdateOrderStatus(){
        switch (playOrder){
            case 0:{
                if (playerActivityInstance != null){playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_0_7e0508f);}
                mediaPlayer.setLooping(false);
                if (useNotificationPlayer){
                    notificationLayout.setImageViewResource(R.id.notification_player_order, R.drawable.mode_0_7e0508f);
                    notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_order, R.drawable.mode_0_7e0508f);
                }
                break;
            }
            case 1:{
                mediaPlayer.setLooping(true);
                if (playerActivityInstance != null){playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_1_3b4f2c2);}
                if (useNotificationPlayer){
                    notificationLayout.setImageViewResource(R.id.notification_player_order, R.drawable.mode_1_3b4f2c2);
                    notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_order, R.drawable.mode_1_3b4f2c2);
                }
                break;
            }
            case 2:{
                if (playerActivityInstance != null){playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_2_5fb7b02);}
                mediaPlayer.setLooping(false);
                if (useNotificationPlayer){
                    notificationLayout.setImageViewResource(R.id.notification_player_order, R.drawable.mode_2_5fb7b02);
                    notificationLayoutSmall.setImageViewResource(R.id.small_notification_player_order, R.drawable.mode_2_5fb7b02);
                }

                break;
            }
        }
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
        if (mediaPlayer != null){mediaPlayer.reset();}
        else {mediaPlayer = new MediaPlayer();}

        try {
            mediaPlayer.setDataSource(path);
            playingSongPath = path;
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrevNextClicked = false;
                isSongItemClicked = false;
                mediaPlayer.start();
                isPreparing = false;
                UpdatePlayerActivityInstance();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
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
//                                prev_next(random.nextInt(pathToListen2.length));
                            playRandom();
                            break;
                        }
                    }
                }
            });
//            progress_total.setText(mediaPlayer.getDuration());
            UpdatePauseStatus();
            isPrevNextClicked = false;
            isSongItemClicked = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void UpdatePlayerActivityInstance() {
        String title = playingSongPath.split("/")[playingSongPath.split("/").length - 1];
//        Log.d("[linearity]","UpdatePlayerActivityInstance:Called");
        if (this.playerActivityInstance != null){
//            Log.d("[linearity]","UpdatePlayerActivityInstance:playerActivityInstance non null");
            playerActivityInstance.progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
            playerActivityInstance.progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
            playerActivityInstance.progressBar.setMax(mediaPlayer.getDuration());
            playerActivityInstance.titleTextView.setText(title);
        }
        if (playingSongPath.endsWith(".mp3")) {
            try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()){
                mmr.setDataSource(playingSongPath);
                String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (author != null) {
                    if (playerActivityInstance != null){
                        playerActivityInstance.authorTextView.setText(author);
                    }
                    if (useNotificationPlayer){
                        notificationLayout.setTextViewText(R.id.notification_song_author, author);
                    }
                } else {
                    if (playerActivityInstance != null){
                        playerActivityInstance.authorTextView.setText("");
                    }
                    if (useNotificationPlayer){notificationLayout.setTextViewText(R.id.notification_song_author, "");}
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        UpdatePauseStatus();
        UpdateNotificationPlayer();
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

//        Log.e("[linearity]", "onCompletion: " + randomPlayedSongs.size());
        isPrevNextClicked = true;
        if (instance.playOrder == 2) {
//            instance.prev_next(instance.random.nextInt(pathToListen2.length));
            playRandom();
            return;
        }
        instance.prev_next(1);
        UpdateNotificationPlayer();
    }

    public void PrevOnClick(){
        isPrevNextClicked = true;
        if (instance.playOrder == 2) {
//            instance.prev_next(instance.random.nextInt(pathToListen2.length));
            playRandom();
            return;
        }
        instance.prev_next(-1);
        UpdateNotificationPlayer();
    }

    public void PauseOnClick(){
        Pause();
        UpdateNotificationPlayer();
    }

    public void ChangeOrderOnClick(){
        instance.playOrder += 1;
        instance.playOrder %= 3;
        instance.UpdateOrderStatus();
        UpdateNotificationPlayer();
        if (playOrder == 2){
            if (songIndexes == null){return;}
            refreshRandomPlayedSongs();
        }
    }
    public static void shuffleArray(int[] array){
        int n = array.length;
        Random random = new SecureRandom();
        // Loop over array.
        for (int i = 0; i < array.length; i++) {
            // Get a random index of the array past the current index.
            // ... The argument is an exclusive bound.
            //     It will not go past the array send.
            int randomValue = i + random.nextInt(n - i);
            // Swap the random element with the present element.
            int randomElement = array[randomValue];
            array[randomValue] = array[i];
            array[i] = randomElement;
        }
    }
    public static void refreshRandomPlayedSongs(){
        int[] randomIndexes = songIndexes.clone();
        shuffleArray(randomIndexes);
        randomPlayedSongs = new LinkedList<>();
        for (int i:randomIndexes){
            randomPlayedSongs.push(i);
        }
    }
    public void playRandom(){
        if (randomPlayedSongs.isEmpty()){
            refreshRandomPlayedSongs();
        }
        Play(pathToListen2[randomPlayedSongs.poll()]);
    }

    public void switchNotificationState(){
        //close
        useNotificationPlayer = !useNotificationPlayer;
        if (!useNotificationPlayer){
            unregisterReceiver(playerServiceBroadcastReceiver);
            nManager.cancel(R.string.app_name);
        }else {
            initIntentFilter();
            initNotificationPlayer();
            nManager.notify(R.string.app_name,NotificationPlayer);
            if (Build.VERSION.SDK_INT >= 33){
                registerReceiver(playerServiceBroadcastReceiver, iFilter, RECEIVER_VISIBLE_TO_INSTANT_APPS | Context.RECEIVER_EXPORTED);
            } else{
                registerReceiver(playerServiceBroadcastReceiver, iFilter, RECEIVER_VISIBLE_TO_INSTANT_APPS);
            }
        }
    }
    public void initIntentFilter(){
        iFilter = new IntentFilter();
        iFilter.addAction(intentPause.getAction());
        iFilter.addAction(intentPrev.getAction());
        iFilter.addAction(intentNext.getAction());
        iFilter.addAction(intentOrder.getAction());
        iFilter.addAction(intentCloseNotification.getAction());
    }
    public void setupIntents(){
        pendingIntentPrev = PendingIntent.getBroadcast(this, 0, intentPrev, PendingIntent.FLAG_IMMUTABLE);

        pendingIntentNext = PendingIntent.getBroadcast(this, 0, intentNext, PendingIntent.FLAG_IMMUTABLE);

        pendingIntentPause = PendingIntent.getBroadcast(this, 0, intentPause, PendingIntent.FLAG_IMMUTABLE);

        pendingIntentOrder = PendingIntent.getBroadcast(this, 0, intentOrder, PendingIntent.FLAG_IMMUTABLE);

        pendingIntentCloseNotification = PendingIntent.getBroadcast(this, 0, intentCloseNotification, PendingIntent.FLAG_IMMUTABLE);
    }

    public void playerServiceRegisterReceiver(){
        playerServiceBroadcastReceiver = new PlayerServiceBroadcastReceiver();
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(playerServiceBroadcastReceiver, iFilter, RECEIVER_VISIBLE_TO_INSTANT_APPS | Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(playerServiceBroadcastReceiver, iFilter);
        }
    }
}
