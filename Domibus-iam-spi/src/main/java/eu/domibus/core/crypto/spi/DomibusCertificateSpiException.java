package eu.domibus.core.crypto.spi;

/**
 * @author Thomas Dussart
 * @since 4.1
 */
public class DomibusCertificateSpiException extends RuntimeException {

    public DomibusCertificateSpiException() {
    }

    public DomibusCertificateSpiException(String message) {
        super(message);
    }

    public DomibusCertificateSpiException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomibusCertificateSpiException(Throwable cause) {
        super(cause);
    }

    public DomibusCertificateSpiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
