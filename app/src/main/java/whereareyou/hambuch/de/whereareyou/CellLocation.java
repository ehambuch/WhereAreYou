package whereareyou.hambuch.de.whereareyou;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class holding cell informations.
 *
 * @author Eric Hambuch
 */

public class CellLocation extends Location {

    public enum NetworkType {GSM, CDMA, WCDMA, LTE}

    public static final String CELL_LOCATION = "CELL_LOCATION";

    /**
     * Country code.
     */
    private int mMcc;

    /**
     * Network code.
     */
    private int mMnc;

    /**
     * Area code.
     */
    private int mAc;

    /**
     * Cell Id.
     */
    private int mCellId;

    /**
     * Type of network.
     */
    private NetworkType mNetworkType;

    /**
     * Timing value (may be unavaile).
     */
    private int mTimingAdvance;

    private List<CellLocation> mLocations = new ArrayList<CellLocation>(5);

    /**
     * Constructor.
     *
     * @param provider
     */
    protected CellLocation(String provider) {
        super(provider);
    }

    public static CellLocation createCellLocation(int mcc, int mnc, int ac, int cellid, NetworkType networkType, int signalStrengh, int timingAdvance) {
        CellLocation location = new CellLocation(CELL_LOCATION);
        location.setTime(System.currentTimeMillis());
        location.setAccuracy(1000.0F); // ca. 1000m exactly or not, depends on much factors as network, cell size etc.
        location.mMcc = mcc;
        location.mMnc = mnc;
        location.mAc = ac;
        location.mCellId = cellid;
        location.mNetworkType = networkType;
        location.mTimingAdvance = timingAdvance;
        return location;
    }

    public int getMcc() {
        return mMcc;
    }

    public int getMnc() {
        return mMnc;
    }

    public int getAreaCode() {
        return mAc;
    }

    public int getCellId() {
        return mCellId;
    }

    public NetworkType getNetworkType() {
        return mNetworkType;
    }

    public int getTimingAdvance() {
        return mTimingAdvance;
    }

    public boolean isTimingAdvanceValid() {
        return mTimingAdvance != Integer.MAX_VALUE;
    }

    public void addOtherCellLocation(CellLocation location) {
        mLocations.add(location);
    }

    public List<CellLocation> getOtherCellLocations() {
        return mLocations;
    }
}
