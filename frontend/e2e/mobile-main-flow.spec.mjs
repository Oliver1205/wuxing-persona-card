import { readFile } from 'node:fs/promises';
import { isIP } from 'node:net';
import { test, expect } from '@playwright/test';

const baseUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:5175';
const adminToken = process.env.E2E_ADMIN_TOKEN || 'dev-token';
assertLocalE2EBaseUrl(baseUrl);

test.use({
  viewport: { width: 390, height: 844 },
  isMobile: true,
  hasTouch: true,
  userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1',
});

test('mobile user flow creates result, shares link, and admin sees metrics', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: {
        writeText: async () => {
          throw new Error('clipboard disabled in fallback e2e');
        },
      },
    });
    Object.defineProperty(navigator, 'share', {
      configurable: true,
      value: undefined,
    });
  });

  await page.goto(attributedUrl('/', 'mobile-main-e2e'));
  await expect(page.getByRole('heading', { name: '五行人格卡' })).toBeVisible();
  await page.getByTestId('start-test-link').click();

  await page.getByTestId('birth-year-quick-2002').click();
  await page.getByTestId('birth-year-plus').click();
  await page.getByTestId('birth-year-minus').click();
  await page.getByTestId('birth-month-8').click();
  await page.getByTestId('birth-inline-primary-action').click();

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
  const resultUrl = page.url();
  expect(resultUrl).toContain('/result/');
  const resultId = new URL(resultUrl).pathname.split('/').pop();
  expect(resultId).toBeTruthy();
  const shortCodeText = await page.locator('.share-box .short-code').innerText();
  const shortUrlText = await page.locator('.share-box .url').innerText();
  expect(shortUrlText).toContain('/s/');
  expect(shortUrlText).not.toContain('channel=');
  expect(shortUrlText).not.toContain('campaign=');
  await expect(page.getByTestId('result-primary-save-image')).toHaveCount(0);
  await expect(page.getByTestId('save-share-image')).toBeVisible();
  await expect(page.getByTestId('native-share')).toBeVisible();
  await expect(page.getByTestId('copy-tools-toggle')).toBeVisible();
  await expect(page.getByTestId('copy-match-code')).toBeHidden();
  await expect(page.getByTestId('copy-share-link')).toBeHidden();
  const shareDownloadPromise = page.waitForEvent('download');
  await page.getByTestId('save-share-image').click();
  const shareDownload = await shareDownloadPromise;
  expect(shareDownload.suggestedFilename()).toBe(`wuxing-${resultId}.png`);
  const shareDownloadPath = await shareDownload.path();
  expect(shareDownloadPath).toBeTruthy();
  const shareImageBody = await readFile(shareDownloadPath);
  expect([...shareImageBody.subarray(0, 8)]).toEqual([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  expect(shareImageBody.length).toBeGreaterThan(32_000);
  expect(shareImageBody.readUInt32BE(16)).toBe(900);
  expect(shareImageBody.readUInt32BE(20)).toBe(1200);
  await expect(page.locator('.share-box')).toBeVisible();
  await expect(page.getByText('分享图已生成')).toBeVisible();
  await expect(page.getByText('分享图生成失败')).toHaveCount(0);
  await page.getByTestId('copy-tools-toggle').click();
  await expect(page.getByTestId('copy-match-code')).toBeVisible();
  await expect(page.getByTestId('copy-share-link')).toBeVisible();
  await page.getByTestId('copy-match-code').click();
  await expect(page.locator('.share-box .tip')).toContainText('长按短码手动复制');
  expect(await page.evaluate(() => String(window.getSelection()))).toBe(shortCodeText);
  await page.getByTestId('copy-share-link').click();
  await expect(page.locator('.share-box .tip')).toContainText('长按链接手动复制');
  expect(await page.evaluate(() => String(window.getSelection()))).toBe(shortUrlText);
  await page.getByTestId('native-share').click();
  await expect(page.locator('.share-box .tip')).toContainText('当前浏览器不支持系统分享');

  await page.goto(new URL(shortUrlText, baseUrl).toString());
  await page.waitForURL(/\/result\/.+sc=/);
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await expect(page.locator('.share-box')).toHaveCount(0);
  await page.locator('a[href*="campaign=result-banner"]').click();
  await expect(page).toHaveURL(/\/test\?channel=shared-result&campaign=result-banner&matchCode=/);

  await page.goto(`${baseUrl}/admin`);
  await page.getByPlaceholder('输入管理 token').fill(adminToken);
  await page.getByTestId('admin-login-button').click();
  await expect(page.locator('.focus-grid')).toBeVisible();
  await expect(page.getByTestId('admin-mobile-report-toggle')).toBeVisible();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toBeHidden();
  await page.getByTestId('admin-mobile-report-toggle').click();
  await expect(page.getByTestId('admin-mobile-report-group-core')).toBeVisible();
  await expect(page.getByTestId('admin-mobile-report-group-trend')).toBeVisible();
  await expect(page.getByTestId('admin-mobile-report-group-attribution')).toBeVisible();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toBeHidden();
  await page.getByTestId('admin-mobile-report-group-trend').click();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Top Channel' })).toBeHidden();
  await page.getByTestId('admin-mobile-report-group-attribution').click();
  await expect(page.getByRole('heading', { name: 'Top Channel' })).toBeVisible();
  await expect(page.getByTestId('admin-export-csv')).toBeEnabled();
  const exportResponsePromise = page.waitForResponse((response) => (
    response.url().includes('/api/admin/short-links/export') && response.status() === 200
  ));
  const exportDownloadPromise = page.waitForEvent('download');
  await page.getByTestId('admin-export-csv').click();
  const [exportResponse, exportDownload] = await Promise.all([exportResponsePromise, exportDownloadPromise]);
  expect(exportResponse.headers()['content-type']).toContain('text/csv');
  expect(exportResponse.headers()['content-disposition']).toContain('wuxing-short-links');
  const exportDownloadPath = await exportDownload.path();
  expect(exportDownloadPath).toBeTruthy();
  const exportBody = await readFile(exportDownloadPath);
  expect([...exportBody.subarray(0, 3)]).toEqual([0xef, 0xbb, 0xbf]);
  expect(exportBody.toString('utf8')).toContain('shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,metricSource,createdAt,lastVisitAt');

  await page.goto(`${baseUrl}/admin/short-links/${shortCodeText}?includeSynthetic=true`);
  await expect(page.getByText('短链访问详情')).toBeVisible();
  await expect(page.getByTestId('shortlink-visit-card').first()).toBeVisible();
  await expect(page.locator('.table-wrap')).toBeHidden();
  await expectNoHorizontalOverflow(page);
});

