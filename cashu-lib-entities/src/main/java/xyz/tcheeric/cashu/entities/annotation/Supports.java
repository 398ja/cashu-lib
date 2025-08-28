package xyz.tcheeric.cashu.entities.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import xyz.tcheeric.cashu.common.PaymentMethod;

@Retention(RetentionPolicy.RUNTIME)
public @interface Supports {
  PaymentMethod[] value();
}
