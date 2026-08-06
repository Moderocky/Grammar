package mx.kenzie.grammar.unwrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// The specification for how to interpret a field (or record parameter)
/// on an object registered for marshalling.
/// If this is absent the details are inferred from the field itself.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Unwrap {
    boolean value() default true;

    String name() default "";

    Class<?> marshalAs() default void.class;

    Class<?> componentType() default void.class;
}
