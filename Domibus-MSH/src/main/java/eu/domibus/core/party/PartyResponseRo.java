package eu.domibus.core.party;

import eu.domibus.api.party.Process;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public class PartyResponseRo {

    private Integer entityId;

    protected Set<IdentifierRo> identifiers; //NOSONAR

    protected String name;

    protected String userName;

    protected String endpoint;

    private String joinedIdentifiers;

    private String joinedProcesses;

    private List<ProcessRo> processesWithMeAsInitiator=new ArrayList<>();

    private List<ProcessRo> processesWithMeAsResponder=new ArrayList<>();

    public Set<IdentifierRo> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Set<IdentifierRo> identifiers) {
        this.identifiers = identifiers;
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

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getJoinedIdentifiers() {
        return joinedIdentifiers;
    }

    public void setJoinedIdentifiers(String joinedIdentifiers) {
        this.joinedIdentifiers = joinedIdentifiers;
    }

    public String getJoinedProcesses() {
        return joinedProcesses;
    }

    public void setJoinedProcesses(String joinedProcesses) {
        this.joinedProcesses = joinedProcesses;
    }

    public List<ProcessRo> getProcessesWithMeAsInitiator() {
        return processesWithMeAsInitiator;
    }

    public void setProcessesWithMeAsInitiator(List<ProcessRo> processesWithMeAsInitiator) {
        this.processesWithMeAsInitiator = processesWithMeAsInitiator;
    }

    public List<ProcessRo> getProcessesWithMeAsResponder() {
        return processesWithMeAsResponder;
    }

    public void setProcessesWithMeAsResponder(List<ProcessRo> processesWithMeAsResponder) {
        this.processesWithMeAsResponder = processesWithMeAsResponder;
    }
}
