package com.vv.meteopolis.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vv.meteopolis.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FavCitiesAdapter extends RecyclerView.Adapter<FavCitiesAdapter.FavCitiesHolder> {

    private Context mContext;
    private List<String> favCitiesList = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String item);
    }

    public FavCitiesAdapter(Context mContext, List<String> favoriteArrayList, OnItemClickListener listener) {
        this.mContext = mContext;
        this.favCitiesList = favoriteArrayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavCitiesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.recyclerview_cities_item_layout, parent, false);
        return new FavCitiesHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull FavCitiesHolder holder, int position) {
        holder.city.setText(favCitiesList.get(position));
    }

    @Override
    public int getItemCount() {
        return favCitiesList.size();
    }

    class FavCitiesHolder extends RecyclerView.ViewHolder{

        @BindView(R.id.tvCity)
        TextView city;

        FavCitiesHolder(View itemView, OnItemClickListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onItemClick(city.getText().toString());
                }
            });
        }
    }

}

