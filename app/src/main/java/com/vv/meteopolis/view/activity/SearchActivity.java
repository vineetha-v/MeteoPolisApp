package com.vv.meteopolis.view.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.vv.meteopolis.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import androidx.appcompat.widget.SearchView;

public class SearchActivity extends AppCompatActivity {

    @BindView(R.id.search)
    SearchView mCitySearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        mCitySearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if(s != null) {
                    Intent intent = new Intent();
                    intent.putExtra("cityName", s);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

    }
}