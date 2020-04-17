package eu.domibus.core.message.converter;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.messaging.MessagingException;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

/**
 * Created by musatmi on 11/05/2017.
 */
@Service
public class MessageConverterDefaultService implements MessageConverterService {
    public static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageConverterDefaultService.class);

    @Autowired
    @Qualifier("jaxbContextEBMS")
    private JAXBContext jaxbContext;

    @Override
    public byte[] getAsByteArray(Messaging message) {

        final Marshaller marshaller;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(message, baos);
        } catch (JAXBException e) {
            throw new MessagingException("Error marshalling the message with id " + message.getId(), e);
        }

        return baos.toByteArray();

    }


}
