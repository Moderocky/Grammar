package mx.kenzie.grammar.unwrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface Unwrap {
    boolean value() default true;

    String name() default "";

    Class<?> marshalAs() default void.class;

    Class<?> componentType() default void.class;
}
