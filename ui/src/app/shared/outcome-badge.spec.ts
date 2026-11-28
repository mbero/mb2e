import { TestBed } from '@angular/core/testing';
import { Outcome } from '../core/models';
import { OutcomeBadge } from './outcome-badge';

describe('OutcomeBadge', () => {
  it.each<[Outcome, string]>([
    ['deployed', 'deployed'],
    ['up_to_date', 'up to date'],
    ['refused', 'refused'],
    ['failed', 'failed'],
  ])(
    'shows %s as the text "%s" with its own style class and a hidden glyph',
    async (outcome, label) => {
      const fixture = TestBed.createComponent(OutcomeBadge);
      fixture.componentRef.setInput('outcome', outcome);
      await fixture.whenStable();
      const host: HTMLElement = fixture.nativeElement;
      expect(host.textContent?.trim()).toBe(label);
      expect(host.classList).toContain(outcome);
      expect(host.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    },
  );
});
