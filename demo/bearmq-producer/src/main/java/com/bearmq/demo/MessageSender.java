package com.bearmq.demo;

import com.bearmq.client.BearTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSender {
  private final BearTemplate bearTemplate;
  private final Random random = new Random();

  @Async
  public void sendExchangeA() {
    final Student student = Student.builder()
            .address("from-A: Address-" + random.nextInt(10, 10000))
            .name("from-A: Name" + random.nextInt(10, 10000))
            .age(random.nextInt(10, 30))
            .build();

    bearTemplate.convertAndSend("exchangeA", "", student);
    log.warn("Sent message to ExchangeA: {}", student);
  }

  @Async
  public void sendExchangeB() {
    final Student student = Student.builder()
            .address("from-B: Address-" + random.nextInt(10, 10000))
            .name("from-B: Name" + random.nextInt(10, 10000))
            .age(random.nextInt(10, 30))
            .build();

    bearTemplate.convertAndSend("exchangeB", "", student);
    log.warn("Sent message to ExchangeB: {}", student);
  }
}
