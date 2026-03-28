package com.bearmq.server.broker.runner;

import com.bearmq.server.broker.dto.Message;
import com.bearmq.server.broker.facade.BrokerServerFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
@NamedInterface
@RequiredArgsConstructor
@SuppressWarnings("all")
public class BrokerServer implements Closeable {
  private static final int DEFAULT_CHUNK_SIZE = 1024 * 4;
  private static final int MAX_MESSAGE_SIZE = 16 * 16 * 1024;

  private final ServerSocket serverSocket;
  private final ExecutorService executorService;
  private final ObjectMapper objectMapper;
  private final BrokerServerFacade brokerFacade;

  public void run() {

    try {
      this.loadCurrentQueues();

      log.warn("Broker server started on port " + this.serverSocket.getLocalPort());

      while (true) {
        final Socket socket = this.serverSocket.accept();
        this.executorService.execute(() -> this.handleClient(socket));
      }
    } catch (final IOException e) {
      Thread.currentThread().interrupt();
      log.error("Error accepting connection", e);
    }
  }

  private void handleClient(final Socket socket) {

    try (final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
      final int messageLength = dataInputStream.readInt();

      if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
        log.error("Invalid message length: " + messageLength);
      }

      final byte[] bytes = new byte[messageLength];

      int offset = 0;
      int expectedIdx = 1;

      while (offset < messageLength) {
        final int chunkIdx = dataInputStream.readInt();

        if (chunkIdx != expectedIdx) {
          throw new IOException("chunk order mismatch");
        }

        final int chunkLen = Math.min(DEFAULT_CHUNK_SIZE, messageLength - offset);

        dataInputStream.readFully(bytes, offset, chunkLen);

        offset += chunkLen;
        expectedIdx++;
      }

      final Message message = this.objectMapper.readValue(bytes, Message.class);

      final Optional<byte[]> body = this.brokerFacade.identifyOperationAndApply(message);

      if (body.isPresent()) {
        this.response(body.get(), dataInputStream, socket);
      }

    } catch (final Exception e) {
      Thread.currentThread().interrupt();
      log.error("Error accepting connection", e);
    }
  }

  private void response(final byte[] body, final DataInputStream dis, final Socket socket) {

    try (final DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
      dos.writeInt(body.length);

      int offset = 0;
      int chunk = 0;

      while (offset < body.length) {
        final int chunkSize = Math.min(DEFAULT_CHUNK_SIZE, body.length - offset);
        dos.writeInt(++chunk);
        dos.write(body, offset, chunkSize);
        offset += chunkSize;
      }

      dos.flush();
    } catch (final IOException e) {
      Thread.currentThread().interrupt();
      log.error("Error accepting connection", e);
    }
  }

  public void loadCurrentQueues() {

    this.brokerFacade.loadQueues();
  }

  @Override
  public void close() throws IOException {

    this.executorService.shutdown();
    this.serverSocket.close();
  }
}
