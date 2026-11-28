import { TestBed } from '@angular/core/testing';
import { Deployment } from '../core/models';
import { SAMPLE_DEPLOYMENTS } from '../testing/sample-data';
import { LastDeployment } from './last-deployment';

describe('LastDeployment', () => {
  const render = async (deployment: Deployment, previous: Deployment | null) => {
    const fixture = TestBed.createComponent(LastDeployment);
    fixture.componentRef.setInput('deployment', deployment);
    fixture.componentRef.setInput('previous', previous);
    fixture.componentRef.setInput('sourceRepo', 'mbero/notes');
    fixture.componentRef.setInput('targetRepo', 'mbero/mbero.github.io');
    fixture.componentRef.setInput('maxAttempts', 3);
    fixture.componentRef.setInput('now', Date.parse('2026-11-25T21:22:03Z'));
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).textContent!.replace(/\s+/g, ' ');
  };

  it('explains a failure, names what the site still shows and opens the cause', async () => {
    const text = await render(SAMPLE_DEPLOYMENTS[0], SAMPLE_DEPLOYMENTS[1]);
    expect(text).toContain('Your last push did not deploy');
    expect(text).toContain('The site still shows the deployment from');
    expect(text).toContain('9f8e7d6');
    expect(text).toContain('token expired');
    expect(text).toContain('Attempts 3 of 3');
  });

  it('drops the "still shows" clause without an earlier deployment', async () => {
    expect(await render(SAMPLE_DEPLOYMENTS[0], null)).not.toContain('still shows');
  });

  it('calls an unchanged site normal, not an error', async () => {
    const text = await render(SAMPLE_DEPLOYMENTS[2], null);
    expect(text).toContain('Nothing to publish');
    expect(text).toContain('This is normal.');
    expect(text).not.toContain('Cause');
  });
});
