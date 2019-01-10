package com.spi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author KyoWang
 * @since 2017/08/25
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceProvider {
    /**
     * Returns provided interfaces
     */
    Class<?>[] value();

    /**
     * Returns the priority
     */
    int priority() default 0;

    /**
     * Returns the alias
     */
    String alias() default "";
}
