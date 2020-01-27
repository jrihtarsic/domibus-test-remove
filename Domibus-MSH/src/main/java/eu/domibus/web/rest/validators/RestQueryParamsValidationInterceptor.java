package eu.domibus.web.rest.validators;

import eu.domibus.api.validators.CustomWhiteListed;
import eu.domibus.api.validators.SkipWhiteListed;
import eu.domibus.common.validators.ObjectPropertiesMapBlacklistValidator;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ion Perpegel
 * @since 4.1
 * A Spring interceptor that ensures that the request parameters of a REST call does not contain blacklisted chars
 */
@ControllerAdvice(annotations = RestController.class)
public class RestQueryParamsValidationInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(RestQueryParamsValidationInterceptor.class);

    @PostConstruct
    public void init() {
        blacklistValidator.init();
    }

    @Autowired
    ObjectPropertiesMapBlacklistValidator blacklistValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod && shouldSkipValidation((HandlerMethod) handler)) {
            return true;
        }

        String method = request.getMethod();

        Map<String, String[]> queryParams = request.getParameterMap();
        return handleQueryParams(queryParams, (HandlerMethod) handler);
    }

    private boolean shouldSkipValidation(HandlerMethod handler) {
        HandlerMethod method = handler;
        SkipWhiteListed skipAnnot = method.getMethodAnnotation(SkipWhiteListed.class);
        if (skipAnnot != null) {
            return true;
        }
        if (ArrayUtils.isNotEmpty(method.getMethodParameters())) {
            boolean skip = Arrays.stream(method.getMethodParameters()).
                    anyMatch(param -> param.getParameterAnnotation(SkipWhiteListed.class) != null
                            // if the parameter is marked as RequestBody, the RestBodyValidationInterceptor will handle the request validation, so skip
                            || param.getParameterAnnotation(RequestBody.class) != null);
            if (skip) {
                return true;
            }
        }
        return false;
    }

    protected boolean handleQueryParams(Map<String, String[]> queryParams, HandlerMethod method) {
        LOG.debug("Validate query params:[{}]", queryParams);
        if (queryParams == null || queryParams.isEmpty()) {
            LOG.debug("Query params are empty, exiting.");
            return true;
        }
        try {
            MethodParameter parameterInfo = null;
            Class parameterType = null;
            CustomWhiteListed parameterAnnotation = null;

            if (method != null) {
                List<MethodParameter> parameters = Arrays.asList(method.getMethodParameters()).stream()
                        .filter(el -> !el.hasParameterAnnotation(RequestPart.class))
                        .filter(el -> !el.hasParameterAnnotation(RequestBody.class))
                        .collect(Collectors.toList());

                if (parameters != null && !parameters.isEmpty()) {
                    // all GET methods should have maximum one request parameter
                    if (parameters.size() == 1) {
                        parameterInfo = parameters.get(0);
                        parameterType = parameterInfo.getParameterType();
                        parameterAnnotation = parameterInfo.getParameterAnnotation(CustomWhiteListed.class);
                    } else {
                        LOG.trace("Method [{}] has [{}] request parameters instead of maximum one so blacklist validation will not have type information!",
                                method.getMethod().getName(), parameters.size());
                    }
                }
            }

            blacklistValidator.validate(new ObjectPropertiesMapBlacklistValidator.Parameter(queryParams, parameterType, parameterAnnotation));
            LOG.debug("Query params:[{}] validated successfully", queryParams);
            return true;
        } catch (ValidationException ex) {
            LOG.debug("Query params:[{}] are invalid: [{}]", queryParams, ex);
            throw ex;
        } catch (Exception ex) {
            LOG.debug("Unexpected exception caught [{}] when validating query params [{}]. Request will be processed downhill.", ex, queryParams);
            return true;
        }
    }

}