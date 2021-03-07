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
import com.vv.meteopolis.view.activity.MainActivity;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastHolder> {

    private Context mContext;
    private  Map<String, List<WeatherForecast>>  weatherListMap;
    private String[] daysList;

    public ForecastAdapter(Context mContext,  Map<String, List<WeatherForecast>> sortedMap) {
        this.mContext = mContext;
        this.weatherListMap = sortedMap;
        daysList = this.weatherListMap.keySet().toArray(new String[sortedMap.size()]);
    }

    @NonNull
    @Override
    public ForecastHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.recyclerview_item_layout, parent, false);
        return new ForecastHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastHolder holder, int position) {
        List<WeatherForecast> weatherObject = weatherListMap.get(daysList[position]);
        holder.day.setText(Utils.convertMillisecToDate(weatherObject.get(0).getDate()));
        holder.temperature.setText(Utils.convertKelvinToCelsius(weatherObject.get(0).getMain().getTemp()));
    }

    @Override
    public int getItemCount() {
        return daysList.length;
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
