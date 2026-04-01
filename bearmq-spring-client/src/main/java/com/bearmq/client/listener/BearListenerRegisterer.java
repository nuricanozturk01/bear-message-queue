package com.bearmq.client.listener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class BearListenerRegisterer implements ApplicationRunner {
  private final ApplicationContext context;
  private final BearerListenerContainer bearerListener;

  public BearListenerRegisterer(
      final ApplicationContext context, final BearerListenerContainer container) {
    this.context = context;
    this.bearerListener = container;
  }

  @Override
  public void run(final ApplicationArguments args) {
    final Map<String, List<Handler>> byQueue = new HashMap<>();

    for (final String name : context.getBeanDefinitionNames()) {
      final Object bean = context.getBean(name);

      for (final Method method : bean.getClass().getMethods()) {
        final BearListener bearAnnotation = method.getAnnotation(BearListener.class);

        if (bearAnnotation == null || method.getParameterCount() > 1) {
          continue;
        }

        for (final String queueName : bearAnnotation.queues()) {
          byQueue
              .computeIfAbsent(queueName, k -> new ArrayList<>())
              .add(
                  new Handler(
                      bean, method, bearAnnotation.maxRetries(), bearAnnotation.deadLetterQueue()));
        }
      }
    }

    bearerListener.register(byQueue);
    bearerListener.start();
  }
}
