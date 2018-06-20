package eu.domibus.core.party;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.party.Identifier;
import eu.domibus.api.party.Party;
import eu.domibus.api.process.Process;
import eu.domibus.common.dao.PartyDao;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.crypto.api.MultiDomainCryptoService;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.ebms3.common.model.Ebms3Constants;
import eu.domibus.pki.CertificateService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@RunWith(JMockit.class)
public class PartyServiceImplTest {

    @Injectable
    private DomainCoreConverter domainCoreConverter;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private PartyDao partyDao;

    @Injectable
    private MultiDomainCryptoService multiDomainCertificateProvider;

    @Injectable
    private DomainContextProvider domainProvider;

    @Injectable
    private CertificateService certificateService;

    @Tested
    private PartyServiceImpl partyService;

    @Test
    public void getParties() throws Exception {
        String name = "name";
        String endPoint = "endPoint";
        String partyId = "partyId";
        String processName = "processName";
        int pageStart=0;
        int pageSize=10;

        new Expectations(partyService){{
            partyService.getSearchPredicate(anyString,anyString,anyString,anyString);
            partyService.linkPartyAndProcesses();times=1;
        }};
        partyService.getParties(name, endPoint, partyId, processName,pageStart,pageSize);
        new Verifications(){{
            partyService.getSearchPredicate(name,endPoint,partyId,processName);times=1;
        }};
    }

    @Test
    public void linkPartyAndProcesses() throws Exception {

        eu.domibus.common.model.configuration.Party partyEntity=new eu.domibus.common.model.configuration.Party();
        final String name = "name";
        partyEntity.setName(name);

        List<eu.domibus.common.model.configuration.Party> partyEntities = Lists.newArrayList(partyEntity);
        List<eu.domibus.common.model.configuration.Process> processEntities = Lists.newArrayList(new eu.domibus.common.model.configuration.Process());

        Party party=new Party();
        party.setName(name);
        List<Party> parties = Lists.newArrayList(party);

        new Expectations(partyService){{
            pModeProvider.findAllParties();
            result=partyEntities;
            pModeProvider.findAllProcesses();
            result=processEntities;
            domainCoreConverter.convert(partyEntities,Party.class);
            result=parties;
            partyService.linkProcessWithPartyAsInitiator(withAny(new HashMap<>()),processEntities);times=1;
            partyService.linkProcessWithPartyAsResponder(withAny(new HashMap<>()),processEntities);times=1;
        }};

        partyService.linkPartyAndProcesses();

        new Verifications(){{
            Map<String,Party> partyMap;
            partyService.linkProcessWithPartyAsInitiator(partyMap=withCapture(),processEntities);times=1;
            assertEquals(partyMap.get(name),party);
            partyService.linkProcessWithPartyAsResponder(partyMap=withCapture(),processEntities);times=1;
            assertEquals(partyMap.get(name),party);
        }};
    }

    @Test
    public void linkProcessWithPartyAsInitiator(final @Mocked eu.domibus.common.model.configuration.Process processEntity) throws Exception {

        Party party=new Party();
        party.setName("name");
        Map<String,Party> partyMap=new HashMap<>();
        partyMap.put("name",party);

        Process process = new Process();
        process.setName("p1");


        eu.domibus.common.model.configuration.Party partyEntity=new eu.domibus.common.model.configuration.Party();
        partyEntity.setName("name");

        Set<eu.domibus.common.model.configuration.Party> responderParties = Sets.newHashSet(partyEntity);
        List<eu.domibus.common.model.configuration.Process> processes = Lists.newArrayList(processEntity);

        new Expectations(){{
            processEntity.getInitiatorParties();
            result=responderParties;

            domainCoreConverter.convert(processEntity,Process.class);
            result=process;
        }};
        partyService.linkProcessWithPartyAsInitiator(partyMap,processes);
        assertEquals(1,party.getProcessesWithPartyAsInitiator().size());
        assertEquals("p1",party.getProcessesWithPartyAsInitiator().get(0).getName());
    }

