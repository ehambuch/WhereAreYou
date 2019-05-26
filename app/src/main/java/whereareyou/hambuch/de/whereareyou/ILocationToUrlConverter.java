package whereareyou.hambuch.de.whereareyou;

import android.location.Location;

/**
 * Interface for different locaters.
 */

public interface ILocationToUrlConverter {
    public String createURLfromLocation(Location location, String label);
}
