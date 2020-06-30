package eu.domibus.core.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Thomas Dussart
 * @since 4.1
 *
 * Metric annotation to add a counter. It will count the parallel
 * executions of a given method.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Counter {
    /**
     * @return the counter name.
     */
    MetricNames value() default MetricNames.VOID;

    /**
     * @return the counter class.
     */
    Class<?> clazz() default Default.class;
}
