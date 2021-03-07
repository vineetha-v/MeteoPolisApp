package com.vv.meteopolis.view.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vv.meteopolis.R;
import com.vv.meteopolis.model.Forecast;
import com.vv.meteopolis.model.Weather;
import com.vv.meteopolis.model.WeatherForecast;
import com.vv.meteopolis.network.APIService;
import com.vv.meteopolis.utils.Utils;
import com.vv.meteopolis.view.adapter.ForecastAdapter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.vv.meteopolis.network.APIService.BASE_URL;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE = 100;
    private static final int SEARCH_CITY_REQUEST_CODE = 101;
    private static final int FAVORITE_CITY_REQUEST_CODE = 102;

    private static  final String TAG = MainActivity.class.getSimpleName();

    private FusedLocationProviderClient mLocationClient;
    private Location mCurrentLocation;
    private Double mLatitude, mLongitude;
    private APIService apiService;
    File httpCacheDirectory;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor prefEditor;
    private boolean cityFetched = false;

    @BindView(R.id.tvWeatherMain) TextView txtMainWeather;
    @BindView(R.id.tvSecTemp) TextView txtSecTemp;
    @BindView(R.id.tvCityName) TextView txtCityName;
    @BindView(R.id.tvCloud) TextView txtCloud;
    @BindView(R.id.tvPressure) TextView txtPressure;
    @BindView(R.id.tvHumidity) TextView txtHumidity;
    @BindView(R.id.tvWind) TextView txtWind;
    @BindView(R.id.tvWeatherDesc) TextView txtWeatherDesc;
    @BindView(R.id.tvMainTemp) TextView txtMainTemp;

    @BindView(R.id.iv_favorite) ImageView imgFavorite;
    @BindView(R.id.iv_search) ImageView imgSearch;
    @BindView(R.id.iv_more) ImageView imgMore;
    @BindView(R.id.ivWeather) ImageView imgWeather;

    @BindView(R.id.rvDayWeather) RecyclerView rvForecast;

    @BindView(R.id.viewRoot) View mainView;
    private List<String> favoriteArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    private void init() {

        mainView = findViewById(R.id.viewRoot);
        sharedPreferences = getSharedPreferences("MeteoPolisPref",MODE_PRIVATE);
        prefEditor = sharedPreferences.edit();
        fetchFavsList();
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpCacheDirectory = new File(getCacheDir(), "offlineCache");
        setupRetrofitAndOkHttp();
    }

    @OnClick(R.id.iv_search)
    void searchCity(){
        startActivityForResult(new Intent(MainActivity.this, SearchActivity.class), SEARCH_CITY_REQUEST_CODE);
    }

    private boolean checkIfCityIsFav(String cityName) {
        if(favoriteArrayList.contains(cityName)) {
            imgFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_selected));
            return true;
        } else {
            imgFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_unselected));
            return false;
        }
    }

    private void fetchFavsList() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString("favorite", null);
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        favoriteArrayList = gson.fromJson(json, type);
        if (favoriteArrayList == null) {
            favoriteArrayList = new ArrayList<>();
        }
    }

    @OnClick(R.id.iv_favorite)
    void setFavoriteCity() {
        Gson gson = new Gson();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String city_name = txtCityName.getText().toString();
        if(checkIfCityIsFav(city_name)) {
            favoriteArrayList.add(city_name);
            imgFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_unselected));
        } else {
            favoriteArrayList.remove(city_name);
            imgFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_selected));
        }
        String jsonConvert = gson.toJson(favoriteArrayList);
        editor.putString("favorite", jsonConvert);
        editor.apply();
    }

    @OnClick(R.id.iv_more)
    void openSettings() {
        PopupMenu popup = new PopupMenu(MainActivity.this, imgMore);
        popup.getMenuInflater().inflate(R.menu.menu_scrolling, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_settings) {
                    Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
                    intent.putStringArrayListExtra("cities", (ArrayList<String>) favoriteArrayList);
                    startActivityForResult(intent, FAVORITE_CITY_REQUEST_CODE);
                }
                return true;
            }
        });
        popup.show();
    }

    private void setupRetrofitAndOkHttp() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor cacheInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {

                CacheControl.Builder cacheBuilder = new CacheControl.Builder();
                cacheBuilder.maxAge(0, TimeUnit.SECONDS);
                cacheBuilder.maxStale(365,TimeUnit.DAYS);
                CacheControl cacheControl = cacheBuilder.build();

                Request request = chain.request();
                if(checkInternetAvailability()){
                    request = request.newBuilder()
                            .cacheControl(cacheControl)
                            .build();
                }
                Response originalResponse = chain.proceed(request);
                if (checkInternetAvailability()) {
                    int maxAge = 60  * 60; // read from cache
                    return originalResponse.newBuilder()
                            .header("Cache-Control", String.format("max-age=%d", maxAge))
                            .build();
                } else {
                    int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
                    return originalResponse.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                            .build();
                }
            }
        };
        File httpCacheDirectory = new File(getCacheDir(), "responses");
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(cacheInterceptor)
                .cache(cache)
                .addInterceptor(httpLoggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .client(httpClient)
                .baseUrl(BASE_URL)
                .build();

        apiService = retrofit.create(APIService.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(checkInternetAvailability() && !cityFetched){
            fetchCurrentLocation();
        } else {
            Snackbar.make(mainView, R.string.no_internet_message,
                    Snackbar.LENGTH_SHORT)
                    .show();
            showLastLocationWeather();
        }
    }

    private void showLastLocationWeather() {
        fetchWeatherForecast(getDouble("Lat", 0), getDouble("Long", 0));
    }

    private void fetchWeatherByCity(String cityName){
        Observable<WeatherForecast> observable = apiService.getWeatherForecastByCity(
                cityName, getResources().getString(R.string.ow_app_id));
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<WeatherForecast, WeatherForecast>() {
                    @Override
                    public WeatherForecast apply(WeatherForecast weatherForecast) throws Exception {
                        return weatherForecast;
                    }
                }).subscribe(new Observer<WeatherForecast>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i(TAG, "onSubscribe");
            }
            @Override
            public void onNext(WeatherForecast value) {
                try {
                    updateUI(value);
                    fetchUpcomingWeatherForecast(cityName);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "onError");
            }
            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });

    }

    private double getDouble(final String key, final double defaultValue) {
        if ( !sharedPreferences.contains(key))
            return defaultValue;

        return Double.longBitsToDouble(sharedPreferences.getLong(key, 0));
    }

    private boolean checkInternetAvailability() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void fetchCurrentLocation() {
        if(checkLocationPermission()){
            getLatestCurrentLocation();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(mainView, R.string.location_permission_requirement,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request the permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_CITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && (data != null && data.hasExtra("cityName"))) {
                cityFetched = true;
                String cityName = data.getStringExtra("cityName");
                fetchWeatherByCity(cityName);
            }
            if (resultCode == RESULT_CANCELED) {
                //do nothing
            }
        } else if (requestCode == FAVORITE_CITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && (data != null && data.hasExtra("cityName"))) {
                cityFetched = true;
                String cityName = data.getStringExtra("cityName");
                fetchWeatherByCity(cityName);
            }
            if (resultCode == RESULT_CANCELED) {
                //do nothing
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE) {
            if(grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Permission has been granted
                Snackbar.make(mainView, R.string.requested_location_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
                getLatestCurrentLocation();
            } else {
                // Permission request was denied.
                Snackbar.make(mainView, R.string.requested_location_permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }

    }

    @SuppressWarnings("MissingPermission")
    private void getLatestCurrentLocation() {
        mLocationClient.getLastLocation().addOnCompleteListener(this,new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    mCurrentLocation = task.getResult();
                    mLatitude = mCurrentLocation.getLatitude();
                    mLongitude = mCurrentLocation.getLongitude();
                    prefEditor.putLong("Lat", Double.doubleToRawLongBits(mLatitude));
                    prefEditor.putLong("Long", Double.doubleToRawLongBits(mLongitude));
                    prefEditor.commit();
                    Log.i(TAG, "Location detected");
                    fetchWeatherForecast(mLatitude, mLongitude);

                } else {
                    Log.i(TAG, "Location can't be detected");
                    Log.i(TAG, "getLastLocation:exception", task.getException());
                }
            }
        });
    }

    private void fetchWeatherForecast(Double mLatitude, Double mLongitude) {
        Observable<WeatherForecast> observable = apiService.getWeatherForecast(mLatitude,
                mLongitude, getResources().getString(R.string.ow_app_id));
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<WeatherForecast, WeatherForecast>() {
                    @Override
                    public WeatherForecast apply(WeatherForecast weatherForecast) throws Exception {
                        return weatherForecast;
                    }
                }).subscribe(new Observer<WeatherForecast>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Log.i(TAG, "onSubscribe");
                        }
                        @Override
                        public void onNext(WeatherForecast value) {
                            try {
                                updateUI(value);
                                fetchUpcomingWeatherForecast(value.getName());
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(Throwable e) {
                            Log.i(TAG, "onError");
                        }
                        @Override
                        public void onComplete() {
                            Log.i(TAG, "onComplete");
                        }
        });
    }

    private void fetchUpcomingWeatherForecast(String cityName) {
        Observable<Forecast> observable = apiService.getFiveWeatherForecastByCity(
                cityName, getResources().getString(R.string.ow_app_id));
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<Forecast, Forecast>() {
                    @Override
                    public Forecast apply(Forecast weatherForecast) throws Exception {
                        return weatherForecast;
                    }
                }).subscribe(new Observer<Forecast>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i(TAG, "onSubscribe");
            }
            @Override
            public void onNext(Forecast value) {
                updateListView(value);
            }
            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "onError");
            }
            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });
    }

    private void updateListView(Forecast value) {
        //sort out the values acc to date
        List<WeatherForecast> weatherList = value.getWeatherForecastList();
        Map<String, List<WeatherForecast>> todayMap = new HashMap<String, List<WeatherForecast>>();
        Map<String, List<WeatherForecast>> sortedMap = new HashMap<String, List<WeatherForecast>>();
        for(WeatherForecast w : weatherList) {
            if(Utils.getCurrentDate().equals(Utils.convertMillisecToDate(w.getDate()))){
                List<WeatherForecast> tempWeatherList = new ArrayList<>();
                tempWeatherList.add(w);
                todayMap.put(Utils.convertMillisecToDate(w.getDate()), tempWeatherList);
            } else {
                if (sortedMap.containsKey(Utils.convertMillisecToDate(w.getDate()))) {
                    List<WeatherForecast> tempList = sortedMap.get(Utils.convertMillisecToDate(w.getDate()));
                    tempList.add(w);
                } else {
                    List<WeatherForecast> tempList = new ArrayList<>();
                    tempList.add(w);
                    sortedMap.put(Utils.convertMillisecToDate(w.getDate()), tempList);
                }
            }
        }
        Map<String, List<WeatherForecast>> treeMap = new TreeMap<String, List<WeatherForecast>>(sortedMap);
        //populate recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvForecast.setLayoutManager(layoutManager);
        rvForecast.setHasFixedSize(true);
        ForecastAdapter adapter = new ForecastAdapter(this, treeMap);
        rvForecast.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void updateUI(WeatherForecast value) throws UnsupportedEncodingException {
       txtCityName.setText(value.getName());
       if(checkIfCityIsFav(value.getName())){
           imgFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_selected));
       } else {
           imgFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_unselected));
       }
        Weather weather = value.getWeather().get(0);
       txtMainWeather.setText(weather.getMain());
       txtWeatherDesc.setText(weather.getDescription());

       String iconUrl = getResources().getString(R.string.weather_icon_base_url)+value.getWeather().get(0).getIcon()+"@2x.png";
        Glide.with(this)
                .load(iconUrl)
                .placeholder(R.drawable.ic_weather_placeholder)
                .error(R.drawable.ic_weather_placeholder)
                .into(imgWeather);

        txtMainTemp.setText(Utils.convertKelvinToCelsius(value.getMain().getTemp()));
        txtSecTemp.setText("Feels like "+Utils.convertKelvinToCelsius(value.getMain().getFeels_like()));

        txtPressure.setText(value.getMain().getPressure()+"hPa");
        txtHumidity.setText(value.getMain().getHumidity()+"%");
        txtCloud.setText(value.getClouds().getAll()+"%");
        txtWind.setText(value.getWind().getSpeed()+"m/s");
    }
}