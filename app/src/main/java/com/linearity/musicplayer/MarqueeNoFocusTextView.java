package com.linearity.musicplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class MarqueeNoFocusTextView extends TextView {
    public MarqueeNoFocusTextView(Context context) {
        super(context);
    }

    public MarqueeNoFocusTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeNoFocusTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MarqueeNoFocusTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //lol
    @Override
    public boolean isSelected(){
        return true;
    }
}
