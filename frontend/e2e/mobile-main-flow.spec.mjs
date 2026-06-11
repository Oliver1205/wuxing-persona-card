import { test, expect } from '@playwright/test';

const baseUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:5174';
const adminToken = process.env.E2E_ADMIN_TOKEN || 'dev-token';

test.use({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true,
});

test('mobile user flow creates result, shares short link, and admin sees metrics', async ({ page }) => {
  await page.goto(baseUrl);
  await expect(page.getByRole('heading', { name: '生成你的五行人格卡' })).toBeVisible();
  await page.getByRole('link', { name: '开始测试' }).click();

  await page.getByTestId('birth-year-quick-2002').click();
  await page.getByTestId('birth-year-plus').click();
  await page.getByTestId('birth-year-minus').click();
  await page.getByTestId('birth-month-8').click();
  await page.getByRole('button', { name: '进入第 1 题' }).click();

  await page.getByText('标准、边界和清晰判断').click();
  await page.getByText('提出计划和方向的人').click();
  await page.getByText('先感受和观察，再慢慢调整').click();
  await page.getByText('热情表达和感染他人').click();
  await page.getByText('资源统筹、稳定执行、兜底收尾').click();
  await page.getByRole('button', { name: '生成我的人格卡' }).click();

  await expect(page.getByText('你的五行人格身份')).toBeVisible();
  const resultUrl = page.url();
  expect(resultUrl).toContain('/result/');
  const shortUrlText = await page.locator('.share-box .url').innerText();
  expect(shortUrlText).toContain('/s/');

  await page.goto(shortUrlText);
  await page.waitForURL(/\/result\/.+sc=/);
  await page.getByRole('link', { name: '我也要测' }).click();
  await expect(page).toHaveURL(/\/test\?channel=shared-result&campaign=result-cta/);

  await page.goto(`${baseUrl}/admin`);
  await page.getByPlaceholder('输入管理 token').fill(adminToken);
  await page.getByRole('button', { name: '进入后台' }).click();
  await expect(page.getByText('增长漏斗')).toBeVisible();
  await expect(page.getByText('Top Channel')).toBeVisible();
});
