package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity as callback from the SMS to open map.
 * <p>
 *     For using Google Maps API, add in the manifest
 *     <meta-data android:name="com.google.android.geo.API_KEY" android:value=""/>
 * </p>
 */
public class DisplayLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static class DeterminateLocationAsyncTask extends AsyncTask<String, Void, IDetermineLocationProvider.GeoLocation> {

        private final IDetermineLocationProvider apiProvider;
        private final DisplayLocationActivity myActivity; // TODO ?
        private String errorMessage;

        public DeterminateLocationAsyncTask(IDetermineLocationProvider apiProvider, DisplayLocationActivity activity) {
            this.apiProvider = apiProvider;
            this.myActivity = activity;
        }

        @Override
        protected IDetermineLocationProvider.GeoLocation doInBackground(String... strings) {
            try {
                return apiProvider.determinateGeoLocation(strings[0], strings[1], strings[2], strings[3], strings[4]);
            }
            catch(IOException e) {
                Log.e(AppInfo.APP_NAME, "Error", e);
                this.errorMessage = e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(IDetermineLocationProvider.GeoLocation result) {
            if ( result != null ) {
                myActivity.showMap(result);
            } else {
                Toast.makeText(myActivity, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private GoogleMap mMap;
    private LatLng mPosition;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, getString(R.string.admob_appid));
        // Test ID ca-app-pub-3940256099942544/6300978111

        setContentView(R.layout.displaylocation);

        final AdView adView = findViewById(R.id.adViewDisplayMap);
        AdRequest adRequest = new AdRequest.Builder().
                addTestDevice("D485D71F04DA4B5772EAE7F2605149C9"). // my S8
                addTestDevice(AdRequest.DEVICE_ID_EMULATOR).
                build(); // my S8
        adView.loadAd(adRequest);

        checkMyPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String networkType = null;
            String mcc = null;
            String mnc = null;
            String areaCode = null;
            String cellId = null;
            // if we are called directly from android, then we only get a Query String
            if (uri != null) {
                networkType = uri.getQueryParameter("networkType");
                mcc = uri.getQueryParameter("mcc");
                mnc = uri.getQueryParameter("mnc");
                areaCode = uri.getQueryParameter("areaCode");
                cellId = uri.getQueryParameter("cellId");
            }

            mTitle = intent.getStringExtra("sender");
            if (mTitle == null)
                mTitle = "Position";

            if (networkType == null)
                networkType = intent.getStringExtra("networkType");
            if (mcc == null)
                mcc = intent.getStringExtra("mcc");
            if( mnc == null)
                mnc = intent.getStringExtra("mnc");
            if (areaCode ==null)
                areaCode = intent.getStringExtra("areaCode");
            if(cellId == null)
                cellId = intent.getStringExtra("cellId");

            mTitle = intent.getStringExtra("sender");
            if (mTitle == null)
                mTitle = "Position";

            ((TextView)findViewById(R.id.displaylocation_network)).setText("Network: " + networkType);
            ((TextView)findViewById(R.id.displaylocation_mcc)).setText("MCC: " + mcc);
            ((TextView)findViewById(R.id.displaylocation_mnc)).setText("MNC: " + mnc);
            ((TextView)findViewById(R.id.displaylocation_area)).setText("Area: " + areaCode);
            ((TextView)findViewById(R.id.displaylocation_cell)).setText("CellId: " + cellId);
            ((Button)findViewById(R.id.button_openmaps)).setEnabled(false);

            new DeterminateLocationAsyncTask(getLocationProvider(), this).execute(networkType, mcc, mnc, areaCode, cellId);
        }
        setResult(RESULT_OK);
    }

    void showMap(@NonNull IDetermineLocationProvider.GeoLocation location) {
        ((TextView)findViewById(R.id.displaylocation_latitude)).setText("Latitude: " + location.latitude);
        ((TextView)findViewById(R.id.displaylocation_longitude)).setText("Longitude: " + location.longitude);


        if (useGoogleMapsWeb()) {
            final Uri gmmIntentUri = Uri.parse(locationToMapsURL(Double.toString(location.latitude), Double.toString(location.longitude), location.radius, mTitle));
            // embed a WebView with Google Maps
            //WebView webView = (WebView)findViewById(R.id.web_view);
            //WebSettings webSettings = webView.getSettings();
            //webSettings.setJavaScriptEnabled(true);
            //webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            //webView.loadUrl(gmmIntentUri.toString());
            // TODO: geht nicht

            ((Button)findViewById(R.id.button_openmaps)).setEnabled(true);
            ((Button)findViewById(R.id.button_openmaps)).setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setData(gmmIntentUri);
                    //mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null)
                        startActivity(mapIntent);
                    else
                        Toast.makeText(getApplicationContext(), getString(R.string.error_no_maps), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // use own view (allows to display ads)
            // add Maps fragment or view, see https://developers.google.com/maps/documentation/android-sdk/map
            //MapView mapView = (MapView) findViewById(R.id.map_view);
            //mapView.getMapAsync(this);

            // mTitle = location.radius;
            positionMap(location.latitude, location.longitude);
        }
        // exit me (as Google Maps opended)
        //finish();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if (mPosition != null)
            mMap.addMarker(new MarkerOptions()
                .position(mPosition).title(mTitle));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mPosition,15)); // zoom to location
    }

    /**
     * Either {@link #onMapReady(GoogleMap)} or this method is called first.
     * @param latitude
     * @param longitude
     */
    public void positionMap(double latitude, double longitude) {
        mPosition = new LatLng(latitude,longitude);
        if (mMap != null)
            onMapReady(mMap);
    }


    protected void checkMyPermissions() {
        List<String> missingPermissions = new ArrayList<String>(5);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(android.Manifest.permission.INTERNET);
        if (missingPermissions.size() > 0) {
            // request missing permissions
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), 0);
        }
    }

    private static boolean useGoogleMapsWeb() {
        return true; // Google Maps ist noch kostenlos - alles andere (integriertes Maps) kosten nun GELD!
    }

    private static String locationToMapsURL(String latitude, String longitude, double radius,  @Nullable String label) {
        //return "https://www.google.com/maps/@?api=1&map_action=map&zoom=14&query="+latitude + "," + longitude + "?ll="+latitude+","+longitude+"&center="+latitude+","+longitude
        //        + (label != null ? ("("+label+")") : "");
        return "geo:"+latitude+","+longitude+"?q="+latitude+","+longitude+"&z=13("+label+")";
    }

    private IDetermineLocationProvider getLocationProvider() {
        String api = PreferenceManager.getDefaultSharedPreferences(this).getString(Prefs.LOCATION_API, "OpenCellID");
        if ( api.equals("Google"))
            return new DetermineLocationProviderGoogle();
        else
            return new DetermineLocationProviderCache(this, new DetermineLocationProviderOpenCellId());
    }
}
