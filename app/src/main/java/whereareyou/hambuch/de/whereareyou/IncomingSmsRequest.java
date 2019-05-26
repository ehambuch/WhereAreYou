package whereareyou.hambuch.de.whereareyou;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.annotation.RequiresPermission;
import android.telephony.SmsMessage;


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
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Prefs.ON_OFF, true);
            if (!enabled)
                return;

            String magicWords = PreferenceManager.getDefaultSharedPreferences(context).getString(Prefs.MAGIC_WORDS,
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
    private void handleOldSMSHandler(Context context, Intent intent, String magicWords) {
        SmsMessage currentMessages[] = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage currentMessage : currentMessages) {
            String msg = currentMessage.getDisplayMessageBody().toLowerCase();
            if (msg.startsWith(magicWords)) {
                // start activity as a service
                Intent startIntent = new Intent(context, CheckLocationService.class);
                startIntent.putExtra(CheckLocationService.SERVICE_ORIGINATING_ADDRESS, currentMessage.getOriginatingAddress());
                context.startService(startIntent);
            }

            if (msg.startsWith("http://whereareyou/")) {
                // start activity as a service
                startLocationService(context, msg, currentMessage.getDisplayOriginatingAddress());
            }
        }
    }

    private void startLocationService(Context context, String msg, String phoneAdr) {
        Intent startIntent = new Intent(context, DisplayLocationActivity.class);
        Uri uri = Uri.parse(msg);
        startIntent.setAction(Intent.ACTION_VIEW);
        startIntent.setPackage(context.getPackageName());
        startIntent.setData(uri);
        String networkType = uri.getQueryParameter("networkType");
        String mcc = uri.getQueryParameter("mcc");
        String mnc = uri.getQueryParameter("mnc");
        String areaCode = uri.getQueryParameter("areaCode");
        String cellId = uri.getQueryParameter("cellId");
        startIntent.putExtra("sender", phoneAdr);
        startIntent.putExtra("networkType", networkType);
        startIntent.putExtra("mcc", mcc);
        startIntent.putExtra("mnc", mnc);
        startIntent.putExtra("areaCode", areaCode);
        startIntent.putExtra("cellId", cellId);
        context.startActivity(startIntent);
    }
}
