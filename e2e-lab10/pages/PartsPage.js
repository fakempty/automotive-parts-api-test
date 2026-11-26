const { expect } = require('@playwright/test');

class PartsPage {
  constructor(page) {
    this.page = page;
    this.nameInput = page.getByTestId('input-name');
    this.categoryInput = page.getByTestId('input-category');
    this.priceInput = page.getByTestId('input-price');
    this.stockInput = page.getByTestId('input-stock');
    this.submitBtn = page.getByTestId('submit-part-btn');
    this.refreshBtn = page.getByTestId('refresh-btn');
    this.statusText = page.getByTestId('status-text');
    this.errorBanner = page.getByTestId('error-banner');
    this.table = page.getByTestId('parts-table');
    this.tableBody = page.getByTestId('parts-tbody');
    this.rows = page.getByTestId('part-row');
  }

  async open() {
    await this.page.goto('/', { waitUntil: 'domcontentloaded', timeout: 15000 });
    await expect(this.nameInput).toBeVisible({ timeout: 15000 });
    await expect(this.stockInput).toBeVisible({ timeout: 15000 });
    await expect(this.table).toBeVisible({ timeout: 15000 });
    await expect(this.tableBody).toBeAttached({ timeout: 15000 });
  }

  async reloadAndWait() {
    await this.page.reload({ waitUntil: 'domcontentloaded', timeout: 15000 });
    await expect(this.table).toBeVisible({ timeout: 15000 });
    await expect(this.tableBody).toBeAttached({ timeout: 15000 });
  }

  async createPart({ name, category, price, stock }) {
    await this.nameInput.fill(name, { timeout: 15000 });
    await this.categoryInput.fill(category, { timeout: 15000 });
    await this.priceInput.fill(String(price), { timeout: 15000 });
    await this.stockInput.fill(String(stock), { timeout: 15000 });

    const createPromise = this.page.waitForResponse(
      (res) => res.url().includes('/api/parts') && res.request().method() === 'POST',
      { timeout: 15000 }
    );
    const reloadPromise = this.page.waitForResponse(
      (res) => res.url().includes('/api/parts') && res.request().method() === 'GET' && res.ok(),
      { timeout: 15000 }
    ).catch(() => null);

    await this.page.evaluate(() => {
      if (typeof createPart === 'function') {
        createPart();
      } else {
        document.querySelector('[data-testid="submit-part-btn"]')?.click();
      }
    });

    const createRes = await createPromise;
    if (createRes.ok()) {
      await reloadPromise;
      await expect(this.statusText).toContainText('Запис успішно створено', { timeout: 15000 });
    }
    return createRes.status();
  }

  rowByName(name) {
    return this.rows.filter({ has: this.page.getByTestId('cell-name').filter({ hasText: name }) }).first();
  }

  async getIdByName(name) {
    const row = this.rowByName(name);
    await expect(row).toBeVisible({ timeout: 15000 });
    const rawId = await row.getByTestId('cell-id').innerText();
    return Number(rawId.trim());
  }

  async cancelPartByName(name) {
    const row = this.rowByName(name);
    await expect(row).toBeVisible({ timeout: 15000 });
    const deletePromise = this.page.waitForResponse(
      (res) => res.url().includes('/api/parts/') && res.request().method() === 'DELETE',
      { timeout: 15000 }
    );
    await row.getByTestId('delete-part-btn').scrollIntoViewIfNeeded();
    await row.getByTestId('delete-part-btn').click({ force: true, timeout: 15000 });
    const delRes = await deletePromise;
    return delRes.status();
  }

  async cleanupPartIfExists(name) {
    if (this.page.isClosed()) return;
    try {
      const row = this.rows.filter({ has: this.page.getByTestId('cell-name').filter({ hasText: name }) });
      const cnt = await row.count({ timeout: 2000 }).catch(() => 0);
      if (cnt > 0) {
        const rawId = await row.first().getByTestId('cell-id').innerText({ timeout: 2000 });
        const id = Number(rawId.trim());
        if (id) {
          await this.page.request.delete('/api/parts/' + id, { timeout: 3000 }).catch(() => {});
        }
      }
    } catch {}
  }
}

module.exports = { PartsPage };