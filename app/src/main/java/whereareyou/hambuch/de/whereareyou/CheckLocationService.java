package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Perform all (long running) activities in a separate thread, so we don't get ANR from the main thread.
 * Therefore we use an IntentService.
 *
 * @author Eric Hambuch
 */
public class CheckLocationService extends IntentService {

    public static final String SERVICE_ORIGINATING_ADDRESS = "originaingAddress";

    public CheckLocationService() {
        super("WhereAreYouService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String originatingAddress = intent.getStringExtra(SERVICE_ORIGINATING_ADDRESS);
        checkLocation(originatingAddress);
    }

    protected void checkLocation(String originatingAddress) {
        final SmsManager sms = SmsManager.getDefault();
        try {
            MyLocationManager locationManager = new MyLocationManager(getApplicationContext());
            List<Location> allLocations = new ArrayList<Location>(5);
            allLocations.add(locationManager.getGPSCoordinates()); // can be null!
            allLocations.add(locationManager.getNetworkIPLocation());
            allLocations.add(locationManager.getNetworkCellLocation());
            allLocations.add(locationManager.getGooglePlayLocation());
            allLocations.add(locationManager.getPassiveLocation());

            Location l = locationManager.findBestLocation(allLocations);

            // send SMS
            // from 2019 on we are not allowed to send SMS that way (Google Play policy)
            String returnMsg;
            if (l != null) {
                ILocationToUrlConverter urlConverter = LocationUrlFactory.createConverter(l, PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
                returnMsg = urlConverter.createURLfromLocation(l, "");
            } else
                returnMsg = getString(R.string.no_place);
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                sms.sendTextMessage(originatingAddress, null, returnMsg, null, null);
            } else {
                // sending an SMS via Intent does not require the SEND_SMS permission
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:"+originatingAddress));
                smsIntent.putExtra("sms_body", returnMsg);
                if ( getPackageManager().resolveActivity(smsIntent, 0) != null )
                    startActivity(smsIntent);
                else
                    Toast.makeText(getApplicationContext(), R.string.security_exception,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e(AppInfo.APP_NAME, "Security Exception smsReceiver" + e);
        } catch (Exception e) {
            Log.e(AppInfo.APP_NAME, "Exception smsReceiver" + e);
        }

    }

    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    private String getPhoneNumber(Context context) {
        TelephonyManager manager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            String phoneNumber = manager.getLine1Number();
            if (phoneNumber == null)
                phoneNumber = manager.getDeviceId(); // use IMEI
            return phoneNumber;
        } else
            return "";
    }
}
