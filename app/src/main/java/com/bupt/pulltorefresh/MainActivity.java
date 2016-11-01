package com.bupt.pulltorefresh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.listview);
        String[] datas = {"1", "2", "3", "4", "5", "6", "7","8","9","10","11","12","13","14","15","16","17"};
        mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, datas));
    }
}
