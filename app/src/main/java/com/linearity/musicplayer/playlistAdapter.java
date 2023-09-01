package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.folderList;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.sharedPreferencesEditor_PathData;
import static com.linearity.musicplayer.MainActivity.sharedPreferences_PathData;
import static com.linearity.musicplayer.PlayerActivity.playerActivityInstance;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class playlistAdapter extends RecyclerView.Adapter<playlistAdapter.VH> {

    public static class VH extends RecyclerView.ViewHolder{
        public final LinearLayout mainLayout;
        public final TextView titleTextView;
        public final TextView authorTextView;
        public VH(View v) {
            super(v);
            mainLayout = v.findViewById(R.id.one_song);
            titleTextView = v.findViewById(R.id.song_name);
            authorTextView = v.findViewById(R.id.song_author);
        }
    }

    private String[] mDatas;//songs abs path
    public playlistAdapter(String[] data) {
        this.mDatas = data;
    }

    //③ 在Adapter中实现3个方法
    @Override
    public void onBindViewHolder(VH holder, int position) {
        String absPath = mDatas[position];
        holder.authorTextView.setText(absPath);
        String folderName = "";
        String[] folderPathList = absPath.split("/");
        if (!Objects.equals(folderPathList[folderPathList.length - 1], "")){
            folderName = folderPathList[folderPathList.length - 1];
        }else {
            if (folderPathList.length >= 2){
                folderName = folderPathList[folderPathList.length - 2];
            }
        }
        holder.titleTextView.setText(folderName);
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instance.playSong = holder.getAdapterPosition() - 1;
                instance.Play(absPath);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.length;
    }

    @NonNull
    @Override
    public playlistAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        //LayoutInflater.from指定写法
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_viewholder, parent, false);
        return new playlistAdapter.VH(v);
    }
}
