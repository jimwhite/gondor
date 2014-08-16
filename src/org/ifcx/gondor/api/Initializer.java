package org.ifcx.gondor.api;

import java.lang.annotation.*;

/**
 * Mark a parameter field as containing input file value(s).
 *
 * @author Jim White
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Initializer {
    Class value();
}
