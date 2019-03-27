package eu.domibus.common.validators;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.services.impl.PullProcessStatus;
import eu.domibus.core.pull.PullMessageService;
import eu.domibus.test.util.PojoInstaciatorUtil;
import mockit.Injectable;
import mockit.NonStrictExpectations;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.domibus.common.services.impl.PullProcessStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@RunWith(JMockit.class)
public class ProcessValidatorTest {

    @Tested
    ProcessValidator processValidator;

    @Injectable
    PullMessageService pullMessageService;

    @Before
    public void init() {
        new NonStrictExpectations() {{
            pullMessageService.allowDynamicInitiatorInPullProcess();
            result = false;

            pullMessageService.allowMultipleLegsInPullProcess();
            result = false;
        }};
    }

    @Test
    public void checkProcessValidityWithMoreThanOneLegAndDifferentResponder() throws Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1];[name:leg2]}");
        Set<PullProcessStatus> pullProcessStatuses = getProcessStatuses(process);
        assertEquals(2, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(MORE_THAN_ONE_LEG_FOR_THE_SAME_MPC));
        assertTrue(pullProcessStatuses.contains(NO_RESPONDER));
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1];[name:leg2]}", "initiatorParties{[name:resp1];[name:resp2]}");
        pullProcessStatuses = getProcessStatuses(process);
        assertEquals(2, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(MORE_THAN_ONE_LEG_FOR_THE_SAME_MPC));
        assertTrue(pullProcessStatuses.contains(TOO_MANY_RESPONDER));
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1];[name:leg2]}", "initiatorParties{[name:resp1]}");
        pullProcessStatuses = getProcessStatuses(process);
        assertEquals(1, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(MORE_THAN_ONE_LEG_FOR_THE_SAME_MPC));
    }

    @Test
    public void checkEmptyProcessStatus() throws Exception {
        Set<PullProcessStatus> processStatuses = getProcessStatuses(PojoInstaciatorUtil.instanciate(Process.class));
        assertTrue(processStatuses.contains(NO_PROCESS_LEG));
        assertTrue(processStatuses.contains(NO_RESPONDER));
    }


    @Test
    public void checkProcessWithNoLegs() throws Exception {
        Set<PullProcessStatus> processStatuses = getProcessStatuses(PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "initiatorParties{[name:resp1]}"));
        assertEquals(1, processStatuses.size());
        assertTrue(processStatuses.contains(NO_PROCESS_LEG));
    }

    @Test
    public void checkTooManyProcesses() throws Exception {
        Process p1 = PojoInstaciatorUtil.instanciate(Process.class, "p1", "mep[name:oneway]", "legs{[name:leg1]}", "responderParties{[name:resp1]}");
        //p1.setName("p1");
        Process p2 = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg2]}", "responderParties{[name:resp2]}");
        p2.setName("p2");
        List<Process> processes = Lists.newArrayList(p1, p2);
        Set<PullProcessStatus> pullProcessStatuses = processValidator.verifyPullProcessStatus(new HashSet<>(processes));
        assertEquals(1, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(TOO_MANY_PROCESSES));
    }

    @Test
    public void checkProcessValidityWithOneLeg() throws Exception {
        Set<PullProcessStatus> processStatuses = getProcessStatuses(PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1]}", "initiatorParties{[name:resp1]}"));
        assertEquals(1, processStatuses.size());
        assertTrue(processStatuses.contains(ONE_MATCHING_PROCESS));
    }

    @Test
    public void checkNoProcess() throws Exception {
        Set<PullProcessStatus> pullProcessStatuses = processValidator.verifyPullProcessStatus(Sets.<Process>newHashSet());
        assertEquals(1, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(NO_PROCESSES));
    }

    @Test
    public void createProcessWarningMessage() {
        Process process = PojoInstaciatorUtil.instanciate(Process.class);
        try {
            processValidator.validatePullProcess(Lists.newArrayList(process));
            assertTrue(false);
        } catch (PModeException e) {
            assertTrue(e.getMessage().contains("No leg configuration found"));
            assertTrue(e.getMessage().contains("No responder configured"));
        }

    }

    @Test
    public void testOneWayPullOnlySupported() throws EbMS3Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:twoway]", "mepBinding[name:pull]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "responderParties{[name:resp1]}");
        try {
            processValidator.validatePullProcess(Lists.newArrayList(process));
            assertTrue(false);
        } catch (PModeException e) {
            assertTrue(e.getMessage().contains("Invalid mep. Only one way supported"));
        }
    }

    private Set<PullProcessStatus> getProcessStatuses(Process process) {
        return processValidator.verifyPullProcessStatus(Sets.newHashSet(process));
    }

}