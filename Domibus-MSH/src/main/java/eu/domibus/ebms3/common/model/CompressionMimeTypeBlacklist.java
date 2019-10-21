package eu.domibus.ebms3.common.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Christian Koch, Stefan Mueller
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS)
public class CompressionMimeTypeBlacklist {

    @Value("#{'${compressionBlacklist}'.split(',')}")
    private List<String> entries;

    public List<String> getEntries() {
        return this.entries;
    }

    public void setEntries(final List<String> entries) {
        this.entries = entries;
    }
}
