package com.vv.meteopolis.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.vv.meteopolis.R;
import com.vv.meteopolis.view.adapter.FavCitiesAdapter;
import com.vv.meteopolis.view.adapter.ForecastAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavoritesActivity extends AppCompatActivity implements FavCitiesAdapter.OnItemClickListener {

    @BindView(R.id.rvCities)
    RecyclerView rvCities;

    private List<String> favoriteArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        ButterKnife.bind(this);
        if(getIntent().hasExtra("cities")){
            favoriteArrayList = getIntent().getStringArrayListExtra("cities");
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvCities.setLayoutManager(layoutManager);
        rvCities.setHasFixedSize(true);
        FavCitiesAdapter adapter = new FavCitiesAdapter(this, favoriteArrayList, this);
        rvCities.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(String item) {
        Intent intent = new Intent();
        intent.putExtra("cityName", item);
        setResult(RESULT_OK, intent);
        finish();
    }
}