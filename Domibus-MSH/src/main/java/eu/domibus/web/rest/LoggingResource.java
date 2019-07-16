package eu.domibus.web.rest;

import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.logging.LoggingException;
import eu.domibus.core.logging.LoggingService;
import eu.domibus.ext.rest.ErrorRO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.LoggingLevelRO;
import eu.domibus.web.rest.ro.LoggingLevelResultRO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * REST resource for setting or retrieving logging levels at runtime
 *
 * @author Catalin Enache
 * since 4.1
 */
@RestController
@RequestMapping(value = "/rest/logging")
public class LoggingResource {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(LoggingResource.class);

    @Autowired
    DomainCoreConverter domainConverter;

    @Autowired
    private LoggingService loggingService;


    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({LoggingException.class})
    public ErrorRO handleLoggingException(Exception ex) {
        LOG.error(ex.getMessage(), ex);
        return new ErrorRO(ex.getMessage());
    }

    /**
     * It will change the logging level for given name and sets to level desired
     *
     * @param request it contains logger name and level
     * @return response of the operation
     */
    @PostMapping(value = "/loglevel")
    public ResponseEntity<String> setLogLevel(@RequestBody LoggingLevelRO request) {
        final String name = request.getName();
        final String level = request.getLevel();

        //set log level on current server
        loggingService.setLoggingLevel(name, level);

        //signals to other servers in a cluster environment
        loggingService.signalSetLoggingLevel(name, level);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("Success while setting log level " + level + " for " + name);

    }

    @GetMapping(value = "/loglevel")
    public ResponseEntity<LoggingLevelResultRO> getLogLevel(@RequestParam(value = "loggerName", defaultValue = "eu.domibus", required = false) String loggerName,
                                                            @RequestParam(value = "showClasses", defaultValue = "false", required = false) boolean showClasses,
                                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                                            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                            @RequestParam(value = "orderBy", required = false) String column,
                                                            @RequestParam(value = "asc", defaultValue = "true") boolean asc) {


        final LoggingLevelResultRO resultRO = new LoggingLevelResultRO();
        List<LoggingLevelRO> loggingEntries = domainConverter.convert(loggingService.getLoggingLevel(loggerName, showClasses), LoggingLevelRO.class);

        int count = loggingEntries.size();
        int fromIndex = pageSize * page;
        int toIndex = fromIndex + pageSize;
        if (toIndex > count) {
            toIndex = count;
        }
        resultRO.setCount(count);
        resultRO.setLoggingEntries(new ArrayList<>(loggingEntries.subList(fromIndex, toIndex)));

        //add the filter
        HashMap<String, Object> filter = new HashMap<>();
        filter.put("loggerName", loggerName);
        filter.put("showClasses", showClasses);
        resultRO.setFilter(filter);

        resultRO.setPage(page);
        resultRO.setPageSize(pageSize);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resultRO);

    }

    /**
     * Reset the logging configuration to default
     *
     * @return string for success or error
     */
    @PostMapping(value = "/reset")
    public ResponseEntity<String> resetLogging() {

        //reset log level on current server
        loggingService.resetLogging();

        // signals to other servers in a cluster environment
        loggingService.signalResetLogging();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("Logging configuration was successfully reset.");
    }

}
