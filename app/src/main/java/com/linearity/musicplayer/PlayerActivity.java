package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.PlayerActivityFolder;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolderAbsPath;
import static com.linearity.musicplayer.MainActivity.PlayerActivityTimer;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isPreparing;
import static com.linearity.musicplayer.MainActivity.isProgressBarChanging;
import static com.linearity.musicplayer.MainActivity.mediaPlayer;
import static com.linearity.musicplayer.PlayerService.getTimeStringFromMills;
import static com.linearity.musicplayer.PlayerService.pathToListen2;
import static com.linearity.musicplayer.PlayerService.songIndexes;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//read songs in the folder
//read "xxxxx.musiclist"which stores absolute patches of songs
public class PlayerActivity extends Activity {


    TimerTask progressBarTask;
    TextView progress_played;
    TextView progress_total;
    SeekBar progressBar;
    TextView authorTextView;
    TextView titleTextView;
    TextView drag2TimeTextView;

    ImageView pause_continue;
    ImageView changeOrder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (instance != null){
            if (instance.playerActivityInstance != null){
                instance.playerActivityInstance.finish();
            }
            instance.playerActivityInstance = this;
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.songlist_activity);
        if (PlayerActivityFolder != null){
            TextView textView = findViewById(R.id.playerTitle);
            textView.setText(PlayerActivityFolder);
        }
        File file = new File(PlayerActivityFolderAbsPath);

        if (file.exists()){//I need to check it.
            File[] files = file.listFiles();
            if (files != null){

                File strArrFile = new File(file,"musiclist.pathArr");
                Kryo strArrReader = new Kryo();
                strArrReader.register(String.class);
                strArrReader.register(String[].class);

                if (!strArrFile.exists()){
                    List<String> Songlist = new ArrayList<>();
                    for (File f : files) {
                        executeFile(f,Songlist);
                    }
                    if (!Songlist.isEmpty()){
                        pathToListen2 = Songlist.toArray(new String[0]);
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(strArrFile);
                            Output output = new Output(fileOutputStream);
                            strArrReader.writeObject(output,pathToListen2);
                            output.close();
                            fileOutputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(strArrFile);
                        Input input = new Input(fileInputStream);
                        pathToListen2 = strArrReader.readObject(input, String[].class);
                        input.close();
                        fileInputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Kryo linkedListReader;
                linkedListReader = new Kryo();
                linkedListReader.register(LinkedList.class);
                linkedListReader.register(Integer.class);

                File linkedListFile = new File(getApplication().getDataDir(),pathToListen2.length + ".linkedlist");
                if (!linkedListFile.exists()) {
                    songIndexes = new LinkedList<>();
                    for (int i = 0; i < pathToListen2.length; i++) {
                        songIndexes.add(i);
                    }
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(linkedListFile);
                        Output output = new Output(fileOutputStream);
                        linkedListReader.writeObject(output,songIndexes);
                        output.close();
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(linkedListFile);
                        Input input = new Input(fileInputStream);
                        songIndexes = (LinkedList<Integer>) linkedListReader.readObject(input, LinkedList.class);
                        input.close();
                        fileInputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                RecyclerView recyclerView = findViewById(R.id.playSongs);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                playlistAdapter PlaylistAdapter = new playlistAdapter(pathToListen2);
                recyclerView.setAdapter(PlaylistAdapter);

            }
        }
        titleTextView = findViewById(R.id.song_title);
        authorTextView = findViewById(R.id.song_author);
        drag2TimeTextView = findViewById(R.id.drag2time);

        progress_played = findViewById(R.id.progress_played);
        progress_total = findViewById(R.id.progress_total);

        progressBar = findViewById(R.id.player_progressbar);

        changeOrder = findViewById(R.id.player_order);
        ImageView player_prev = findViewById(R.id.player_prev);
        ImageView player_next = findViewById(R.id.player_next);
        pause_continue = findViewById(R.id.player_pause);
        instance.UpdatePauseStatus();
        instance.UpdateOrderStatus();

        titleTextView.setOnClickListener(
                v -> {
                    if (instance != null){
                        instance.switchNotificationState();
                    }
                }
        );
        changeOrder.setOnClickListener(v -> instance.ChangeOrderOnClick());
        pause_continue.setOnClickListener(v -> instance.PauseOnClick());
        player_prev.setOnClickListener(v -> instance.PrevOnClick());
        player_next.setOnClickListener(v -> instance.NextOnClick());
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isProgressBarChanging){
                    drag2TimeTextView.setText(getTimeStringFromMills(progressBar.getProgress()));
                }
                if (!mediaPlayer.isPlaying() && !mediaPlayer.isLooping()){return;}
                progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
                progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isProgressBarChanging = true;
                drag2TimeTextView.setText(getTimeStringFromMills(progressBar.getProgress()));
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isProgressBarChanging = false;
                mediaPlayer.seekTo(progressBar.getProgress());
                progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
                drag2TimeTextView.setText("");
            }
        });
        if (PlayerActivityTimer != null){
            PlayerActivityTimer.cancel();
        }
        PlayerActivityTimer = new Timer();
        progressBarTask = new TimerTask() {
            @Override
            public void run() {
                if(!isProgressBarChanging && !isPreparing){
                    if (mediaPlayer == null){return;}
                    progressBar.setProgress(mediaPlayer.getCurrentPosition());
                }
            }
        };
        PlayerActivityTimer.schedule(progressBarTask,0,50);
        instance.UpdatePlayerActivityInstance();
//        instance.UpdatePauseStatus();

    }
    private void executeFile(File f,List<String> Songlist) {
        executeFile(f,Songlist,new ArrayList<>());
    }

    private void executeFile(File f,List<String> Songlist,List<String> Folders) {
        if (f.isDirectory()){
            if (Folders.contains(f.getAbsolutePath())){return;}
            Folders.add(f.getAbsolutePath());
            for (File f0:f.listFiles()){
                executeFile(f0,Songlist,Folders);
            }
            return;
        }
        String fileAbs = f.getAbsolutePath();
        String end = fileAbs.toLowerCase();
        String[] arr = end.split("\\.");
        end = arr[arr.length-1];
        if (end.equals("mp3")
                || end.equals("wav")) {//I don't want to check it.
            Songlist.add(fileAbs);
        }
        else if(end.equals("musiclist") && f.canRead()){
            try {
                FileInputStream fileInputStream = new FileInputStream(f);
                byte[] fileBytes = new byte[(int) f.length()];
                fileInputStream.read(fileBytes);
                fileInputStream.close();
                String fileStr = new String(fileBytes, StandardCharsets.UTF_8);
                for (String str:fileStr.split("\n")){
//                                str = str.replace("\n","\\\n");
                    if (str.isEmpty()){continue;}
                    if (str.endsWith("\r")){
                        str = str.substring(0,str.length() - 1);
                    }
                    File file1 = new File(str);
                    if (file1.exists()) {
                        //and I'll always check it.
                        String str1 = str.toLowerCase();
                        if (str1.endsWith(".mp3") || str1.endsWith(".wav")){
                            Songlist.add(str);
                        }
                        else if (file1.isDirectory() || str1.endsWith(".musiclist")){
                            executeFile(file1,Songlist,Folders);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        Log.d("linearity", String.valueOf(Songlist.size()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance != null){
            instance.playerActivityInstance = null;
        }
    }
}
