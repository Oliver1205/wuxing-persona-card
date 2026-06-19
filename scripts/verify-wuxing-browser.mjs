import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const require = createRequire(import.meta.url);
const { chromium } = require('../frontend/node_modules/playwright');

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const pageUrl = pathToFileURL(path.join(root, 'outputs', 'wuxing-frontend-flow-preview.html')).href;
const viewport = { width: 426, height: 922 };
const deviceScaleFactor = 2;
const screenshotResults = [];

function assert(name, condition) {
  if (!condition) {
    throw new Error(name);
  }
}

function nearly(actual, expected, tolerance = 1) {
  return Math.abs(actual - expected) <= tolerance;
}

async function activePage(page) {
  return page.locator('.page.active').getAttribute('data-page');
}

function assertPng(filePath, expectedWidth, expectedHeight, minBytes) {
  const bytes = fs.readFileSync(filePath);
  assert(`${path.basename(filePath)} is PNG`, bytes.subarray(0, 8).toString('hex') === '89504e470d0a1a0a');
  assert(`${path.basename(filePath)} width`, bytes.readUInt32BE(16) === expectedWidth);
  assert(`${path.basename(filePath)} height`, bytes.readUInt32BE(20) === expectedHeight);
  assert(`${path.basename(filePath)} has rendered content`, bytes.length >= minBytes);
}

async function settlePage(page) {
  await page.evaluate(async () => {
    await document.fonts?.ready;
    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
  });
}

async function clearToast(page) {
  await page.evaluate(() => {
    const toast = document.querySelector('.toast');
    if (!toast) {
      return;
    }
    toast.textContent = '';
    toast.classList.remove('show');
  });
  await settlePage(page);
}

async function waitForActivePage(page, expectedPage) {
  await page.waitForFunction((name) => {
    const active = document.querySelector('.page.active');
    const image = active?.querySelector('img');
    return active?.dataset.page === name
      && image?.complete
      && image.naturalWidth === 853
      && image.naturalHeight === 1844;
  }, expectedPage);
  await settlePage(page);
  assert(`active page is ${expectedPage}`, await activePage(page) === expectedPage);
}

async function assertStageLayout(page, expectedPage) {
  const metrics = await page.evaluate(() => {
    const stage = document.querySelector('.stage');
    const active = document.querySelector('.page.active');
    const image = active?.querySelector('img');
    const stageRect = stage?.getBoundingClientRect();
    const activeRect = active?.getBoundingClientRect();
    return {
      page: active?.dataset.page,
      stageWidth: Math.round(stageRect?.width || 0),
      activeWidth: Math.round(activeRect?.width || 0),
      activeHeight: Math.round(activeRect?.height || 0),
      scrollWidth: document.documentElement.scrollWidth,
      viewportWidth: window.innerWidth,
      imageComplete: Boolean(image?.complete),
      imageNaturalWidth: image?.naturalWidth || 0,
      imageNaturalHeight: image?.naturalHeight || 0,
    };
  });
  assert(`${expectedPage} stage page`, metrics.page === expectedPage);
  assert(`${expectedPage} stage width`, nearly(metrics.stageWidth, viewport.width));
  assert(`${expectedPage} active width`, nearly(metrics.activeWidth, viewport.width));
  assert(`${expectedPage} active height`, nearly(metrics.activeHeight, viewport.height));
  assert(`${expectedPage} no horizontal overflow`, metrics.scrollWidth <= metrics.viewportWidth + 1);
  assert(`${expectedPage} image loaded`, metrics.imageComplete);
  assert(`${expectedPage} image natural size`, metrics.imageNaturalWidth === 853 && metrics.imageNaturalHeight === 1844);
}

async function assertHitTarget(page, testId) {
  const result = await page.locator(`[data-testid="${testId}"]`).evaluate((element) => {
    const rect = element.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const hit = document.elementFromPoint(centerX, centerY);
    return {
      width: rect.width,
      height: rect.height,
      hitOk: hit === element || element.contains(hit) || hit?.closest?.(`[data-testid="${element.dataset.testid}"]`) === element,
      hitTestId: hit?.dataset?.testid || '',
      hitTag: hit?.tagName || '',
    };
  });
  assert(`${testId} hit target width`, result.width >= 44);
  assert(`${testId} hit target height`, result.height >= 44);
  assert(`${testId} center is clickable`, result.hitOk);
}

