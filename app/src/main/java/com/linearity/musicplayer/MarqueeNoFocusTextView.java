package com.linearity.musicplayer;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class MarqueeNoFocusTextView extends androidx.appcompat.widget.AppCompatTextView {
    public MarqueeNoFocusTextView(Context context) {
        super(context);
    }

    public MarqueeNoFocusTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeNoFocusTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //lol
    @Override
    public boolean isSelected(){
        return true;
    }
}
