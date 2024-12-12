package xyz.tcheeric.cashu.common.annotation;

import xyz.tcheeric.cashu.common.model.PaymentMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Supports {
    PaymentMethod[] value();
}