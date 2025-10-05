package com.linearity.musicplayer;

import static com.linearity.musicplayer.Consts.LoggerTag;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolder;
import static com.linearity.musicplayer.MainActivity.PlayerActivityFolderAbsPath;
import static com.linearity.musicplayer.MainActivity.folderList;
import static com.linearity.musicplayer.MainActivity.mainActivityInstance;
import static com.linearity.musicplayer.MainActivity.sharedPreferences_PathData;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//import com.netease.cloudmusic.R;

import java.io.File;
import java.net.URLDecoder;
import java.util.List;
import java.util.Objects;

public class PlayerFolderAdapter extends RecyclerView.Adapter<PlayerFolderAdapter.VH> {

    public static class VH extends RecyclerView.ViewHolder{
        public final LinearLayout mainLayout;
        public final TextView titleTextView;
        public final TextView absPathTextView;
        public VH(View v) {
            super(v);
            mainLayout = v.findViewById(R.id.one_folder);
            titleTextView = v.findViewById(R.id.folder_name);
            absPathTextView = v.findViewById(R.id.folder_abs_path);
        }
    }

    private final List<String> mDatas;
    public PlayerFolderAdapter(List<String> data) {
        this.mDatas = data;
    }


    public static String fileNameFromAbsPath(String absPath){
        String fileName = "";
        String[] folderPathList;
        try {
            absPath = URLDecoder.decode(absPath, "utf-8");
        }catch (Exception e){
            Log.e(LoggerTag,"absPath");
            e.printStackTrace();
        }
        if (absPath.endsWith("/")){
            folderPathList = absPath.substring(0,absPath.length() - 1).split("/");
        }else {
            folderPathList = absPath.split("/");
        }
        if (!Objects.equals(folderPathList[folderPathList.length - 1], "")){
            fileName = folderPathList[folderPathList.length - 1];
        }else {
            if (folderPathList.length >= 2){
                fileName = folderPathList[folderPathList.length - 2];
            }
        }
        return fileName;
    }
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(VH holder, int position) {
        String absPath = folderList.get(position);
        holder.absPathTextView.setText(absPath);
        String folderName = fileNameFromAbsPath(absPath);
        holder.titleTextView.setText(folderName);
        holder.mainLayout.setOnLongClickListener(v -> {
            View deleteFolderView = View.inflate(v.getContext(), R.layout.deletefolder_confirm, null);
            AlertDialog alertDialog = new AlertDialog.Builder(v.getContext()).setView(deleteFolderView).setCancelable(true).show();

            Button btnCancel = deleteFolderView.findViewById(R.id.deletefolder_edittext_btn_cancel);
            Button btnConfirm = deleteFolderView.findViewById(R.id.deletefolder_edittext_btn_confirm);
            Button btnClearCache = deleteFolderView.findViewById(R.id.clear_list_cache);
            btnCancel.setOnClickListener(
                    v1 -> {
                        v1.cancelLongPress();
                        alertDialog.cancel();
                    }
            );
            btnConfirm.setOnClickListener(v12 -> {
                v12.cancelLongPress();
                SharedPreferences.Editor sharedPreferencesEditor_PathData = sharedPreferences_PathData.edit();
                sharedPreferencesEditor_PathData.remove(absPath);
                sharedPreferencesEditor_PathData.apply();
//                        sharedPreferences_PathData = getSharedPreferences("PlayerPathData", MODE_PRIVATE);
                folderList.remove(absPath);
                mDatas.remove(absPath);
                notifyDataSetChanged();
                File f = new File(absPath,"musiclist.pathArr");
                if (f.exists()){
                    f.delete();
                }
                alertDialog.cancel();
            });

            btnClearCache.setOnClickListener(v13 -> {
                v13.cancelLongPress();
                File f = new File(absPath,"musiclist.pathArr");
                if (f.exists()){
                    f.delete();
                }
                alertDialog.cancel();
            });
            return false;

        });
        holder.mainLayout.setOnClickListener(v -> {
            PlayerActivityFolder = folderName;
            PlayerActivityFolderAbsPath = absPath;
            Intent intent = new Intent(v.getContext(), PlayerActivity.class);
            mainActivityInstance.startActivity(intent);//stole this sill instead of Broadcast from slimefun plugins lol
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_viewholder, parent, false);
        return new VH(v);
    }
}
