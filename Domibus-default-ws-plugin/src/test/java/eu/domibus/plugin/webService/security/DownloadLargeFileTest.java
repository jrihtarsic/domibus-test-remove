package eu.domibus.plugin.webService.security;

import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.ObjectFactory;
import eu.domibus.plugin.webService.generated.BackendService11;
import eu.domibus.plugin.webService.generated.DownloadMessageRequest;
import eu.domibus.plugin.webService.generated.DownloadMessageResponse;
import org.apache.commons.io.IOUtils;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;


public class DownloadLargeFileTest {

    public void testDownloadLargeFile() throws Exception {
        BackendService11 client = new BackendService11(new URL("http://localhost:8191/domibus/services/backend?wsdl"), new QName("http://org.ecodex.backend/1_1/", "BackendService_1_1"));
        DownloadMessageRequest request = new DownloadMessageRequest();
        request.setMessageID("15cc5c34-798a-496f-a41f-87ce38f0f5da@domibus.eu");
        Holder<DownloadMessageResponse> downloadMessageResponse = new Holder(new eu.domibus.plugin.webService.generated.ObjectFactory().createDownloadMessageResponse());
        Holder<Messaging> result = new Holder(new ObjectFactory().createMessaging());
        System.out.println("Downloading.... ");
        client.getBACKENDPORT().downloadMessage(request, downloadMessageResponse, result);
        final DataHandler value = downloadMessageResponse.value.getPayload().get(0).getValue();
        System.out.println("Data handler " + value);

        InputStream is = value.getInputStream();
        OutputStream os = new FileOutputStream(new File("c:/DEV/work/_soapui/mypayload.zip"));

        // This will copy the file from the two streams
        IOUtils.copy(is, os);

        // This will close two streams catching exception
        IOUtils.closeQuietly(os);
        IOUtils.closeQuietly(is);

        System.out.println("Finished downloading " + downloadMessageResponse.value.getPayload().get(0).getValue());
    }

}
