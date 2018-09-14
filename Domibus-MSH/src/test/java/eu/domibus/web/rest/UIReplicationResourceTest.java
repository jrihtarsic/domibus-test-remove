package eu.domibus.web.rest;

import eu.domibus.core.replication.UIMessageDiffService;
import eu.domibus.core.replication.UIReplicationSignalService;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Random;

/**
 * JUnit for {@link UIReplicationResource} class
 *
 * @author Catalin Enache
 * @since 4.0
 */
@RunWith(JMockit.class)
public class UIReplicationResourceTest {

    @Tested
    private UIReplicationResource uiReplicationResource;

    @Injectable
    private UIMessageDiffService uiMessageDiffService;

    @Injectable
    private UIReplicationSignalService uiReplicationSignalService;

    @Test
    public void testCountData() {
        final int countRecordsToSync = new Random().nextInt();
        new Expectations() {{
            uiMessageDiffService.countAll();
            result = countRecordsToSync;

            uiReplicationSignalService.isReplicationEnabled();
            result = true;

        }};

        //tested method
        final ResponseEntity<String> restResponse = uiReplicationResource.countData();

        Assert.assertEquals(HttpStatus.OK, restResponse.getStatusCode());
        Assert.assertEquals(countRecordsToSync + " records to be synced for TB_MESSAGE_UI table", restResponse.getBody());

        new FullVerifications() {{
        }};
    }

    @Test
    public void testSyncData_ThereAreRowsToSync_ResultOK() {
        final int limit = new Random().nextInt();
        final int syncedRows = new Random().nextInt();
        new Expectations() {{
            uiMessageDiffService.findAndSyncUIMessages(limit);
            result = syncedRows;

            uiReplicationSignalService.isReplicationEnabled();
            result = true;

        }};

        //tested method
        final ResponseEntity<String> restResponse = uiReplicationResource.syncData(limit);

        Assert.assertEquals(HttpStatus.OK, restResponse.getStatusCode());
        Assert.assertEquals(syncedRows + " were successfully synced for TB_MESSAGE_UI table", restResponse.getBody());

        new FullVerifications() {{
        }};
    }

    @Test
    public void testSyncData_NoRowsToSync_ResultOK() {
        final int limit = new Random().nextInt();
        final int syncedRows = 0;
        new Expectations() {{
            uiMessageDiffService.findAndSyncUIMessages(limit);
            result = syncedRows;

            uiReplicationSignalService.isReplicationEnabled();
            result = true;

        }};

        //tested method
        final ResponseEntity<String> restResponse = uiReplicationResource.syncData(limit);

        Assert.assertEquals(HttpStatus.OK, restResponse.getStatusCode());
        Assert.assertEquals("no records were synced for TB_MESSAGE_UI table", restResponse.getBody());

        new FullVerifications() {{
        }};
    }
}