test('mobile match flow accepts a short code and opens pair result', async ({ page }) => {
  const partner = await createResultByApi(page, {
    birthYear: 1999,
    birthMonth: 3,
    answers: [
      { questionCode: 'Q1', optionCode: 'WOOD' },
      { questionCode: 'Q2', optionCode: 'FIRE' },
      { questionCode: 'Q3', optionCode: 'WOOD' },
      { questionCode: 'Q4', optionCode: 'EARTH' },
      { questionCode: 'Q5', optionCode: 'WATER' },
    ],
  }, 'mobile-match-e2e');

  await page.goto(attributedUrl('/', 'mobile-match-e2e', { skipClipboardAuto: '1' }));
  await page.getByTestId('manual-match-code').fill(partner.shortCode);
  await page.getByTestId('manual-match-submit').click();
  await expect(page.getByText('要和这张人格卡做双人匹配吗？')).toBeVisible();
  await page.getByTestId('match-accept-button').click();
  await expect(page).toHaveURL(/\/test\?.*matchCode=/);

  await page.getByTestId('birth-year-quick-2002').click();
  await page.getByTestId('birth-month-8').click();
  await page.getByTestId('birth-inline-primary-action').click();
  await page.getByTestId('question-Q1-option-METAL').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q2-option-WOOD').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q3-option-WATER').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q4-option-FIRE').click();
  await page.getByTestId('test-primary-action').click();
  await page.getByTestId('question-Q5-option-EARTH').click();
  const matchRequestPromise = page.waitForRequest((request) => (
    new URL(request.url()).pathname === '/api/matches' && request.method() === 'POST'
  ));
  await page.getByTestId('test-primary-action').click();
  const matchRequest = await matchRequestPromise;
  expect(matchRequest.headers()['x-channel']).toBe('perf-test');
  expect(matchRequest.headers()['x-campaign']).toBe('mobile-match-e2e');
  const matchPayload = JSON.parse(matchRequest.postData() || '{}');
  expect(matchPayload).toMatchObject({
    partnerShortCode: partner.shortCode,
    birthYear: 2002,
    birthMonth: 8,
    birthDay: null,
    birthTimeRange: null,
  });
  expect(matchPayload.answers).toEqual([
    { questionCode: 'Q1', optionCode: 'METAL' },
    { questionCode: 'Q2', optionCode: 'WOOD' },
    { questionCode: 'Q3', optionCode: 'WATER' },
    { questionCode: 'Q4', optionCode: 'FIRE' },
    { questionCode: 'Q5', optionCode: 'EARTH' },
  ]);

  await page.waitForURL(/\/match\/.+\/.+/);
  await expect(page.getByText('双人五行匹配')).toBeVisible();
  await expect(page.getByText('你们容易形成的相处优势')).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await expect(page.locator('.error-state')).toHaveCount(0);

  const nextPartner = await createResultByApi(page, {
    birthYear: 2005,
    birthMonth: 11,
    answers: [
      { questionCode: 'Q1', optionCode: 'WATER' },
      { questionCode: 'Q2', optionCode: 'METAL' },
      { questionCode: 'Q3', optionCode: 'EARTH' },
      { questionCode: 'Q4', optionCode: 'WOOD' },
      { questionCode: 'Q5', optionCode: 'FIRE' },
    ],
  }, 'mobile-match-next-code-e2e');
  const currentShortCode = new URL(page.url()).pathname.split('/').filter(Boolean).at(-1);
  await page.getByTestId('match-next-code').fill(nextPartner.shortCode);
  await page.getByTestId('match-next-submit').click();
  await expect(page).toHaveURL(new RegExp(`/match/${nextPartner.shortCode}/${currentShortCode}`));
  await expect(page.getByText('双人五行匹配')).toBeVisible();
});

