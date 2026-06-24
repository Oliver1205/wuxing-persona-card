import { mkdir } from 'node:fs/promises';
import { isIP } from 'node:net';
import path from 'node:path';
import { expect, test } from '@playwright/test';

const baseUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:5175';
const adminToken = process.env.E2E_ADMIN_TOKEN || 'dev-token';
const outputDir = process.env.SHOWCASE_SCREENSHOT_DIR || path.resolve('../docs/screenshots/showcase');
assertLocalE2EBaseUrl(baseUrl);

const mobileViewports = [
  {
    name: 'iphone-se',
    width: 375,
    height: 667,
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1',
  },
  {
    name: 'android-wide',
    width: 430,
    height: 932,
    userAgent: 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36',
  },
];

for (const viewport of mobileViewports) {
  test.describe(`${viewport.name} showcase screenshots`, () => {
    test.use({
      viewport: { width: viewport.width, height: viewport.height },
      isMobile: true,
      hasTouch: true,
      userAgent: viewport.userAgent,
    });

    test(`capture ${viewport.name} mobile product flow`, async ({ page }) => {
      await mkdir(outputDir, { recursive: true });
      await captureMobileFlow(page, viewport);
    });

    test(`capture ${viewport.name} mobile admin overview`, async ({ page }) => {
      await mkdir(outputDir, { recursive: true });
      await captureMobileAdminOverview(page, viewport);
    });

    test(`capture ${viewport.name} mobile short link detail`, async ({ page }) => {
      await mkdir(outputDir, { recursive: true });
      await captureMobileShortLinkDetail(page, viewport);
    });
  });
}

test.describe('desktop showcase screenshots', () => {
  test.use({
    viewport: { width: 1280, height: 900 },
    isMobile: false,
    hasTouch: false,
  });

  test('capture desktop public result', async ({ page }) => {
    await mkdir(outputDir, { recursive: true });
    const campaign = 'desktop-result-showcase-e2e';
    const result = await createShowcaseResult(page, campaign);
    await page.goto(attributedUrl(`/result/${result.resultId}`, campaign));
    await expect(page.getByText('你的五行人格身份')).toBeVisible();
    await expect(page.locator('.interpretation-panel')).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await expectNoControlTextOverflow(page, 'desktop result');
    await page.screenshot({ path: screenshotPath('desktop-07-result.png'), fullPage: true });
  });

  test('capture desktop admin overview', async ({ page }) => {
    await mkdir(outputDir, { recursive: true });
    await page.goto(`${baseUrl}/admin`);
    await page.getByPlaceholder('输入管理 token').fill(adminToken);
    await page.getByTestId('admin-login-button').click();
    await expect(page.getByText('增长漏斗')).toBeVisible();
    await expect(page.getByText('转化链路诊断')).toBeVisible();
    await expect(page.locator('[aria-label="关键指标口径"]')).toBeAttached();
    await expectNoControlTextOverflow(page, 'desktop admin overview');
    await page.screenshot({ path: screenshotPath('desktop-06-admin-overview.png'), fullPage: true });
  });

  test('capture desktop short link detail', async ({ page }) => {
    await mkdir(outputDir, { recursive: true });
    await page.addInitScript((token) => localStorage.setItem('wuxing_admin_token', token), adminToken);
    const campaign = 'desktop-shortlink-detail-showcase-e2e';
    const result = await createShowcaseResult(page, campaign);
    await page.goto(attributedUrl(`/s/${result.shortCode}`, campaign));
    await page.waitForURL(/\/result\/.+sc=/);

    const detailUrl = new URL(`/admin/short-links/${result.shortCode}`, baseUrl);
    detailUrl.searchParams.set('includeSynthetic', 'true');
    detailUrl.searchParams.set('keyword', result.shortCode);
    detailUrl.searchParams.set('statSource', 'local');
    await page.goto(detailUrl.toString());
    await expect(page.getByText('短链访问详情')).toBeVisible();
    await expect(page.getByRole('cell', { name: '访问短链' })).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await expectNoControlTextOverflow(page, 'desktop short link detail');
    await page.screenshot({ path: screenshotPath('desktop-08-shortlink-detail.png'), fullPage: true });
  });

  test('capture desktop match result', async ({ page }) => {
    await mkdir(outputDir, { recursive: true });
    const campaign = 'desktop-match-showcase-e2e';
    const partner = await createShowcaseResult(page, `${campaign}-partner`, {
      birthYear: 1999,
      birthMonth: 3,
      answers: partnerShowcaseAnswers,
    });
    const current = await createShowcaseResult(page, `${campaign}-current`);
    await page.goto(attributedUrl(`/match/${partner.shortCode}/${current.shortCode}`, campaign));
    await expect(page.getByText('双人五行匹配')).toBeVisible();
    await expect(page.getByText('你们容易形成的相处优势')).toBeVisible();
    await expect(page.locator('.error-state')).toHaveCount(0);
    await expectNoHorizontalOverflow(page);
    await expectNoElementMarkGraphics(page);
    await expectNoControlTextOverflow(page, 'desktop match');
    await page.screenshot({ path: screenshotPath('desktop-09-match.png'), fullPage: true });
  });

  test('capture desktop not found state', async ({ page }) => {
    await mkdir(outputDir, { recursive: true });
    await page.goto(attributedUrl('/no/such/page', 'desktop-not-found-showcase-e2e'));
    await expect(page.getByText('页面不存在')).toBeVisible();
    await expect(page.getByText('重新测一张')).toBeVisible();
    await expectNoHorizontalOverflow(page);
    await expectNoControlTextOverflow(page, 'desktop not found');
    await page.screenshot({ path: screenshotPath('desktop-10-not-found.png'), fullPage: true });
  });
});

