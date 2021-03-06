package org.ifcx.gondor.api;

import java.lang.annotation.*;

/**
 * Mark a field as containing input file value(s).
 * If the name attribute is non-empty then it is a process attribute with the given name.
 *
 * @author Jim White
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InputFile {
    String name() default "";
}
