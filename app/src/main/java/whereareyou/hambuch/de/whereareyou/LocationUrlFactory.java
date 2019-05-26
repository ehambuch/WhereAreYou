package whereareyou.hambuch.de.whereareyou;

import android.content.SharedPreferences;
import android.location.Location;

/**
 * Factory that creates the correct Location To URL converter.
 */

public class LocationUrlFactory {

    private LocationUrlFactory() {
        // empty
    }

    public static ILocationToUrlConverter createConverter(Location location, SharedPreferences preferences) {
        if ( location instanceof CellLocation )
            return new CellTowerToUrlConverter();
        else
            return new GoogleMapsLocationToUrlConverter();
    }
}