const showcaseAnswers = [
  { questionCode: 'Q1', optionCode: 'METAL' },
  { questionCode: 'Q2', optionCode: 'WOOD' },
  { questionCode: 'Q3', optionCode: 'WATER' },
  { questionCode: 'Q4', optionCode: 'FIRE' },
  { questionCode: 'Q5', optionCode: 'EARTH' },
];

const partnerShowcaseAnswers = [
  { questionCode: 'Q1', optionCode: 'WOOD' },
  { questionCode: 'Q2', optionCode: 'FIRE' },
  { questionCode: 'Q3', optionCode: 'WOOD' },
  { questionCode: 'Q4', optionCode: 'EARTH' },
  { questionCode: 'Q5', optionCode: 'WATER' },
];

async function captureMobileFlow(page, viewport) {
  await page.goto(attributedUrl('/', `${viewport.name}-showcase-e2e`));
  await expect(page.getByRole('heading', { name: '五行人格卡' })).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await expectNoControlTextOverflow(page, `${viewport.name} home`);
  await expectMinimumTouchTargets(page, `${viewport.name} home`);
  await expectVerticalOrder(page, [
    '.hero-title-group',
    '.hero-metrics',
    '.actions',
    '.manual-match-entry',
    '.manual-match-tools',
    '.notice',
    '.hero-preview',
  ], `${viewport.name} home`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-01-home.png`), fullPage: true });

  await page.getByTestId('start-test-link').click();
  await expect(page.getByRole('heading', { name: '先选出生年月' })).toBeVisible();
  await expectNoControlTextOverflow(page, `${viewport.name} birth`);
  await expectMinimumTouchTargets(page, `${viewport.name} birth`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-02-test-birth-card.png`), fullPage: true });

  await page.getByTestId('birth-year-quick-2002').click();
  await page.getByTestId('birth-month-8').click();
  await expect(page.getByTestId('birth-inline-primary-action')).toBeVisible();
  await expect(page.getByTestId('birth-inline-primary-action')).toHaveText('进入第 1 题');
  await expectNoControlTextOverflow(page, `${viewport.name} birth ready`);
  await expectMinimumTouchTargets(page, `${viewport.name} birth ready`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-02b-test-birth-ready.png`), fullPage: true });
  await page.getByTestId('birth-inline-primary-action').click();
  await expect(page.getByTestId('question-Q1-option-METAL')).toBeVisible();
  await expectNoControlTextOverflow(page, `${viewport.name} question`);
  await expectMinimumTouchTargets(page, `${viewport.name} question`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-03-test-question-card.png`), fullPage: true });

  await page.getByTestId('question-Q1-option-METAL').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q2-option-WOOD').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q3-option-WATER').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q4-option-FIRE').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q5-option-EARTH').click();
  await page.getByTestId('test-primary-action').click();
  await expect(page.getByText('你的五行人格身份')).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await expectNoControlTextOverflow(page, `${viewport.name} result`);
  await expectMinimumTouchTargets(page, `${viewport.name} result`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-04-result.png`), fullPage: true });

  const shortUrlText = await page.locator('.share-box .url').innerText();
  expect(shortUrlText).not.toContain('channel=');
  expect(shortUrlText).not.toContain('campaign=');
  const shortUrl = new URL(shortUrlText.replace(/\s+/g, ''), baseUrl);
  await page.goto(shortUrl.toString());
  await page.waitForURL(/\/result\/.+sc=/);
  await expect(page.getByText('朋友分享给你的五行人格卡')).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await expectNoControlTextOverflow(page, `${viewport.name} shared result`);
  await expectMinimumTouchTargets(page, `${viewport.name} shared result`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-05-shared-result.png`), fullPage: true });

  const currentShortCode = shortUrl.pathname.split('/').pop();
  const partner = await createShowcaseResult(page, `${viewport.name}-match-partner-showcase-e2e`, {
    birthYear: 1999,
    birthMonth: 3,
    answers: partnerShowcaseAnswers,
  });
  await page.goto(attributedUrl(`/match/${partner.shortCode}/${currentShortCode}`, `${viewport.name}-match-showcase-e2e`));
  await expect(page.getByText('双人五行匹配')).toBeVisible();
  await expect(page.getByText('你们容易形成的相处优势')).toBeVisible();
  await expect(page.locator('.error-state')).toHaveCount(0);
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await expectNoControlTextOverflow(page, `${viewport.name} match`);
  await expectMinimumTouchTargets(page, `${viewport.name} match`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-06-match.png`), fullPage: true });

  await page.goto(attributedUrl('/no/such/page', `${viewport.name}-not-found-showcase-e2e`));
  await expect(page.getByText('页面不存在')).toBeVisible();
  await expect(page.getByText('重新测一张')).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectNoControlTextOverflow(page, `${viewport.name} not found`);
  await expectMinimumTouchTargets(page, `${viewport.name} not found`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-07-not-found.png`), fullPage: true });
}

