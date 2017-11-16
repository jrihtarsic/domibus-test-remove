package eu.domibus.core.certificate;

import eu.domibus.common.model.certificate.Certificate;

import java.util.Date;
import java.util.List;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public interface CertificateDao {

    void saveOrUpdate(Certificate certificate);

    List<Certificate> getCloseToRevocation(Date startDate, Date endDate);

    void notifyRevocation(Certificate certificate);
}
