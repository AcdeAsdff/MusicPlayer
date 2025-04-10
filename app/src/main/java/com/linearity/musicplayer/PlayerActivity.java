package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.PlayerActivityFolder;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolderAbsPath;
import static com.linearity.musicplayer.MainActivity.PlayerActivityTimer;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isPreparing;
import static com.linearity.musicplayer.MainActivity.isProgressBarChanging;
import static com.linearity.musicplayer.MainActivity.mediaPlayer;
import static com.linearity.musicplayer.MainActivity.playingSongPath;
import static com.linearity.musicplayer.PlayerService.getTimeStringFromMills;
import static com.linearity.musicplayer.PlayerService.pathToListen2;
import static com.linearity.musicplayer.PlayerService.songIndexes;

import android.app.Activity;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
//import com.netease.cloudmusic.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                    Set<String> songSet = new HashSet<>();
                    for (File f : files) {
                        executeFile(f,songSet);
                    }
                    pathToListen2 = songSet.toArray(new String[0]);
                    if (!songSet.isEmpty()){
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

                Kryo kryoInstance;
                kryoInstance = new Kryo();
                kryoInstance.register(int.class);
                kryoInstance.register(int[].class);

                File songIndexesFile = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    songIndexesFile = new File(getApplication().getDataDir(),pathToListen2.length + ".songIndexes");
                }else {
                    songIndexesFile = new File(getApplication().getCacheDir(),pathToListen2.length + ".songIndexes");
                }
                if (!songIndexesFile.exists()) {
                    songIndexes = new int[pathToListen2.length];
                    for (int i = 0; i < pathToListen2.length; i++) {
                        songIndexes[i]=i;
                    }
                    try {
                        if (!songIndexesFile.createNewFile()){
                            throw new IOException("cannot create indexes file");
                        }

                        FileOutputStream fileOutputStream = new FileOutputStream(songIndexesFile);
                        Output output = new Output(fileOutputStream);
                        kryoInstance.writeObject(output,songIndexes);
                        output.close();
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(songIndexesFile);
                        Input input = new Input(fileInputStream);
                        songIndexes = kryoInstance.readObject(input, int[].class);
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
        instance.updatePauseStatus();
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

        if (instance != null){
            instance.playerActivityInstance = this;
            updateSelf();
        }
//        instance.UpdatePauseStatus();

    }
    private void executeFile(File f,Set<String> songSet) {
        executeFile(f,songSet,new ArrayList<>());
    }

    private static final String[] SUPPORTED_FORMATS = {
            ".wav",
            ".mp3"
    };
    private void executeFile(@NonNull File f, Set<String> songSet, List<String> folders) {
        if (f.isDirectory()){
            if (folders.contains(f.getAbsolutePath())){return;}
            folders.add(f.getAbsolutePath());
            File[] subFiles = f.listFiles();
            if (subFiles != null){
                for (File f0:subFiles){
                    executeFile(f0,songSet,folders);
                }
            }
            return;
        }
        String fileAbs = f.getAbsolutePath();
//        String end = fileAbs.toLowerCase();
//        String[] arr = end.split("\\.");
//        end = arr[arr.length-1];
        for (String fileFormat:SUPPORTED_FORMATS){
            if (fileAbs.endsWith(fileFormat)){
                songSet.add(fileAbs);
                break;
            }
        }
//        if (end.equals("mp3")
//                || end.equals("wav")) {//I don't want to check it.
//            songSet.add(fileAbs);
//        }
        if(fileAbs.endsWith(".musiclist") && f.canRead()){
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
                        for (String fileFormat:SUPPORTED_FORMATS){
                            if (str1.endsWith(fileFormat)){
                                songSet.add(str);
                                break;
                            }
                        }
                        if (file1.isDirectory() || str1.endsWith(".musiclist")){
                            executeFile(file1,songSet,folders);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        Log.d("linearity", String.valueOf(songSet.size()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance != null){
            instance.playerActivityInstance = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (instance != null){
            instance.playerActivityInstance = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (instance != null){
            instance.playerActivityInstance = this;
            updateSelf();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (instance != null){
            instance.playerActivityInstance = this;
            updateSelf();
        }
    }

    public void updateSelf() {
        String title = playingSongPath.split("/")[playingSongPath.split("/").length - 1];
//        Log.d("[linearity]","UpdatePlayerActivityInstance:Called");

        this.progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
        this.progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
        this.progressBar.setMax(mediaPlayer.getDuration());
        this.titleTextView.setText(title);
        if (playingSongPath.endsWith(".mp3")) {
            try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()){
                mmr.setDataSource(playingSongPath);
                String author = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (author != null) {
                    this.authorTextView.setText(author);
                    if (instance != null){
                        if (instance.useNotificationPlayer){
                            instance.notificationLayout.setTextViewText(R.id.notification_song_author, author);
                        }
                    }
                } else {
                    this.authorTextView.setText("");
                    if (instance != null){
                        if (instance.useNotificationPlayer){
                            instance.notificationLayout.setTextViewText(R.id.notification_song_author, "");
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
