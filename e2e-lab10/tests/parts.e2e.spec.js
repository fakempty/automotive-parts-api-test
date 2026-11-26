const { test, expect } = require('@playwright/test');
const { PartsPage } = require('../pages/PartsPage');

test.describe('Lab 10 - UI/E2E Testing for AutoApiTesting', () => {
  test('1. Positive scenario: create part via UI and verify display @cross-browser', async ({ page }) => {
    const partsPage = new PartsPage(page);
    const uniqueName = 'E2E-Positive-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

    try {
      await partsPage.open();
      const status = await partsPage.createPart({
        name: uniqueName,
        category: 'Brakes',
        price: 95.5,
        stock: 8
      });

      expect(status).toBe(200);
      const createdRow = partsPage.rowByName(uniqueName);
      await expect(createdRow).toBeVisible({ timeout: 15000 });
      await expect(createdRow.getByTestId('cell-category')).toHaveText('Brakes');
      await expect(createdRow.getByTestId('cell-price')).toHaveText('95.5');
      await expect(createdRow.getByTestId('cell-stock')).toHaveText('8');
      await expect(createdRow.getByTestId('cell-status')).toHaveText('ACTIVE');
    } finally {
      await partsPage.cleanupPartIfExists(uniqueName);
    }
  });

  test('2. Business action via UI: cancel/delete part and verify removal', async ({ page }) => {
    const partsPage = new PartsPage(page);
    const uniqueName = 'E2E-Cancel-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

    try {
      await partsPage.open();
      const createStatus = await partsPage.createPart({
        name: uniqueName,
        category: 'Suspension',
        price: 120,
        stock: 4
      });
      expect(createStatus).toBe(200);
      await expect(partsPage.rowByName(uniqueName)).toBeVisible();

      const deleteStatus = await partsPage.cancelPartByName(uniqueName);
      expect(deleteStatus).toBe(200);
      await expect(partsPage.rowByName(uniqueName).getByTestId('cell-status')).toHaveText('CANCELLED', { timeout: 10000 });
    } finally {
      await partsPage.cleanupPartIfExists(uniqueName);
    }
  });

  test('3. Backend business rejection via UI: duplicate/forbidden action shows error banner', async ({ page }) => {
    const partsPage = new PartsPage(page);
    const uniqueName = 'E2E-Reject-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

    try {
      await partsPage.open();
      await partsPage.createPart({
        name: uniqueName,
        category: 'Engine',
        price: 60,
        stock: 3
      });

      const partId = await partsPage.getIdByName(uniqueName);

      const firstCancel = await page.request.delete('/api/parts/' + partId);
      expect(firstCancel.status()).toBe(200);

      await page.evaluate((id) => {
        const tbody = document.getElementById('partsBody');
        const tr = document.createElement('tr');
        tr.setAttribute('data-testid', 'part-row');
        tr.innerHTML =
          '<td data-testid="cell-id">' + id + '</td>' +
          '<td data-testid="cell-name">Already-Cancelled-' + id + '</td>' +
          '<td data-testid="cell-category">Engine</td>' +
          '<td data-testid="cell-price">60</td>' +
          '<td data-testid="cell-stock">3</td>' +
          '<td data-testid="cell-status">CANCELLED</td>' +
          '<td data-testid="cell-rule">STANDARD_TIER</td>' +
          '<td><button class="danger" data-testid="delete-part-btn" onclick="deletePart(\'' + id + '\')">РЎРєР°СЃСѓРІР°С‚Рё / Р’РёРґР°Р»РёС‚Рё</button></td>';
        tbody.prepend(tr);
      }, partId);

      const rejectedRow = page.getByTestId('part-row').filter({ hasText: 'Already-Cancelled-' + partId });
      const resPromise = page.waitForResponse(
        (res) => res.url().endsWith('/api/parts/' + partId) && res.request().method() === 'DELETE'
      );
      await rejectedRow.getByTestId('delete-part-btn').click();
      const rejectedRes = await resPromise;

      expect(rejectedRes.status()).toBe(409);
      await expect(partsPage.errorBanner).toBeVisible();
      await expect(partsPage.errorBanner).toContainText('409');
    } finally {
      await partsPage.cleanupPartIfExists(uniqueName);
    }
  });

  test('4. Personal business rule (price >= 150): both triggered and not-triggered cases', async ({ page }) => {
    const partsPage = new PartsPage(page);
    const highName = 'E2E-RuleHigh-' + Date.now();
    const stdName = 'E2E-RuleStd-' + Date.now();

    try {
      await partsPage.open();

      await partsPage.createPart({
        name: highName,
        category: 'Turbo',
        price: 250,
        stock: 2
      });
      const highRow = partsPage.rowByName(highName);
      await expect(highRow).toBeVisible();
      await expect(highRow.getByTestId('cell-rule')).toHaveText('HIGH_TIER');

      await partsPage.createPart({
        name: stdName,
        category: 'Filters',
        price: 45,
        stock: 12
      });
      const stdRow = partsPage.rowByName(stdName);
      await expect(stdRow).toBeVisible();
      await expect(stdRow.getByTestId('cell-rule')).toHaveText('STANDARD_TIER');
    } finally {
      await partsPage.cleanupPartIfExists(highName);
      await partsPage.cleanupPartIfExists(stdName);
    }
  });

  test('5. Reload persistence: data and status remain after page reload', async ({ page }) => {
    const partsPage = new PartsPage(page);
    const uniqueName = 'E2E-Reload-' + Date.now() + '-' + Math.floor(Math.random() * 1000);

    try {
      await partsPage.open();
      const createStatus = await partsPage.createPart({
        name: uniqueName,
        category: 'Ignition',
        price: 175,
        stock: 6
      });
      expect(createStatus).toBe(200);
      await expect(partsPage.rowByName(uniqueName)).toBeVisible();

      await partsPage.reloadAndWait();

      const persistedRow = partsPage.rowByName(uniqueName);
      await expect(persistedRow).toBeVisible();
      await expect(persistedRow.getByTestId('cell-category')).toHaveText('Ignition');
      await expect(persistedRow.getByTestId('cell-status')).toHaveText('ACTIVE');
      await expect(persistedRow.getByTestId('cell-rule')).toHaveText('HIGH_TIER');
    } finally {
      await partsPage.cleanupPartIfExists(uniqueName);
    }
  });
});