package eu.domibus.web.rest;

import eu.domibus.core.logging.LoggingService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.LoggingLevelRO;
import eu.domibus.web.rest.ro.LoggingLevelResponseRO;
import eu.domibus.web.rest.ro.LoggingLevelResultRO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

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
    LoggingService loggingService;

    @PostMapping(value = "/loglevel")
    public ResponseEntity<LoggingLevelResponseRO> setLogLevel(@RequestBody LoggingLevelRO request) {

        LoggingLevelResponseRO loggingLevelResponseRO = loggingService.setLoggingLevel(request);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(javax.ws.rs.core.MediaType.APPLICATION_JSON))
                .body(loggingLevelResponseRO);
    }

    @GetMapping(value = "/loglevel")
    public LoggingLevelResultRO getLogLevel(@RequestParam(value = "loggerName", defaultValue = "eu.domibus", required = false) String loggerName,
                                            @RequestParam(value = "showClasses", defaultValue = "false", required = false) boolean showClasses,
                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                            @RequestParam(value = "orderBy", required = false) String column,
                                            @RequestParam(value = "asc", defaultValue = "true") boolean asc) {


        final LoggingLevelResultRO resultRO  = loggingService.getLoggingLevel(loggerName, showClasses, page, pageSize );

        //add the filter
        HashMap<String, Object> filter = new HashMap<>();
        filter.put("loggerName", loggerName);
        filter.put("showClasses", showClasses);
        resultRO.setFilter(filter);

        resultRO.setPage(page);
        resultRO.setPageSize(pageSize);
        return resultRO;

    }

}
