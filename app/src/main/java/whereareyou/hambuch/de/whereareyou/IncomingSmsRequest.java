package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.annotation.RequiresPermission;


/**
 * Main class that receives SMS and handles all logic.
 *
 * @author Eric Hambuch
 */

public class IncomingSmsRequest extends BroadcastReceiver {

    // IMPORTANT NOTE: do not store any member large variables (esp. objects) here, as they may be cached as data and therefore drain memory!!!

    @Override
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS})
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            boolean enabled = context.getSharedPreferences(AppInfo.APP_NAME, Context.MODE_PRIVATE).getBoolean(Prefs.ON_OFF, true);
            if (!enabled)
                return;

            String magicWords = context.getSharedPreferences(AppInfo.APP_NAME, Context.MODE_PRIVATE).getString(Prefs.MAGIC_WORDS,
                    context.getString(R.string.pref_default_magic_word)).toLowerCase();

            handleOldSMSHandler(context, intent, magicWords);
        }
    }

    /**
     * This works only up to Android 8 and the change of the Google Play policy (which does not allow READ_SMS and RECEIVE_SMS anymore)
     *
     * @param context
     * @param intent
     * @param magicWords
     */
    @RequiresPermission(allOf = {Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS})
    @Deprecated
    @TargetApi(26)
    private void handleOldSMSHandler(Context context, Intent intent, String magicWords) {
        SmsMessage currentMessages[] = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage currentMessage : currentMessages) {
            String msg = currentMessage.getDisplayMessageBody().toLowerCase();
            if (msg.startsWith(magicWords)) {
                // start activity as a service
                Intent startIntent = new Intent(context, CheckLocationService.class);
                startIntent.putExtra(CheckLocationService.SERVICE_ORIGINATING_ADDRESS, currentMessage.getOriginatingAddress());
                if (AppInfo.useForegroundService())  // starting with Android 8 we have to perform a ForeGround Service due to Google policy
                    context.startForegroundService(startIntent);
                else
                    context.startService(startIntent);
            }

            if (msg.startsWith("http://whereareyou/")) {
                // start activity as a service
                startLocationService(context, msg, currentMessage.getDisplayOriginatingAddress());
            }
        }
    }

    /**
     * Start own display to display the retrieved location (e.g. using Google Maps).
     * @param context
     * @param msg
     * @param phoneAdr
     */
    private void startLocationService(Context context, String msg, String phoneAdr) {
        Intent startIntent = new Intent(context, DisplayLocationActivity.class);
        Uri uri = Uri.parse(msg);
        startIntent.setAction(Intent.ACTION_VIEW);
        startIntent.setPackage(context.getPackageName());
        startIntent.setData(uri);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String networkType = uri.getQueryParameter(DisplayLocationActivity.EXTRA_NETWORK);
        String mcc = uri.getQueryParameter(DisplayLocationActivity.EXTRA_MCC);
        String mnc = uri.getQueryParameter(DisplayLocationActivity.EXTRA_MNC);
        String areaCode = uri.getQueryParameter(DisplayLocationActivity.EXTRA_AREACODE);
        String cellId = uri.getQueryParameter(DisplayLocationActivity.EXTRA_CELLID);
        startIntent.putExtra(DisplayLocationActivity.EXTRA_SENDER, phoneAdr);
        startIntent.putExtra(DisplayLocationActivity.EXTRA_NETWORK, networkType);
        startIntent.putExtra(DisplayLocationActivity.EXTRA_MCC, mcc);
        startIntent.putExtra(DisplayLocationActivity.EXTRA_MNC, mnc);
        startIntent.putExtra(DisplayLocationActivity.EXTRA_AREACODE, areaCode);
        startIntent.putExtra(DisplayLocationActivity.EXTRA_CELLID, cellId);
        context.startActivity(startIntent);
    }
}
