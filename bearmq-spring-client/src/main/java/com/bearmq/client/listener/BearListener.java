package com.bearmq.client.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SuppressWarnings("unused")
public @interface BearListener {
  String[] queues() default {};

  long delay() default 200;

  int maxRetries() default 3;

  String deadLetterQueue() default "";
}
