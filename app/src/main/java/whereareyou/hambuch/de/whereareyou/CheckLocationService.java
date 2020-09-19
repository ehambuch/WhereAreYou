package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Perform all (long running) activities in a separate thread, so we don't get ANR from the main thread.
 * Therefore we use an IntentService resp. starting with Android 9 we have to use a Foreground Service,
 * as due to Android restrictions and Google policy we cannot retrieve a location from the background.
 *
 * @author Eric Hambuch
 */
public class CheckLocationService extends IntentService {

    public static final String SERVICE_ORIGINATING_ADDRESS = "originaingAddress";

    public CheckLocationService() {
        super("WhereAreYouService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // inform user about incoming request
        Toast.makeText(this, getString(R.string.get_location_request), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        if (AppInfo.useForegroundService())
            stopForeground(true);
        super.onDestroy();
    }

    @Override
    @TargetApi(26)
    protected void onHandleIntent(Intent intent) {
        // Create the NotificationChannel, but only on API 26+ because we have to enfore a foreground service
        String channelId = "default";
        if (AppInfo.useForegroundService()) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(AppInfo.APP_NAME, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            channelId = AppInfo.APP_NAME;
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.get_location_request))
                .setContentText(getString(R.string.get_location_request))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true).build();

        if (AppInfo.useForegroundService())
            startForeground(1, notification);

        String originatingAddress = intent.getStringExtra(SERVICE_ORIGINATING_ADDRESS);
        checkLocation(originatingAddress);

        if (AppInfo.useForegroundService())
            stopForeground(true);
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
                ILocationToUrlConverter urlConverter = LocationUrlFactory.createConverter(l, getSharedPreferences(AppInfo.APP_NAME, MODE_PRIVATE));
                returnMsg = urlConverter.createURLfromLocation(l, "");
            } else
                returnMsg = getString(R.string.no_place);
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                sms.sendTextMessage(originatingAddress, null, returnMsg, null, null);
            } else {
                // sending an SMS via Intent does not require the SEND_SMS permission
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + originatingAddress));
                smsIntent.putExtra("sms_body", returnMsg);
                if (getPackageManager().resolveActivity(smsIntent, 0) != null)
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
}
