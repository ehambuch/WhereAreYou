package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Determinate location by using different algorithms.
 * <p>
 * <p>
 * Ideas for improvement:
 * <ol>
 * <li>Check if mobile phone is moving through cells</li>
 * <li>Interpoliation of different cell information</li>
 * <li>Request a location update from the providers</li>
 * </ol>
 * </p>
 *
 * @author Eric Hambuch
 * @link https://developer.android.com/guide/topics/location/strategies.html
 */

public class MyLocationManager {

    /**
     * Time frame in which we treat two locations as similar (5 minutes).
     */
    private static final long ACTUALITY = 5 * 60 * 1000;

    private final Context context;

    public MyLocationManager(Context c) {
        this.context = c;
    }

    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION})
    @Nullable
    public Location getGPSCoordinates() {
        Location location = null;
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            // just take what is known and do not wait for any GPS fixing etc.
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        return location;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Nullable
    public Location getNetworkIPLocation() {
        Location location = null;
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            // check if internet available
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if ( connectivityManager == null )
                return null;
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        return location;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION})
    @Nullable
    public Location getPassiveLocation() {
        Location location = null;
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if ( locationManager == null )
                return null;
            location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        return location;
    }

    /**
     * Another way to retrieve last location from Google Play ("Fuse service")
     *
     * @return location
     * @link https://developer.android.com/training/location/retrieve-current.html
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION})
    @Nullable
    public Location getGooglePlayLocation() {
        Location location = null;
        GoogleApiClient clientAPI = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();
        if (clientAPI != null && (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            try {
                clientAPI.connect();
                location = LocationServices.FusedLocationApi.getLastLocation(clientAPI);
            } finally {
                clientAPI.disconnect();
            }
        }
        return location;
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_COARSE_LOCATION})
    @Nullable
    public Location getNetworkCellLocation() {
        CellLocation location = null;
        // write Cell ID
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            TelephonyManager manager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (manager != null) {
                List<CellInfo> cells = manager.getAllCellInfo(); // requires permission ACCESS_COARSE_LOCATION
                if (cells != null && cells.size() > 0) {
                    int countryCode = -1;
                    int networkCode = -1;
                    int areaCode = -1;
                    int cellInfo = -1;
                    // TODO: We can receive multiple cells and also multiple registered cells (e.g. on dual phones). To get the nearest tower, we need other checks, e.g. on the signal strength
                    int signalStrength = 0;
                    int timingAdvance = Integer.MAX_VALUE;
                    whereareyou.hambuch.de.whereareyou.CellLocation.NetworkType cellType = null;
                    for (CellInfo info : cells) {
                        if (info.isRegistered()) {
                            if (info instanceof CellInfoGsm) {
                                countryCode = ((CellInfoGsm) info).getCellIdentity()
                                        .getMcc();
                                networkCode = ((CellInfoGsm) info).getCellIdentity()
                                        .getMnc();
                                areaCode = ((CellInfoGsm) info).getCellIdentity()
                                        .getLac();
                                cellInfo = ((CellInfoGsm) info).getCellIdentity()
                                        .getCid();
                                signalStrength = ((CellInfoGsm) info).getCellSignalStrength().getDbm();
                                timingAdvance = getTimingAdvance(info);
                                cellType = whereareyou.hambuch.de.whereareyou.CellLocation.NetworkType.GSM;
                            } else if (info instanceof CellInfoWcdma) {
                                countryCode = ((CellInfoWcdma) info).getCellIdentity()
                                        .getMcc();
                                networkCode = ((CellInfoWcdma) info).getCellIdentity()
                                        .getMnc();
                                areaCode = ((CellInfoWcdma) info).getCellIdentity()
                                        .getLac();
                                cellInfo = ((CellInfoWcdma) info).getCellIdentity()
                                        .getCid();
                                timingAdvance = getTimingAdvance(info);
                                cellType = whereareyou.hambuch.de.whereareyou.CellLocation.NetworkType.WCDMA;
                            } else if (info instanceof CellInfoCdma) {
                                cellInfo = ((CellInfoCdma) info).getCellIdentity()
                                        .getBasestationId();
                                networkCode = ((CellInfoCdma) info).getCellIdentity()
                                        .getNetworkId();
                                areaCode = ((CellInfoCdma) info).getCellIdentity()
                                        .getSystemId();
                                countryCode = Integer.MAX_VALUE;
                                timingAdvance = getTimingAdvance(info);
                                cellType = whereareyou.hambuch.de.whereareyou.CellLocation.NetworkType.CDMA;
                            } else if (info instanceof CellInfoLte) {
                                countryCode = ((CellInfoLte) info).getCellIdentity().getMcc();
                                networkCode = ((CellInfoLte) info).getCellIdentity().getMnc();
                                cellInfo = ((CellInfoLte) info).getCellIdentity().getCi();
                                areaCode = ((CellInfoLte) info).getCellIdentity().getTac();
                                cellType = whereareyou.hambuch.de.whereareyou.CellLocation.NetworkType.LTE;
                                timingAdvance = getTimingAdvance(info);
                            }
                            if (cellType != null) {
                                CellLocation newLocation = CellLocation.createCellLocation(countryCode, networkCode, areaCode, cellInfo, cellType, signalStrength, timingAdvance);
                                if ( location == null )
                                    location = newLocation;
                                else
                                    location.addOtherCellLocation(newLocation); // TODO: We can receive multiple cells ... no idea yet how to handle it correctly
                            }
                        }
                    }

                } else { // deprecated solution!
                    android.telephony.CellLocation cLocation = manager.getCellLocation();
                    if (cLocation != null && cLocation instanceof GsmCellLocation && manager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                        GsmCellLocation gsmLoc = (GsmCellLocation) cLocation;
                        String simOperator = manager.getSimOperator();
                        if (simOperator.length() >= 5) {
                            int mcc = Integer.valueOf(simOperator.substring(0, 3));
                            int mnc = Integer.valueOf(simOperator.substring(3));
                            location = CellLocation.createCellLocation(mcc, mnc, gsmLoc.getLac(), gsmLoc.getCid(), CellLocation.NetworkType.GSM, 0, Integer.MAX_VALUE);
                        }
                    }

                }
            }
        }
        return location;
    }

    private int getTimingAdvance(CellInfo cellInfo) {
        if ( Build.VERSION.SDK_INT >= 26 ) {
            if (cellInfo instanceof CellInfoGsm) {
                return ((CellInfoGsm) cellInfo).getCellSignalStrength().getTimingAdvance();
            } else if (cellInfo instanceof CellInfoWcdma) {
                return Integer.MAX_VALUE;
            } else if (cellInfo instanceof CellInfoLte) {
                return ((CellInfoLte) cellInfo).getCellSignalStrength().getTimingAdvance();
            }
        }
        // else unknown:
        return Integer.MAX_VALUE;
    }

    /**
     * Find out (by time) which is the latest location and sort everything by accuracy.
     *
     * @param locations list of locations, may contain nulls
     * @return the latest and best location or null
     */
    @Nullable
    public Location findBestLocation(Collection<Location> locations) {
        final List<Location> allLocations = new ArrayList<Location>(5);
        for (Location loc : locations) {
            if (loc != null)
                allLocations.add(loc);
        }

        // sort according to accuracy, lowest one first
        Collections.sort(allLocations, new Comparator<Location>() {
            public int compare(Location a, Location b) {
                return Float.compare(a.getAccuracy(),b.getAccuracy());
            }
        });

        long latestTime = -1;
        Location latestLocation = null;
        for (Location location : allLocations) {
            if (location.getTime() > latestTime + ACTUALITY) {
                latestLocation = location;
                latestTime = location.getTime();
            }
        }
        return latestLocation;
    }
}
