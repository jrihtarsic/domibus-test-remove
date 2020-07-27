package eu.domibus.core.metrics;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.ext.domain.metrics.Counter;
import eu.domibus.ext.domain.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Thomas Dussart
 * @since 4.1
 * <p>
 * This aspect looks after framework annotation timer and counter, and then configure metrics. In this scenario,
 * drop wizard metrics.
 */
@Aspect
@Component
public class MetricsAspect {

    static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MetricsAspect.class);

    @Autowired
    private MetricRegistry metricRegistry;

    @Around("@annotation(timer)")
    public Object surroundWithATimer(ProceedingJoinPoint pjp, Timer timer) throws Throwable {
        Class<?> clazz = timer.clazz();
        String value = timer.value();
        com.codahale.metrics.Timer.Context methodTimer = metricRegistry.timer(getMetricsName(clazz, value, "_timer")).time();
        try {
            return pjp.proceed();
        } finally {
            if (methodTimer != null) {
                methodTimer.stop();
            }
        }
    }

    private String getMetricsName(Class<?> clazz, String name,String suffix) {
        return name(clazz, name + suffix);

    }

    @Around("@annotation(counter)")
    public Object surroundWithACounter(ProceedingJoinPoint pjp, Counter counter) throws Throwable {
        Class<?> clazz = counter.clazz();
        String value = counter.value();
        com.codahale.metrics.Counter methodCounter = metricRegistry.counter(getMetricsName(clazz,value, "_counter"));
        try {
            methodCounter.inc();
            return pjp.proceed();
        } finally {
            methodCounter.dec();
        }
    }

}
