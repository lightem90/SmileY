package com.example.matteo.smiley;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class Main extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, FloatingWindowService.class));
        finish();
    }

}