test('home manual match accepts a full shared short link url', async ({ page }) => {
  const partner = await createResultByApi(page, {
    birthYear: 1999,
    birthMonth: 3,
    answers: [
      { questionCode: 'Q1', optionCode: 'WOOD' },
      { questionCode: 'Q2', optionCode: 'FIRE' },
      { questionCode: 'Q3', optionCode: 'WOOD' },
      { questionCode: 'Q4', optionCode: 'EARTH' },
      { questionCode: 'Q5', optionCode: 'WATER' },
    ],
  }, 'manual-full-share-url-e2e');

  await page.goto(attributedUrl('/', 'manual-full-share-url-e2e', { skipClipboardAuto: '1' }));
  await page.getByTestId('manual-match-code').fill(`${baseUrl}/s/${partner.shortCode}?channel=share&campaign=result-card`);
  await page.getByTestId('manual-match-submit').click();
  await expect(page.getByText('要和这张人格卡做双人匹配吗？')).toBeVisible();
  await expect(page.locator('.match-invite')).toContainText(`短码 ${partner.shortCode}`);
});

test('home manual short code validates input accessibly', async ({ page }) => {
  const partner = await createResultByApi(page, {
    birthYear: 1999,
    birthMonth: 3,
    answers: [
      { questionCode: 'Q1', optionCode: 'WOOD' },
      { questionCode: 'Q2', optionCode: 'FIRE' },
      { questionCode: 'Q3', optionCode: 'WOOD' },
      { questionCode: 'Q4', optionCode: 'EARTH' },
      { questionCode: 'Q5', optionCode: 'WATER' },
    ],
  }, 'manual-short-code-validation-e2e');

  await page.goto(attributedUrl('/', 'manual-short-code-validation-e2e', { skipClipboardAuto: '1' }));
  let delayedFirstCandidate = true;
  let releaseFirstCandidate = () => {};
  let markFirstCandidateContinued = () => {};
  const releaseFirstCandidatePromise = new Promise((resolve) => {
    releaseFirstCandidate = resolve;
  });
  const firstCandidateContinuedPromise = new Promise((resolve) => {
    markFirstCandidateContinued = resolve;
  });
  await page.route(`**/api/matches/candidates/${partner.shortCode}`, async (route) => {
    if (delayedFirstCandidate) {
      delayedFirstCandidate = false;
      await releaseFirstCandidatePromise;
    }
    await route.continue();
    markFirstCandidateContinued();
  });
  await page.getByTestId('manual-match-code').fill(partner.shortCode);
  await page.getByTestId('manual-match-submit').click();
  await page.getByTestId('manual-match-code').fill('abc');
  await expect(page.getByTestId('manual-match-code')).toHaveAttribute('aria-invalid', 'true');
  await page.getByTestId('manual-match-submit').click();
  await expect(page.locator('#manual-match-message')).toHaveAttribute('role', 'status');
  await expect(page.locator('#manual-match-message')).toHaveAttribute('aria-live', 'polite');
  await expect(page.getByText('请输入 6 到 7 位短码')).toBeVisible();
  const staleCandidateResponsePromise = page.waitForResponse((response) => (
    response.url().includes(`/api/matches/candidates/${partner.shortCode}`) && response.status() === 200
  ));
  releaseFirstCandidate();
  await firstCandidateContinuedPromise;
  await staleCandidateResponsePromise;
  await expect(page.getByText('要和这张人格卡做双人匹配吗？')).toHaveCount(0);

  await page.getByTestId('manual-match-code').fill(partner.shortCode);
  await page.getByTestId('manual-match-submit').click();
  await expect(page.getByText('要和这张人格卡做双人匹配吗？')).toBeVisible();
  await expect(page.locator('.match-invite')).toContainText(`短码 ${partner.shortCode}`);
  await page.getByRole('button', { name: '暂时不用' }).click();
  await expect(page.getByText('要和这张人格卡做双人匹配吗？')).toHaveCount(0);
  await page.getByTestId('manual-match-submit').click();
  await expect(page.getByText('要和这张人格卡做双人匹配吗？')).toBeVisible();
  await expect(page.locator('.match-invite')).toContainText(`短码 ${partner.shortCode}`);
});