async function captureMobileAdminOverview(page, viewport) {
  await page.addInitScript((token) => localStorage.setItem('wuxing_admin_token', token), adminToken);
  await page.goto(`${baseUrl}/admin`);
  if (await page.getByPlaceholder('输入管理 token').isVisible().catch(() => false)) {
    await page.getByPlaceholder('输入管理 token').fill(adminToken);
    await page.getByTestId('admin-login-button').click();
  }
  await expect(page.locator('.focus-grid')).toBeVisible();
  await expect(page.getByTestId('admin-mobile-report-toggle')).toBeVisible();
  await expectLocalizedDatePlaceholders(page, `${viewport.name} mobile admin empty dates`);
  await expectMinimumTouchTargets(page, `${viewport.name} mobile admin initial`);
  const includeSyntheticToggle = page.getByLabel(/包含测试流量/);
  if (!(await includeSyntheticToggle.isChecked())) {
    await includeSyntheticToggle.check();
    await page.getByRole('button', { name: '应用筛选' }).click();
    await expect(page.locator('.scope-status').getByText('包含 perf-test 测试流量')).toBeVisible();
  }
  await expect(page.locator('#shortlink-section')).toBeHidden();
  await page.getByTestId('admin-mobile-report-toggle').click();
  await expect(page.getByTestId('admin-mobile-report-group-core')).toBeVisible();
  await expect(page.getByTestId('admin-mobile-report-group-trend')).toBeVisible();
  await expect(page.getByTestId('admin-mobile-report-group-attribution')).toBeVisible();
  await expect(page.locator('#journey-section')).toBeVisible();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toBeHidden();
  await expect(page.getByRole('heading', { name: 'Top Channel' })).toBeHidden();
  await expectNoHorizontalOverflow(page);
  await expectNoControlTextOverflow(page, `${viewport.name} mobile admin core report`);
  await expectMinimumTouchTargets(page, `${viewport.name} mobile admin core report`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-11-admin-report-core.png`), fullPage: true });

  await page.getByTestId('admin-mobile-report-group-trend').click();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Top Channel' })).toBeHidden();
  await expectNoHorizontalOverflow(page);
  await expectNoControlTextOverflow(page, `${viewport.name} mobile admin trend report`);
  await expectMinimumTouchTargets(page, `${viewport.name} mobile admin trend report`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-12-admin-report-trend.png`), fullPage: true });

  const shortLinkEvidenceAction = page.locator('.action-panel .text-link[href="#shortlink-section"]');
  expect(await shortLinkEvidenceAction.count()).toBeGreaterThan(0);
  await shortLinkEvidenceAction.first().click();
  await expect(page.locator('#shortlink-section')).toBeVisible();
  await expect.poll(async () => page.locator('#shortlink-section').evaluate((element) => {
    const rect = element.getBoundingClientRect();
    return rect.top < window.innerHeight && rect.bottom > 0;
  })).toBe(true);
  await expect(page.getByTestId('admin-shortlink-mobile-list')).toBeVisible();
  await expect(page.locator('#shortlink-section .shortlink-table-wrap')).toBeHidden();
  await expectNoHorizontalOverflow(page);
  await expectNoControlTextOverflow(page, `${viewport.name} mobile admin expanded report`);
  await expectMinimumTouchTargets(page, `${viewport.name} mobile admin expanded report`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-10-admin-report-expanded.png`), fullPage: true });
  await page.getByTestId('admin-mobile-report-toggle').click();
  await expect(page.locator('#shortlink-section')).toBeHidden();
  const adminCanvas = await page.locator('.admin-desktop-page').evaluate((element) => ({
    scrollLeft: Math.round(element.scrollLeft),
    scrollWidth: Math.round(element.scrollWidth),
    clientWidth: Math.round(element.clientWidth),
  }));
  expect(adminCanvas.scrollLeft).toBe(0);
  expect(adminCanvas.scrollWidth).toBeLessThanOrEqual(adminCanvas.clientWidth + 1);
  const adminPageLeft = await page.locator('.admin-page').evaluate((element) => (
    Math.round(element.getBoundingClientRect().left)
  ));
  expect(adminPageLeft).toBeGreaterThanOrEqual(0);
  expect(adminPageLeft).toBeLessThanOrEqual(20);
  await expectNoHorizontalOverflow(page);
  await expectNoControlTextOverflow(page, `${viewport.name} mobile admin`);
  await expectMinimumTouchTargets(page, `${viewport.name} mobile admin collapsed`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-08-admin-overview.png`), fullPage: true });
}

