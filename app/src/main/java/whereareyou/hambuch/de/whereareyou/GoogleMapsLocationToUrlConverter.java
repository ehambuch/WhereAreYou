package whereareyou.hambuch.de.whereareyou;

import android.location.Location;

/**
 * This class converts a <code>Location</code> to a URL that can be
 * opened by the user to get a map.
 *
 * @author Eric Hambuch
 */

public class GoogleMapsLocationToUrlConverter implements ILocationToUrlConverter {

    public String createURLfromLocation(Location location, String label) {
        return "http://maps.google.com/?z=16&t=m&q=" + location.getLatitude() + "," + location.getLongitude();
    }
}
