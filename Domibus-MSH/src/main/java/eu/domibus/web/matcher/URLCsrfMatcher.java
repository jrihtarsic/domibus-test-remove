package eu.domibus.web.matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;


public class URLCsrfMatcher implements RequestMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(URLCsrfMatcher.class);

    protected String url;

    private RegexRequestMatcher unprotectedMatcher = null;
    private final HashSet<String> allowedMethods = new HashSet<String>( Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));

    @PostConstruct
    public void init() {
        LOGGER.debug("Initializing the matcher with [{}]", url);
        unprotectedMatcher = new RegexRequestMatcher(url, null);

    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if(this.allowedMethods.contains(request.getMethod())) {
            LOGGER.debug("Matched method [{}]", request.getMethod());
            return false;
        }
        return !unprotectedMatcher.matches(request);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}