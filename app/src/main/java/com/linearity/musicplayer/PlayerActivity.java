package com.linearity.musicplayer;

import static com.linearity.musicplayer.Consts.LoggerTag;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolder;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolderAbsPath;
import static com.linearity.musicplayer.MainActivity.PlayerActivityTimer;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isPreparing;
import static com.linearity.musicplayer.MainActivity.isProgressBarChanging;
import static com.linearity.musicplayer.MainActivity.mediaPlayer;
import static com.linearity.musicplayer.MainActivity.playingSongPath;
import static com.linearity.musicplayer.MainActivity.remoteFilesFlag;
import static com.linearity.musicplayer.MainActivity.serverURL;
import static com.linearity.musicplayer.PlayerService.getTimeStringFromMills;
import static com.linearity.musicplayer.PlayerService.pathToListen2;
import static com.linearity.musicplayer.PlayerService.songIndexes;

import android.app.Activity;
import android.app.Application;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

    /*result will be at songIndexes*/
    public static void initSongIndexes(Application application,String[] songPaths /*i mean pathToListen2*/){
        Kryo kryoInstance;
        kryoInstance = new Kryo();
        kryoInstance.register(int.class);
        kryoInstance.register(int[].class);

        File songIndexesFile;
        songIndexesFile = new File(application.getCacheDir(),songPaths.length + ".songIndexes");
        if (!songIndexesFile.exists()) {
            songIndexes = new int[songPaths.length];
            for (int i = 0; i < songPaths.length; i++) {
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
    }


    public static String readStringFromInputStream(InputStream in) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader
                (in, StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        return textBuilder.toString();
    }
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
                    Collection<String> songCollection = new ArrayList<>();
                    for (File f : files) {
                        executeFile(f,songCollection);
                    }
                    pathToListen2 = songCollection.toArray(new String[0]);
                    if (!songCollection.isEmpty()){
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

                initSongIndexes(getApplication(),pathToListen2);
                RecyclerView recyclerView = findViewById(R.id.playSongs);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                playlistAdapter PlaylistAdapter = new playlistAdapter(pathToListen2);
                recyclerView.setAdapter(PlaylistAdapter);

            }
        }
        else if (PlayerActivityFolderAbsPath != null){
            if (PlayerActivityFolderAbsPath.startsWith("http://") || PlayerActivityFolderAbsPath.startsWith("https://")){
                serverURL = PlayerActivityFolderAbsPath;
                remoteFilesFlag = true;
                new Thread(()->{
                    try {
                        URL url = new URL(PlayerActivityFolderAbsPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");

                        if (conn.getResponseCode() == 200){

                            Log.d(LoggerTag, String.valueOf(url));
                            Log.d(LoggerTag,conn.getResponseMessage());

                            String outText = readStringFromInputStream(conn.getInputStream());
                            String[] paths = outText.replace("\r\n","\n").split("\n");
                            Log.d(LoggerTag, Arrays.toString(paths));
                            Set<String> pathSet = new HashSet<>();
                            //no recursive here
                            for (String folderPath:paths){

                                url = new URL(PlayerActivityFolderAbsPath);
                                String urlStr = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/" + URLEncoder.encode(folderPath,"utf-8");
                                Log.d(LoggerTag,"accessing "+ urlStr);
                                url = new URL(urlStr);
                                conn.disconnect();
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestProperty("content-type", "text/plain; charset=utf-8");
                                conn.setRequestMethod("GET");
                                conn.connect();

                                outText = readStringFromInputStream(conn.getInputStream());
                                String[] musicPaths = outText.replace("\r\n","\n").split("\n");

                                Log.d(LoggerTag, Arrays.toString(musicPaths));
                                for (String s:musicPaths){
                                    boolean supportedFormatFlag = false;
                                    for (String formatEnding:SUPPORTED_FORMATS){
                                        if (s.endsWith(formatEnding)){
                                            supportedFormatFlag = true;
                                            break;
                                        }
                                    }
                                    if (!supportedFormatFlag){
                                        continue;
                                    }
                                    String realPath = folderPath + (folderPath.endsWith("/")?"":"/")+s;
                                    pathSet.add(url.getProtocol() + "://"
                                            + url.getHost() + ":" + url.getPort() + "/"
                                            + URLEncoder.encode(realPath,"utf-8").replace("+","%20"));
                                }
                            }
                            conn.disconnect();
                            pathToListen2 = pathSet.toArray(new String[0]);
                            Log.d(LoggerTag, Arrays.toString(pathToListen2));
                            runOnUiThread(()->{
                                initSongIndexes(getApplication(),pathToListen2);
                                RecyclerView recyclerView = findViewById(R.id.playSongs);
                                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                                recyclerView.setLayoutManager(linearLayoutManager);
                                recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
                                recyclerView.setItemAnimator(new DefaultItemAnimator());
                                playlistAdapter PlaylistAdapter = new playlistAdapter(pathToListen2);
                                recyclerView.setAdapter(PlaylistAdapter);
                            });
                        } else {
                            runOnUiThread(() ->Toast.makeText(PlayerActivity.this, R.string.path_not_found, Toast.LENGTH_SHORT).show());
                        }
                    }catch (Exception e){
//                            notFoundRunnable.run();
                        e.printStackTrace();
                    }
                }).start();
            }
            else {
                serverURL = "";
                remoteFilesFlag = false;
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
    private void executeFile(File f,Collection<String> songSet) {
        executeFile(f,songSet,new ArrayList<>());
    }

    private static final String[] SUPPORTED_FORMATS = {
            ".wav",
            ".mp3",
            ".flac",
            ".ogg",
    };
    private void executeFile(@NonNull File f, Collection<String> songSet, List<String> folders) {
        if (f.isDirectory()){
            if (folders.contains(f.getAbsolutePath())){
                return;
            }
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
        for (String fileFormat:SUPPORTED_FORMATS){
            if (fileAbs.endsWith(fileFormat)){
                songSet.add(fileAbs);
                break;
            }
        }
        if(fileAbs.toLowerCase().endsWith(".musiclist") && f.canRead()){
            try {
                FileInputStream fileInputStream = new FileInputStream(f);
                byte[] fileBytes = new byte[(int) f.length()];
                fileInputStream.read(fileBytes);
                fileInputStream.close();
                String fileStr = new String(fileBytes, StandardCharsets.UTF_8);
                fileStr = fileStr.replace("\r\n","\n");
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
//        Log.d(LoggerTag,"UpdatePlayerActivityInstance:Called");

        this.progress_total.setText(getTimeStringFromMills(mediaPlayer.getDuration()));
        this.progress_played.setText(getTimeStringFromMills(mediaPlayer.getCurrentPosition()));
        this.progressBar.setMax(mediaPlayer.getDuration());
        try {
            if (remoteFilesFlag){
                title = URLDecoder.decode(title,"utf-8");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        this.titleTextView.setText(title);
        //maybe more than mp3 can set image for itself.
        try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()){

            if (playingSongPath.startsWith("http://") || playingSongPath.startsWith("https://")){
                mmr.setDataSource(playingSongPath,new HashMap<>());
            }
            else{
                mmr.setDataSource(playingSongPath);
            }
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
