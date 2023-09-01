package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.PlayerActivityFolder;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolderAbsPath;
import static com.linearity.musicplayer.MainActivity.getTimeStringFromMills;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isPreparing;
import static com.linearity.musicplayer.MainActivity.isPrevNextClicked;
import static com.linearity.musicplayer.MainActivity.isProgressBarChanging;
import static com.linearity.musicplayer.MainActivity.mediaPlayer;
import static com.linearity.musicplayer.MainActivity.pathToListen2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

//read songs in the folder
//read "xxxxx.musiclist"which stores absolute patches of songs
public class PlayerActivity extends Activity {

    public static PlayerActivity playerActivityInstance;

    TextView progress_played;
    TextView progress_total;
    SeekBar progressBar;

    TextView authorTextView;
    TextView titleTextView;

    ImageView pause_continue;
    ImageView changeOrder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        playerActivityInstance = this;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.songlist_activity);
        if (PlayerActivityFolder != null){
            TextView textView = findViewById(R.id.playerTitle);
            textView.setText(PlayerActivityFolder);
        }
        File file = new File(PlayerActivityFolderAbsPath);

        if (file.exists()){//I need to check it.
//            ArrayList<File> fileList = new ArrayList<>();
            File[] files = file.listFiles();
            List<String> Songlist = new ArrayList<>();
            if (files != null){
                for (File f : files) {
                    String fileAbs = f.getAbsolutePath();
                    if (fileAbs.endsWith(".mp3") || fileAbs.endsWith(".wav")) {//I don't need to check it.
//                        fileList.add(f);
                        Songlist.add(fileAbs);
                    }
                    else if(fileAbs.endsWith(".musicList") && f.canRead()){
                        try {
                            FileInputStream fileInputStream = new FileInputStream(f);
                            byte[] fileBytes = new byte[(int) f.length()];
                            fileInputStream.read(fileBytes);
                            fileInputStream.close();
                            String fileStr = new String(fileBytes, StandardCharsets.UTF_8);
                            for (String str:fileStr.split("\n")){
//                                str = str.replace("\n","\\\n");
                                if (!str.endsWith("v")
                                        && !str.endsWith("3")
                                        && !str.endsWith("t")){
                                    str = str.substring(0,str.length() - 1);
                                };
                                File file1 = new File(str);
                                if (file1.exists()) {//and I'll always check it.
                                    if (str.endsWith(".mp3") || str.endsWith(".wav")){
                                        Songlist.add(str);
                                    }
//                                    else if (file1.isDirectory()){
//
//                                    }//I am too lazy to face StackOverFlowException
                                }else {
                                    Log.d("[linearity]", "onCreate: " + str);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
//                File[] filesToListen2 = fileList.toArray(new File[0]);
                pathToListen2 = Songlist.toArray(new String[0]);
                RecyclerView recyclerView = findViewById(R.id.playSongs);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
                recyclerView.setItemAnimator( new DefaultItemAnimator());
                playlistAdapter PlaylistAdapter = new playlistAdapter(pathToListen2);
                recyclerView.setAdapter(PlaylistAdapter);

            }
        }
        titleTextView = findViewById(R.id.song_title);
        authorTextView = findViewById(R.id.song_author);

        progress_played = findViewById(R.id.progress_played);
        progress_total = findViewById(R.id.progress_total);

        progressBar = findViewById(R.id.player_progressbar);

        changeOrder = findViewById(R.id.player_order);
        ImageView player_prev = findViewById(R.id.player_prev);
        ImageView player_next = findViewById(R.id.player_next);
        pause_continue = findViewById(R.id.player_pause);
        instance.UpdatePauseStatus();
        instance.UpdateOrderStatus();

        changeOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instance.playOrder += 1;
                instance.playOrder %= 3;
                instance.UpdateOrderStatus();
            }
        });
        pause_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instance.Pause();
            }
        });
        player_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPrevNextClicked = true;
                if (instance.playOrder == 2) {
                    instance.prev_next(instance.random.nextInt(pathToListen2.length));
                    return;
                }
                instance.prev_next(-1);
            }
        });
        player_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPrevNextClicked = true;
                if (instance.playOrder == 2) {
                    instance.prev_next(instance.random.nextInt(pathToListen2.length));
                    return;
                }
                instance.prev_next(1);
            }
        });
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
                progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isProgressBarChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isProgressBarChanging = false;
                mediaPlayer.seekTo(progressBar.getProgress());
                progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
            }
        });
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isProgressBarChanging && !isPreparing){
                    progressBar.setProgress(mediaPlayer.getCurrentPosition());
                }
            }
        },0,50);
    }

}
