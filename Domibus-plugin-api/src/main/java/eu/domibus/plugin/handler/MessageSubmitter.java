
package eu.domibus.plugin.handler;


import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.Submission;

/**
 * Implementations of this interface handle the plugin of messages from the
 * backend to holodeck.
 *
 * @author Christian Koch, Stefan Mueller
 */
public interface MessageSubmitter {

    /**
     * Submits a message to Domibus to be processed.
     *
     * @param messageData the message to be processed
     * @param submitterName
     * @return the messageId of the submitted message
     * @throws MessagingProcessingException if the message was rejected by the Domibus MSH
     */
    public String submit(Submission messageData, String submitterName) throws MessagingProcessingException;
}
