package com.upiottxat.wheatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView cityNameText, TemperatureText, HumidityText, WindText, descriptionText;
    private ImageView WeatherIcon;
    private Button refreshButton;
    private EditText CitynameInput;

    private static final String API_KEY = String.valueOf(BuildConfig.API_KEY);
    private static final String TAG = "locationDemo";
    private static final String WEATHER_API_BASE_URL = "https://api.weatherapi.com/v1/current.json";
    private FusedLocationProviderClient fusedlocationProviderClient;
    private CancellationTokenSource cancellationTokenSource;

    // Getting Location permission
    private final ActivityResultLauncher<String> requestPermisson =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getApplicationContext(), "Permisson Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permisson is needed to be granted to get the location", Toast.LENGTH_LONG);

                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        fusedlocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        cancellationTokenSource = new CancellationTokenSource();
        cityNameText = findViewById(R.id.cityNameText);
        TemperatureText = findViewById(R.id.TemperatureText);
        HumidityText = findViewById(R.id.HumidityText);
        WindText = findViewById(R.id.WindText);
        descriptionText = findViewById(R.id.descriptionText);
        WeatherIcon = findViewById(R.id.WeatherIcon);
        refreshButton = findViewById(R.id.FetchWeatherButton);
        CitynameInput = findViewById(R.id.CitynameInput);
        refreshButton.setOnClickListener(v -> FetchWeatherData(CitynameInput.getText().toString(), 0, 0));
        CheckAndAskForGPS();

    }

    private void CheckAndAskForGPS() {
        boolean IsGPSEnables;
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            IsGPSEnables=false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            IsGPSEnables=locationManager.isLocationEnabled();
        }else{
            int mode= Settings.Secure.getInt(this.getContentResolver(),Settings.Secure.LOCATION_MODE,Settings.Secure.LOCATION_MODE_OFF);
            IsGPSEnables=(mode!=Settings.Secure.LOCATION_MODE_OFF);
        }
        if (!IsGPSEnables){
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
        }
        requestLocationPermissionAndFetchDataIfNeeded();
    }


    private void requestLocationPermissionAndFetchDataIfNeeded() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location permission already granted. Fetching location.");
            GetLocationAndFetchData();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Location permission is needed to get weather for your current area.", Toast.LENGTH_LONG).show();
            requestPermisson.launch(Manifest.permission.ACCESS_FINE_LOCATION); // Renamed launcher variable
        } else {
            Log.i(TAG, "Requesting location permission.");
            requestPermisson.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

    }

    private void FetchWeatherData(String cityName,double latitude,double longitude) {



        String url;
        if (cityName != null && !cityName.isEmpty()) {
            url = WEATHER_API_BASE_URL + "?key=" + API_KEY + "&q=" + cityName + "&aqi=no";
        } else if (latitude != 0 || longitude != 0) {
            url = WEATHER_API_BASE_URL + "?key=" + API_KEY + "&q=" + latitude + "," + longitude;
        } else {
            Log.w(TAG, "Cannot fetch weather data: City name or coordinates are missing.");
            Toast.makeText(getApplicationContext(), "Location information is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Request URL: " + url); // Use Log.d for debug info
        
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                try {
                    Response response = client.newCall(request).execute();
                    String result = response.body().string();
                    System.out.println(result);
                    runOnUiThread(() -> updateUi(result));
                } catch (IOException e) {
                    Log.e(TAG, "Network request failed", e); // Log with error level and exception
                    runOnUiThread(() -> {
                        if (e instanceof java.net.UnknownHostException) {
                            Toast.makeText(getApplicationContext(), "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Could not retrieve weather data. Please try again later.", Toast.LENGTH_LONG).show();
                        }
                    });
//                throw new RuntimeException(e);
                }
            });

    }
    private void GetLocationAndFetchData(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.w(TAG, "fetchLocationForWeather called without permission.");
            return;
        }
        fusedlocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            long maxAgeMillis=15*60*1000;
            if (location != null && (System.currentTimeMillis() - location.getTime()) <= maxAgeMillis){
                Log.d(TAG,"using last location"+location.getLatitude()+location.getLongitude());
                //Got old the location
                FetchWeatherData(null,location.getLatitude(),location.getLongitude());
            }else {
                Log.d(TAG, "Last known location is null or too old. Requesting current location.");
                requestFreshLocationAndCallFetchWeatherData();
            }
        });
    }

    private void requestFreshLocationAndCallFetchWeatherData() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            Log.w(TAG,"permisson required");
            return ;

        }else{
            fusedlocationProviderClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY ,cancellationTokenSource.getToken()).addOnSuccessListener(this,location -> {
                if (location != null) {
                    Log.d(TAG, "Fetched current location: Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                    FetchWeatherData(null,location.getLatitude(),location.getLongitude());
                }else{

                }
            });
        }
    }

    private void updateUi(String result) {
        if(result != null && !result.isEmpty()){

            try {

                JSONObject jsonObject=new JSONObject(result);
                JSONObject current=jsonObject.getJSONObject("current");
                int temp_c=(int)current.getDouble("temp_c");
                double  humidity= current.getDouble("humidity");
                double windspeed=current.getDouble("wind_kph");
                String description=current.getJSONObject("condition").getString("text");
//                String icon=current.getJSONObject("condition").getString("icon");
                cityNameText.setText(jsonObject.getJSONObject("location").getString("name"));
                TemperatureText.setText(String.valueOf(temp_c) + "°C");
                HumidityText.setText(String.valueOf(humidity) +"%");
                WindText.setText(String.valueOf(windspeed)+" km/h");
                descriptionText.setText(description);
            }catch (JSONException e){

                Log.e(TAG, "Error parsing weather JSON", e);
                Toast.makeText(getApplicationContext(), getString(R.string.error_parsing_weather_data), Toast.LENGTH_LONG).show();

            }
        }else {
            Log.w(TAG, "updateUi called with null or empty result");

        }
    }
}
