package xyz.tcheeric.cashu.entities.annotation;

import xyz.tcheeric.cashu.common.nut18.PaymentMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Supports {
    PaymentMethod[] value();
}