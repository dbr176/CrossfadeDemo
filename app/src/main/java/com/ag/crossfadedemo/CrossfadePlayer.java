package com.ag.crossfadedemo;

import android.media.MediaPlayer;

import java.lang.Thread;

public final class CrossfadePlayer {
    private final static int DONT_UPDATE_CROSSFADE_VALUE = -1;

    MediaPlayer mPlayerTrack1;
    MediaPlayer mPlayerTrack2;

    Thread mControlThread;

    int mCrossfadeValue;

    boolean mPlaying;

    private States mPrevState = States.NONE;
    private States mState = States.NONE;
    private boolean mNeedPause;
    private boolean mNeedClose;
    private int mNewCrossfadeValue = DONT_UPDATE_CROSSFADE_VALUE;


    synchronized void setState(States state) {
        mState = state;
    }

    public void updateCrossfadeValue(int crossfadeValue) {
        mNewCrossfadeValue = crossfadeValue;
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
        TRACK_SWAP,
        CLOSED
    }

    private void setPause() {
        mPrevState = mState;
        mState = States.PAUSED;
        mNeedPause = false;
    }

    private void _close() {
        mState = States.CLOSED;
        mPlaying = false;
    }

    private class CrossfadeStateThread extends Thread {
        CrossfadePlayer mParent;

        public CrossfadeStateThread(CrossfadePlayer player) {
            mParent = player;
        }

        public void run() {
            int duration1 = 0;
            int duration2 = 0;
            float timeToFinish = 1;
            float timeFromStart = 0.0f;

            while (true) {
                if(mNeedClose) {
                    _close();
                    return;
                }

                switch (mState) {
                    case TRACK_SWAP: {
                        mPlayerTrack1.seekTo(0);

                        MediaPlayer temp = mPlayerTrack1;
                        mParent.mPlayerTrack1 = mPlayerTrack2;
                        mParent.mPlayerTrack2 = temp;

                        duration1 = mPlayerTrack1.getDuration();
                        timeToFinish = 1;
                        setState(States.PLAYING);
                        break;
                    }
                    // Если воспроизведение только началось
                    case STARTED: {
                        mPlayerTrack1.setVolume(1, 1);
                        duration1 = mPlayerTrack1.getDuration();
                        mPlayerTrack1.start();

                        setState(States.PLAYING);
                        break;
                    }
                    // Если трек воспроизводится
                    case PLAYING: {
                        if (mNeedPause) setPause();
                        if (mPlayerTrack1.getCurrentPosition() > duration1 - mCrossfadeValue)
                            setState(States.CROSSFADE_STARTED);
                        if (mNewCrossfadeValue != DONT_UPDATE_CROSSFADE_VALUE) {
                            mCrossfadeValue = mNewCrossfadeValue;
                            mNewCrossfadeValue = DONT_UPDATE_CROSSFADE_VALUE;
                        }
                        break;
                    }
                    // Если началось переключение между треками
                    case CROSSFADE_STARTED: {
                        duration2 = mPlayerTrack2.getDuration();
                        mPlayerTrack2.seekTo(0);
                        mPlayerTrack2.start();
                        mPlayerTrack2.setVolume(0f, 0f);
                        setState(States.CROSSFADE);
                        break;
                    }
                    case CROSSFADE: {
                        if (mNeedPause) setPause();
                        if (!mPlayerTrack1.isPlaying()) {
                            setState(States.TRACK_SWAP);
                            break;
                        }

                        timeToFinish = (duration1 - mPlayerTrack1.getCurrentPosition())
                                / (float) mCrossfadeValue;
                        mPlayerTrack1.setVolume(timeToFinish, timeToFinish);

                        timeFromStart = Math.max(0f,
                                mPlayerTrack2.getCurrentPosition() / (float) mCrossfadeValue);
                        mPlayerTrack2.setVolume(timeFromStart, timeFromStart);

                        break;
                    }
                    case PAUSED: {
                        mPlayerTrack1.pause();
                        mPlayerTrack2.pause();
                        break;
                    }
                    case STOPPED: {
                        mPlayerTrack1.stop();
                        mPlayerTrack1.seekTo(0);
                        mPlayerTrack2.stop();
                        mPlayerTrack2.seekTo(0);
                        break;
                    }
                    case RESUMING: {
                        mState = mPrevState;
                        mPlayerTrack1.start();
                        if (mPrevState == States.CROSSFADE)
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
            mControlThread = new CrossfadeStateThread(this);

            mPlaying = true;
            setState(States.STARTED);

            mControlThread.start();
        }
    }

    public void resume() {
        setState(States.RESUMING);
    }

    public void close() {
        mNeedClose = true;
    }

    public void stop() {
        mState = States.STOPPED;
    }

    public void pause() {
        mNeedPause = true;
    }

}
