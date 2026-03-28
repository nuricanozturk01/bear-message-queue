package com.bearmq.shared.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.h2.tools.Server")
@ConditionalOnProperty(prefix = "h2.tcp-server", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${spring.datasource.url:}'.startsWith('jdbc:h2')")
public class H2TcpServerConfig {

  @Value("${h2.tcp-server.port:9092}")
  private int port;

  @Bean(initMethod = "start", destroyMethod = "stop")
  public Server h2TcpServer() throws SQLException {

    return Server.createTcpServer("-tcp", "-tcpPort", String.valueOf(this.port));
  }
}
