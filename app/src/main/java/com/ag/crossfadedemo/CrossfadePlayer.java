package com.ag.crossfadedemo;

import android.media.MediaPlayer;
import java.lang.Thread;
import java.util.concurrent.ThreadPoolExecutor;

public class CrossfadePlayer {
    MediaPlayer mPlayerTrack1;
    MediaPlayer mPlayerTrack2;

    Thread mTrack1Thread;
    Thread mTrack2Thread;

    int mCrossfadeValue;
    boolean mPlaying;
    boolean mCrossfadeTrigger;

    private class FirstTrackThread extends Thread {
        CrossfadePlayer mParent;

        public FirstTrackThread(CrossfadePlayer player) {
            mParent = player;
        }

        public void run() {
            mParent.mPlayerTrack1.start();
            int duration = mParent.mPlayerTrack1.getDuration();

            while (mParent.mPlayerTrack1
                    .getCurrentPosition() < duration - mParent.mCrossfadeValue);
            mCrossfadeTrigger = true;

            // Тут нужно начать уменьшать звук
        }
    }

    private class SecondTrackThread extends Thread {
        CrossfadePlayer mParent;

        public SecondTrackThread(CrossfadePlayer player) {
            mParent = player;
        }

        public void run() {
            while(!mCrossfadeTrigger) ;

            mParent.mPlayerTrack2.start();
            int duration = mParent.mPlayerTrack2.getDuration();

            // уменьшаем звук
            while (mParent.mPlayerTrack2.getCurrentPosition() < mParent.mCrossfadeValue) {
                // увеличиваем звук
            }
        }

    }

    public CrossfadePlayer(int crossfadeValue, MediaPlayer player1, MediaPlayer player2) {
        mPlayerTrack1 = player1;
        mPlayerTrack2 = player2;
        mCrossfadeValue = crossfadeValue;

        if (mPlayerTrack1 == null
                || mPlayerTrack2 == null)
            throw new NullPointerException("MediaPlayer can't be null.");
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    public void start() {
        mTrack1Thread = new FirstTrackThread(this);
        mTrack2Thread = new SecondTrackThread(this);

        mTrack1Thread.start();
        mTrack2Thread.start();
    }

    public void pause() {
        mPlayerTrack1.pause();
        mPlayerTrack2.pause();
    }

}
