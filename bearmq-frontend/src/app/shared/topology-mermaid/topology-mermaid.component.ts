import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  PLATFORM_ID,
  signal,
  viewChild,
  effect,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import type { BindingSummaryDto } from '../../core/models/api.types';

@Component({
  selector: 'app-topology-mermaid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './topology-mermaid.component.html',
  styleUrl: './topology-mermaid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopologyMermaidComponent {
  private readonly platformId = inject(PLATFORM_ID);

  readonly bindings = input.required<BindingSummaryDto[]>();
  private readonly host = viewChild<ElementRef<HTMLElement>>('host');

  readonly renderError = signal<string | null>(null);

  private readonly viewReady = signal(false);
  private paintGen = 0;

  constructor() {
    afterNextRender(() => this.viewReady.set(true));
    effect(() => {
      this.bindings();
      if (!this.viewReady()) {
        return;
      }
      this.triggerPaint();
    });
  }

  private triggerPaint(): void {
    if (!isPlatformBrowser(this.platformId) || !this.viewReady()) {
      return;
    }
    const el = this.host()?.nativeElement;
    if (!el) {
      return;
    }
    const list = this.bindings();
    const gen = ++this.paintGen;
    void this.paint(el, list, gen);
  }

  private async paint(el: HTMLElement, list: BindingSummaryDto[], gen: number): Promise<void> {
    this.renderError.set(null);
    el.innerHTML =
      '<div class="topology-mermaid-loading text-body-secondary small py-5 text-center">' +
      '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Rendering…</div>';

    if (list.length === 0) {
      el.innerHTML = '';
      return;
    }

    try {
      const definition = this.buildDefinition(list);
      const mermaid = (await import('mermaid')).default;
      const dark =
        typeof document !== 'undefined' &&
        document.documentElement.getAttribute('data-bs-theme') === 'dark';

      mermaid.initialize({
        startOnLoad: false,
        securityLevel: 'loose',
        theme: dark ? 'dark' : 'base',
        themeVariables: dark
          ? {
              primaryColor: '#7c3aed',
              primaryTextColor: '#f5f3ff',
              secondaryColor: '#0891b2',
              tertiaryColor: '#1e293b',
              lineColor: '#94a3b8',
              edgeLabelBackground: '#334155',
              fontFamily: 'ui-sans-serif, system-ui, sans-serif',
            }
          : {
              primaryColor: '#ede9fe',
              primaryTextColor: '#5b21b6',
              secondaryColor: '#cffafe',
              tertiaryColor: '#f8fafc',
              lineColor: '#64748b',
              edgeLabelBackground: '#f1f5f9',
              fontFamily: 'ui-sans-serif, system-ui, sans-serif',
            },
        flowchart: {
          curve: 'basis',
          padding: 20,
          nodeSpacing: 48,
          rankSpacing: 64,
          htmlLabels: true,
        },
      });

      const uid = `mmd-topo-${gen}-${Math.random().toString(36).slice(2, 10)}`;
      const { svg } = await mermaid.render(uid, definition);
      if (gen !== this.paintGen) {
        return;
      }
      el.innerHTML = svg;
    } catch (e: unknown) {
      if (gen !== this.paintGen) {
        return;
      }
      const msg = e instanceof Error ? e.message : 'Could not render diagram.';
      this.renderError.set(msg);
      el.innerHTML = '';
    }
  }

  private buildDefinition(bindings: BindingSummaryDto[]): string {
    const dark =
      typeof document !== 'undefined' &&
      document.documentElement.getAttribute('data-bs-theme') === 'dark';

    const init = dark
      ? "%%{init: {'theme':'dark'}}%%\n"
      : "%%{init: {'theme':'base','themeVariables': {'primaryColor':'#ede9fe','primaryTextColor':'#5b21b6','secondaryColor':'#cffafe','lineColor':'#64748b'}}}%%\n";

    const lines: string[] = [init, 'flowchart LR'];

    if (dark) {
      lines.push(
        'classDef exNode fill:#5b21b633,stroke:#a78bfa,stroke-width:2px,color:#e9d5ff;',
        'classDef quNode fill:#0e749033,stroke:#22d3ee,stroke-width:2px,color:#cffafe;',
      );
    } else {
      lines.push(
        'classDef exNode fill:#7c3aed22,stroke:#7c3aed,stroke-width:2px,color:#5b21b6;',
        'classDef quNode fill:#0891b222,stroke:#0891b2,stroke-width:2px,color:#0e7490;',
      );
    }

    const ids = new Map<string, string>();
    let c = 0;
    const nodeId = (kind: 'ex' | 'qu', name: string): string => {
      const key = `${kind}\0${name}`;
      let id = ids.get(key);
      if (!id) {
        id = `v${c++}`;
        ids.set(key, id);
      }
      return id;
    };

    const declared = new Set<string>();

    const emitExchange = (id: string, name: string): void => {
      if (declared.has(id)) {
        return;
      }
      declared.add(id);
      const label = this.safeNodeLabel(name, 'EXCHANGE');
      lines.push(`  ${id}[["${label}"]]:::exNode`);
    };

    const emitQueue = (id: string, name: string): void => {
      if (declared.has(id)) {
        return;
      }
      declared.add(id);
      const label = this.safeNodeLabel(name, 'QUEUE');
      lines.push(`  ${id}[("${label}")]:::quNode`);
    };

    for (const b of bindings) {
      const sid = nodeId('ex', b.sourceExchangeName);
      emitExchange(sid, b.sourceExchangeName);
      if (b.destinationType === 'QUEUE') {
        const tid = nodeId('qu', b.destinationName);
        emitQueue(tid, b.destinationName);
        lines.push(`  ${sid} -->|"${this.safeEdgeLabel(b.routingKey)}"| ${tid}`);
      } else {
        const tid = nodeId('ex', b.destinationName);
        emitExchange(tid, b.destinationName);
        lines.push(`  ${sid} -->|"${this.safeEdgeLabel(b.routingKey)}"| ${tid}`);
      }
    }

    return lines.join('\n');
  }

  private safeNodeLabel(name: string, role: string): string {
    const n = name.replace(/"/g, '#quot;').replace(/[\n\r]/g, ' ').trim() || '—';
    return `${n}<br/><sub>${role}</sub>`;
  }

  private safeEdgeLabel(routingKey: string | null | undefined): string {
    const t = (routingKey?.trim() || '∅').replace(/["|\\]/g, ' ').replace(/\|/g, '/');
    return t.length > 48 ? t.slice(0, 46) + '…' : t;
  }
}
