package whereareyou.hambuch.de.whereareyou;

import android.location.Location;

/**
 * Converter that transfers a cell tower information to a URI that calls our own application.
 */

public class CellTowerToUrlConverter implements ILocationToUrlConverter {

    @Override
    public String createURLfromLocation(Location location, String label) {
        CellLocation cell = ((CellLocation)location);
        return "http://whereareyou/l?networkType="+ convertNetworkType(cell.getNetworkType()) +"&mcc=" + cell.getMcc() + "&mnc=" + cell.getMnc() + "&areaCode=" + cell.getAreaCode() + "&cellId=" + cell.getCellId();
    }

    private String convertNetworkType(CellLocation.NetworkType type) {
        switch(type) {
            case LTE:
                return "lte";
            case GSM:
                return "gsm";
            case WCDMA:
                return "wcdma";
            case CDMA:
                return "cdma";
            default:
                return "";
        }
    }
}