async function captureMobileShortLinkDetail(page, viewport) {
  await page.addInitScript((token) => localStorage.setItem('wuxing_admin_token', token), adminToken);
  const campaign = `${viewport.name}-shortlink-detail-showcase-e2e`;
  const result = await createShowcaseResult(page, campaign);
  await page.goto(attributedUrl(`/s/${result.shortCode}`, campaign));
  await page.waitForURL(/\/result\/.+sc=/);

  const detailUrl = new URL(`/admin/short-links/${result.shortCode}`, baseUrl);
  detailUrl.searchParams.set('includeSynthetic', 'true');
  detailUrl.searchParams.set('keyword', result.shortCode);
  detailUrl.searchParams.set('statSource', 'local');
  await page.goto(detailUrl.toString());
  await expect(page.getByText('短链访问详情')).toBeVisible();
  await expect(page.getByTestId('shortlink-visit-card')).toHaveCount(1);
  await expect(page.getByTestId('shortlink-visit-card')).toBeVisible();
  await expect(page.locator('.table-wrap')).toBeHidden();
  await expectLocalizedDatePlaceholders(page, `${viewport.name} short link detail empty dates`);
  await expectMinimumTouchTargets(page, `${viewport.name} short link detail`);
  await expectNoHorizontalOverflow(page);
  await expectNoControlTextOverflow(page, `${viewport.name} short link detail`);
  await page.screenshot({ path: screenshotPath(`${viewport.name}-09-shortlink-detail.png`), fullPage: true });

  detailUrl.searchParams.set('startDate', '2026-06-01');
  detailUrl.searchParams.set('endDate', '2026-06-19');
  await page.goto(detailUrl.toString());
  await expect(page.getByText('短链访问详情')).toBeVisible();
  await expectFilledDateValues(page, `${viewport.name} short link detail filled dates`);

  detailUrl.searchParams.set('keyword', 'very-long-keyword-这是一段非常长的排查关键词-ABCDEFGHIJKLMNOPQRSTUVWXYZ');
  await page.goto(detailUrl.toString());
  await expect(page.getByText('短链访问详情')).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectScopeChipsWithinViewport(page, `${viewport.name} short link detail long keyword`);
}

