package eu.domibus.ebms3.common.validators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Party;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author musatmi
 * @since 3.3
 */
public class RolesValidatorTest extends AbstractValidatorTest {

    private RolesValidator validator = new RolesValidator();
    @Test
    public void validate() throws Exception {
        Configuration configuration = newConfiguration("RolesConfiguration.json");
        final List<String> results = validator.validate(configuration);
        assertTrue(results.size() == 1);
        assertEquals(results.get(0), "For business process TestProcess the initiator role and the responder role are identical (eCODEXRole)");
    }
}