    @Test
    public void linProcessWithPartyAsResponder(final @Mocked eu.domibus.common.model.configuration.Process processEntity) throws Exception {
        Party party=new Party();
        party.setName("name");
        Map<String,Party> partyMap=new HashMap<>();
        partyMap.put("name",party);

        Process process = new Process();
        process.setName("p1");


        eu.domibus.common.model.configuration.Party partyEntity=new eu.domibus.common.model.configuration.Party();
        partyEntity.setName("name");

        Set<eu.domibus.common.model.configuration.Party> responderParties = Sets.newHashSet(partyEntity);
        List<eu.domibus.common.model.configuration.Process> processes = Lists.newArrayList(processEntity);

        new Expectations(){{
           processEntity.getResponderParties();
           result=responderParties;

           domainCoreConverter.convert(processEntity,Process.class);
           result=process;
        }};
        partyService.linkProcessWithPartyAsResponder(partyMap,processes);
        assertEquals(1,party.getProcessesWithPartyAsResponder().size());
        assertEquals("p1",party.getProcessesWithPartyAsResponder().get(0).getName());

    }

    @Test
    public void getSearchPredicate() throws Exception {

        final String name = "name";
        final String endPoint = "endPoint";
        final String partyId = "partyId";
        final String processName = "processName";

        new Expectations(partyService){{

        }};
        partyService.getSearchPredicate(name, endPoint, partyId, processName);

        new Verifications(){{
            partyService.namePredicate(name);times=1;
            partyService.endPointPredicate(endPoint);times=1;
            partyService.partyIdPredicate(partyId);times=1;
            partyService.processPredicate(processName);times=1;
        }};
    }

    @Test
    public void namePredicate() throws Exception {
        Party party=new Party();
        party.setName("name");

        assertTrue(partyService.namePredicate("").test(party));
        assertTrue(partyService.namePredicate("name").test(party));
        assertFalse(partyService.namePredicate("wrong").test(party));
    }

    @Test
    public void endPointPredicate() throws Exception {
        Party party=new Party();
        party.setEndpoint("http://localhost:8080");
        assertTrue(partyService.endPointPredicate("").test(party));
        assertTrue(partyService.endPointPredicate("8080").test(party));
        assertFalse(partyService.endPointPredicate("7070").test(party));
    }

    @Test
    public void partyIdPredicate() throws Exception {
        Party party=new Party();
        Identifier identifier = new Identifier();
        identifier.setPartyId("partyId");
        party.setIdentifiers(Sets.newHashSet(identifier));
        party.setIdentifiers(Sets.newHashSet(identifier));
        assertTrue(partyService.partyIdPredicate("").test(party));
        assertTrue(partyService.partyIdPredicate("party").test(party));
        assertFalse(partyService.partyIdPredicate("wrong").test(party));
    }

    @Test
    public void processPredicate() throws Exception {
        Party party=new Party();
        Process process = new Process();
        process.setName("processName");
        party.addProcessesWithPartyAsInitiator(process);
        party.addprocessesWithPartyAsResponder(process);
        assertTrue(partyService.processPredicate(null).test(party));
        assertTrue(partyService.processPredicate("cessName").test(party));
        assertFalse(partyService.processPredicate("wrong").test(party));
    }

    @Test
    public void testFindPartyNamesByServiceAndAction() throws EbMS3Exception {
        // Given
        List<String> parties = new ArrayList<>();
        parties.add("test");
        new Expectations() {{
           pModeProvider.findPartyIdByServiceAndAction(Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION);
           result = parties;
        }};

        // When
        List<String> partyNamesByServiceAndAction = partyService.findPartyNamesByServiceAndAction(Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION);

        // Then
        Assert.assertEquals(parties, partyNamesByServiceAndAction);
    }

    @Test
    public void testGetGatewayPartyIdentifier() {
        // Given
        String expectedGatewayPartyId = "testGatewayPartyId";
        eu.domibus.common.model.configuration.Party gatewayParty = new eu.domibus.common.model.configuration.Party();
        Set<eu.domibus.common.model.configuration.Identifier> identifiers = new HashSet<>();
        eu.domibus.common.model.configuration.Identifier identifier = new eu.domibus.common.model.configuration.Identifier();
        identifier.setPartyId(expectedGatewayPartyId);
        identifiers.add(identifier);
        gatewayParty.setIdentifiers(identifiers);
        new Expectations() {{
            pModeProvider.getGatewayParty();
            result = gatewayParty;
        }};

        // When
        String gatewayPartyId = partyService.getGatewayPartyIdentifier();

        // Then
        Assert.assertEquals(expectedGatewayPartyId, gatewayPartyId);
    }

    @Test
    public void getProcesses() throws Exception {
        partyService.getAllProcesses();
        new Verifications(){{
            pModeProvider.findAllProcesses();times=1;
        }};
    }

}