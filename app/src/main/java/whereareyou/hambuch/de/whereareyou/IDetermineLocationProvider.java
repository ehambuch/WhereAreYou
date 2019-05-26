package whereareyou.hambuch.de.whereareyou;

import android.support.annotation.Nullable;

import java.io.IOException;

public interface IDetermineLocationProvider {

    public static class GeoLocation {

        double longitude;
        double latitude;
        double radius = 0.0;

        public GeoLocation(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
        public GeoLocation(String longitude, String latitude) throws NumberFormatException {
            this.longitude = Double.valueOf(longitude);
            this.latitude = Double.valueOf(latitude);
        }
    }

    /**
     * Determinate geo coordinate from mobile network infos.
     * @param networkType the network
     * @param mcc the MCC
     * @param mnc the MNC
     * @param areaCode the Area code (or LAC)
     * @param cellId the Cell
     * @return the geo coordinates or null
     * @throws IOException on errors
     */
    public @Nullable GeoLocation determinateGeoLocation(String networkType, String mcc, String mnc, String areaCode, String cellId) throws IOException;

}