async function createShowcaseResult(page, campaign, overrides = {}) {
  const response = await page.request.post(`${baseUrl}/api/results`, {
    headers: {
      'X-Channel': 'perf-test',
      'X-Campaign': campaign,
    },
    data: {
      birthYear: 2002,
      birthMonth: 8,
      birthDay: null,
      birthTimeRange: null,
      answers: showcaseAnswers,
      ...overrides,
    },
  });
  expect(response.ok()).toBeTruthy();
  const payload = await response.json();
  expect(payload.code).toBe(0);
  expect(payload.data.resultId).toBeTruthy();
  return payload.data;
}

async function expectNoHorizontalOverflow(page) {
  const hasHorizontalOverflow = await page.evaluate(() => (
    document.documentElement.scrollWidth > document.documentElement.clientWidth
  ));
  expect(hasHorizontalOverflow).toBe(false);
}

async function expectNoElementMarkGraphics(page) {
  const graphicCount = await page.locator('.element-mark svg, .element-mark path, .element-mark img, .element-mark canvas').count();
  expect(graphicCount).toBe(0);
  const backgroundImages = await page.locator('.element-mark, .element-mark *').evaluateAll((marks) => (
    marks
      .map((mark) => getComputedStyle(mark).backgroundImage)
      .filter((backgroundImage) => backgroundImage && backgroundImage !== 'none')
  ));
  expect(backgroundImages).toEqual([]);
}

async function expectNoControlTextOverflow(page, label) {
  const overflowingControls = await page.evaluate(() => {
    const visible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = getComputedStyle(element);
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
    };
    return [...document.querySelectorAll('button, a, [role="button"], input, select, textarea, .short-code, .url')]
      .filter(visible)
      .map((element) => ({
        tag: element.tagName.toLowerCase(),
        testId: element.getAttribute('data-testid') || '',
        text: (element.innerText || element.value || element.getAttribute('placeholder') || '')
          .replace(/\s+/g, ' ')
          .trim()
          .slice(0, 80),
        scrollWidth: element.scrollWidth,
        clientWidth: element.clientWidth,
        scrollHeight: element.scrollHeight,
        clientHeight: element.clientHeight,
      }))
      .filter((item) => (
        item.scrollWidth > item.clientWidth + 1 || item.scrollHeight > item.clientHeight + 1
      ));
  });
  expect(overflowingControls, `${label} controls fit their text`).toEqual([]);
}

async function expectLocalizedDatePlaceholders(page, label) {
  const dateStates = await page.evaluate(() => [...document.querySelectorAll('.date-input-shell')].map((shell) => {
    const input = shell.querySelector('input');
    const rect = shell.getBoundingClientRect();
    return {
      className: shell.className,
      afterContent: getComputedStyle(shell, '::after').content,
      value: input?.value || '',
      type: input?.getAttribute('type') || '',
      ariaLabel: input?.getAttribute('aria-label') || '',
      width: Math.round(rect.width),
      height: Math.round(rect.height),
    };
  }));
  expect(dateStates.length, `${label} has date controls`).toBeGreaterThanOrEqual(2);
  expect(dateStates.slice(0, 2), `${label} empty date controls are localized`).toEqual([
    expect.objectContaining({
      className: expect.stringContaining('empty'),
      afterContent: '"选择开始日期"',
      value: '',
      type: 'date',
      ariaLabel: '开始日期，格式 YYYY-MM-DD',
    }),
    expect.objectContaining({
      className: expect.stringContaining('empty'),
      afterContent: '"选择结束日期"',
      value: '',
      type: 'date',
      ariaLabel: '结束日期，格式 YYYY-MM-DD',
    }),
  ]);
}

