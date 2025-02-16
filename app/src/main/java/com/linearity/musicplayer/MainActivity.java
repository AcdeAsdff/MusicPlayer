package com.linearity.musicplayer;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Objects;
import java.util.Timer;

public class MainActivity extends Activity {
    private static final String[] REQUIRES_PERMISSIONS;
    static {

        if (30 > Build.VERSION.SDK_INT){
            REQUIRES_PERMISSIONS = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            };
        }
        else if (33 > Build.VERSION.SDK_INT){
            REQUIRES_PERMISSIONS = new String[]{
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
            };
        }else {
            REQUIRES_PERMISSIONS = new String[]{
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
    }
    public static String playerChannelId = "linearityNotificationPlayer";
    public static Timer PlayerActivityTimer;
    public static String playingSongPath = "";
    public static boolean isPrevNextClicked = false;
    public static boolean isSongItemClicked = false;
    public static SharedPreferences sharedPreferences_PathData;
    public static List<String> folderList = new ArrayList<>();
    public static String PlayerActivityFolder;
    public static String PlayerActivityFolderAbsPath;
    public static PlayerService instance;

    public static MediaPlayer mediaPlayer;
    public static int playSong;
    public static Boolean isProgressBarChanging = false;
    public static boolean isPreparing = true;
    public static MainActivity mainActivityInstance;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mainActivityInstance = this;
        startService(new Intent(this, PlayerService.class));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        sharedPreferences_PathData = getSharedPreferences("PlayerPathData", MODE_PRIVATE);
        folderList.addAll(sharedPreferences_PathData.getAll().keySet());
//        sharedPreferencesEditor_PathData = sharedPreferences_PathData.edit(); do it when U want to use

        RecyclerView recyclerView = findViewById(R.id.playFolders);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator( new DefaultItemAnimator());
        playerFolderAdapter playerFolderAdapter = new playerFolderAdapter(folderList);
        recyclerView.setAdapter(playerFolderAdapter);

        Button addFolder = findViewById(R.id.addFolder);
        addFolder.setOnClickListener(v -> {
            View addFolderView = View.inflate(MainActivity.this,R.layout.addfolder_edittext,null);
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setView(addFolderView).show();

            EditText folderLocation = addFolderView.findViewById(R.id.et_name);
            Button cancel = addFolderView.findViewById(R.id.addfolder_edittext_btn_cancel);
            Button confirm = addFolderView.findViewById(R.id.addfolder_edittext_btn_confirm);
            folderLocation.setText("/storage/emulated/0/");
            cancel.setOnClickListener(v12 -> alertDialog.cancel());
            confirm.setOnClickListener(v1 -> {
                File file = new File(String.valueOf(folderLocation.getText()));

                if (file.exists()){
//                            Log.d("[linearity]", "onClick: " + file.getAbsolutePath());
                    if (!sharedPreferences_PathData.contains(file.getAbsolutePath())){
                        SharedPreferences.Editor sharedPreferencesEditor_PathData = sharedPreferences_PathData.edit();
                        sharedPreferencesEditor_PathData.putInt(file.getAbsolutePath(),1);
                        sharedPreferencesEditor_PathData.apply();
                        sharedPreferences_PathData = getSharedPreferences("PlayerPathData", MODE_PRIVATE);
                        folderList.clear();
                        folderList.addAll(sharedPreferences_PathData.getAll().keySet());
                        alertDialog.cancel();
                        playerFolderAdapter.notifyItemInserted(folderList.size() - 1);
                    }else {
                        Toast.makeText(MainActivity.this, R.string.path_exists, Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(MainActivity.this, R.string.path_not_found, Toast.LENGTH_SHORT).show();
                }
            });

        });

        checkPermissions();
        if (!isIgnoringBatteryOptimizations()){
            requestIgnoreBatteryOptimizations();
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

    private void checkPermissions(){
        List<String> permissionList = new ArrayList<>();
        for (String requirePermission:REQUIRES_PERMISSIONS){
            if (!(ContextCompat.checkSelfPermission(this,requirePermission) ==PERMISSION_GRANTED)){
                permissionList.add(requirePermission);
            }
        }
        if (!permissionList.isEmpty()){
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[0]),PERMISSION_GRANTED);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean denied = false;
        for (int i=0;i<permissions.length;i++){
            if (grantResults[i] == PERMISSION_DENIED && !Objects.equals(permissions[i], Manifest.permission.MANAGE_EXTERNAL_STORAGE)){
                denied = true;
                Log.d("[linearity]","Permission denied : " + permissions[i]);//break
            }
        }
        if (denied){
            Toast.makeText(this,R.string.permission_denied_alert, Toast.LENGTH_LONG).show();
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
}
