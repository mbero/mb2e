import { TestBed } from '@angular/core/testing';
import { AccessDeniedPage } from './access-denied-page';
import { SignInPage } from './sign-in-page';

describe('pages', () => {
  it('the sign-in page starts the GitHub login of the server', async () => {
    const fixture = TestBed.createComponent(SignInPage);
    await fixture.whenStable();
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('a.github');
    expect(link.getAttribute('href')).toBe('/oauth2/authorization/github');
    expect(link.textContent).toContain('Sign in with GitHub');
  });

  it('the denied page names the GitHub account as text', async () => {
    const fixture = TestBed.createComponent(AccessDeniedPage);
    fixture.componentRef.setInput('login', '<b>intruder</b>');
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('strong')?.textContent).toBe('<b>intruder</b>');
    expect(element.querySelector('strong b')).toBeNull();
  });
});