async function expectFilledDateValues(page, label) {
  const dateStates = await page.evaluate(() => [...document.querySelectorAll('.date-input-shell')].map((shell) => {
    const input = shell.querySelector('input');
    return {
      className: shell.className,
      afterContent: getComputedStyle(shell, '::after').content,
      value: input?.value || '',
    };
  }));
  expect(dateStates.slice(0, 2), `${label} filled date controls show values`).toEqual([
    expect.objectContaining({
      className: 'date-input-shell',
      afterContent: 'none',
      value: '2026-06-01',
    }),
    expect.objectContaining({
      className: 'date-input-shell',
      afterContent: 'none',
      value: '2026-06-19',
    }),
  ]);
}

async function expectMinimumTouchTargets(page, label) {
  const tinyTargets = await page.evaluate(() => {
    const visible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = getComputedStyle(element);
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
    };
    return [...document.querySelectorAll('button, a, [role="button"], summary, input:not([type="checkbox"]), select')]
      .filter(visible)
      .map((element) => {
        const rect = element.getBoundingClientRect();
        return {
          tag: element.tagName.toLowerCase(),
          testId: element.getAttribute('data-testid') || '',
          className: String(element.className || '').slice(0, 80),
          text: (element.innerText || element.value || element.getAttribute('placeholder') || element.getAttribute('aria-label') || '')
            .replace(/\s+/g, ' ')
            .trim()
            .slice(0, 80),
          width: Math.round(rect.width),
          height: Math.round(rect.height),
        };
      })
      .filter((item) => item.width < 44 || item.height < 44);
  });
  expect(tinyTargets, `${label} keeps touch targets at least 44px`).toEqual([]);
}

async function expectScopeChipsWithinViewport(page, label) {
  const overflowingChips = await page.evaluate(() => [...document.querySelectorAll('.scope-line span')]
    .map((element) => {
      const rect = element.getBoundingClientRect();
      const style = getComputedStyle(element);
      return {
        text: (element.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 100),
        width: Math.round(rect.width),
        left: Math.round(rect.left),
        right: Math.round(rect.right),
        whiteSpace: style.whiteSpace,
        overflowWrap: style.overflowWrap,
        maxWidth: style.maxWidth,
      };
    })
    .filter((item) => item.left < -1 || item.right > window.innerWidth + 1 || item.whiteSpace !== 'normal' || item.overflowWrap !== 'anywhere'));
  expect(overflowingChips, `${label} keeps scope chips wrapping inside viewport`).toEqual([]);
}

async function expectVerticalOrder(page, selectors, label) {
  const boxes = await page.evaluate((targetSelectors) => targetSelectors.map((selector) => {
    const element = document.querySelector(selector);
    if (!element) {
      return { selector, missing: true };
    }
    const rect = element.getBoundingClientRect();
    return {
      selector,
      top: Math.round(rect.top),
      bottom: Math.round(rect.bottom),
      height: Math.round(rect.height),
    };
  }), selectors);

  for (const box of boxes) {
    expect(box.missing, `${label} missing ${box.selector}`).toBeFalsy();
    expect(box.height, `${label} ${box.selector} has height`).toBeGreaterThan(0);
  }

  for (let index = 1; index < boxes.length; index += 1) {
    const previous = boxes[index - 1];
    const current = boxes[index];
    expect(current.top, `${label} ${previous.selector} before ${current.selector}`).toBeGreaterThanOrEqual(previous.bottom - 2);
  }
}

function screenshotPath(fileName) {
  return path.join(outputDir, fileName);
}

function attributedUrl(pathname, campaign) {
  const target = new URL(pathname, baseUrl);
  target.searchParams.set('channel', 'perf-test');
  target.searchParams.set('campaign', campaign);
  return target.toString();
}

function assertLocalE2EBaseUrl(rawUrl) {
  if (process.env.ALLOW_PUBLIC_E2E === '1') {
    return;
  }
  const host = new URL(rawUrl).hostname;
  if (host === 'localhost' || host === '0.0.0.0' || host === '::1' || host.startsWith('127.')) {
    return;
  }
  if (isPrivateIpv4(host)) {
    return;
  }
  throw new Error('Refusing to capture showcase screenshots against a public URL. Set ALLOW_PUBLIC_E2E=1 only during an authorized test window.');
}

function isPrivateIpv4(host) {
  if (isIP(host) !== 4) {
    return false;
  }
  const [first, second] = host.split('.').map(Number);
  return first === 10 || (first === 172 && second >= 16 && second <= 31) || (first === 192 && second === 168);
}
