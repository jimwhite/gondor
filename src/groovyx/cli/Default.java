package groovyx.cli;

import java.lang.annotation.*;
import java.util.concurrent.Callable;

/**
 * Mark a parameter field as containing input file value(s).
 *
 * @author Jim White
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Default {
    Class<Callable> value();
}
