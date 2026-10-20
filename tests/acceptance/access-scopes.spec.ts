import { expect, test } from '@playwright/test';

const ADMIN_USER = process.env['SEC_SYNTHETIC_ADMIN_USER'] ?? 'admin';
const ADMIN_PASSWORD = process.env['SEC_SYNTHETIC_ADMIN_PASSWORD'] ?? 'Synthetic-Admin-2026';
const CAREER_ID = '11111111-1111-1111-1111-111111111111';
const CONFLICT_POLICY_ID = '30000000-0000-0000-0000-000000000001';

test.describe('I0 acceso y ambitos autorizados', () => {
  test('administra identidad, asignacion, excepcion y auditoria', async ({page}) => {
    await page.goto('/access/login');
    await page.getByLabel('Usuario', {exact: true}).fill(ADMIN_USER);
    await page.getByLabel(/Contrase/).fill(ADMIN_PASSWORD);
    await page.getByRole('button', {name: /Ingresar/}).click();

    await expect(page.getByRole('heading', {name: /Acceso y/})).toBeVisible();
    await expect(page.getByText('Administrador Sintetico')).toBeVisible();

    await page.getByRole('link', {name: /Usuarios y asignaciones/}).click();
    const username = `sintetico.${Date.now()}`;
    await page.getByLabel('Usuario', {exact: true}).last().fill(username);
    await page.getByLabel(/Nombre visible/).fill('Identidad Sintetica E2E');
    await page.getByLabel(/Contrase/).fill('Synthetic-User-2026');
    await page.getByRole('button', {name: /Registrar usuario/}).click();
    await expect(page.getByRole('status')).toContainText(/Usuario registrado correctamente/);

    const row = page.getByRole('row', {name: new RegExp(username)});
    await expect(row).toBeVisible();
    const userId = await row.getAttribute('data-user-id');
    expect(userId).not.toBeNull();
    await row.getByRole('button', {name: /Asignaciones/}).click();
    await page.getByLabel(/Carrera/).fill(CAREER_ID);
    await page.getByRole('button', {name: /Registrar asignaci/}).click();
    await expect(page.getByRole('status')).toContainText(/Asignaci/);

    await page.getByRole('link', {name: /Conflictos y excepciones/}).click();
    await page.getByLabel(/Pol.tica de conflicto/).fill(CONFLICT_POLICY_ID);
    await page.getByLabel(/Usuario destino/).fill(userId ?? '');
    await page.getByLabel(/Carrera/).fill(CAREER_ID);
    await page.getByLabel(/Justificaci/).fill('Excepcion sintetica para recorrido de aceptacion.');
    await page.getByRole('button', {name: /Registrar excepci/}).click();

    await page.getByRole('link', {name: /Auditor.a de acceso/}).click();
    await expect(page.getByRole('heading', {name: /Auditor.a de acceso/})).toBeVisible();
    await expect(page.getByRole('cell', {name: 'SESSION_LOGIN'}).first()).toBeVisible();

    await page.getByRole('link', {name: 'Acceso', exact: true}).click();
    await page.getByRole('button', {name: /Cerrar sesi/}).click();
    await expect(page.getByRole('heading', {name: /Iniciar sesi/})).toBeVisible();
  });
});
