package com.cleveroad.cyclemenusample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView rvExampleList = (RecyclerView) findViewById(R.id.rvExamplelist);
        rvExampleList.setLayoutManager(new LinearLayoutManager(this));
        rvExampleList.setAdapter(new RecyclerViewAdapter(this));
    }
}