test('test page keeps question loading failure visible and disabled', async ({ page }) => {
  await page.route('**/api/questions', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ code: 500, message: '题目加载失败，请刷新重试' }),
    });
  });

  await page.goto(attributedUrl('/test', 'questions-failure-e2e'));
  await expect(page.getByText('题目加载失败，请刷新重试')).toBeVisible();
  await page.getByTestId('birth-year-quick-2002').click();
  await page.getByTestId('birth-month-8').click();
  await expect(page.getByTestId('birth-inline-primary-action')).toBeDisabled();
  await expect(page.getByTestId('birth-inline-primary-action')).toHaveText('题目加载失败');
  await expect(page.getByText('题目加载失败，请刷新重试')).toBeVisible();
});

test('mobile native share passes the attributed short link payload', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'share', {
      configurable: true,
      value: async (payload) => {
        window.__nativeSharePayload = payload;
      },
    });
  });
  const result = await createResultByApi(page, {
    birthYear: 2002,
    birthMonth: 8,
    answers: [
      { questionCode: 'Q1', optionCode: 'METAL' },
      { questionCode: 'Q2', optionCode: 'WOOD' },
      { questionCode: 'Q3', optionCode: 'WATER' },
      { questionCode: 'Q4', optionCode: 'FIRE' },
      { questionCode: 'Q5', optionCode: 'EARTH' },
    ],
  }, 'native-share-e2e');

  await page.goto(attributedUrl(`/result/${result.resultId}`, 'native-share-e2e'));
  await expect(page.getByText('你的五行人格身份')).toBeVisible();
  await expectNoHorizontalOverflow(page);
  await expectNoElementMarkGraphics(page);
  await page.getByTestId('native-share').click();
  const sharedPayload = await page.evaluate(() => window.__nativeSharePayload);
  expect(sharedPayload).toMatchObject({
    title: '我的五行人格卡',
    text: '我刚生成了一张五行人格卡，看看像不像我。',
  });
  expect(sharedPayload.url).toContain(`/s/${result.shortCode}`);
  expect(sharedPayload.url).toContain('channel=share');
  expect(sharedPayload.url).toContain('campaign=result-card');
  await expect(page.locator('.share-box .tip')).toHaveCount(0);
});

