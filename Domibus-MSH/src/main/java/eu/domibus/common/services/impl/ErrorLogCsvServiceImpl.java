package eu.domibus.common.services.impl;

import eu.domibus.common.ErrorCode;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * @author Tiago Miguel
 * @since 4.0
 */

@Service
public class ErrorLogCsvServiceImpl extends CsvServiceImpl {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ErrorLogCsvServiceImpl.class);


    @Override
    public String exportToCSV(List<?> list) throws EbMS3Exception {
        if(list == null || list.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        final Class<?> aClass = list.get(0).getClass();

        Field[] fields = aClass.getDeclaredFields();

        // Column Header
        for(Field field : fields) {
            final String varName = field.getName();
            result.append(uncamelcase(varName));
            result.append(",");
        }
        result.deleteCharAt(result.length() - 1);
        result.append(System.lineSeparator());

        // CSV contents
        for(Object elem : list) {
            for (Field field : fields) {
                String varName = field.getName();
                varName = varName.substring(0,1).toUpperCase() + varName.substring(1);
                try {
                    final Method getMethod = aClass.getMethod("get" + varName);
                    if(varName.equals("ErrorCode")) {
                        final Object invoke = getMethod.invoke(elem);
                        result.append(Objects.toString(((ErrorCode)invoke).getErrorCodeName(), ""));
                    } else {
                        result.append(Objects.toString(getMethod.invoke(elem), ""));
                    }
                    result.append(",");
                } catch (Exception e) {
                    LOG.error("Exception while writing on CSV ", e);
                    throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "Impossible to export as CSV", null, e);
                }
            }
            result.deleteCharAt(result.length() - 1);
            result.append(System.lineSeparator());
        }

        return result.toString();
    }
}
