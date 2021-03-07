package com.vv.meteopolis.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vv.meteopolis.R;
import com.vv.meteopolis.model.WeatherForecast;
import com.vv.meteopolis.utils.Utils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastHolder> {

    private Context mContext;
    private List<WeatherForecast> weatherList;

    public ForecastAdapter(Context mContext, List<WeatherForecast> weatherList) {
        this.mContext = mContext;
        this.weatherList = weatherList;
    }

    @NonNull
    @Override
    public ForecastHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.recyclerview_item_layout, parent, false);
        return new ForecastHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastHolder holder, int position) {
        WeatherForecast weatherObject = weatherList.get(position);
        holder.day.setText(Utils.convertMillisecToDate(weatherObject.getDate()));
        holder.temperature.setText(Utils.convertKelvinToCelsius(weatherObject.getMain().getTemp()));
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

}
class ForecastHolder extends RecyclerView.ViewHolder{

    @BindView(R.id.tvDay)
    TextView day;
    @BindView(R.id.tvDayTemp)
    TextView temperature;

    ForecastHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
