package com.ag.crossfadedemo;

import android.media.MediaPlayer;
import android.util.Log;

import java.lang.Thread;
import java.util.concurrent.ThreadPoolExecutor;

public class CrossfadePlayer {
    MediaPlayer mPlayerTrack1;
    MediaPlayer mPlayerTrack2;

    Thread mTrack1Thread;
    Thread mTrack2Thread;

    int mCrossfadeValue;

    boolean mPlaying;

    private States mPrevState = States.NONE;
    private States mState = States.NONE;
    private boolean mNeedPause;
    private int mResumedTracks = 0;

    synchronized void setState(States state) {
        mState = state;
    }

    private enum States {
        NONE,
        STARTED,
        STOPPED,
        PAUSED,
        RESUMING,
        CROSSFADE_STARTED,
        CROSSFADE,
        PLAYING,
        CROSSFADE_FINISHED,
        TRACK_SWAP_STARTED,
        TRACK_SWAP_FINISHED
    }

    private void setPause() {
        mPrevState = mState;
        mState = States.PAUSED;
        mNeedPause = false;
        mResumedTracks = 0;
    }

    private class FirstTrackThread extends Thread {
        CrossfadePlayer mParent;

        public FirstTrackThread(CrossfadePlayer player) {
            mParent = player;
        }

        public void run() {
            int duration = 0;
            float timeToFinish = 1;

            while (true) {
                switch (mState) {
                    // При переключении треков
                    case TRACK_SWAP_FINISHED: {
                        duration = mPlayerTrack1.getDuration();
                        timeToFinish = 1;
                        setState(States.PLAYING);
                        break;
                    }
                    // Если воспроизведение только началось
                    case STARTED: {
                        mPlayerTrack1.setVolume(1, 1);
                        duration = mPlayerTrack1.getDuration();
                        mPlayerTrack1.start();

                        setState(States.PLAYING);
                        break;
                    }
                    // Если трек воспроизводится
                    case PLAYING: {
                        if (mPlayerTrack1.getCurrentPosition() > duration - mCrossfadeValue)
                            setState(States.CROSSFADE_STARTED);
                        if (mNeedPause)
                            setState(States.PAUSED);
                        break;
                    }
                    // Если началось переключение между треками
                    case CROSSFADE_STARTED: {
                        break;
                    }
                    case CROSSFADE: {
                        if (!mPlayerTrack1.isPlaying()) {
                            setState(States.CROSSFADE_FINISHED);
                            break;
                        }
                        timeToFinish = (duration - mPlayerTrack1.getCurrentPosition())
                                / (float) mCrossfadeValue;
                        mPlayerTrack1.setVolume(timeToFinish, timeToFinish);
                        if(mNeedPause)
                            setState(States.PAUSED);
                        break;
                    }
                    case PAUSED: {
                        mPlayerTrack1.pause();
                        break;
                    }
                    case STOPPED: {
                        mPlayerTrack1.stop();
                        mPlayerTrack1.seekTo(0);
                        break;
                    }
                    case RESUMING: {
                        mState = mPrevState;
                        mPlayerTrack1.start();
                        mPlayerTrack2.start();
                    }
                }
            //mStreamFlag = !mStreamFlag;
            }
        }
    }

    private class SecondTrackThread extends Thread {
        CrossfadePlayer mParent;

        public SecondTrackThread(CrossfadePlayer player) {
            mParent = player;
        }

        public void run() {
            int duration = 0;
            float timeFromStart = 0.0f;

            while (true) {
                switch (mState) {
                    case TRACK_SWAP_STARTED: {
                        //mPlayerTrack1.reset();
                        mPlayerTrack1.seekTo(0);

                        MediaPlayer temp = mPlayerTrack1;
                        mParent.mPlayerTrack1 = mPlayerTrack2;
                        mParent.mPlayerTrack2 = temp;

                        setState(States.TRACK_SWAP_FINISHED);
                        break;
                    }
                    case CROSSFADE_STARTED: {
                        duration = mPlayerTrack2.getDuration();
                        mPlayerTrack2.seekTo(0);
                        mPlayerTrack2.start();
                        mPlayerTrack2.setVolume(0f, 0f);
                        setState(States.CROSSFADE);
                        break;
                    }
                    case CROSSFADE: {
                        timeFromStart = Math.max(0f,
                                mPlayerTrack2.getCurrentPosition() / (float) mCrossfadeValue);
                        mPlayerTrack2.setVolume(timeFromStart, timeFromStart);
                        break;
                    }
                    case CROSSFADE_FINISHED: {
                        setState(States.TRACK_SWAP_STARTED);
                        break;
                    }
                    case STOPPED: {
                        mPlayerTrack2.stop();
                        mPlayerTrack2.seekTo(0);
                    }
                    case RESUMING: {
                        mState = mPrevState;
                        mPlayerTrack1.start();
                        mPlayerTrack2.start();
                    }
                }
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

    public boolean isPaused() {
        return mState == States.PAUSED;
    }

    public void start() {
        if (mState == States.STOPPED || mState == States.NONE) {
            mTrack1Thread = new FirstTrackThread(this);
            mTrack2Thread = new SecondTrackThread(this);

            mPlaying = true;
            mState = States.STARTED;

            mTrack1Thread.start();
            mTrack2Thread.start();
        }
    }

    public void continuePlay() {
        mState = mPrevState;
    }

    public void stop() {
        mState = States.STOPPED;
    }

    public void pause() {
        mNeedPause = true;
    }

}
