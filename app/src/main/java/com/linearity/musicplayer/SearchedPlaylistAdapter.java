package com.linearity.musicplayer;

import static com.linearity.musicplayer.Consts.LoggerTag;
import static com.linearity.musicplayer.MainActivity.instance;
import static com.linearity.musicplayer.MainActivity.isProgressBarChanging;
import static com.linearity.musicplayer.PlayerFolderAdapter.fileNameFromAbsPath;

import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

//import com.netease.cloudmusic.R;


public class SearchedPlaylistAdapter extends RecyclerView.Adapter<SearchedPlaylistAdapter.VH> {

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
    private final String[] mDataSource;
    private final List<Pair<Integer,String>> mCurrentData = new CopyOnWriteArrayList<>();//songs abs path
    public SearchedPlaylistAdapter(String[] data) {
        this.mDataSource = data;
    }

    //③ 在Adapter中实现3个方法
    @Override
    public void onBindViewHolder(VH holder, int position) {
        String absPath = mCurrentData.get(position).second;
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
        return mCurrentData.size();
    }

    @NonNull
    @Override
    public SearchedPlaylistAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_viewholder, parent, false);
        return new SearchedPlaylistAdapter.VH(v);
    }

    public void doSearch(String searchText){
        if (Objects.equals("",searchText) || searchText == null){
            mCurrentData.clear();
        }else {
            mCurrentData.clear();
            List<Pair<Integer,String>> songs = new LinkedList<>();
            for (int i=0;i<mDataSource.length;i+=1){
                String s = mDataSource[i];
                if (s.contains(searchText)){
                    songs.add(new Pair<>(i,s));
//                    Log.d(LoggerTag,i + s);
                }
            }
            mCurrentData.addAll(songs);
        }
        notifyDataSetChanged();
    }
}