async function assertQuestionTextLayout(page, label) {
  const metrics = await page.evaluate(() => {
    const card = document.querySelector('[data-testid="question-card"]');
    const count = document.querySelector('.question-count');
    const title = document.querySelector('[data-question-title]');
    const hint = document.querySelector('.question-hint');
    const options = document.querySelector('[data-option-list]');
    const relative = (element) => {
      const cardRect = card.getBoundingClientRect();
      const rect = element.getBoundingClientRect();
      return {
        top: Math.round(rect.top - cardRect.top),
        bottom: Math.round(rect.bottom - cardRect.top),
      };
    };
    return {
      count: relative(count),
      title: relative(title),
      hint: relative(hint),
      options: relative(options),
    };
  });
  assert(`${label} count does not overlap title`, metrics.title.top - metrics.count.bottom >= 4);
  assert(`${label} title does not overlap hint`, metrics.hint.top - metrics.title.bottom >= 8);
  assert(`${label} hint does not overlap options`, metrics.options.top - metrics.hint.bottom >= 8);
}

async function capture(page, fileName, expectedPage, minBytes = 180000) {
  await waitForActivePage(page, expectedPage);
  await assertStageLayout(page, expectedPage);
  const outputPath = path.join(root, 'outputs', fileName);
  await page.screenshot({
    path: outputPath,
    fullPage: true,
    animations: 'disabled',
  });
  assertPng(outputPath, viewport.width * deviceScaleFactor, viewport.height * deviceScaleFactor, minBytes);
  screenshotResults.push(`outputs/${fileName}`);
}

