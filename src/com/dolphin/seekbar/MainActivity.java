package com.dolphin.seekbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.dolphin.multitouchseekbar.R;

public class MainActivity extends Activity {

    RangeSeekbar mSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSeekBar = (RangeSeekbar) findViewById(R.id.seekbar);
        mSeekBar.setLeftSelection(2);
        mSeekBar.setRightSelection(4);

        findViewById(R.id.set_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setLeftSelection(0);
            }
        });

        findViewById(R.id.set_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setRightSelection(5);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
