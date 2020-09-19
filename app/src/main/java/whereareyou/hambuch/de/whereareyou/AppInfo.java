package whereareyou.hambuch.de.whereareyou;

import android.os.Build;

/**
 * AppInfo for "WhereAreYou".
 *
 * @author Eric Hambuch (erichambuch@googlemail.com)
 *         <p>
 *             <ul></ul></li>
 *         V1.0 (11.02.2017): first version</li>
 *         <li>V1.1 (12.02.2017): support Android 6 permissions, German translation</li>
 *         <li>V1.2 (24.03.2017): OpenCellId.org service suspended, now uses Mozilla API</li>
 *         <li>V1.3 (4), 9.6.2018: Added AdMob, DSVGO</li>
 *         <li>V1.4 (5): 21.06.2018: Fehlerkorrekturen. </li>
 *         <li>V1.5 (6): 14.09.2018: AdMob, mehrere Sprachen, OpenCellID.org Nutzung</li>
 *         <li>V1.6 (7): 19.09.2020: Update Android 10</li>
 * @link https://developers.google.com/maps/documentation/geolocation/get-api-key#key
 * @link http://www.cell2gps.com/
 * @link http://opencellid.org/
 */

abstract class AppInfo {

    static final String APP_NAME = "WhereAreYou";

    /**
     * Starting with Android 8, Android and Google Play policy requires a Foreground service to be used
     * in case any location services are requested (due to user privacy).
     * @return true if Foreground service logic should be used
     */
    static boolean useForegroundService() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }
}
