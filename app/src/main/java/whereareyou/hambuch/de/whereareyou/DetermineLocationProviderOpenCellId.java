package whereareyou.hambuch.de.whereareyou;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Use the API of OpenCellID to determine geo coordinates from mobile network.
 *
 * @link www.opencellid.org
 */
public class DetermineLocationProviderOpenCellId implements IDetermineLocationProvider {

    @Nullable
    @Override
    public GeoLocation determinateGeoLocation(String networkType, String mcc, String mnc, String areaCode, String cellId) throws IOException {
        HttpsURLConnection httpConnection = null;
        try {
            String url = "https://www.opencellid.org/ajax/searchCell.php?mcc=" + mcc + "&mnc=" + mnc + "&lac=" + areaCode + "&cell_id=" + cellId;
            httpConnection = (HttpsURLConnection) new URL(url).openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setDoOutput(true);
            httpConnection.connect();
            // now wait for response
            final int responseCode = httpConnection.getResponseCode();
            if (responseCode >= 500) {
                throw new IOException("Error calling OpenCellID API: " + responseCode);
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            Log.i(AppInfo.APP_NAME, "OpenCellID API response: "+stringBuilder.toString());
            // we are expecting a JSON in any case
            if ( stringBuilder.toString().startsWith("false"))
                throw new IOException("The OpenCellID API is not aware of the location");
            JSONObject responseJSON = new JSONObject(stringBuilder.toString());
            if (responseCode == 200 || responseCode == 201) {
                if (responseJSON != null) {
                    String latitude = String.valueOf(responseJSON.get("lat"));
                    String longitude = String.valueOf(responseJSON.get("lon"));
                    String range = String.valueOf(responseJSON.get("range"));
                    GeoLocation locationResult =  new GeoLocation(longitude, latitude);
                    locationResult.radius = Double.parseDouble(range);
                    return locationResult;
                } else
                    throw new IOException("Error calling OpenCellID API: no response");
            } else {
                Log.e("ERROR", "Error calling OpenCellID API " + stringBuilder.toString());
                JSONObject errorJSON = responseJSON.getJSONObject("error");
                if (errorJSON != null) {
                    throw new IOException("Error calling OpenCellID API: " + responseCode + ": " + errorJSON.getString("message"));
                } else
                    throw new IOException("Error calling OpenCellID API: no error response");
            }
        }
        catch(JSONException e) {
            Log.e(AppInfo.APP_NAME, "JSON", e);
            throw new IOException("Error talking to OpenCellID API: "+e.getLocalizedMessage(), e);
        }
        finally {
            if ( httpConnection != null)
                httpConnection.connect();
        }
    }
}
