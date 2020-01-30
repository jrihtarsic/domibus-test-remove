package eu.domibus.ext.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Party class for external API
 *
 * @since 4.2
 * @author Catalin Enache
 */
public class PartyDTO {

    private Integer entityId;

    @NotNull
    private String name;

    private String userName;

    @NotNull
    private String endpoint;

    @NotNull
    private Set<PartyIdentifierDTO> identifiers;

    private List<ProcessDTO> processesWithPartyAsInitiator = new ArrayList<>();

    private List<ProcessDTO> processesWithPartyAsResponder = new ArrayList<>();

    private String certificateContent;

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Set<PartyIdentifierDTO> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Set<PartyIdentifierDTO> identifiers) {
        this.identifiers = identifiers;
    }

    public List<ProcessDTO> getProcessesWithPartyAsInitiator() {
        return processesWithPartyAsInitiator;
    }

    public void setProcessesWithPartyAsInitiator(List<ProcessDTO> processesWithPartyAsInitiator) {
        this.processesWithPartyAsInitiator = processesWithPartyAsInitiator;
    }

    public List<ProcessDTO> getProcessesWithPartyAsResponder() {
        return processesWithPartyAsResponder;
    }

    public void setProcessesWithPartyAsResponder(List<ProcessDTO> processesWithPartyAsResponder) {
        this.processesWithPartyAsResponder = processesWithPartyAsResponder;
    }

    public String getCertificateContent() {
        return certificateContent;
    }

    public void setCertificateContent(String certificateContent) {
        this.certificateContent = certificateContent;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("entityId", entityId)
                .append("name", name)
                .append("userName", userName)
                .append("endpoint", endpoint)
                .append("identifiers", identifiers)
                .append("processesWithPartyAsInitiator", processesWithPartyAsInitiator)
                .append("processesWithPartyAsResponder", processesWithPartyAsResponder)
                .append("certificateContent", certificateContent)
                .toString();
    }
}
