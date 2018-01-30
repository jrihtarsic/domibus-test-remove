package eu.domibus.common.services.impl;

import eu.domibus.api.csv.CsvException;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.common.ErrorCode;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.ErrorLogRO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * @author Tiago Miguel
 * @since 4.0
 */

@Service
public class ErrorLogCsvServiceImpl extends CsvServiceImpl<ErrorLogRO> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ErrorLogCsvServiceImpl.class);

    @Override
    public void createCSVContents(List<?> list, StringBuilder result, Field[] fields) {
        for(Object elem : list) {
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object varResult = field.get(elem);
                    // special case for ErrorCode
                    if(field.getName().equals("errorCode")) {
                        varResult = ((ErrorCode)varResult).getErrorCodeName();
                    }
                    String fieldValue = Objects.toString(varResult, StringUtils.EMPTY);
                    if(fieldValue.contains(COMMA)) {
                        fieldValue = DOUBLE_QUOTES + fieldValue + DOUBLE_QUOTES;
                    }
                    result.append(fieldValue);
                    result.append(COMMA);
                } catch (IllegalAccessException e) {
                    LOG.error("Exception while writing on CSV ", e);
                    throw new CsvException(DomibusCoreErrorCode.DOM_001, "Exception while writing on CSV", e);
                }
            }
            result.deleteCharAt(result.length() - 1);
            result.append(System.lineSeparator());
        }
    }

    @Override
    public void setExcludedItems(List<String> excludedItems) {
    }
}
