import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { BrokerService } from '../../core/services/broker.service';
import { ToastService } from '../../core/services/toast.service';
import type {
  BindingSummaryDto,
  ExchangeSummaryDto,
  PeekedMessageDto,
  QueueMetricDto,
  QueuePeekResponseDto,
  QueueSummaryDto,
  VhostMetricsDto,
  VirtualHostInfo,
} from '../../core/models/api.types';
import { SnippetComponent, type SnippetTab } from '../../shared/snippet/snippet.component';
import { TopologyMermaidComponent } from '../../shared/topology-mermaid/topology-mermaid.component';

type TabId =
  | 'overview'
  | 'preview'
  | 'queues'
  | 'exchanges'
  | 'bindings'
  | 'metrics'
  | 'snippets';

@Component({
  selector: 'app-instance-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, SnippetComponent, TopologyMermaidComponent],
  templateUrl: './instance-detail.component.html',
  styleUrl: './instance-detail.component.scss',
})
export class InstanceDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly broker = inject(BrokerService);
  private readonly toast = inject(ToastService);
  readonly auth = inject(AuthService);

  readonly vhost = signal<VirtualHostInfo | null>(null);
  readonly queues = signal<QueueSummaryDto[]>([]);
  readonly exchanges = signal<ExchangeSummaryDto[]>([]);
  readonly bindings = signal<BindingSummaryDto[]>([]);
  readonly metrics = signal<VhostMetricsDto | null>(null);
  readonly tab = signal<TabId>('overview');
  readonly loading = signal(true);
  readonly copyHint = signal<string | null>(null);
  readonly statusUpdating = signal(false);
  readonly pendingDelete = signal<{
    kind: 'queue' | 'exchange' | 'binding' | 'vhost';
    id: string;
    label: string;
  } | null>(null);
  readonly deleteBusy = signal(false);

  readonly peekTarget = signal<{ queueId: string; queueName: string } | null>(null);
  readonly peekData = signal<QueuePeekResponseDto | null>(null);
  readonly peekLoading = signal(false);
  readonly peekError = signal<string | null>(null);

  /** Exchanges that never appear as a binding source (may still receive via default exchange). */
  readonly topologyOrphanExchanges = computed(() => {
    const sources = new Set(this.bindings().map((b) => b.sourceExchangeName));
    return this.exchanges()
      .filter((e) => !sources.has(e.name))
      .map((e) => e.name);
  });

  /** Queues with no inbound binding from an exchange. */
  readonly topologyOrphanQueues = computed(() => {
    const bound = new Set(
      this.bindings()
        .filter((b) => b.destinationType === 'QUEUE')
        .map((b) => b.destinationName),
    );
    return this.queues().filter((q) => !bound.has(q.name)).map((q) => q.name);
  });

  readonly snippetTabs = computed<SnippetTab[]>(() => {
    const v = this.vhost();
    if (!v) return [];
    const host = v.domain ?? 'localhost';
    const vhost = v.name;
    const user = v.username;
    const pass = v.password;
    const broker_port = '6667';

    return [
      {
        label: 'Java',
        lang: 'java',
        code: [
          '// Maven: com.bearmq:bearmq-spring-client',
          '@SpringBootApplication',
          '@EnableBear',
          'public class App { public static void main(String[] a) { SpringApplication.run(App.class, a); } }',
          '',
          '// application.yml snippet:',
          '// bearmq:',
          `//   host: ${host}`,
          `//   port: ${broker_port}`,
          `//   virtual-host: ${vhost}`,
          `//   username: ${user}`,
          `//   password: ${pass}`,
          '',
          '// Publish',
          '@Service',
          'public class OrderService {',
          '    @Autowired private BearTemplate bear;',
          '',
          '    public void placeOrder(Order order) {',
          `        bear.convertAndSend("${vhost}", "events", "orders.placed", order);`,
          '    }',
          '}',
          '',
          '// Consume',
          '@Component',
          'public class OrderConsumer {',
          `    @BearListener(queues = "orders-queue", vhost = "${vhost}")`,
          '    public void handle(Message msg) {',
          '        System.out.println("Received: " + new String(msg.getBody()));',
          '    }',
          '}',
        ].join('\n'),
      },
      {
        label: 'Kotlin',
        lang: 'kotlin',
        code: [
          '// application.yml: same as Java tab above',
          '',
          '// Publish (suspend function)',
          '@Service',
          'class OrderService(@Autowired private val bear: BearTemplate) {',
          '    fun placeOrder(order: Order) {',
          `        bear.convertAndSend("${vhost}", "events", "orders.placed", order)`,
          '    }',
          '}',
          '',
          '// Consume',
          '@Component',
          'class OrderConsumer {',
          `    @BearListener(queues = ["orders-queue"], vhost = "${vhost}")`,
          '    fun handle(msg: Message) {',
          '        println("Received: ${String(msg.body)}")',
          '    }',
          '}',
        ].join('\n'),
      },
      {
        label: 'Scala',
        lang: 'scala',
        code: [
          '// Using bearmq-spring-client via Spring Boot on JVM',
          '',
          '@SpringBootApplication',
          'class App',
          'object App extends App {',
          '  SpringApplication.run(classOf[App])',
          '}',
          '',
          '// Publish',
          '@Service',
          'class OrderService @Autowired()(bear: BearTemplate) {',
          '  def placeOrder(order: Order): Unit =',
          `    bear.convertAndSend("${vhost}", "events", "orders.placed", order)`,
          '}',
          '',
          '// Consume',
          '@Component',
          'class OrderConsumer {',
          `  @BearListener(queues = Array("orders-queue"), vhost = "${vhost}")`,
          '  def handle(msg: Message): Unit =',
          '    println(s"Received: ${new String(msg.getBody())}")',
          '}',
        ].join('\n'),
      },
      {
        label: 'Python',
        lang: 'python',
        code: [
          'import socket, json, base64',
          '',
          `HOST, PORT = "${host}", ${broker_port}`,
          `VHOST, USER, PASS = "${vhost}", "${user}", "${pass}"`,
          '',
          'def connect():',
          '    conn = socket.create_connection((HOST, PORT))',
          '    auth = json.dumps({',
          '        "type": "AUTH",',
          '        "vhost": base64.b64encode(VHOST.encode()).decode(),',
          '        "username": base64.b64encode(USER.encode()).decode(),',
          '        "password": base64.b64encode(PASS.encode()).decode(),',
          '    })',
          '    conn.sendall((auth + "\\n").encode())',
          '    return conn',
          '',
          'def publish(conn, exchange, routing_key, body):',
          '    msg = json.dumps({',
          '        "type": "PUBLISH",',
          '        "exchange": exchange,',
          '        "routingKey": routing_key,',
          '        "body": body,',
          '    })',
          '    conn.sendall((msg + "\\n").encode())',
        ].join('\n'),
      },
      {
        label: 'curl',
        lang: 'bash',
        code: [
          '# Get access token',
          `TOKEN=$(curl -s -X POST http://${host}:3333/api/auth/login \\`,
          '  -H "Content-Type: application/json" \\',
          `  -d '{"username":"${user}","password":"${pass}"}' \\`,
          "  | jq -r '.access_token')",
          '',
          '# List queues',
          `curl -s http://${host}:3333/api/broker/vhost/${vhost}/queues \\`,
          '  -H "Authorization: Bearer $TOKEN" | jq .',
          '',
          '# Apply topology',
          `curl -s -X POST http://${host}:3333/api/broker \\`,
          '  -H "Authorization: Bearer $TOKEN" \\',
          '  -H "Content-Type: application/json" \\',
          `  -d '{"vhost":"${vhost}","schemaVersion":1,"exchanges":[{"name":"events","type":"TOPIC","durable":true,"internal":false,"delayed":false,"args":{}}],"queues":[{"name":"jobs","durable":true,"exclusive":false,"auto_delete":false,"arguments":{}}],"bindings":[{"source":"events","destination":"jobs","destination_type":"QUEUE","routing_key":"#"}]}'`,
        ].join('\n'),
      },
    ];
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.broker.listVhosts(0, 500).subscribe({
      next: (page) => {
        const v = page.content.find((x) => x.id === id) ?? null;
        this.vhost.set(v);
        this.loading.set(false);
        if (v) {
          this.loadTables(v.id);
        }
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load instance details.');
      },
    });
  }

  private loadTables(vhostId: string): void {
    this.broker.listQueues(vhostId).subscribe({ next: (q) => this.queues.set(q) });
    this.broker.listExchanges(vhostId).subscribe({ next: (e) => this.exchanges.set(e) });
    this.broker.listBindings(vhostId).subscribe({ next: (b) => this.bindings.set(b) });
    this.broker.getVhostMetrics(vhostId).subscribe({ next: (m) => this.metrics.set(m) });
  }

  onStatusSelect(event: Event): void {
    const v = this.vhost();
    if (!v) return;
    const el = event.target as HTMLSelectElement;
    const next = el.value as 'ACTIVE' | 'PAUSED' | 'INACTIVE';
    const current = v.status || 'ACTIVE';
    if (next === current) return;
    this.statusUpdating.set(true);
    this.broker.patchVhostStatus(v.id, next).subscribe({
      next: (info) => {
        this.vhost.set(info);
        this.statusUpdating.set(false);
        this.toast.success(`Instance status set to ${next}.`);
        this.loadTables(info.id);
      },
      error: (err: { error?: { message?: string } }) => {
        this.statusUpdating.set(false);
        el.value = current;
        this.toast.error(err.error?.message ?? 'Could not update status.');
      },
    });
  }

  confirmDelete(kind: 'queue' | 'exchange' | 'binding', id: string, label: string, ev: Event): void {
    ev.stopPropagation();
    this.pendingDelete.set({ kind, id, label });
  }

  confirmDeleteInstance(ev: Event): void {
    ev.preventDefault();
    const v = this.vhost();
    if (!v) return;
    this.pendingDelete.set({ kind: 'vhost', id: v.id, label: v.name });
  }

  cancelDelete(): void {
    this.pendingDelete.set(null);
  }

  executeDelete(): void {
    const p = this.pendingDelete();
    const v = this.vhost();
    if (!p || !v) return;
    this.deleteBusy.set(true);
    const done = (): void => {
      this.deleteBusy.set(false);
      this.cancelDelete();
      this.loadTables(v.id);
    };
    const fail = (): void => {
      this.deleteBusy.set(false);
      this.toast.error('Delete failed.');
    };
    if (p.kind === 'vhost') {
      this.broker.deleteVhost(p.id).subscribe({
        next: () => {
          this.deleteBusy.set(false);
          this.cancelDelete();
          this.toast.success('Instance removed.');
          void this.router.navigate(['/instances']);
        },
        error: fail,
      });
      return;
    }
    if (p.kind === 'queue') {
      this.broker.deleteQueue(v.id, p.id).subscribe({ next: () => { done(); this.toast.success('Queue removed.'); }, error: fail });
    } else if (p.kind === 'exchange') {
      this.broker.deleteExchange(v.id, p.id).subscribe({ next: () => { done(); this.toast.success('Exchange removed.'); }, error: fail });
    } else {
      this.broker.deleteBinding(v.id, p.id).subscribe({ next: () => { done(); this.toast.success('Binding removed.'); }, error: fail });
    }
  }

  setTab(t: TabId): void {
    this.tab.set(t);
  }

  openPeek(q: QueueSummaryDto): void {
    const v = this.vhost();
    if (!v) return;
    this.peekTarget.set({ queueId: q.id, queueName: q.name });
    this.peekData.set(null);
    this.peekError.set(null);
    this.peekLoading.set(true);
    this.broker.peekQueue(v.id, q.id).subscribe({
      next: (d) => {
        this.peekData.set(d);
        this.peekLoading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.peekError.set(err.error?.message ?? 'Could not load messages.');
        this.peekLoading.set(false);
      },
    });
  }

  closePeek(): void {
    this.peekTarget.set(null);
    this.peekData.set(null);
    this.peekError.set(null);
    this.peekLoading.set(false);
  }

  peekBodyKind(m: PeekedMessageDto): 'json' | 'text' | 'base64' {
    if (m.json !== undefined && m.json !== null) {
      return 'json';
    }
    if (m.text != null) {
      return 'text';
    }
    if (m.base64 != null) {
      return 'base64';
    }
    return 'text';
  }

  peekBodyDisplay(m: PeekedMessageDto): string {
    if (m.json !== undefined && m.json !== null) {
      return JSON.stringify(m.json, null, 2);
    }
    if (m.text != null) {
      return m.text;
    }
    if (m.base64 != null) {
      return m.base64;
    }
    return '';
  }

  pendingMessagesLabel(q: QueueMetricDto): string {
    if (q.approximatePendingMessages < 0) {
      return '—';
    }
    if (q.approximatePendingCapped) {
      return `${q.approximatePendingMessages}+`;
    }
    return String(q.approximatePendingMessages);
  }

  async copy(text: string, label: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.copyHint.set(`${label} copied`);
      setTimeout(() => this.copyHint.set(null), 2000);
    } catch {
      this.copyHint.set('Copy failed');
    }
  }
}
