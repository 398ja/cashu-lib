package cashu.common.annotation;

import cashu.common.model.PaymentMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Supports {
    PaymentMethod[] value();
}