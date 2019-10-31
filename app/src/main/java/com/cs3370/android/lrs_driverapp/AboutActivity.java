package com.cs3370.android.lrs_driverapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    private TextView mTitle;
    private TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mTitle = (TextView) findViewById(R.id.titleText);
        mText = (TextView) findViewById(R.id.aboutText);

    }
}
