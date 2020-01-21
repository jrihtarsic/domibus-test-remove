package eu.domibus.ext.delegate.services.pmode;

import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.api.pmode.PModeIssue;
import eu.domibus.api.pmode.PModeService;
import eu.domibus.ext.delegate.converter.DomainExtConverter;
import eu.domibus.ext.domain.PModeArchiveInfoDTO;
import eu.domibus.ext.services.PModeExtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Catalin Enache
 * @since 4.1.1
 */
@Service
public class PModeServiceDelegate implements PModeExtService {

    @Autowired
    private PModeService pModeService;

    @Autowired
    private DomainExtConverter domainConverter;

    @Override
    public byte[] getPModeFile(int id) {
        return pModeService.getPModeFile(id);
    }

    @Override
    public PModeArchiveInfoDTO getCurrentPmode() {
        final PModeArchiveInfo pModeArchiveInfo = pModeService.getCurrentPMode();
        return domainConverter.convert(pModeArchiveInfo, PModeArchiveInfoDTO.class);
    }

    @Override
    public List<String> updatePModeFile(byte[] bytes, String description) {
        List<PModeIssue> issues = pModeService.updatePModeFile(bytes, description);
        return issues.stream().map(i -> i.getMessage()).collect(Collectors.toList());
    }
}
