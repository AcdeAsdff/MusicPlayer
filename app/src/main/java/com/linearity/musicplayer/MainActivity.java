package com.linearity.musicplayer;

import static com.linearity.musicplayer.PlayerActivity.playerActivityInstance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    public static String playingSongPath = "";
    public static boolean isPrevNextClicked = false;
    public static SharedPreferences sharedPreferences_PathData;
    public static SharedPreferences.Editor sharedPreferencesEditor_PathData;
    public static List<String> folderList = new ArrayList<>();
    public static String PlayerActivityFolder;
    public static String PlayerActivityFolderAbsPath;
    public static MainActivity instance;

    public static MediaPlayer mediaPlayer;
    public int playOrder;
    public static int playSong;
    Random random = new Random();
    public static String[] pathToListen2;
    public static Boolean isProgressBarChanging = false;
    public static boolean isPreparing = true;
    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        playOrder = 0;
        playSong = 0;
        instance = this;
        mediaPlayer = new MediaPlayer();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        sharedPreferences_PathData = getSharedPreferences("PlayerPathData", MODE_PRIVATE);
        folderList.addAll(sharedPreferences_PathData.getAll().keySet());
//        sharedPreferencesEditor_PathData = sharedPreferences_PathData.edit(); do it when U want to use

        RecyclerView recyclerView = findViewById(R.id.playFolders);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(instance);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(instance,DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator( new DefaultItemAnimator());
        playerFolderAdapter playerFolderAdapter = new playerFolderAdapter(folderList);
        recyclerView.setAdapter(playerFolderAdapter);

        Button addFolder = findViewById(R.id.addFolder);
        addFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View addFolderView = View.inflate(instance,R.layout.addfolder_edittext,null);
                AlertDialog alertDialog = new AlertDialog.Builder(instance)
                        .setView(addFolderView).show();

                EditText folderLocation = (EditText) addFolderView.findViewById(R.id.et_name);
                Button cancel = (Button) addFolderView.findViewById(R.id.addfolder_edittext_btn_cancel);
                Button confirm = (Button) addFolderView.findViewById(R.id.addfolder_edittext_btn_confirm);
                folderLocation.setText("/storage/emulated/0/");
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.cancel();
                    }
                });
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File file = new File(String.valueOf(folderLocation.getText()));

                        if (file.exists()){
                            Log.d("[linearity]", "onClick: " + file.getAbsolutePath());
                            if (!sharedPreferences_PathData.contains(file.getAbsolutePath())){
                                sharedPreferencesEditor_PathData = sharedPreferences_PathData.edit();
                                sharedPreferencesEditor_PathData.putInt(file.getAbsolutePath(),1);
                                sharedPreferencesEditor_PathData.apply();
                                sharedPreferences_PathData = getSharedPreferences("PlayerPathData", MODE_PRIVATE);
                                folderList.clear();
                                folderList.addAll(sharedPreferences_PathData.getAll().keySet());
                                alertDialog.cancel();
                                playerFolderAdapter.notifyItemInserted(folderList.size() - 1);
                            }else {
                                Toast.makeText(instance, R.string.path_exists, Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            Toast.makeText(instance, R.string.path_not_found, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

        if (!isIgnoringBatteryOptimizations()){
            requestIgnoreBatteryOptimizations();
        }


        for (String str:new String[]{
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE

        })
        {
//            ActivityCompat.requestPermissions(this,new String[]{str}, 0);
//            int PermissionState = ContextCompat.checkSelfPermission(this, str);

//            Toast.makeText(this, "已授权！", Toast.LENGTH_LONG).show();

//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivity(intent);
//                ActivityCompat.shouldShowRequestPermissionRationale(this,str);
//                ActivityCompat.requestPermissions(this,new String[]{str}, 0);

        }
    }

    public void requestIgnoreBatteryOptimizations() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        return isIgnoring;
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

    public void UpdatePauseStatus(){
        if (mediaPlayer.isPlaying()){
            playerActivityInstance.pause_continue.setImageResource(R.drawable._o);
        }else {
            playerActivityInstance.pause_continue.setImageResource(R.drawable.dv);
        }
    }

    public void UpdateOrderStatus(){
        switch (playOrder){
            case 0:{
                playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_0_7e0508f);
                mediaPlayer.setLooping(false);
                break;
            }
            case 1:{
                playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_1_3b4f2c2);
                mediaPlayer.setLooping(true);
                break;
            }
            case 2:{
                playerActivityInstance.changeOrder.setImageResource(R.drawable.mode_2_5fb7b02);
                mediaPlayer.setLooping(false);
                break;
            }
        }
    }

    public void Play(String path){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        isPreparing = true;
        playerActivityInstance.titleTextView.setText("");
        playerActivityInstance.authorTextView.setText("");
        mediaPlayer.release();
        mediaPlayer = null;
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(path);
            playingSongPath = path;
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    isPreparing = false;
                    playerActivityInstance.progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
                    playerActivityInstance.progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
                    playerActivityInstance.progressBar.setMax(mediaPlayer.getDuration());
                    UpdatePauseStatus();
                    playerActivityInstance.titleTextView.setText(path.split("/")[path.split("/").length - 1]);
                    if (path.endsWith(".mp3")){
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(path);
                        String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        if (author != null) {
                            playerActivityInstance.authorTextView.setText(author);
                        } else {
                            playerActivityInstance.authorTextView.setText("");
                        }
                    }
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (pathToListen2.length > 0){
                        switch (playOrder){
                            case 0:{
                                if (isPrevNextClicked){
                                    isPrevNextClicked = false;
                                    break;
                                }
                                prev_next(1);
                                break;
                            }
                            case 1:{
                                if (isPrevNextClicked){
                                    isPrevNextClicked = false;
                                    break;
                                }
                                if (!mediaPlayer.isLooping()){
                                    Play(playingSongPath);
                                    mediaPlayer.setLooping(true);
                                }
                                break;
                            }
                            case 2:{
                                if (isPrevNextClicked){
                                    isPrevNextClicked = false;
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
}
