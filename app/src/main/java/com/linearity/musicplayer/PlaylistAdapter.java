package com.linearity.musicplayer;

import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isProgressBarChanging;
import static com.linearity.musicplayer.PlayerFolderAdapter.fileNameFromAbsPath;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//import com.netease.cloudmusic.R;


public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {

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

    private final String[] mDatas;//songs abs path
    public PlaylistAdapter(String[] data) {
        this.mDatas = data;
    }

    //③ 在Adapter中实现3个方法
    @Override
    public void onBindViewHolder(VH holder, int position) {
        String absPath = mDatas[position];
        holder.authorTextView.setText(absPath);
        String folderName = fileNameFromAbsPath(absPath);
        holder.titleTextView.setText(folderName);
        holder.mainLayout.setOnClickListener(v -> {
            try {
                if (isProgressBarChanging){return;}
                MainActivity.playSong = holder.getAdapterPosition();
                instance.Play(absPath);
                MainActivity.isSongItemClicked = true;
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.length;
    }

    @NonNull
    @Override
    public PlaylistAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_viewholder, parent, false);
        return new PlaylistAdapter.VH(v);
    }
}