test('admin rejects invalid token without persisting stale data', async ({ page }) => {
  await page.goto(`${baseUrl}/admin`);
  await page.evaluate(() => localStorage.setItem('wuxing_admin_token', 'stale-token'));
  await page.reload();

  await expect(page.getByText('管理 token 无效，请重新输入。')).toBeVisible();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toHaveCount(0);
  await expect(page.getByTestId('admin-export-csv')).toBeDisabled();
  await expect(page.getByPlaceholder('输入管理 token')).toHaveValue('');
  expect(await page.evaluate(() => localStorage.getItem('wuxing_admin_token'))).toBeNull();

  await page.getByPlaceholder('输入管理 token').fill('wrong-token');
  await page.getByTestId('admin-login-button').click();
  await expect(page.getByText('管理 token 无效，请重新输入。')).toBeVisible();
  await expect(page.getByRole('heading', { name: '增长漏斗' })).toHaveCount(0);
  await expect(page.getByTestId('admin-export-csv')).toBeDisabled();
  await expect(page.getByPlaceholder('输入管理 token')).toHaveValue('');
  expect(await page.evaluate(() => localStorage.getItem('wuxing_admin_token'))).toBeNull();
});

test('admin short link detail rejects invalid token accessibly', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('wuxing_admin_token', 'stale-token'));
  await page.goto(`${baseUrl}/admin/short-links/sample`);

  await expect(page.getByText('短链访问详情')).toBeVisible();
  await expect(page.getByText('管理 token 无效，请返回后台重新登录。')).toBeVisible();
  await expect(page.locator('.error-text')).toHaveAttribute('role', 'alert');
  await expect(page.locator('.error-text')).toHaveAttribute('aria-live', 'polite');
  expect(await page.evaluate(() => localStorage.getItem('wuxing_admin_token'))).toBeNull();
});

test('admin short link detail renders external visits with missing fingerprints', async ({ page }) => {
  await page.addInitScript((token) => localStorage.setItem('wuxing_admin_token', token), adminToken);
  await page.route('**/api/admin/short-links/external-null/visits**', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'success',
        data: {
          page: 1,
          pageSize: 20,
          total: 1,
          records: [
            {
              createdAt: '2026-06-19T01:30:00',
              eventType: 'EXTERNAL_SHORT_LINK_VISIT',
              clientIdHash: null,
              ipHash: null,
              userAgentHash: null,
              channel: null,
              campaign: null,
              deviceType: null,
              referer: null,
              statSource: 'external',
            },
          ],
        },
      }),
    });
  });

  await page.goto(`${baseUrl}/admin/short-links/external-null?includeSynthetic=true&statSource=external`);

  await expect(page.getByText('短链访问详情')).toBeVisible();
  await expect(page.getByText('明细来源 外部平台')).toBeVisible();
  await expect(page.getByText('测试流量开关只影响 perf-test 排除')).toBeVisible();
  await expect(page.getByTestId('shortlink-visit-card')).toBeVisible();
  await expect(page.getByTestId('shortlink-visit-card').getByText('外部短链访问')).toBeVisible();
  await expect(page.getByTestId('shortlink-visit-card').getByText('外部平台未返回')).toHaveCount(2);
  const dashHashCount = await page.locator('.mobile-debug code').evaluateAll((codes) => (
    codes.filter((code) => code.textContent?.trim() === '-').length
  ));
  expect(dashHashCount).toBe(3);
  await expectNoHorizontalOverflow(page);
});

async function createResultByApi(page, overrides, campaign = 'mobile-match-e2e') {
  const response = await page.request.post(`${baseUrl}/api/results`, {
    headers: {
      'X-Channel': 'perf-test',
      'X-Campaign': campaign,
    },
    data: {
      birthDay: null,
      birthTimeRange: null,
      ...overrides,
    },
  });
  expect(response.ok()).toBeTruthy();
  const payload = await response.json();
  expect(payload.code).toBe(0);
  expect(payload.data.shortCode).toBeTruthy();
  return payload.data;
}

function attributedUrl(path, campaign, extraParams = {}) {
  const target = new URL(path, baseUrl);
  target.searchParams.set('channel', 'perf-test');
  target.searchParams.set('campaign', campaign);
  for (const [key, value] of Object.entries(extraParams)) {
    target.searchParams.set(key, value);
  }
  return target.toString();
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
  throw new Error('Refusing to run browser E2E against a public URL. Set ALLOW_PUBLIC_E2E=1 only during an authorized test window.');
}

function isPrivateIpv4(host) {
  if (isIP(host) !== 4) {
    return false;
  }
  const [first, second] = host.split('.').map(Number);
  return first === 10 || (first === 172 && second >= 16 && second <= 31) || (first === 192 && second === 168);
}
