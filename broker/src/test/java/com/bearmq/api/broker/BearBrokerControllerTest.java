package com.bearmq.api.broker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bearmq.api.auth.services.TenantContext;
import com.bearmq.api.broker.controllers.BearBrokerController;
import com.bearmq.api.broker.dtos.read.QueuePeekResponseDto;
import com.bearmq.api.broker.dtos.read.QueueSummaryDto;
import com.bearmq.api.common.mapper.ApiErrorResponseMapperImpl;
import com.bearmq.api.facades.BrokerApiFacade;
import com.bearmq.api.facades.BrokerReadFacade;
import com.bearmq.api.security.JwtAuthenticationEntryPoint;
import com.bearmq.api.security.TenantAuthenticationFilter;
import com.bearmq.server.broker.runner.BrokerServer;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.tenant.TenantRole;
import com.bearmq.shared.tenant.TenantStatus;
import com.bearmq.shared.tenant.dto.TenantInfo;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BearBrokerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JwtAuthenticationEntryPoint.class, ApiErrorResponseMapperImpl.class})
class BearBrokerControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BrokerServer brokerServer;
  @MockitoBean private BrokerApiFacade brokerApiFacade;
  @MockitoBean private BrokerReadFacade brokerReadFacade;
  @MockitoBean private TenantContext tenantContext;
  @MockitoBean private TenantAuthenticationFilter tenantAuthenticationFilter;

  private static final TenantInfo TENANT =
      new TenantInfo("t1", "testuser", TenantStatus.ACTIVE, TenantRole.ADMIN);

  @BeforeEach
  void setUp() {
    when(tenantContext.requireTenant()).thenReturn(TENANT);
  }

  @Test
  void listVhosts_returns200WithPage() throws Exception {
    final VirtualHostInfo vhost =
        new VirtualHostInfo(
            "v1", "vhost-name", "user", "pass", "domain.test", "url", null, "ACTIVE");
    when(brokerApiFacade.findAllVhosts(any())).thenReturn(new PageImpl<>(List.of(vhost)));

    mockMvc
        .perform(get("/api/broker/vhost"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("vhost-name"));
  }

  @Test
  void createVhost_returns200WithVhostInfo() throws Exception {
    final VirtualHostInfo vhost =
        new VirtualHostInfo("v2", "new-vhost", "user2", "pass2", "dom.test", "url", null, "ACTIVE");
    when(brokerApiFacade.createVirtualHost(TENANT)).thenReturn(vhost);

    mockMvc
        .perform(post("/api/broker/vhost"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("v2"))
        .andExpect(jsonPath("$.name").value("new-vhost"));
  }

  @Test
  void updateVhostStatus_returns200() throws Exception {
    final VirtualHostInfo vhost =
        new VirtualHostInfo("v1", "n", "u", "p", "d", "u", null, "INACTIVE");
    when(brokerApiFacade.updateVirtualHostStatus(eq("v1"), eq(Status.INACTIVE))).thenReturn(vhost);

    mockMvc
        .perform(
            patch("/api/broker/vhost/v1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  void peekQueue_returns200() throws Exception {
    when(brokerReadFacade.peekQueue(eq("v1"), eq("q1")))
        .thenReturn(new QueuePeekResponseDto(true, "my-q", false, List.of()));

    mockMvc
        .perform(get("/api/broker/vhost/v1/queues/q1/peek"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runtimeLoaded").value(true))
        .andExpect(jsonPath("$.queueName").value("my-q"))
        .andExpect(jsonPath("$.truncated").value(false))
        .andExpect(jsonPath("$.messages").isArray());
  }

  @Test
  void listQueues_returns200WithQueues() throws Exception {
    final QueueSummaryDto q =
        new QueueSummaryDto("q1", "my-q", "q-actual", true, false, false, false, "ACTIVE");
    when(brokerReadFacade.listQueues("v1")).thenReturn(List.of(q));

    mockMvc
        .perform(get("/api/broker/vhost/v1/queues"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("my-q"));
  }

  @Test
  void deleteQueue_returns204() throws Exception {
    doNothing().when(brokerApiFacade).deleteQueue("v1", "q1");

    mockMvc.perform(delete("/api/broker/vhost/v1/queues/q1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteVhost_returns204() throws Exception {
    doNothing().when(brokerApiFacade).deleteVirtualHost(eq("v1"), eq(TENANT));

    mockMvc.perform(delete("/api/broker/vhost/v1")).andExpect(status().isNoContent());
  }

  @Test
  void applyTopology_withValidPayload_returns200() throws Exception {
    doNothing().when(brokerApiFacade).createBrokerObjects(any(), eq(TENANT));

    final String payload =
        "{"
            + "\"vhost\":\"my-vhost\","
            + "\"schemaVersion\":1,"
            + "\"exchanges\":[],"
            + "\"queues\":[],"
            + "\"bindings\":[]"
            + "}";

    mockMvc
        .perform(post("/api/broker").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));
  }
}
