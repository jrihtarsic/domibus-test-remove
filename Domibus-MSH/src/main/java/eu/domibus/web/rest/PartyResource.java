package eu.domibus.web.rest;

import com.google.common.collect.Lists;
import eu.domibus.api.csv.CsvException;
import eu.domibus.api.party.Party;
import eu.domibus.api.party.PartyService;
import eu.domibus.common.services.CsvService;
import eu.domibus.common.services.impl.CsvServiceImpl;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.party.IdentifierRo;
import eu.domibus.core.party.PartyProcessLinkRo;
import eu.domibus.core.party.PartyResponseRo;
import eu.domibus.core.party.ProcessRo;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.PModePartiesRequestRO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@RestController
@RequestMapping(value = "/rest/party")
public class PartyResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyResource.class);

    @Autowired
    private DomainCoreConverter domainConverter;

    @Autowired
    private PartyService partyService;

    @Autowired
    private CsvServiceImpl csvServiceImpl;

    @RequestMapping(value = {"/list"}, method = RequestMethod.GET)
    public List<PartyResponseRo> listParties(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "endPoint", required = false) String endPoint,
            @RequestParam(value = "partyId", required = false) String partyId,
            @RequestParam(value = "process", required = false) String process,
            @RequestParam(value = "pageStart", defaultValue = "0") int pageStart,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching party with parameters");
            LOG.debug("name [{}]", name);
            LOG.debug("endPoint [{}]", endPoint);
            LOG.debug("partyId [{}]", partyId);
            LOG.debug("processName [{}]", process);
            LOG.debug("pageStart [{}]", pageStart);
            LOG.debug("pageSize [{}]", pageSize);
        }
        List<PartyResponseRo> partyResponseRos = domainConverter.convert(
                partyService.getParties(
                        name,
                        endPoint,
                        partyId,
                        process,
                        pageStart,
                        pageSize),
                PartyResponseRo.class);


        flattenIdentifiers(partyResponseRos);

        flattenProcesses(partyResponseRos);



        partyResponseRos.forEach(partyResponseRo -> {
            final List<ProcessRo> processesWithPartyAsInitiator = partyResponseRo
                    .getProcessesWithPartyAsInitiator();
            final List<ProcessRo> processesWithPartyAsResponder = partyResponseRo.getProcessesWithPartyAsResponder();

            final Set<ProcessRo> processRos = new HashSet<>(processesWithPartyAsInitiator);
            processRos
                    .addAll(processesWithPartyAsResponder);

            processRos
                    .stream()
                    .map(item->new PartyProcessLinkRo(item.getName(),processesWithPartyAsInitiator.contains(item),processesWithPartyAsResponder.contains(item)))
                    .collect(Collectors.toSet());
        });

        return partyResponseRos;
    }

    @RequestMapping(value = {"/count"}, method = RequestMethod.GET)
    public long countParties(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "endPoint", required = false) String endPoint,
            @RequestParam(value = "partyId", required = false) String partyId,
            @RequestParam(value = "process", required = false) String process
    ) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Counting party with parameters");
            LOG.debug("name [{}]", name);
            LOG.debug("endPoint [{}]", endPoint);
            LOG.debug("partyId [{}]", partyId);
            LOG.debug("processName [{}]", process);
        }
        return partyService.countParties(
                name,
                endPoint,
                partyId,
                process
        );
    }

    /**
     * This method returns a CSV file with the contents of Party table
     *
     * @return CSV file with the contents of Party table
     */
    @RequestMapping(path = "/csv", method = RequestMethod.GET)
    public ResponseEntity<String> getCsv(@RequestParam(value = "name", required = false) String name,
                                         @RequestParam(value = "endPoint", required = false) String endPoint,
                                         @RequestParam(value = "partyId", required = false) String partyId,
                                         @RequestParam(value = "process", required = false) String process) {
        String resultText;
        final List<PartyResponseRo> partyResponseRoList = listParties(name,endPoint,partyId,process,0, CsvService.MAX_NUMBER_OF_ENTRIES);

        // excluding unneeded columns
        csvServiceImpl.setExcludedItems(CsvExcludedItems.PARTY_RESOURCE.getExcludedItems());

        // needed for empty csv file purposes
        csvServiceImpl.setClass(PartyResponseRo.class);

        // column customization
        csvServiceImpl.customizeColumn(CsvCustomColumns.PARTY_RESOURCE.getCustomColumns());

        try {
            resultText = csvServiceImpl.exportToCSV(partyResponseRoList);
        } catch (CsvException e) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CsvService.APPLICATION_EXCEL_STR))
                .header("Content-Disposition", "attachment; filename=" + csvServiceImpl.getCsvFilename("party"))
                .body(resultText);
    }

    @RequestMapping(value = {"/update"}, method = RequestMethod.PUT)
    public void updateParties(@RequestBody List<PartyResponseRo> partiesRo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating parties [{}]", Arrays.toString(partiesRo.toArray()));
        }

        List<Party> partyList = domainConverter.convert(partiesRo, Party.class);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating partyList [{}]", Arrays.toString(partyList.toArray()));
        }

        partyService.updateParties(partyList);
    }

    /**
     * Flatten the list of identifiers of each party into a comma separated list for displaying in the console.
     *
     * @param partyResponseRos the list of party to be adapted.
     */
    protected void flattenIdentifiers(List<PartyResponseRo> partyResponseRos) {
        partyResponseRos.forEach(
                partyResponseRo -> {
                    String joinedIdentifiers = partyResponseRo.getIdentifiers().
                            stream().
                            map(IdentifierRo::getPartyId).
                            sorted().
                            collect(Collectors.joining(", "));
                    partyResponseRo.setJoinedIdentifiers(joinedIdentifiers);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Flatten identifiers for [{}]=[{}]", partyResponseRo.getName(), partyResponseRo.getJoinedIdentifiers());
                    }
                });
    }


    /**
     * Flatten the list of processes of each party into a comma separated list for displaying in the console.
     *
     * @param partyResponseRos the list of party to be adapted.
     */
    protected void flattenProcesses(List<PartyResponseRo> partyResponseRos) {
        partyResponseRos.forEach(
                partyResponseRo -> {

                    List<ProcessRo> processesWithPartyAsInitiator = partyResponseRo.getProcessesWithPartyAsInitiator();
                    List<ProcessRo> processesWithPartyAsResponder = partyResponseRo.getProcessesWithPartyAsResponder();

                    List<ProcessRo> processesWithPartyAsInitiatorAndResponder =
                            processesWithPartyAsInitiator.
                                    stream().
                                    filter(processesWithPartyAsResponder::contains).
                                    collect(Collectors.toList());

                    List<ProcessRo> processWithPartyAsInitiatorOnly = processesWithPartyAsInitiator
                            .stream()
                            .filter(processRo -> !processesWithPartyAsInitiatorAndResponder.contains(processRo))
                            .collect(Collectors.toList());

                    List<ProcessRo> processWithPartyAsResponderOnly = processesWithPartyAsResponder
                            .stream()
                            .filter(processRo -> !processesWithPartyAsInitiatorAndResponder.contains(processRo))
                            .collect(Collectors.toList());

                    String joinedProcessesWithMeAsInitiatorOnly = processWithPartyAsInitiatorOnly.
                            stream().
                            map(ProcessRo::getName).
                            map(name -> name.concat("(I)")).
                            collect(Collectors.joining(", "));

                    String joinedProcessesWithMeAsResponderOnly = processWithPartyAsResponderOnly.
                            stream().
                            map(ProcessRo::getName).
                            map(name -> name.concat("(R)")).
                            collect(Collectors.joining(","));

                    String joinedProcessesWithMeAsInitiatorAndResponder = processesWithPartyAsInitiatorAndResponder.
                            stream().
                            map(ProcessRo::getName).
                            map(name -> name.concat("(IR)")).
                            collect(Collectors.joining(","));

                    List<String> joinedProcess= Lists.newArrayList();

                    if(StringUtils.isNotEmpty(joinedProcessesWithMeAsInitiatorOnly)){
                        joinedProcess.add(joinedProcessesWithMeAsInitiatorOnly);
                    }

                    if(StringUtils.isNotEmpty(joinedProcessesWithMeAsResponderOnly)){
                        joinedProcess.add(joinedProcessesWithMeAsResponderOnly);
                    }

                    if(StringUtils.isNotEmpty(joinedProcessesWithMeAsInitiatorAndResponder)){
                        joinedProcess.add(joinedProcessesWithMeAsInitiatorAndResponder);
                    }

                    partyResponseRo.setJoinedProcesses(
                            StringUtils.join(joinedProcess,", "));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Flatten processes for [{}]=[{}]", partyResponseRo.getName(), partyResponseRo.getJoinedProcesses());
                    }
                });
    }
}
