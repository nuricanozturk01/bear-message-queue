package com.bearmq;

import com.bearmq.api.security.CorsProperties;
import com.bearmq.api.security.JwtProperties;
import com.bearmq.server.broker.Constant;
import com.bearmq.server.broker.runner.BrokerServer;
import com.bearmq.server.metrics.runner.MetricServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class BrokerApplication implements ApplicationRunner {

  private static final String[] REQUIRED_OPENS = {
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
    "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
    "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
    "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
  };

  private final BrokerServer brokerServer;
  private final Optional<MetricServer> metricServer;

  @Value("${bearmq.server.metrics.enabled}")
  private boolean isMetricEnabled;

  public static void main(final String[] args) throws Exception {

    if (!isChronicleCompatible()) {
      relaunchWithRequiredFlags(args);
      return;
    }
    SpringApplication.run(BrokerApplication.class, args);
  }

  private static boolean isChronicleCompatible() {

    try {
      final Module javaLangReflect = Class.forName("java.lang.reflect.Method").getModule();
      final Module unnamed = BrokerApplication.class.getClassLoader().getUnnamedModule();
      return javaLangReflect.isOpen("java.lang.reflect", unnamed);
    } catch (final Exception e) {
      return false;
    }
  }

  private static void relaunchWithRequiredFlags(final String[] args) throws Exception {

    log.info(
        "Chronicle Queue requires --add-opens flags. "
            + "Relaunching JVM with the required arguments...");

    final String javaExe = resolveJavaExecutable();
    final List<String> command = new ArrayList<>();
    command.add(javaExe);
    command.addAll(Arrays.asList(REQUIRED_OPENS));

    final List<String> currentJvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    for (final String arg : currentJvmArgs) {
      if (!arg.startsWith("--add-opens")
          && !arg.startsWith("--add-exports")
          && !arg.startsWith("-javaagent:")) {
        command.add(arg);
      }
    }

    final String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
    command.add("-cp");
    command.add(classPath);
    command.add(BrokerApplication.class.getName());
    command.addAll(Arrays.asList(args));

    final Process process = new ProcessBuilder(command).inheritIO().start();
    System.exit(process.waitFor());
  }

  private static String resolveJavaExecutable() {

    final String javaHome = System.getProperty("java.home");
    if (javaHome != null) {
      final File candidate = new File(javaHome, "bin/java");
      if (candidate.exists()) {
        return candidate.getAbsolutePath();
      }
    }
    return "java";
  }

  @Override
  public void run(final ApplicationArguments args) {

    final Thread brokerThread = new Thread(this.brokerServer::run, Constant.BROKER_THREAD_NAME);
    brokerThread.setDaemon(false);
    brokerThread.start();

    if (this.isMetricEnabled && this.metricServer.isPresent()) {
      final Thread metricsThread =
          new Thread(this.metricServer.get()::run, Constant.METRICS_THREAD_NAME);
      this.metricServer.get().getThreads().add(brokerThread);
      metricsThread.setDaemon(false);
      metricsThread.start();
    }
  }
}