async function setRange(locator, value) {
  await locator.evaluate((element, nextValue) => {
    element.value = String(nextValue);
    element.dispatchEvent(new Event('input', { bubbles: true }));
  }, value);
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
    viewport,
    deviceScaleFactor,
    isMobile: true,
  });

  await page.goto(pageUrl, { waitUntil: 'load' });
  await waitForActivePage(page, 'home');

  await assertHitTarget(page, 'start-test');
  await page.locator('[data-testid="start-test"]').click();
  await waitForActivePage(page, 'birth');
  await page.locator('[data-testid="year-plus"]').click();
  await page.locator('[data-testid="year-input"]').fill('1998');
  await page.locator('[data-testid="year-input"]').blur();
  await setRange(page.locator('[data-testid="year-range"]'), 2005);
  await assertHitTarget(page, 'month-6');
  await page.locator('[data-testid="month-6"]').click();
  await page.locator('[data-testid="optional-toggle"]').click();
  assert('optional panel opened', await page.locator('[data-testid="optional-panel"]').evaluate((element) => element.classList.contains('open')));
  await page.locator('[data-optional-day="15"]').click();
  await page.locator('[data-optional-time="night"]').click();
  const optionalSummary = await page.locator('[data-optional-summary]').textContent();
  assert('optional summary updates', optionalSummary.includes('15日') && optionalSummary.includes('夜间'));
  await capture(page, 'browser-check-01b-birth-optional.png', 'birth', 400000);
  await page.locator('[data-testid="optional-toggle"]').click();
  await capture(page, 'browser-check-01-birth.png', 'birth', 400000);
  assert('year range visible value', await page.locator('[data-year-text]').textContent() === '2005');

  await assertHitTarget(page, 'enter-question');
  await page.locator('[data-testid="enter-question"]').click();
  await waitForActivePage(page, 'question');
  assert('starts at q1', await page.locator('[data-question-number]').textContent() === '1');
  assert('no preselected option', await page.locator('[data-testid="option-list"] .option-row.selected').count() === 0);
  assert('next starts aria disabled', await page.locator('[data-testid="question-next"]').getAttribute('aria-disabled') === 'true');
  await assertHitTarget(page, 'question-prev');
  await assertHitTarget(page, 'question-next');
  await assertHitTarget(page, 'option-C');
  assert('q1 prev label is basic info', await page.locator('[data-testid="question-prev"]').textContent() === '基础信息');
  await assertQuestionTextLayout(page, 'q1');
  await capture(page, 'browser-check-02-question-q1.png', 'question', 300000);

  await page.locator('[data-testid="question-next"]').click({ force: true });
  assert('unanswered next stays on q1', await page.locator('[data-question-number]').textContent() === '1');
  assert('unanswered next shows toast', (await page.locator('.toast').textContent()).includes('先选一个更像你的选项'));
  await clearToast(page);
  await page.locator('[data-testid="option-C"]').click();
  assert('only C selected', await page.locator('[data-testid="option-list"] .option-row.selected').count() === 1);
  assert('C selected', await page.locator('[data-testid="option-C"]').evaluate((element) => element.classList.contains('selected')));
  assert('next aria enabled after selection', await page.locator('[data-testid="question-next"]').getAttribute('aria-disabled') === 'false');
  await capture(page, 'browser-check-03-question-q1-selected.png', 'question', 300000);

  await page.locator('[data-testid="question-next"]').click();
  assert('moves to q2', await page.locator('[data-question-number]').textContent() === '2');
  assert('q2 prev label is previous question', await page.locator('[data-testid="question-prev"]').textContent() === '上一题');
  await page.locator('[data-testid="question-prev"]').click();
  assert('previous returns to q1', await page.locator('[data-question-number]').textContent() === '1');
  assert('q1 keeps C selected after previous', await page.locator('[data-testid="option-C"]').evaluate((element) => element.classList.contains('selected')));
  await page.locator('[data-testid="question-next"]').click();
  assert('moves back to q2 with preserved q1 answer', await page.locator('[data-question-number]').textContent() === '2');
  const restAnswers = ['B', 'A', 'D', 'D'];
  for (const [index, answer] of restAnswers.entries()) {
    await page.locator(`[data-testid="option-${answer}"]`).click();
    assert(`only ${answer} selected`, await page.locator('[data-testid="option-list"] .option-row.selected').count() === 1);
    if (index === restAnswers.length - 1) {
      assert('q5 is shown before result', await page.locator('[data-question-number]').textContent() === '5');
      await assertQuestionTextLayout(page, 'q5');
      await capture(page, 'browser-check-03b-question-q5-selected.png', 'question', 300000);
    }
    await page.locator('[data-testid="question-next"]').click();
  }

  await waitForActivePage(page, 'result');
  await assertHitTarget(page, 'copy-result-link');
  await page.locator('[data-testid="copy-result-link"]').click();
  await page.waitForFunction(() => document.querySelector('.toast')?.textContent.includes('分享链接已复制'));
  await clearToast(page);
  await page.locator('[data-testid="result-element-detail"]').click();
  await page.waitForFunction(() => document.querySelector('.toast')?.textContent.includes('逐项解读'));
  await clearToast(page);
  await page.locator('[data-testid="result-overview"]').click();
  await page.waitForFunction(() => document.querySelector('.toast')?.textContent.includes('互动总览'));
  await clearToast(page);
  await capture(page, 'browser-check-04-result.png', 'result', 180000);

  await assertHitTarget(page, 'go-share');
  await page.locator('[data-testid="go-share"]').click();
  await waitForActivePage(page, 'share');
  await assertHitTarget(page, 'save-share-image');
  await assertHitTarget(page, 'copy-share-link');
  await assertHitTarget(page, 'back-result-from-share');
  await capture(page, 'browser-check-05-share.png', 'share', 120000);

  console.log(JSON.stringify({
    ok: true,
    strategy: launched.strategy,
    url: pageUrl,
    viewport: {
      width: viewport.width,
      height: viewport.height,
      deviceScaleFactor,
    },
    screenshots: screenshotResults,
  }, null, 2));
} finally {
  await browser.close();
}
