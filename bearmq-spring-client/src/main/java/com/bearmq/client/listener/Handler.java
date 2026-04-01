package com.bearmq.client.listener;

import java.lang.reflect.Method;

public record Handler(Object bean, Method method, int maxRetries, String deadLetterQueue) {}
