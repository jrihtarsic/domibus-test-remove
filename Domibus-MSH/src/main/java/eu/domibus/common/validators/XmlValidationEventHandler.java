package eu.domibus.common.validators;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 @author Federico Martini
 @since 3.1
 <br> Class to handle and customize the validation events triggered during XML parsing.
 */
public class XmlValidationEventHandler implements ValidationEventHandler {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(XmlValidationEventHandler.class);

    protected List<String> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getErrorMessage() {
        return StringUtils.join(errors, "\n");
    }

    public boolean handleEvent(ValidationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("No event found!");
        }

        String severity = null;
        boolean retVal = false;
        switch (event.getSeverity()) {
            case ValidationEvent.WARNING:
                severity = "WARNING";
                retVal = true; // continue after warnings
                break;
            case ValidationEvent.ERROR:
                severity = "ERROR";
                retVal = true; // continue after errors because we want to show all errors together
                break;
            case ValidationEvent.FATAL_ERROR:
                severity = "FATAL_ERROR";
                retVal = true; // continue after fatal errors because we want to show all errors together
                break;
            default:
                severity = "UNRECOGNIZED_SEVERITY";
        }

        String location = getLocation(event);
        String errorMessage = "[" + severity + "] is [" + event.getMessage() + "] at [" + location + "]";
        errors.add(errorMessage);

        LOG.debug(errorMessage);

        return retVal;
    }

    public List<String> getErrors() {
        return errors;
    }

    /**
     * Calculates a location message for the event
     */
    private String getLocation(ValidationEvent event) {
        StringBuffer msg = new StringBuffer();

        ValidationEventLocator locator = event.getLocator();

        if (locator != null) {

            URL url = locator.getURL();
            Object obj = locator.getObject();
            Node node = locator.getNode();
            int line = locator.getLineNumber();

            if (url != null || line != -1) {
                msg.append("line " + line);
                if (url != null)
                    msg.append(" of " + url);
            } else if (obj != null) {
                msg.append(" obj: " + obj.toString());
            } else if (node != null) {
                msg.append(" node: " + node.toString());
            }
        } else {
            msg.append("LOCATION_UNAVAILABLE");
        }

        return msg.toString();
    }
}
