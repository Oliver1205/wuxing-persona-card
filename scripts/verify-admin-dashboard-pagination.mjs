import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const { chromium } = require('../frontend/node_modules/playwright');

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const baseUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:5176';
const adminToken = process.env.E2E_ADMIN_TOKEN || 'dev-token';
const screenshotDir = process.env.SHOWCASE_SCREENSHOT_DIR || path.join(root, 'docs/screenshots/showcase');
const screenshotPath = path.join(screenshotDir, 'admin-dashboard-pagination-20260704.png');

const expectedPages = ['实时监控', '核心概览', '流量趋势', '转化漏斗', '结果排行', '事件明细', '性能压测'];

function assert(name, condition, details = {}) {
  if (!condition) {
    const suffix = Object.keys(details).length ? ` ${JSON.stringify(details)}` : '';
    throw new Error(`${name}${suffix}`);
  }
}

async function launchBrowser() {
  const attempts = [
    {
      name: 'bundled-chromium-default',
      options: { headless: true },
    },
    {
      name: 'bundled-chromium-machport-off',
      options: {
        headless: true,
        args: ['--disable-features=MachPortRendezvous'],
      },
    },
    {
      name: 'bundled-chromium-single-process',
      options: {
        headless: true,
        args: ['--single-process', '--disable-features=MachPortRendezvous'],
      },
    },
    {
      name: 'system-google-chrome',
      options: {
        headless: true,
        executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
        args: ['--disable-features=MachPortRendezvous'],
      },
    },
  ];

  const errors = [];
  for (const attempt of attempts) {
    try {
      return {
        browser: await chromium.launch(attempt.options),
        strategy: attempt.name,
      };
    } catch (error) {
      errors.push(`${attempt.name}: ${String(error.message || error).split('\n')[0]}`);
    }
  }

  throw new Error(`Unable to launch a browser:\n${errors.join('\n')}`);
}

const launched = await launchBrowser();
const browser = launched.browser;

try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1100 },
    deviceScaleFactor: 1,
  });

  await page.addInitScript((token) => {
    window.localStorage.setItem('wuxing_admin_token', token);
  }, adminToken);
  await page.goto(`${baseUrl}/admin`, { waitUntil: 'networkidle' });
  await page.waitForSelector('[data-testid="admin-monitor-console"]', { timeout: 15000 });

  const navLabels = await page.locator('.dashboard-page-nav button strong').allTextContents();
  assert('admin dashboard has seven pages', navLabels.length === expectedPages.length, { navLabels });
  assert('admin dashboard page labels', expectedPages.every((label) => navLabels.includes(label)), { navLabels });

  await page.getByRole('button', { name: /事件明细/ }).click();
  await page.waitForSelector('[data-testid="admin-events-console"]', { timeout: 5000 });
  assert('events page visible', await page.locator('[data-testid="admin-events-console"]').isVisible());

  await page.getByRole('button', { name: /性能压测/ }).click();
  await page.waitForSelector('[data-testid="admin-performance-console"]', { timeout: 5000 });
  assert('performance page visible', await page.locator('[data-testid="admin-performance-console"]').isVisible());

  const metrics = await page.evaluate(() => ({
    navCount: document.querySelectorAll('.dashboard-page-nav button').length,
    activeText: document.querySelector('.dashboard-page-nav button.active strong')?.textContent || '',
    bodyScrollWidth: document.documentElement.scrollWidth,
    bodyClientWidth: document.documentElement.clientWidth,
  }));
  assert('desktop has no body overflow', metrics.bodyScrollWidth <= metrics.bodyClientWidth + 1, metrics);

  fs.mkdirSync(screenshotDir, { recursive: true });
  await page.screenshot({ path: screenshotPath, fullPage: true, animations: 'disabled' });

  console.log(JSON.stringify({
    ok: true,
    strategy: launched.strategy,
    baseUrl,
    navLabels,
    screenshotPath: path.relative(root, screenshotPath),
    metrics,
  }, null, 2));
} finally {
  await browser.close();
}
