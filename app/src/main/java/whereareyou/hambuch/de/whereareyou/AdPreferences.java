package whereareyou.hambuch.de.whereareyou;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
 * Preferences element that allows to embed an Admob ad.
 */
public class AdPreferences extends Preference {

    public AdPreferences(Context context, AttributeSet attrs, int defStyle) {
        super    (context, attrs, defStyle);
    }

    public AdPreferences(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdPreferences(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        parent.setPadding(0, 0, 0, 0);
        View view = super.onCreateView(parent);  // deflates ad_layout

        final AdView adView = view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().
                addTestDevice("D485D71F04DA4B5772EAE7F2605149C9").
                addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build(); // my S8
        adView.loadAd(adRequest);

        return view;
    }
}