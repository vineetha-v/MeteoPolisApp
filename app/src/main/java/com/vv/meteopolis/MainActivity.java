package com.vv.meteopolis;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

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
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.vv.meteopolis.APIService.BASE_URL;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE = 100;
    private static  final String TAG = MainActivity.class.getSimpleName();
    private FusedLocationProviderClient mLocationClient;
    private View mainView;
    private Location mCurrentLocation;
    private Double mLatitude, mLongitude;
    private APIService apiService;
    File httpCacheDirectory;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        init();


    }

    private void init() {

        mainView = findViewById(R.id.viewRoot);
        sharedPreferences = getSharedPreferences("MeteoPolisPref",MODE_PRIVATE);
        prefEditor = sharedPreferences.edit();
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpCacheDirectory = new File(getCacheDir(), "offlineCache");
        setupRetrofitAndOkHttp();
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
        if(checkInternetAvailability()){
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
                "Pondicherry", getResources().getString(R.string.ow_app_id));
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
                    fetchWeatherByCity("");

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

    private void updateUI(WeatherForecast value) throws UnsupportedEncodingException {
        String output = new Gson().toJson(value);
        byte[] utf8OutString = output.getBytes("UTF8");
        Log.i(TAG, String.valueOf(utf8OutString));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}