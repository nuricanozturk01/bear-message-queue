import {
  AfterViewChecked,
  Component,
  ElementRef,
  QueryList,
  ViewChildren,
  input,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import hljs from 'highlight.js/lib/core';
import java from 'highlight.js/lib/languages/java';
import kotlin from 'highlight.js/lib/languages/kotlin';
import scala from 'highlight.js/lib/languages/scala';
import python from 'highlight.js/lib/languages/python';
import bash from 'highlight.js/lib/languages/bash';
import yaml from 'highlight.js/lib/languages/yaml';

hljs.registerLanguage('java', java);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('scala', scala);
hljs.registerLanguage('python', python);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('yaml', yaml);

export interface SnippetTab {
  label: string;
  lang: string;
  code: string;
}

@Component({
  selector: 'app-snippet',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './snippet.component.html',
  styleUrl: './snippet.component.scss',
})
export class SnippetComponent implements AfterViewChecked {
  readonly tabs = input.required<SnippetTab[]>();

  readonly activeIdx = signal(0);
  readonly copyHint = signal<string | null>(null);

  @ViewChildren('codeEl') codeElements!: QueryList<ElementRef<HTMLElement>>;
  private highlightedIdx = -1;

  setTab(i: number): void {
    this.activeIdx.set(i);
    this.highlightedIdx = -1;
  }

  ngAfterViewChecked(): void {
    const idx = this.activeIdx();
    if (idx === this.highlightedIdx) return;

    const el = this.codeElements.get(0)?.nativeElement;
    if (!el) return;

    el.removeAttribute('data-highlighted');
    el.textContent = this.tabs()[idx]?.code ?? '';
    hljs.highlightElement(el);
    this.highlightedIdx = idx;
  }

  async copy(): Promise<void> {
    const code = this.tabs()[this.activeIdx()]?.code;
    if (!code) return;
    try {
      await navigator.clipboard.writeText(code);
      this.copyHint.set('Copied!');
      setTimeout(() => this.copyHint.set(null), 2000);
    } catch {
      this.copyHint.set('Failed');
    }
  }
}
