package mx.kenzie.grammar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an array or special type as accepting any subclass.
 * If specific classes are provided, then this will attempt only the known types during de-serialisation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Any {

    Class[] value() default {};

}
