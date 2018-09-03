package eu.domibus.web.rest;

import eu.domibus.api.audit.AuditLog;
import eu.domibus.api.csv.CsvException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.model.common.ModificationType;
import eu.domibus.common.services.AuditService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.csv.CsvCustomColumns;
import eu.domibus.core.csv.CsvExcludedItems;
import eu.domibus.core.csv.CsvService;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.criteria.AuditCriteria;
import eu.domibus.web.rest.ro.AuditResponseRo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Thomas Dussart
 * @since 4.0
 * <p>
 * Rest entry point to retrieve the audit logs.
 */
@RestController
@RequestMapping(value = "/rest/audit")
public class AuditResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AuditResource.class);

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private DomainCoreConverter domainConverter;

    @Autowired
    private AuditService auditService;

    @Autowired
    DateUtil dateUtil;

    @Autowired
    CsvServiceImpl csvServiceImpl;

    /**
     * Entry point of the Audit rest service to list the system audit logs.
     *
     * @param auditCriteria the audit criteria used to filter the returned list.
     * @return an audit list.
     */
    @RequestMapping(value = {"/list"}, method = RequestMethod.POST)
    public List<AuditResponseRo> listAudits(@RequestBody AuditCriteria auditCriteria) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Audit criteria received:");
            LOG.debug(auditCriteria.toString());
        }
        List<AuditLog> sourceList = auditService.listAudit(
                auditCriteria.getAuditTargetName(),
                changeActionType(auditCriteria.getAction()),
                auditCriteria.getUser(),
                auditCriteria.getFrom(),
                auditCriteria.getTo(),
                auditCriteria.getStart(),
                auditCriteria.getMax());

        return domainConverter.convert(sourceList, AuditResponseRo.class);
    }


    @RequestMapping(value = {"/count"}, method = RequestMethod.POST)
    public Long countAudits(@RequestBody AuditCriteria auditCriteria) {
        return auditService.countAudit(
                auditCriteria.getAuditTargetName(),
                changeActionType(auditCriteria.getAction()),
                auditCriteria.getUser(),
                auditCriteria.getFrom(),
                auditCriteria.getTo());
    }

    /**
     * Action type send from the admin console are different from the one used in the database.
     * Eg: In the admin console the filter for a modified entity is Modified where in the database a modified reccord
     * has the MOD flag. This method does the translation.
     *
     * @param actions
     * @return
     */
    private Set<String> changeActionType(Set<String> actions) {
        Set<String> modificationTypes = new HashSet<>();
        if(actions == null || actions.isEmpty()) {
            return modificationTypes;
        }
        actions.forEach(action -> {
            Set<String> collect = Arrays.stream(ModificationType.values()).
                    filter(modificationType -> modificationType.getLabel().equals(action)).
                    map(Enum::name).
                    collect(Collectors.toSet());
            modificationTypes.addAll(collect);
        });
        return modificationTypes;
    }

    @RequestMapping(value = {"/targets"}, method = RequestMethod.GET)
    public List<String> auditTargets() {
        return auditService.listAuditTarget();
    }

    /**
     * This method returns a CSV file with the contents of Audit table
     * @param auditTargetName the type of audit.
     * @param user the user that performed the audited action.
     * @param action the type of action linked with audit.
     * @param from the date from which we want to retrieve audit logs.
     * @param to the date to which we want to retrieve audit logs.
     *
     * @return CSV file with the contents of Audit table
     */
    @RequestMapping(path = "/csv", method = RequestMethod.GET)
    public ResponseEntity<String> getCsv(
            @RequestParam(value = "auditTargetName", required = false) Set<String> auditTargetName,
            @RequestParam(value = "user", required = false) Set<String> user,
            @RequestParam(value = "action", required = false) Set<String> action,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        String resultText;

        // get list of audits
        AuditCriteria auditCriteria = new AuditCriteria();
        auditCriteria.setAuditTargetName(auditTargetName);
        auditCriteria.setUser(user);
        auditCriteria.setAction(action);
        Date receivedFrom = dateUtil.fromString(from);
        auditCriteria.setFrom(receivedFrom);
        Date receivedTo = dateUtil.fromString(to);
        auditCriteria.setTo(receivedTo);
        auditCriteria.setStart(0);
        auditCriteria.setMax(csvServiceImpl.getMaxNumberRowsToExport());
        final List<AuditResponseRo> auditResponseRos = listAudits(auditCriteria);

        try {
            resultText = csvServiceImpl.exportToCSV(auditResponseRos, AuditResponseRo.class,
                    CsvCustomColumns.AUDIT_RESOURCE.getCustomColumns(), CsvExcludedItems.AUDIT_RESOURCE.getExcludedItems());
        } catch (CsvException e) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CsvService.APPLICATION_EXCEL_STR))
                .header("Content-Disposition", "attachment; filename=" + csvServiceImpl.getCsvFilename("audit"))
                .body(resultText);
    }
}
