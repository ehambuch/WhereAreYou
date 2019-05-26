package whereareyou.hambuch.de.whereareyou;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * A cache for the location provider to avoid too much calls to the APIs during testing.
 */
public class DetermineLocationProviderCache implements IDetermineLocationProvider {

    private final Context context;
    private final IDetermineLocationProvider originalProvider;

    public DetermineLocationProviderCache(Context context, IDetermineLocationProvider originalProvider) {
        this.context = context;
        this.originalProvider = originalProvider;
    }

    @Nullable
    @Override
    public GeoLocation determinateGeoLocation(String networkType, String mcc, String mnc, String areaCode, String cellId) throws IOException {
        int hashCode = (networkType + mcc + mnc + areaCode + cellId).hashCode();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int storedHash = prefs.getInt("cache.location.hash", -1);
        if ( hashCode == storedHash ) {
            return new GeoLocation(prefs.getFloat("cache.location.longitude", 0), prefs.getFloat("cache.location.latitude", 0));
        } else {
            GeoLocation location = originalProvider.determinateGeoLocation(networkType, mcc, mnc, areaCode, cellId);
            if ( location != null ) {
                prefs.edit().putInt("cache.location.hash", hashCode).
                        putFloat("cache.location.longitude", (float) location.longitude).
                        putFloat("cache.location.latitude", (float) location.latitude).
                        apply();
            }
            return location;
        }
    }
}
