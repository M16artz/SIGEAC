import {expect, test} from '@playwright/test';

// Run against the explicit sqlite-test,d9-archive-test fixture, without HTTP mocks.
const processId = 'd7000000-0000-0000-0000-000000000003';
const screens = [
  {path: '/careers', user: 'admin'},
  {path: '/models', user: 'admin'},
  {path: '/processes', user: 'coordinador'},
  {path: `/processes/${processId}/planning`, user: 'coordinador'},
  {path: `/processes/${processId}/activities`, user: 'coordinador'},
  {path: '/evidence', user: 'responsable'},
  {path: '/evidence/new', user: 'responsable'},
  {path: '/reviews', user: 'revisor'},
  {path: '/reporting', user: 'coordinador'},
  {path: '/reporting/history', user: 'coordinador'},
  {path: '/reporting/closure', user: 'coordinador'},
  {path: '/evaluations', user: 'evaluador'},
  {path: '/evaluations/quantitative', user: 'evaluador'},
  {path: '/evaluations/reconciliation', user: 'evaluador'},
  {path: '/reports', user: 'coordinador'},
  {path: '/improvement', user: 'coordinador'},
  {path: '/archive', user: 'coordinador'}
];

for (const screen of screens) {
  test(`keyboard, landmarks and control names: ${screen.path}`, async ({page}) => {
    await page.goto(`/access/login?returnUrl=${encodeURIComponent(screen.path)}`);
    await expect(page.locator('#username')).toBeVisible();
    await page.keyboard.press('Tab');
    const skip = page.getByRole('link', {name: 'Saltar al contenido principal'});
    await expect(skip).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('#main-content')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('#username')).toBeFocused();
    await page.keyboard.type(screen.user);
    await page.keyboard.press('Tab');
    await expect(page.locator('#password')).toBeFocused();
    await page.keyboard.type(screen.user === 'admin' ? 'Synthetic-Admin-2026' : 'Synthetic-User-2026');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('button', {name: 'Ingresar', exact: true})).toBeFocused();
    await page.keyboard.press('Enter');
    await page.waitForURL(url => url.pathname === screen.path);
    await expect(page.getByRole('heading', {level: 2}).first()).toBeVisible();

    // Select the fixture process with the keyboard to expose dynamic forms.
    const processes = page.locator('select').filter({has: page.locator(`option[value="${processId}"]`)}).first();
    if (await processes.count()) {
      await processes.focus();
      await processes.press('Home');
      const options = await processes.locator('option').evaluateAll(nodes => nodes.map(n => (n as HTMLOptionElement).value));
      for (let i = 0; i < options.indexOf(processId); i++) await processes.press('ArrowDown');
      await processes.press('Tab');
      await expect(processes).toHaveValue(processId);
    }
    await expect(page.getByRole('main')).toHaveCount(1);
    await expect(page.getByRole('navigation', {name: 'Navegación principal'})).toBeVisible();
    if (['/reports', '/improvement', '/archive'].includes(screen.path)) {
      await expect(page.locator(`nav[aria-label="Navegación principal"] a[href="${screen.path}"]`)).toBeVisible();
    }
    for (const control of await page.locator('main input:not([type="hidden"]), main select, main textarea, main button').all()) {
      if (await control.isVisible()) await expect(control).toHaveAccessibleName(/\S/);
    }
    for (const header of await page.locator('main th').all()) {
      await expect(header).toHaveAttribute('scope', /^(col|row|colgroup|rowgroup)$/);
    }
    // The next tab enters a control inside the main content.
    await page.locator('#main-content').focus();
    await page.keyboard.press('Tab');
    await expect(page.locator('main :focus')).toHaveCount(1);
    await expect(page.locator('footer')).toContainText('no constituye acreditación');
  });
}

test('invalid login exposes a readable live error with keyboard recovery', async ({page}) => {
  await page.goto('/access/login');
  await page.getByRole('button', {name: 'Ingresar', exact: true}).click();
  await expect(page.getByRole('alert')).toHaveCount(2);
  for (const alert of await page.getByRole('alert').all()) await expect(alert).toContainText(/Ingrese/);
  await page.locator('#username').focus();
  await page.keyboard.type('coordinador');
  await page.keyboard.press('Tab');
  await expect(page.locator('#password')).toBeFocused();
});
