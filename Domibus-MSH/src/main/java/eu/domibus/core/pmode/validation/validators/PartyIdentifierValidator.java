package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.core.pmode.validation.PModeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Soumya Chandran
 * @since 4.2
 * Validates duplicate Identifiers in the party.
 */
@Component
@Order(9)
public class PartyIdentifierValidator implements PModeValidator {

    @Autowired
    PModeValidationHelper pModeValidationHelper;


    /**
     * Validate the Identifiers in the pmode
     *
     * @param pMode configuration of pmode
     * @return list of ValidationIssue
     */
    @Override
    public List<ValidationIssue> validate(Configuration pMode) {
        List<ValidationIssue> issues = new ArrayList<>();
        List<Party> allParties = pMode.getBusinessProcesses().getParties();
        allParties.forEach(party -> {
            issues.addAll(validateDuplicatePartyIdentifiers(party));
        });

        allParties.forEach(party -> {
            issues.addAll(validateDuplicateIdentifiersInAllParties(party, allParties));
        });
        return issues;
    }

    /**
     * check the duplicate identifiers of the parties
     *
     * @param party party with identifiers
     * @return list of ValidationIssue
     */
    protected List<ValidationIssue> validateDuplicatePartyIdentifiers(Party party) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<Identifier> duplicateIdentifiers = party.getIdentifiers().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(m -> m.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        duplicateIdentifiers.forEach(identifier -> {
            issues.add(createIssue(identifier.getPartyId(), party.getName(), "Duplicate party identifier [%s] found for the party [%s]"));
        });

        return issues;
    }


    /**
     * check the duplicate identifiers in all the parties
     *
     * @param allParties list of all parties
     * @return list of ValidationIssue
     */
    protected List<ValidationIssue> validateDuplicateIdentifiersInAllParties(Party party, List<Party> allParties) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<Identifier> identifierSet = new HashSet<>(party.getIdentifiers());

        allParties.stream().filter(party1 -> allParties.indexOf(party1) > allParties.indexOf(party)).forEach(party1 -> {
            List<Identifier> duplicateIdentifiers = getDuplicateIdentifiers(identifierSet, party1);

            duplicateIdentifiers.forEach(identifier -> {
                issues.add(createIssue(identifier.getPartyId(), party.getName(), "Duplicate party identifier [%s] found in party [%s] and in party [" + party1.getName() + "]"));
            });

        });
        return issues;
    }

    protected List<Identifier> getDuplicateIdentifiers(Set<Identifier> identifierSet, Party party1) {
        return identifierSet.stream().filter(identifier -> party1.getIdentifiers().contains(identifier)).collect(Collectors.toList());
    }

    /**
     * Creates pmode validation issue
     */
    protected ValidationIssue createIssue(String partyId, String name, String message) {
        return pModeValidationHelper.createValidationIssue(message, partyId, name);
    }

}
