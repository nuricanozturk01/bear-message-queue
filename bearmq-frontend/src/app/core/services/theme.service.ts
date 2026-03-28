import { Injectable, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<ThemeMode>('light');

  constructor() {
    const stored = localStorage.getItem('bearmq-theme') as ThemeMode | null;
    if (stored === 'dark' || stored === 'light') {
      this.apply(stored);
    } else if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      this.apply('dark');
    } else {
      this.apply('light');
    }
  }

  toggle(): void {
    this.apply(this.theme() === 'light' ? 'dark' : 'light');
  }

  setTheme(mode: ThemeMode): void {
    this.apply(mode);
  }

  private apply(mode: ThemeMode): void {
    this.theme.set(mode);
    document.documentElement.setAttribute('data-bs-theme', mode);
    localStorage.setItem('bearmq-theme', mode);
  }
}
