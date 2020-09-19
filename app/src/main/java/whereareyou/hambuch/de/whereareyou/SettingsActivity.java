package whereareyou.hambuch.de.whereareyou;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object value) {
            if (Prefs.ON_OFF.equals(preference.getKey())) {
                if ( Boolean.TRUE.equals(value) ) {
                    // show general warning
                    final Context context = preference.getContext();
                    final AlertDialog.OnClickListener clickListener = new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    };
                    final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                    alertDialog.setTitle(context.getString(R.string.app_name));
                    alertDialog.setMessage(context.getString(R.string.warning_location));
                    alertDialog.setIcon(R.drawable.ic_info_black_24dp);
                    alertDialog.setCancelable(true);
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, context.getString(R.string.button_label_okay), clickListener);
                    alertDialog.show();
                }
                // accept value
                return true;
            } else if (Prefs.MAGIC_WORDS.equals(preference.getKey())) {
                if ( value == null || value.toString().length() <= 1 )
                    return false;
                // accept value
                return true;
            }
            return false;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, getString(R.string.admob_appid));

        setupActionBar();

        // replace settings by fragment...
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
    }


    @Override
    public void onStart() {
        super.onStart();
        checkMyPermissions();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
   /* @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }
*/

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            MobileAds.initialize(getActivity().getApplicationContext(), getString(R.string.admob_appid));

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            Preference onOffPreference = findPreference(Prefs.ON_OFF);
            onOffPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(onOffPreference,
                    PreferenceManager
                            .getDefaultSharedPreferences(onOffPreference.getContext())
                            .getBoolean(onOffPreference.getKey(), true));

            Preference magicWordsPreference = findPreference(Prefs.MAGIC_WORDS);
            onOffPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(magicWordsPreference,
                    PreferenceManager
                            .getDefaultSharedPreferences(onOffPreference.getContext())
                            .getString(magicWordsPreference.getKey(),
                                    onOffPreference.getContext().getString(R.string.pref_default_magic_word)));


        }
    }

    /**
     * Simple activity that show a "how to".
     */
    public static class HowToActivity extends Activity {

        @Override
        public void onStart() {
            super.onStart();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.app_name));
            builder.setMessage(Html.fromHtml(getString(R.string.howto_text)));
            builder.setIcon(R.drawable.ic_info_black_24dp);
            builder.setPositiveButton(R.string.button_label_okay, null);
            builder.setCancelable(true);
            AlertDialog dialog = builder.create();
            dialog.show();
            TextView view = (TextView)dialog.findViewById(android.R.id.message);
            if (view != null ) view.setMovementMethod(LinkMovementMethod.getInstance()); // make links clickable
            setResult(RESULT_OK);
        }
    }

    /**
     * Simple activity that sends out a SMS with the magic words.
     */
    public static class SendSmsActivity extends Activity {

        @Override
        public void onCreate(Bundle budle) {
            super.onCreate(budle);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
               ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SEND_SMS}, 456);
        }

        @Override
        public void onStart() {
            super.onStart();

            // to pick a contact, check out: https://developer.android.com/guide/components/intents-common#PickContact
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(ContactsContract.Contacts.CONTENT_URI, ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, 123);
            }
            setResult(RESULT_OK);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 123 && resultCode == RESULT_OK) {
                final SmsManager smsManager = SmsManager.getDefault();
                final String magicWords = getSharedPreferences(AppInfo.APP_NAME, MODE_PRIVATE).
                        getString(Prefs.MAGIC_WORDS, getString(R.string.pref_default_magic_word));
                // Get the URI and query the content provider for the phone number
                Uri contactUri = data.getData();
                if (contactUri != null ) {
                    String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                    Cursor cursor = null;
                    String number = null;
                    try {
                        cursor = getContentResolver().query(contactUri, projection,
                                null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            number = cursor.getString(numberIndex);
                        }
                    }
                    finally {
                        if (cursor != null)
                            cursor.close();
                    }

                    if ( number != null ) {
                        // Starting from 2019 we are not allowed to send SMS due to new Google Play policies
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                            smsManager.sendTextMessage(number, null, magicWords, null, null);
                            Toast.makeText(this.getApplicationContext(), R.string.message_sms_sent, Toast.LENGTH_SHORT ).show();
                        } else {
                            // half-automated way: user has finally to send SMS
                            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                            smsIntent.setData(Uri.parse("smsto:"+number));
                            smsIntent.putExtra("sms_body", magicWords);
                            startActivity(smsIntent);
                        }
                    }
                }
            } else if (requestCode == 456) {
                recreate(); // got permissions
            } else if ( resultCode != RESULT_CANCELED )
                super.onActivityResult(requestCode, resultCode, data);
            finish(); // ensure activity is finished and not called again
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void checkMyPermissions() {
        List<String> missingPermissions = new ArrayList<String>(5);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.SEND_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.RECEIVE_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.READ_SMS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        if (missingPermissions.size() > 0) {
            // request missing permissions
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), 0);
        }
    }
}
