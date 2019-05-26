package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class contains access to different API providers who contains a Location API that
 * allows to calculate geo coordinates from mobile network information.
 */
public class DetermineLocationProviderGoogle implements IDetermineLocationProvider {

    private static final String GOOGLE_API_URL = "https://www.googleapis.com/geolocation/v1/geolocate?key=";

    /**
     * Top secret API key. https://www.googleapis.com/geolocation/v1/geolocate?key=.
     * At least we should obfuscate the key and use ProGuard to hide the source code.
     * Another way to use the Android APIs is to code the meta-data in the manifest and limit the key to the android app.
     */
    private static final String API_KEY = "";


    /**
     * Use the Google GeoLocation API with JSON to perform the transformation between cell tower and geo location.
     *
     * @param networkType the network
     * @param mcc the MCC
     * @param mnc the MNC
     * @param areaCode the Area Code
     * @param cellId the Cell ID
     * @return may be null or coords
     * @throws IOException
     */
    @RequiresPermission(Manifest.permission.INTERNET)
    public GeoLocation determinateGeoLocation(String networkType, String mcc, String mnc, String areaCode, String cellId) throws IOException {
        // encoding POST data
        HttpsURLConnection httpConnection = null;
        BufferedReader reader = null;
        try {
            httpConnection = (HttpsURLConnection) new URL(GOOGLE_API_URL + decodeKey(API_KEY)).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);
            httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpConnection.setRequestProperty("User-Agent", "Android");
            httpConnection.connect();

            JSONObject request = new JSONObject().put("homeMobileCountryCode", mcc).put("homeMobileNetworkCode", mnc).put("radioType", networkType).put("considerIp", false).put("cellTowers",
                    new JSONArray().put(new JSONObject().put("homeMobileCountryCode", mcc).put("homeMobileNetworkCode", mnc).put("locationAreaCode", areaCode).put("cellId", cellId)));

            Writer writer = new OutputStreamWriter(httpConnection.getOutputStream(), Charset.forName("UTF-8"));
            writer.write(request.toString());
            writer.flush();
            writer.close();

            // now wait for response
            final int responseCode = httpConnection.getResponseCode();
            if ( responseCode >= 500 ) {
                throw new IOException("Error calling Google Location API: "+responseCode);
            }
            if (responseCode == 403) {
                throw new IOException("Google Location API usage limit exceeded: "+responseCode);
            }
            if ( responseCode == 404 ) {
                throw new IOException("Google Location API not available: "+responseCode);
            }

            reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), Charset.forName("UTF-8")));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            // we are expecting a JSON in any case
            JSONObject responseJSON = new JSONObject(stringBuilder.toString());
            if ( responseCode == 200 || responseCode == 201 ) {
                JSONObject locationJSON = responseJSON.getJSONObject("location");
                if (locationJSON != null) {
                    String latitude = String.valueOf(locationJSON.get("lat"));
                    String longitude = String.valueOf(locationJSON.get("lng"));
                    return new GeoLocation(longitude,latitude);
                } else
                    throw new IOException("Error calling Google GeoLocation API: no response");
            } else {
                Log.e("ERROR", "Error calling Google GeoLocation API "+stringBuilder.toString());
                JSONObject errorJSON = responseJSON.getJSONObject("error");
                if (errorJSON != null) {
                    throw new IOException("Error calling Google GeoLocation API: "+responseCode+": "+errorJSON.getString("message"));
                } else
                    throw new IOException("Error calling Google GeoLocation API: no error response");
            }
        } catch (JSONException e) {
            Log.e("ERROR", e.getMessage(), e);
            throw new IOException("Error parsing API result: "+e.getLocalizedMessage(), e);
        }
        finally {
            if (reader != null)
                reader.close();
            if ( httpConnection != null )
                httpConnection.disconnect();
        }
    }

    private static String decodeKey(String input) {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            sb.append(c);
        }
        return sb.toString();
    }
}
