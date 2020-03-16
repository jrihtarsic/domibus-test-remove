package eu.domibus.core.util;

/**
 * @author Thomas Dussart
 * @since 3.3
 */

public class WarningUtil {

    public static String warnOutput(String message) {
        return "\n\n\n"+
        "**************** WARNING **************** WARNING **************** WARNING **************** \n"+
        message+"\n"+
        "*******************************************************************************************\n\n\n";
    }

}
