package eu.domibus.ext.rest;

import eu.domibus.ext.domain.UserMessageDTO;
import eu.domibus.ext.exceptions.UserMessageExtException;
import eu.domibus.ext.services.UserMessageExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Tiago Miguel
 * @since 3.3.1
 */
@RestController
@RequestMapping(value = "/ext/messages/usermessages")
public class UserMessageResource {

    public static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageResource.class);

    @Autowired
    UserMessageExtService userMessageExtService;

    /**
     * Gets the User Message by messageId
     *
     * @param messageId The message Id
     * @return The User Message with the specified messageId
     * @throws UserMessageExtException Raised in case an exception occurs while trying to get a User Message
     */
    @ApiOperation(value = "Get user message", notes = "Retrieve the user message with the specified message id",
            authorizations = @Authorization(value = "basicAuth"), tags = "usermessage")
    @RequestMapping(path = "/{messageId:.+}", method = RequestMethod.GET)
    public UserMessageDTO getUserMessage(@PathVariable(value = "messageId") String messageId) throws UserMessageExtException{
        LOG.debug("Getting User Message with id = '" + messageId + "'");
        return userMessageExtService.getMessage(messageId);
    }
}
