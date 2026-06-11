import { mkdir } from 'node:fs/promises';
import path from 'node:path';
import { expect, test } from '@playwright/test';

const baseUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:5174';
const adminToken = process.env.E2E_ADMIN_TOKEN || 'dev-token';
const outputDir = process.env.SHOWCASE_SCREENSHOT_DIR || path.resolve('../docs/screenshots/showcase');

const mobileViewports = [
  {
    name: 'iphone-se',
    width: 375,
    height: 667,
  },
  {
    name: 'android-wide',
    width: 430,
    height: 932,
  },
];

for (const viewport of mobileViewports) {
  test.describe(`${viewport.name} showcase screenshots`, () => {
    test.use({
      viewport: { width: viewport.width, height: viewport.height },
      isMobile: true,
      hasTouch: true,
    });

    test(`capture ${viewport.name} mobile product flow`, async ({ page }) => {
      await mkdir(outputDir, { recursive: true });
      await captureMobileFlow(page, viewport);
    });
  });
}

test.describe('desktop showcase screenshots', () => {
  test.use({
    viewport: { width: 1280, height: 900 },
    isMobile: false,
    hasTouch: false,
  });

  test('capture desktop admin overview', async ({ page }) => {
    await mkdir(outputDir, { recursive: true });
    await page.goto(`${baseUrl}/admin`);
    await page.getByPlaceholder('输入管理 token').fill(adminToken);
    await page.getByRole('button', { name: '进入后台' }).click();
    await expect(page.getByText('增长漏斗')).toBeVisible();
    await expect(page.locator('[aria-label="关键指标口径"]')).toBeAttached();
    await page.screenshot({ path: screenshotPath('desktop-06-admin-overview.png'), fullPage: true });
  });
});

async function captureMobileFlow(page, viewport) {
  await page.goto(baseUrl);
  await expect(page.getByRole('heading', { name: '生成你的五行人格卡' })).toBeVisible();
  await page.screenshot({ path: screenshotPath(`${viewport.name}-01-home.png`), fullPage: true });

  await page.getByRole('link', { name: '开始测试' }).click();
  await expect(page.getByRole('button', { name: '进入第 1 题' })).toBeVisible();
  await page.screenshot({ path: screenshotPath(`${viewport.name}-02-test-birth-card.png`), fullPage: true });

  await page.getByTestId('birth-year-quick-2002').click();
  await page.getByTestId('birth-month-8').click();
  await page.getByRole('button', { name: '进入第 1 题' }).click();
  await expect(page.getByText('标准、边界和清晰判断')).toBeVisible();
  await page.screenshot({ path: screenshotPath(`${viewport.name}-03-test-question-card.png`), fullPage: true });

  await page.getByText('标准、边界和清晰判断').click();
  await page.getByText('提出计划和方向的人').click();
  await page.getByText('先感受和观察，再慢慢调整').click();
  await page.getByText('热情表达和感染他人').click();
  await page.getByText('资源统筹、稳定执行、兜底收尾').click();
  await page.getByRole('button', { name: '生成我的人格卡' }).click();
  await expect(page.getByText('你的五行人格身份')).toBeVisible();
  await page.screenshot({ path: screenshotPath(`${viewport.name}-04-result.png`), fullPage: true });

  const shortUrlText = await page.locator('.share-box .url').innerText();
  await page.goto(shortUrlText);
  await page.waitForURL(/\/result\/.+sc=/);
  await expect(page.getByText('朋友分享给你的五行人格卡')).toBeVisible();
  await page.screenshot({ path: screenshotPath(`${viewport.name}-05-shared-result.png`), fullPage: true });
}

function screenshotPath(fileName) {
  return path.join(outputDir, fileName);
}
