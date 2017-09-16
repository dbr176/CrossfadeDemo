package com.ag.crossfadedemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final int CROSSFADE_MIN_VALUE = 2;

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
                    crossfadeValueView.setText(Integer.toString(progress + CROSSFADE_MIN_VALUE)
                            + " " + R.string.secondsText);
                }
    };

    private void initCrossfadeSeekBar() {
        SeekBar crossfadeValue = (SeekBar) findViewById(R.id.seekBarCrossfadeValue);
        crossfadeValue.setOnSeekBarChangeListener(onCrossfadeValueChanged);

        TextView crossfadeValueView = (TextView) findViewById(R.id.crossfadeTextView);
        crossfadeValueView.setText("0 " + R.string.secondsText);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initCrossfadeSeekBar();
    }
}
