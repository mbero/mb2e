import { TestBed } from '@angular/core/testing';
import { SAMPLE_DEPLOYMENTS } from '../testing/sample-data';
import { DeploymentTable } from './deployment-table';

describe('DeploymentTable', () => {
  const setUp = async () => {
    const fixture = TestBed.createComponent(DeploymentTable);
    fixture.componentRef.setInput('deployments', SAMPLE_DEPLOYMENTS);
    fixture.componentRef.setInput('sourceRepo', 'mbero/notes');
    fixture.componentRef.setInput('targetRepo', 'mbero/mbero.github.io');
    fixture.componentRef.setInput('maxAttempts', 3);
    fixture.componentRef.setInput('now', Date.parse('2026-11-25T21:22:03Z'));
    await fixture.whenStable();
    return fixture;
  };

  it('offers an expand button only for runs with a cause', async () => {
    const element: HTMLElement = (await setUp()).nativeElement;
    expect(element.querySelectorAll('table .expand')).toHaveLength(4);
    expect(element.querySelector('table')?.textContent).toContain('2 of 3');
    expect(element.querySelector('table')?.textContent).toContain('— no change');
  });

  it('opens a cause and keeps it open after a refresh with new objects', async () => {
    const fixture = await setUp();
    const element: HTMLElement = fixture.nativeElement;
    const button = element.querySelector<HTMLButtonElement>('table .expand')!;
    expect(button.getAttribute('aria-expanded')).toBe('false');
    button.click();
    await fixture.whenStable();
    expect(element.querySelector('table .cause-row pre')?.textContent).toContain('token expired');
    fixture.componentRef.setInput('deployments', SAMPLE_DEPLOYMENTS.map((d) => ({ ...d })));
    await fixture.whenStable();
    expect(element.querySelector('table .expand')?.getAttribute('aria-expanded')).toBe('true');
    expect(element.querySelector('table .cause-row')).not.toBeNull();
  });

  it('shows a cause as text, never as markup', async () => {
    const fixture = TestBed.createComponent(DeploymentTable);
    fixture.componentRef.setInput('deployments', [{ ...SAMPLE_DEPLOYMENTS[0], cause: '<img src=x onerror=alert(1)>' }]);
    fixture.componentRef.setInput('sourceRepo', 'a/b');
    fixture.componentRef.setInput('targetRepo', 'a/c');
    fixture.componentRef.setInput('maxAttempts', 3);
    fixture.componentRef.setInput('now', Date.now());
    await fixture.whenStable();
    fixture.nativeElement.querySelector('table .expand').click();
    await fixture.whenStable();
    const pre: HTMLElement = fixture.nativeElement.querySelector('table .cause-row pre');
    expect(pre.textContent).toBe('<img src=x onerror=alert(1)>');
    expect(pre.querySelector('img')).toBeNull();
  });
});
