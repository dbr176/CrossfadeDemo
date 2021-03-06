package com.ag.crossfadedemo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private class UpdatePlayButton extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mPlayer != null) switchPlayPause(!mPlayer.isPaused());
                }
            });
        }
    }

    private static final int CROSSFADE_MIN_VALUE = 2;
    private static final int CHOOSE_TRACK1_REQUEST_CODE = 771;
    private static final int CHOOSE_TRACK2_REQUEST_CODE = 772;

    private Toast mToast;
    private Timer mTimer;
    private Thread mCheckPlayerThread;

    private CharSequence mSecondsText;
    private MediaPlayer mTrack1;
    private MediaPlayer mTrack2;

    private CrossfadePlayer mPlayer;

    private SeekBar.OnSeekBarChangeListener onCrossfadeValueChanged =
            new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    TextView crossfadeValueView = (TextView)findViewById(R.id.crossfadeTextView);
                    crossfadeValueView.setText(
                            String.format(Locale.getDefault(),
                                    "%d %s",
                                    progress + CROSSFADE_MIN_VALUE, mSecondsText));
                }
    };

    private int getCrossfadeValue() {
        SeekBar crossfadeValue = (SeekBar) findViewById(R.id.seekBarCrossfadeValue);
        crossfadeValue.setOnSeekBarChangeListener(onCrossfadeValueChanged);

        return crossfadeValue.getProgress();
    }

    private void initCrossfadeSeekBar() {
        SeekBar crossfadeValue = (SeekBar) findViewById(R.id.seekBarCrossfadeValue);
        crossfadeValue.setOnSeekBarChangeListener(onCrossfadeValueChanged);

        TextView crossfadeValueView = (TextView) findViewById(R.id.crossfadeTextView);
        crossfadeValueView.setText(
                String.format(Locale.getDefault(),
                        "%d %s", CROSSFADE_MIN_VALUE, mSecondsText));

    }

    private MediaPlayer onActivityResultTracks(Uri uri) {
        return MediaPlayer.create(getApplicationContext(), uri);
    }

    private void chooseTrack(int requestCode) {
        Intent intent = new Intent()
                .setType("audio/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(
                intent,
                getString(R.string.selectFileText)),
                requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if((requestCode == CHOOSE_TRACK1_REQUEST_CODE || requestCode == CHOOSE_TRACK2_REQUEST_CODE)
                && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            DocumentFile file = DocumentFile.fromSingleUri(getApplicationContext(), uri);
            Button button;

            if (requestCode == CHOOSE_TRACK1_REQUEST_CODE) {
                mTrack1 = onActivityResultTracks(uri);
                if (mTrack1.getDuration() < getCrossfadeValue() * 1000) {
                    mToast = Toast.makeText(getApplicationContext(), "Selected file is too short",
                            Toast.LENGTH_SHORT);
                    mTrack1 = null;
                }
                else {
                    button = (Button) findViewById(R.id.buttonTrack1);
                    button.setText(file.getName());
                }
            }
            else {
                mTrack2 = onActivityResultTracks(uri);
                if (mTrack2.getDuration() < getCrossfadeValue() * 1000) {
                    mToast = Toast.makeText(getApplicationContext(), "Selected file is too short",
                            Toast.LENGTH_SHORT);
                    mTrack2 = null;
                }
                else {
                    button = (Button) findViewById(R.id.buttonTrack2);
                    button.setText(file.getName());
                }
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSecondsText = getString(R.string.secondsText);
        initCrossfadeSeekBar();

        mTimer = new Timer();
        mTimer.schedule(new UpdatePlayButton(), 0, 2);
    }

    public void onButtonChooseTrack2Click(View view) {
        chooseTrack(CHOOSE_TRACK2_REQUEST_CODE);
    }
    public void onButtonChooseTrack1Click(View view) {
        chooseTrack(CHOOSE_TRACK1_REQUEST_CODE);
    }

    private void switchPlayPause(boolean toPause) {
        ImageButton playButton = (ImageButton)findViewById(R.id.buttonPlay);
        SeekBar crossfadeSeekBar = (SeekBar)findViewById(R.id.seekBarCrossfadeValue);

        if(mPlayer != null)
        if(toPause) {
            playButton.setImageResource(android.R.drawable.ic_media_pause);
            crossfadeSeekBar.setEnabled(false);

        }
        else {
            playButton.setImageResource(android.R.drawable.ic_media_play);
            crossfadeSeekBar.setEnabled(true);
        }
    }

    private void disableTrackButtons() {
        View trackButton1 = findViewById(R.id.buttonTrack1);
        View trackButton2 = findViewById(R.id.buttonTrack2);

        trackButton1.setEnabled(false);
        trackButton2.setEnabled(false);
    }

    public void onPlayButtonClick(View view) {
        if (mPlayer == null || !mPlayer.isPlaying()) {
            View crossfadeSeekBar = findViewById(R.id.seekBarCrossfadeValue);
            crossfadeSeekBar.setActivated(false);
            if (mTrack1 != null & mTrack2 != null) {
                mPlayer = new CrossfadePlayer(1000 * getCrossfadeValue(), mTrack1, mTrack2);
                mPlayer.start();

                disableTrackButtons();
            }
            else {
                mToast = Toast.makeText(getApplicationContext(),
                        R.string.filesNotSelectedText,
                        Toast.LENGTH_SHORT);
                mToast.show();
            }
        }
        else {
            if (mPlayer.isPaused()) {
                mPlayer.resume();
                mPlayer.mCrossfadeValue = getCrossfadeValue();
            }
            else {
                mPlayer.pause();
                mPlayer.updateCrossfadeValue(getCrossfadeValue());
            }
        }
    }
}
