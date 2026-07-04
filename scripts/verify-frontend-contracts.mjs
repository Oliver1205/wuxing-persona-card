#!/usr/bin/env node
import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(import.meta.url);
const failures = [];

function fail(name, detail = '') {
  failures.push(detail ? `${name}: ${detail}` : name);
}

function assert(name, condition, detail = '') {
  if (!condition) {
    fail(name, detail);
  }
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function requireFrontendDependency(name) {
  return require(path.join(root, 'frontend/node_modules', name));
}

function evaluateTypeScriptModule(relativePath) {
  const ts = requireFrontendDependency('typescript');
  const source = read(relativePath);
  const transpiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  }).outputText;
  const module = { exports: {} };
  const context = vm.createContext({
    exports: module.exports,
    module,
    require,
    console,
  });
  new vm.Script(transpiled, { filename: relativePath }).runInContext(context);
  return module.exports;
}

function listFiles(dir, predicate) {
  const absoluteDir = path.join(root, dir);
  if (!fs.existsSync(absoluteDir)) {
    return [];
  }
  return fs.readdirSync(absoluteDir, { withFileTypes: true })
    .flatMap((entry) => {
      const relativePath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        return listFiles(relativePath, predicate);
      }
      return predicate(relativePath) ? [relativePath] : [];
    });
}

function assertContains(name, text, snippet) {
  assert(name, text.includes(snippet), `missing ${snippet}`);
}

function assertDecoratedField(name, source, fieldName, annotations) {
  const fieldPattern = new RegExp(`((?:\\s*@[^\\n]+\\n)+)\\s*private\\s+[^;]+\\s+${fieldName};`);
  const match = source.match(fieldPattern);
  assert(`${name} field exists with annotations`, Boolean(match), fieldName);
  if (!match) {
    return;
  }
  annotations.forEach((annotation) => {
    if (annotation instanceof RegExp) {
      assert(`${name} keeps ${annotation}`, annotation.test(match[1]), match[1]);
      return;
    }
    assertContains(`${name} keeps ${annotation}`, match[1], annotation);
  });
}

const distFiles = listFiles('frontend/dist', (filePath) => /\.(html|js|css)$/.test(filePath));
assert('frontend dist exists', distFiles.length > 0, 'run npm --prefix frontend run build first');
const distText = distFiles.map(read).join('\n');

[
  '/api/questions',
  '/api/results',
  '/api/matches',
  '/api/admin/short-links/export',
  'manual-match-code',
  'matchCode',
  'shared-result',
  'share-box',
  'start-test-link',
  'test-primary-action',
  'copy-share-link',
  'admin-login-button',
  'X-Channel',
  'X-Campaign',
  'beian.miit.gov.cn',
].forEach((snippet) => assertContains(`dist keeps ${snippet}`, distText, snippet));

const sourceFiles = [
  'frontend/src/api/questions.ts',
  'frontend/src/api/results.ts',
  'frontend/src/api/matches.ts',
  'frontend/src/api/admin.ts',
  'frontend/src/api/request.ts',
  'frontend/src/pages/GuidePage.vue',
  'frontend/src/pages/TestPage.vue',
  'frontend/src/pages/ResultPage.vue',
  'frontend/src/pages/MatchPage.vue',
  'frontend/src/components/ElementMark.vue',
  'frontend/src/components/RegulatoryFooter.vue',
  'frontend/src/components/ShareLinkBox.vue',
  'frontend/src/pages/AdminDashboard.vue',
  'frontend/src/pages/AdminShortLinkDetail.vue',
  'frontend/src/pages/NotFoundPage.vue',
  'frontend/src/style.css',
  'frontend/src/utils/attribution.ts',
  'frontend/src/utils/shareCard.ts',
  'frontend/e2e/mobile-main-flow.spec.mjs',
  'frontend/e2e/showcase-screenshots.spec.mjs',
  'scripts/mobile-e2e.sh',
  'scripts/capture-showcase-screenshots.sh',
  'scripts/quality-check.sh',
];
const sourceText = sourceFiles.map(read).join('\n');
const resultPageSource = read('frontend/src/pages/ResultPage.vue');

[
  'manual-match-code',
  '.share-box',
  'start-test-link',
  'manual-match-submit',
  'match-accept-button',
  'test-primary-action',
  'copy-share-link',
  'admin-login-button',
  'admin-export-csv',
  'X-Channel',
  'X-Campaign',
  'perf-test',
  'VITE_ICP_RECORD_NO',
  'beian.miit.gov.cn',
].forEach((snippet) => assertContains(`source and e2e keep ${snippet}`, sourceText, snippet));

[
  '底色清晰型',
  '双气互照型',
  '星宿部分',
  'result-data-strip',
  '主五行',
  '副五行',
].forEach((snippet) => {
  assert(`result page hides backend/redundant label ${snippet}`, !resultPageSource.includes(snippet));
});

const mobileE2e = read('frontend/e2e/mobile-main-flow.spec.mjs');
const showcaseE2e = read('frontend/e2e/showcase-screenshots.spec.mjs');
const eightHourArtifacts = read('scripts/verify-eight-hour-artifacts.sh');
const mobileE2eScript = read('scripts/mobile-e2e.sh');
const showcaseScript = read('scripts/capture-showcase-screenshots.sh');
const qualityCheckScript = read('scripts/quality-check.sh');
const qualityGateWorkflow = read('.github/workflows/quality-gate.yml');
const frontendTypes = read('frontend/src/api/types.ts');
const backendCreateResultRequest = read('backend/src/main/java/com/wuxing/persona/dto/CreateResultRequest.java');
const backendCreateMatchRequest = read('backend/src/main/java/com/wuxing/persona/dto/CreateMatchRequest.java');
const backendAnswerRequest = read('backend/src/main/java/com/wuxing/persona/dto/AnswerRequest.java');
const backendTestFlowPolicy = read('backend/src/main/java/com/wuxing/persona/common/TestFlowPolicy.java');
const appProperties = read('backend/src/main/java/com/wuxing/persona/config/AppProperties.java');
const corsWebConfig = read('backend/src/main/java/com/wuxing/persona/config/CorsWebConfig.java');
const backendApplicationYml = read('backend/src/main/resources/application.yml');
const mvpFlowIntegrationTest = read('backend/src/test/java/com/wuxing/persona/MvpFlowIntegrationTest.java');
const externalProvider = read('backend/src/main/java/com/wuxing/persona/service/shortlink/ExternalShortLinkProvider.java');
const externalStatsAdapter = read('backend/src/main/java/com/wuxing/persona/service/shortlink/ExternalShortLinkStatsAdapter.java');
const externalStatsAdapterTest = read('backend/src/test/java/com/wuxing/persona/service/shortlink/ExternalShortLinkStatsAdapterTest.java');
const backendAdminStatService = read('backend/src/main/java/com/wuxing/persona/service/AdminStatService.java');
const backendSchema = read('backend/src/main/resources/db/schema.sql');
const externalSmokeScript = read('scripts/external-shortlink-smoke-test.sh');
const apiSpec = read('docs/api-spec.md');
const productionRunbook = read('docs/production-operations-runbook.md');
const deployEnvExample = read('deploy/.env.example');
const deployEnvExternalExample = read('deploy/.env.external.example');
assertContains('AppProperties exposes CORS settings', appProperties, 'private CorsProperties cors = new CorsProperties()');
assertContains('AppProperties CORS defaults closed', appProperties, 'private List<String> allowedOrigins = new ArrayList<>()');
assertContains('AppProperties trims blank CORS origins', appProperties, '.filter(origin -> !origin.isBlank())');
assertContains('CorsWebConfig is global MVC config', corsWebConfig, 'implements WebMvcConfigurer');
assertContains('CorsWebConfig keeps CORS closed when whitelist is empty', corsWebConfig, 'if (allowedOrigins.isEmpty())');
assertContains('CorsWebConfig allows frontend attribution headers', corsWebConfig, 'X-Campaign');
assertContains('CorsWebConfig allows admin token header', corsWebConfig, 'X-Admin-Token');
assertContains('CorsWebConfig exposes CSV download header', corsWebConfig, 'Content-Disposition');
assertContains('CorsWebConfig supports preflight method', corsWebConfig, '"OPTIONS"');
assertContains('application.yml exposes CORS origins env', backendApplicationYml, 'CORS_ALLOWED_ORIGINS');
assertContains('application.yml exposes CORS max age env', backendApplicationYml, 'CORS_MAX_AGE_SECONDS');
assertContains('MvpFlowIntegrationTest covers allowed CORS preflight', mvpFlowIntegrationTest, 'shouldAllowConfiguredCorsPreflightForIndependentFrontendDomain');
assertContains('MvpFlowIntegrationTest covers rejected CORS preflight', mvpFlowIntegrationTest, 'shouldRejectCorsPreflightFromUnconfiguredOrigin');
assertContains('MvpFlowIntegrationTest verifies Access-Control-Allow-Origin', mvpFlowIntegrationTest, 'Access-Control-Allow-Origin');
assertContains('MvpFlowIntegrationTest verifies admin token preflight header', mvpFlowIntegrationTest, 'X-Admin-Token');
assertContains('API spec documents CORS_ALLOWED_ORIGINS', apiSpec, 'CORS_ALLOWED_ORIGINS=https://www.wuxingcard.cn,https://wuxingcard.cn');
assertContains('API spec documents default CORS closed', apiSpec, '默认不配置 `CORS_ALLOWED_ORIGINS` 时不开放跨域');
assertContains('production runbook documents independent API CORS', productionRunbook, '独立 API 域名可选配置');
assertContains('deploy env example documents optional CORS', deployEnvExample, 'CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com');
assertContains('external deploy env example documents optional CORS', deployEnvExternalExample, 'CORS_ALLOWED_ORIGINS=https://wuxing.example.com');
assertContains('deploy env example documents ICP record variable', deployEnvExample, 'VITE_ICP_RECORD_NO=replace-with-issued-icp-record-no');
assertContains('deploy env example documents MIIT ICP link', deployEnvExample, 'VITE_ICP_LINK=https://beian.miit.gov.cn/');
assertContains('external deploy env example documents ICP record variable', deployEnvExternalExample, 'VITE_ICP_RECORD_NO=replace-with-issued-icp-record-no');
assertContains('docker compose passes ICP record build arg', read('deploy/docker-compose.yml'), 'VITE_ICP_RECORD_NO: ${VITE_ICP_RECORD_NO:-}');
assert('E2E API seed is isolated by channel', /['"]X-Channel['"]:\s*['"]perf-test['"]/.test(mobileE2e));
assert('E2E API seed helper keeps default campaign', /campaign\s*=\s*['"]mobile-match-e2e['"]/.test(mobileE2e));
assert('E2E API seed uses per-test campaign header', /['"]X-Campaign['"]:\s*campaign/.test(mobileE2e));
[
  'question-Q1-option-METAL',
  'question-Q2-option-WOOD',
  'question-Q3-option-WATER',
  'question-Q4-option-FIRE',
  'question-Q5-option-EARTH',
].forEach((testId) => {
  assertContains(`mobile e2e keeps ${testId}`, mobileE2e, testId);
  assertContains(`showcase e2e keeps ${testId}`, showcaseE2e, testId);
});
[
  'start-test-link',
  'test-primary-action',
  'admin-login-button',
].forEach((testId) => {
  assertContains(`mobile e2e uses ${testId}`, mobileE2e, testId);
  assertContains(`showcase e2e uses ${testId}`, showcaseE2e, testId);
});
[
  'manual-match-submit',
  'match-accept-button',
  'save-share-image',
  'copy-match-code',
  'copy-share-link',
  'native-share',
].forEach((testId) => assertContains(`mobile e2e uses ${testId}`, mobileE2e, testId));
[
  'clipboard disabled in fallback e2e',
  ".share-box .short-code",
  "toBe(shortCodeText)",
  '分享图已生成',
  '分享图生成失败',
  '长按短码手动复制',
  '可以使用系统分享',
  '当前浏览器不支持系统分享',
  'String(window.getSelection())',
].forEach((snippet) => assertContains(`mobile e2e verifies share fallback ${snippet}`, mobileE2e, snippet));
[
  'mobile native share passes the attributed short link payload',
  '__nativeSharePayload',
  "title: '我的五行人格卡'",
  "text: '我刚生成了一张五行人格卡，看看像不像我。'",
  'channel=share',
  'campaign=result-card',
].forEach((snippet) => assertContains(`mobile e2e verifies native share success ${snippet}`, mobileE2e, snippet));
[
  'admin rejects invalid token without persisting stale data',
  'stale-token',
  'wrong-token',
  '管理 token 无效，请重新输入。',
  'localStorage.getItem(\'wuxing_admin_token\')',
  "toHaveValue('')",
  "getByRole('heading', { name: '增长漏斗' })",
  "getByTestId('admin-export-csv')).toBeDisabled()",
].forEach((snippet) => assertContains(`mobile e2e verifies admin invalid token ${snippet}`, mobileE2e, snippet));
[
  'admin short link detail rejects invalid token accessibly',
  '管理 token 无效，请返回后台重新登录。',
  "toHaveAttribute('role', 'alert')",
  "toHaveAttribute('aria-live', 'polite')",
  '/admin/short-links/sample',
  'shortlink-visit-card',
  "expect(page.locator('.table-wrap')).toBeHidden()",
].forEach((snippet) => assertContains(`mobile e2e verifies admin detail invalid token ${snippet}`, mobileE2e, snippet));
[
  '/api/admin/short-links/export',
  "from 'node:fs/promises'",
  "from 'node:net'",
  'assertLocalE2EBaseUrl(baseUrl)',
  "channel', 'perf-test'",
  "shareDownload.suggestedFilename()).toBe(`wuxing-${resultId}.png`)",
  '0x89, 0x50, 0x4e, 0x47',
  'shareImageBody.length).toBeGreaterThan(32_000)',
  'shareImageBody.readUInt32BE(16)).toBe(900)',
  'shareImageBody.readUInt32BE(20)).toBe(1200)',
  "expect(matchRequest.headers()['x-channel']).toBe('perf-test')",
  "page.waitForEvent('download')",
  'exportDownload.path()',
  'text/csv',
  'content-disposition',
  'wuxing-short-links',
  '0xef, 0xbb, 0xbf',
  'shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,metricSource,createdAt,lastVisitAt',
].forEach((snippet) => assertContains(`mobile e2e verifies admin csv ${snippet}`, mobileE2e, snippet));
assert('mobile e2e does not read CSV download from response body', !/exportResponse\.body\(\)/.test(mobileE2e));
assertContains('mobile e2e uses a real mobile user agent', mobileE2e, 'Mobile/15E148');
assertContains('mobile e2e checks flow overflow', mobileE2e, 'expectNoHorizontalOverflow(page)');
assertContains('mobile e2e checks text-only element marks', mobileE2e, 'expectNoElementMarkGraphics(page)');
assert('mobile e2e checks at least four flow overflow surfaces', (mobileE2e.match(/await expectNoHorizontalOverflow\(page\)/g) || []).length >= 4);
assert('mobile e2e checks at least four element mark surfaces', (mobileE2e.match(/await expectNoElementMarkGraphics\(page\)/g) || []).length >= 4);
assertContains('mobile e2e element mark guard covers image/canvas', mobileE2e, '.element-mark svg, .element-mark path, .element-mark img, .element-mark canvas');
assertContains('mobile e2e element mark guard checks background image', mobileE2e, 'getComputedStyle(mark).backgroundImage');
assertContains('mobile e2e element mark guard checks descendants', mobileE2e, '.element-mark, .element-mark *');
assertContains('mobile e2e rejects match error state', mobileE2e, "page.locator('.error-state')).toHaveCount(0)");
assertContains('mobile e2e seeds match helper with match campaign', mobileE2e, "}, 'mobile-match-e2e');");
assertContains('mobile e2e seeds manual short code helper with manual campaign', mobileE2e, "}, 'manual-short-code-validation-e2e');");
assertContains('mobile e2e seeds native share helper with native campaign', mobileE2e, "}, 'native-share-e2e');");
assertContains('mobile e2e verifies match POST partner short code', mobileE2e, 'partnerShortCode: partner.shortCode');
assertContains('mobile e2e verifies match POST birth year', mobileE2e, 'birthYear: 2002');
assertContains('mobile e2e verifies match POST birth month', mobileE2e, 'birthMonth: 8');
assertContains('mobile e2e uses exact match POST path', mobileE2e, "new URL(request.url()).pathname === '/api/matches'");
assertContains('mobile e2e verifies null birth day', mobileE2e, 'birthDay: null');
assertContains('mobile e2e verifies null birth time range', mobileE2e, 'birthTimeRange: null');
assertContains('mobile e2e verifies match POST answers', mobileE2e, 'expect(matchPayload.answers).toEqual');
assertContains('mobile e2e mocks external visits with missing fingerprints', mobileE2e, 'EXTERNAL_SHORT_LINK_VISIT');
assertContains('mobile e2e verifies null external fingerprints render as dash', mobileE2e, 'expect(dashHashCount).toBe(3)');
assertContains('mobile e2e verifies detail statSource context copy', mobileE2e, '明细来源 外部平台');
assertContains('showcase e2e has public guard', showcaseE2e, 'assertLocalE2EBaseUrl(baseUrl)');
assertContains('showcase e2e keeps synthetic channel', showcaseE2e, "channel', 'perf-test'");
assertContains('showcase e2e uses iPhone mobile user agent', showcaseE2e, 'iPhone; CPU iPhone OS');
assertContains('showcase e2e uses Android mobile user agent', showcaseE2e, 'Android 13; Pixel 7');
assertContains('showcase e2e checks mobile home overflow', showcaseE2e, 'expectNoHorizontalOverflow(page)');
assertContains('showcase e2e checks text-only element marks', showcaseE2e, 'expectNoElementMarkGraphics(page)');
assertContains('showcase e2e checks control text fit', showcaseE2e, 'expectNoControlTextOverflow(page');
assert('showcase e2e checks at least twelve control text surfaces', (showcaseE2e.match(/await expectNoControlTextOverflow\(page/g) || []).length >= 12);
assertContains('showcase e2e captures mobile admin overview', showcaseE2e, 'captureMobileAdminOverview(page, viewport)');
assertContains('CI quality gate has browser e2e job', qualityGateWorkflow, 'browser-e2e:');
assertContains('CI browser e2e installs Playwright Chromium', qualityGateWorkflow, 'playwright install --with-deps chromium');
assertContains('CI browser e2e runs mobile E2E', qualityGateWorkflow, 'scripts/mobile-e2e.sh');
assertContains('CI browser e2e captures showcase screenshots', qualityGateWorkflow, 'scripts/capture-showcase-screenshots.sh');
assertContains('CI browser e2e verifies showcase artifacts', qualityGateWorkflow, 'scripts/verify-eight-hour-artifacts.sh');
assertContains('CI quality gate checks committed quality scripts', qualityGateWorkflow, 'Verify quality gate scripts are committed');
assertContains('CI quality gate checks frontend contract script is tracked', qualityGateWorkflow, 'scripts/verify-frontend-contracts.mjs');
assertContains('local quality check reports quality script tracking status', qualityCheckScript, 'Verify quality gate script tracking status');
assertContains('local quality check can enforce tracked scripts in CI', qualityCheckScript, 'REQUIRE_TRACKED_QUALITY_SCRIPTS');
assertContains('local quality check warns during dirty-tree iteration', qualityCheckScript, 'local dirty-tree checks continue for iteration');
assertContains('CI browser e2e uploads Playwright test results', qualityGateWorkflow, 'frontend/test-results/');
assertContains('CI browser e2e uploads Playwright HTML report', qualityGateWorkflow, 'frontend/playwright-report/');
assertContains('mobile e2e keeps trace artifacts on failure', mobileE2eScript, '--trace=retain-on-failure');
assertContains('showcase e2e keeps trace artifacts on failure', showcaseScript, '--trace=retain-on-failure');
assertContains('showcase e2e checks mobile admin scroll start', showcaseE2e, 'expect(adminCanvas.scrollLeft).toBe(0)');
assertContains('showcase e2e checks mobile admin no page scroll', showcaseE2e, 'expect(adminCanvas.scrollWidth).toBeLessThanOrEqual(adminCanvas.clientWidth + 1)');
assertContains('showcase e2e checks mobile admin left edge', showcaseE2e, 'expect(adminPageLeft).toBeGreaterThanOrEqual(0)');
assertContains('showcase e2e verifies mobile report toggle', showcaseE2e, "getByTestId('admin-mobile-report-toggle')");
assertContains('showcase e2e keeps mobile admin detail collapsed by default', showcaseE2e, "expect(page.locator('#shortlink-section')).toBeHidden()");
assertContains('showcase e2e clicks mobile evidence link before detail expands', showcaseE2e, "text-link[href=\"#shortlink-section\"]");
assertContains('showcase e2e verifies mobile admin detail expands', showcaseE2e, "expect(page.locator('#shortlink-section')).toBeVisible()");
assertContains('showcase e2e verifies mobile short link cards', showcaseE2e, "getByTestId('admin-shortlink-mobile-list')");
assertContains('showcase e2e hides wide short link table on mobile', showcaseE2e, "locator('#shortlink-section .shortlink-table-wrap')).toBeHidden()");
assertContains('showcase e2e verifies evidence target enters viewport', showcaseE2e, 'rect.top < window.innerHeight && rect.bottom > 0');
assertContains('showcase e2e verifies localized empty date placeholders', showcaseE2e, 'expectLocalizedDatePlaceholders');
assertContains('showcase e2e verifies filled date values hide placeholders', showcaseE2e, 'expectFilledDateValues');
assertContains('showcase e2e verifies minimum touch targets', showcaseE2e, 'expectMinimumTouchTargets');
assertContains('showcase e2e verifies long scope chips stay in viewport', showcaseE2e, 'expectScopeChipsWithinViewport');
assertContains('showcase e2e uses long keyword for chip wrapping', showcaseE2e, 'very-long-keyword-这是一段非常长的排查关键词-ABCDEFGHIJKLMNOPQRSTUVWXYZ');
assertContains('showcase e2e includes disclosure summaries in touch target checks', showcaseE2e, '[role="button"], summary');
assertContains('showcase e2e verifies date placeholder text', showcaseE2e, 'afterContent: \'"选择开始日期"\'');
assertContains('showcase e2e verifies 44px touch target floor', showcaseE2e, 'keeps touch targets at least 44px');
assertContains('showcase e2e saves mobile admin screenshot', showcaseE2e, '-08-admin-overview.png');
assertContains('showcase e2e saves expanded mobile admin report screenshot', showcaseE2e, '-10-admin-report-expanded.png');
assertContains('showcase e2e saves core mobile admin report screenshot', showcaseE2e, '-11-admin-report-core.png');
assertContains('showcase e2e saves trend mobile admin report screenshot', showcaseE2e, '-12-admin-report-trend.png');
assertContains('showcase e2e opens trend group for visual coverage', showcaseE2e, "getByTestId('admin-mobile-report-group-trend').click()");
assertContains('showcase e2e captures mobile short link detail screenshots', showcaseE2e, '-09-shortlink-detail.png');
assertContains('showcase e2e verifies mobile short link detail cards', showcaseE2e, "getByTestId('shortlink-visit-card')");
assertContains('showcase e2e hides mobile short link detail table', showcaseE2e, "expect(page.locator('.table-wrap')).toBeHidden()");
assertContains('showcase e2e element mark guard covers image/canvas', showcaseE2e, '.element-mark svg, .element-mark path, .element-mark img, .element-mark canvas');
assertContains('showcase e2e element mark guard checks background image', showcaseE2e, 'getComputedStyle(mark).backgroundImage');
assertContains('showcase e2e element mark guard checks descendants', showcaseE2e, '.element-mark, .element-mark *');
assertContains('showcase e2e control text guard covers selects', showcaseE2e, 'input, select, textarea');
assertContains('showcase e2e checks mobile home vertical rhythm', showcaseE2e, 'expectVerticalOrder(page');
assertContains('showcase e2e keeps manual match tools in home rhythm check', showcaseE2e, "'.manual-match-tools'");
assertContains('showcase e2e captures desktop result page', showcaseE2e, 'desktop-07-result.png');
assertContains('showcase e2e verifies result interpretation panel', showcaseE2e, '.interpretation-panel');
assertContains('showcase e2e keeps desktop admin artifact name', showcaseE2e, 'desktop-06-admin-overview.png');
assertContains('showcase e2e captures desktop short link detail', showcaseE2e, 'desktop-08-shortlink-detail.png');
assertContains('showcase e2e verifies short link detail page', showcaseE2e, '短链访问详情');
assertContains('showcase e2e seeds short link detail visit', showcaseE2e, 'desktop-shortlink-detail-showcase-e2e');
assertContains('showcase e2e captures mobile match screenshots', showcaseE2e, '-06-match.png');
assertContains('showcase e2e captures desktop match screenshot', showcaseE2e, 'desktop-09-match.png');
assertContains('showcase e2e verifies match page', showcaseE2e, '双人五行匹配');
assertContains('showcase e2e seeds match partner result', showcaseE2e, 'partnerShowcaseAnswers');
assertContains('showcase e2e rejects match error state', showcaseE2e, "page.locator('.error-state')).toHaveCount(0)");
assertContains('showcase e2e checks mobile match overflow', showcaseE2e, "expectNoHorizontalOverflow(page)");
assertContains('showcase e2e captures mobile not found screenshots', showcaseE2e, '-07-not-found.png');
assertContains('showcase e2e captures desktop not found screenshot', showcaseE2e, 'desktop-10-not-found.png');
assertContains('showcase e2e verifies not found page', showcaseE2e, '页面不存在');
assertContains('showcase e2e verifies mobile not found retest action', showcaseE2e, '重新测一张');
assertContains('showcase e2e checks not found overflow', showcaseE2e, "expectNoHorizontalOverflow(page)");
assertContains('showcase e2e attributes desktop result view', showcaseE2e, "attributedUrl(`/result/${result.resultId}`, campaign)");
const screenshotGeometryExpectations = [
  ['docs/screenshots/showcase/iphone-se-01-home.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-02-test-birth-card.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-02b-test-birth-ready.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-03-test-question-card.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-04-result.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-05-shared-result.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-06-match.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-07-not-found.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-08-admin-overview.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-09-shortlink-detail.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-10-admin-report-expanded.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-11-admin-report-core.png', '375', '667'],
  ['docs/screenshots/showcase/iphone-se-12-admin-report-trend.png', '375', '667'],
  ['docs/screenshots/showcase/android-wide-01-home.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-02-test-birth-card.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-02b-test-birth-ready.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-03-test-question-card.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-04-result.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-05-shared-result.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-06-match.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-07-not-found.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-08-admin-overview.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-09-shortlink-detail.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-10-admin-report-expanded.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-11-admin-report-core.png', '430', '932'],
  ['docs/screenshots/showcase/android-wide-12-admin-report-trend.png', '430', '932'],
  ['docs/screenshots/showcase/desktop-06-admin-overview.png', '1280', '900'],
  ['docs/screenshots/showcase/desktop-07-result.png', '1280', '900'],
  ['docs/screenshots/showcase/desktop-08-shortlink-detail.png', '1280', '900'],
  ['docs/screenshots/showcase/desktop-09-match.png', '1280', '900'],
  ['docs/screenshots/showcase/desktop-10-not-found.png', '1280', '900'],
];
screenshotGeometryExpectations.forEach(([screenshotPath, expectedWidth, minHeight]) => {
  assertContains(`artifact gate requires ${screenshotPath}`, eightHourArtifacts, screenshotPath);
  assertContains(
    `artifact gate checks geometry for ${screenshotPath}`,
    eightHourArtifacts,
    `"${screenshotPath} ${expectedWidth} ${minHeight}"`,
  );
});
assert(
  'artifact gate checks every showcase screenshot geometry',
  (eightHourArtifacts.match(/docs\/screenshots\/showcase\/[^\s"]+\.png \d+ \d+/g) || []).length >= 31,
);
assertContains('artifact gate loops over screenshot geometry checks', eightHourArtifacts, 'for screenshot_check in "${screenshot_geometry_checks[@]}"');
assertContains('artifact gate invokes PNG geometry helper from loop', eightHourArtifacts, 'require_png_geometry "$screenshot_path" "$expected_width" "$min_height"');
assert('artifact gate does not require untracked outputs contact sheet', !/find outputs/.test(eightHourArtifacts));
assertContains('showcase captures ready birth action state', showcaseE2e, '-02b-test-birth-ready.png');
assertContains('showcase verifies enabled bottom birth action label', showcaseE2e, "toHaveText('进入第 1 题')");
[
  [mobileE2eScript, 'mobile E2E script'],
  [showcaseScript, 'showcase screenshot script'],
].forEach(([script, label]) => {
  assertContains(`${label} has public URL override`, script, 'ALLOW_PUBLIC_E2E');
  assertContains(`${label} rejects public base url`, script, 'refusing');
  assertContains(`${label} checks local/private host`, script, 'is_local_or_private');
});
[
  'node scripts/verify-wuxing-preview.mjs',
  'node scripts/verify-wuxing-preview-flow.mjs',
].forEach((snippet) => assertContains(`quality check runs ${snippet}`, qualityCheckScript, snippet));

const requestApi = read('frontend/src/api/request.ts');
assert('request exports apiUrl helper', /export function apiUrl\(/.test(requestApi));
assert('request uses apiUrl for fetch', /const requestUrl = apiUrl\(path\);/.test(requestApi) && /fetch\(requestUrl,/.test(requestApi));
const apiUrlSource = requestApi.match(/export function apiUrl\([\s\S]*?\n}/)?.[0] || '';
assert('apiUrl helper source found', apiUrlSource.length > 0);
assert('apiUrl helper uses API_BASE_URL', /\bAPI_BASE_URL\b/.test(apiUrlSource));
assert('apiUrl helper preserves request path', /\bpath\b/.test(apiUrlSource));
assert('apiUrl helper trims trailing slashes', apiUrlSource.includes('replace(/\\/+$/'));
const runnableApiUrl = apiUrlSource.replace(/export function apiUrl\(path: string\)/, 'function apiUrl(path)');
function evaluateApiUrl(baseUrl, requestPath) {
  return Function('API_BASE_URL', `${runnableApiUrl}; return apiUrl(${JSON.stringify(requestPath)});`)(baseUrl);
}
[
  ['', '/api/results', '/api/results'],
  ['', 'api/results', '/api/results'],
  ['http://127.0.0.1:48081', '/api/results', 'http://127.0.0.1:48081/api/results'],
  ['http://127.0.0.1:48081/', '/api/results', 'http://127.0.0.1:48081/api/results'],
  ['https://api.example.com/base/', 'api/results', 'https://api.example.com/base/api/results'],
].forEach(([baseUrl, requestPath, expected]) => {
  assert('apiUrl runtime shape', evaluateApiUrl(baseUrl, requestPath) === expected, `${baseUrl} + ${requestPath}`);
});
assert('request keeps campaign attribution header', /headers\.set\(['"]X-Campaign['"],\s*attribution\.campaign\)/.test(requestApi));
assert('request guards non-json gateway responses', /content-type/.test(requestApi) && /未返回 JSON|response\.text\(\)/.test(requestApi));

const adminApi = read('frontend/src/api/admin.ts');
assert('admin api imports apiUrl helper', /import\s*\{[^}]*\bapiUrl\b[^}]*\}\s*from\s*['"]\.\/request['"]/.test(adminApi));
assert('admin export uses apiUrl helper', /fetch\(apiUrl\(`\/api\/admin\/short-links\/export/.test(adminApi));
assertContains('admin api exposes forceRefresh overview filter', adminApi, 'forceRefresh?: boolean');
assertContains('admin api appends forceRefresh query', adminApi, "params.set('forceRefresh', 'true')");

[
  'birthYear',
  'birthMonth',
  'birthDay',
  'birthTimeRange',
  'answers',
].forEach((field) => {
  assertContains(`frontend CreateResultRequest keeps ${field}`, frontendTypes, `${field}:`);
  assertContains(`backend CreateResultRequest keeps ${field}`, backendCreateResultRequest, field);
});
[
  ['birthYear', /birthYear:\s*number;/],
  ['birthMonth', /birthMonth:\s*number;/],
  ['birthDay', /birthDay:\s*number\s*\|\s*null;/],
  ['birthTimeRange', /birthTimeRange:\s*string\s*\|\s*null;/],
  ['answers', /answers:\s*Answer\[\];/],
].forEach(([field, pattern]) => {
  assert(`frontend CreateResultRequest keeps ${field} type`, pattern.test(frontendTypes));
});
[
  'questionCode',
  'optionCode',
].forEach((field) => {
  assertContains(`frontend Answer keeps ${field}`, frontendTypes, `${field}:`);
  assertContains(`backend AnswerRequest keeps ${field}`, backendAnswerRequest, field);
});
assertContains('frontend CreateMatchRequest extends result request', frontendTypes, 'export interface CreateMatchRequest extends CreateResultRequest');
assertContains('frontend CreateMatchRequest keeps partnerShortCode', frontendTypes, 'partnerShortCode: string');
assertContains('backend CreateMatchRequest extends result request', backendCreateMatchRequest, 'class CreateMatchRequest extends CreateResultRequest');
assertContains('backend CreateMatchRequest keeps partnerShortCode', backendCreateMatchRequest, 'partnerShortCode');
assertContains('backend TestFlowPolicy keeps minimum birth year', backendTestFlowPolicy, 'MIN_BIRTH_YEAR = 1950');
assertContains('backend TestFlowPolicy keeps maximum birth year', backendTestFlowPolicy, 'MAX_BIRTH_YEAR = 2026');
assertContains('backend TestFlowPolicy keeps required question count', backendTestFlowPolicy, 'REQUIRED_QUESTION_COUNT = 5');
assertDecoratedField('backend birthYear request validation', backendCreateResultRequest, 'birthYear', ['@NotNull', '@Min(TestFlowPolicy.MIN_BIRTH_YEAR)', '@Max(TestFlowPolicy.MAX_BIRTH_YEAR)']);
assertDecoratedField('backend birthMonth request validation', backendCreateResultRequest, 'birthMonth', ['@NotNull', '@Min(1)', '@Max(12)']);
assertDecoratedField('backend birthDay request validation', backendCreateResultRequest, 'birthDay', ['@Min(1)', '@Max(31)']);
assertDecoratedField('backend answers request validation', backendCreateResultRequest, 'answers', ['@Valid', '@NotNull', /@Size\(\s*min\s*=\s*TestFlowPolicy\.REQUIRED_QUESTION_COUNT\s*,\s*max\s*=\s*TestFlowPolicy\.REQUIRED_QUESTION_COUNT\s*\)/]);
assertDecoratedField('backend match partner short code validation', backendCreateMatchRequest, 'partnerShortCode', ['@NotBlank']);
assertDecoratedField('backend answer question code validation', backendAnswerRequest, 'questionCode', ['@NotBlank']);
assertDecoratedField('backend answer option code validation', backendAnswerRequest, 'optionCode', ['@NotBlank']);

const attribution = read('frontend/src/utils/attribution.ts');
assert('share links keep share channel attribution', /channel:\s*['"]share['"]/.test(attribution));
assert('share links keep result-card campaign attribution', /campaign:\s*['"]result-card['"]/.test(attribution));
const globalStyle = read('frontend/src/style.css');

const questionCard = read('frontend/src/components/QuestionCard.vue');
assert(
  'QuestionCard exposes dynamic option test ids',
  questionCard.includes('`question-${question.questionCode}-option-${option.optionCode}`'),
);
assert('QuestionCard option state is icon-only', !/option-state[^>]*>\s*\{\{/.test(questionCard));
assert('QuestionCard does not render choose/selected text badges', !/已选|'\s*选择\s*'|"\s*选择\s*"/.test(questionCard));
assert('QuestionCard hides inactive option state', /\.option-state\s*\{[\s\S]*?opacity:\s*0;/.test(questionCard));
assert('QuestionCard shows active option state', /\.option\.active\s+\.option-state\s*\{[\s\S]*?opacity:\s*1;/.test(questionCard));

const testPage = read('frontend/src/pages/TestPage.vue');
const testFlowMachine = read('frontend/src/utils/testFlowMachine.ts');
const testFlowMachineRuntime = evaluateTypeScriptModule('frontend/src/utils/testFlowMachine.ts');
assert('TestPage flow stage avoids hanging Transition wrapper', !/<Transition\b/.test(testPage));
assertContains('TestPage uses explicit test flow machine', testPage, 'deriveTestFlowMachineState');
assertContains('TestPage imports birth year lower bound', testPage, 'MIN_BIRTH_YEAR');
assertContains('TestPage imports birth year upper bound', testPage, 'MAX_BIRTH_YEAR');
assert('TestPage birth month uses visible grid', /class="choice-grid month-grid"/.test(testPage));
assert('TestPage optional day uses visible grid', /class="choice-grid day-grid"/.test(testPage));
assert('TestPage does not hide birth choices in horizontal rail', !/class="choice-rail (?:month|day)-rail"/.test(testPage));
assert('TestPage quick years use visible grid', /\.quick-row\s*\{[\s\S]*?display:\s*grid;/.test(testPage));
assert('TestPage quick years avoid horizontal scrolling', !/\.quick-row\s*\{[\s\S]*?overflow-x:\s*auto;/.test(testPage));
assertContains('TestPage quick years include current upper bound', testPage, '2026');
assertContains('TestPage quick years keep common young-user anchor', testPage, '2002');
assertContains('TestPage quick years include 1950 lower bound', testPage, '1950');
assert('TestPage keeps optional day grid span', /\.day-grid\s+\.day-chip\.optional\s*\{[\s\S]*?grid-column:\s*span 2;/.test(testPage));
assert('TestPage keeps compact narrow month grid at four columns', /@media \(max-width:\s*430px\)\s*\{[\s\S]*?\.month-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\);/.test(testPage));
assert('TestPage makes disabled primary action visually distinct', /\.primary-action-button:disabled\s*\{[\s\S]*?background:\s*#d8e3dd;[\s\S]*?opacity:\s*1;[\s\S]*?box-shadow:\s*none;/.test(testPage));
assertContains('TestPage announces loading state', testPage, 'role="status" aria-live="polite"');
assertContains('TestPage announces error state', testPage, 'class="error-text" role="alert" aria-live="polite"');
assertContains('TestPage announces submit lock', testPage, 'class="submit-lock" role="status" aria-live="polite"');
assertContains('TestPage guards unavailable question list', testPage, 'questionListUnavailable');
assertContains('TestPage preserves question loading failure', testPage, "error.value = error.value || '题目加载失败，请刷新重试'");
assertContains('Test flow machine uses contextual previous action', testFlowMachine, "activeQuestionIndex === 0 ? '基础信息' : '上一题'");
assert('TestPage previous action avoids card wording', !/>\s*上一张\s*</.test(testPage));
assert('TestPage mobile primary action is ordered after previous action', /\.primary-action-button\s*\{[\s\S]*?order:\s*2;[\s\S]*?min-height:\s*44px;/.test(testPage));
assert('TestPage mobile previous action is ordered before primary action', /\.nav-button\s*\{[\s\S]*?order:\s*1;[\s\S]*?min-height:\s*44px;/.test(testPage));
assertContains('TestPage keeps top-left back button', testPage, 'class="test-back-button"');
assertContains('TestPage keeps compact question progress after birth step', testPage, 'class="question-progress"');
assertContains('Test flow machine fixes minimum birth year', testFlowMachine, 'MIN_BIRTH_YEAR = 1950');
assertContains('Test flow machine fixes maximum birth year', testFlowMachine, 'MAX_BIRTH_YEAR = 2026');
assertContains('Test flow machine exposes birth stage', testFlowMachine, "TestFlowStage = 'birth'");
assertContains('Test flow machine exposes question stage', testFlowMachine, "'question'");
assert('Test flow machine clamps too-early birth year', testFlowMachineRuntime.clampBirthYear(1949) === 1950);
assert('Test flow machine clamps too-late birth year', testFlowMachineRuntime.clampBirthYear(2027) === 2026);
assert('Test flow machine clamps invalid low step', testFlowMachineRuntime.clampTestStepIndex(-1, 5) === 0);
assert('Test flow machine clamps invalid high step', testFlowMachineRuntime.clampTestStepIndex(9, 5) === 5);
const birthIncompleteState = testFlowMachineRuntime.deriveTestFlowMachineState({
  stepIndex: 0,
  questionCount: 5,
  birthInfoComplete: false,
  activeQuestionAnswered: false,
  submitting: false,
  loading: false,
  questionListUnavailable: false,
  matchMode: false,
});
assert('Test flow machine birth incomplete stays on birth stage', birthIncompleteState.stage === 'birth');
assert('Test flow machine birth incomplete disables primary action', birthIncompleteState.primaryActionDisabled === true);
assert('Test flow machine birth incomplete keeps helpful CTA', birthIncompleteState.primaryActionText === '选择月份后继续');
const birthCompleteState = testFlowMachineRuntime.deriveTestFlowMachineState({
  stepIndex: 0,
  questionCount: 5,
  birthInfoComplete: true,
  activeQuestionAnswered: false,
  submitting: false,
  loading: false,
  questionListUnavailable: false,
  matchMode: false,
});
assert('Test flow machine birth complete enables primary action', birthCompleteState.primaryActionDisabled === false);
assert('Test flow machine birth complete enters first question', birthCompleteState.primaryActionText === '进入第 1 题');
const firstQuestionState = testFlowMachineRuntime.deriveTestFlowMachineState({
  stepIndex: 1,
  questionCount: 5,
  birthInfoComplete: true,
  activeQuestionAnswered: false,
  submitting: false,
  loading: false,
  questionListUnavailable: false,
  matchMode: false,
});
assert('Test flow machine first question can go previous', firstQuestionState.canGoPrevious === true);
assert('Test flow machine first question previous label points to birth info', firstQuestionState.previousActionText === '基础信息');
assert('Test flow machine first question keeps right action disabled before answer', firstQuestionState.primaryActionDisabled === true);
assert('Test flow machine first question caption is compact', firstQuestionState.stepCaption === '第 1 / 5 题');
const secondAnsweredQuestionState = testFlowMachineRuntime.deriveTestFlowMachineState({
  stepIndex: 2,
  questionCount: 5,
  birthInfoComplete: true,
  activeQuestionAnswered: true,
  submitting: false,
  loading: false,
  questionListUnavailable: false,
  matchMode: false,
});
assert('Test flow machine later question previous label is intuitive', secondAnsweredQuestionState.previousActionText === '上一题');
assert('Test flow machine answered non-last question uses next CTA', secondAnsweredQuestionState.primaryActionText === '下一题');
const lastAnsweredQuestionState = testFlowMachineRuntime.deriveTestFlowMachineState({
  stepIndex: 5,
  questionCount: 5,
  birthInfoComplete: true,
  activeQuestionAnswered: true,
  submitting: false,
  loading: false,
  questionListUnavailable: false,
  matchMode: false,
});
assert('Test flow machine last question generates result card', lastAnsweredQuestionState.primaryActionText === '生成我的人格卡');
const blockedBirthState = testFlowMachineRuntime.deriveTestFlowMachineState({
  stepIndex: 0,
  questionCount: 0,
  birthInfoComplete: true,
  activeQuestionAnswered: false,
  submitting: false,
  loading: false,
  questionListUnavailable: true,
  matchMode: false,
});
assert('Test flow machine blocks when question list is unavailable', blockedBirthState.stage === 'blocked');
assert('Test flow machine shows question failure CTA', blockedBirthState.primaryActionText === '题目加载失败');
assert('Test flow machine blocks future unopened question when previous answer missing',
  testFlowMachineRuntime.canOpenTestStep(2, true, [false, false, false, false, false], 0) === false);
assert('Test flow machine opens next question after previous answer',
  testFlowMachineRuntime.canOpenTestStep(2, true, [true, false, false, false, false], 1) === true);
[
  ':data-testid="\'birth-month-\' + month"',
  'data-testid="birth-day-none"',
  'data-testid="test-previous-action"',
  'data-testid="test-primary-action"',
].forEach((testId) => assertContains(`TestPage keeps ${testId}`, testPage, testId));
assertContains('TestPage keeps mobile birth action scoped to birth step', testPage, '.sticky-action.birth-action');
assert('TestPage no longer exposes inline birth continuation after month grid', !testPage.includes('data-testid="birth-inline-primary-action"'));
assert('mobile e2e uses bottom birth continuation', /getByTestId\('test-primary-action'\)\.click\(\);[\s\S]*?getByTestId\('question-Q1-option-METAL'\)/.test(mobileE2e));
assertContains('showcase e2e uses bottom birth continuation', showcaseE2e, "getByTestId('test-primary-action').click()");
assertContains('mobile e2e covers test flow state machine', mobileE2e, 'test-flow-state-machine-e2e');
assertContains('mobile e2e covers 1950 year shortcut', mobileE2e, "getByTestId('birth-year-quick-1950')");
assertContains('mobile e2e covers 2026 year shortcut', mobileE2e, "getByTestId('birth-year-quick-2026')");
assertContains('mobile e2e verifies left previous action', mobileE2e, "getByTestId('test-previous-action')");
assertContains('mobile e2e verifies browser back within question flow', mobileE2e, 'page.goBack()');
assertContains('mobile e2e verifies question flow stays on test page', mobileE2e, "toHaveURL(/\\/test/)");
assertContains('mobile e2e covers question loading failure', mobileE2e, 'questions-failure-e2e');
assertContains('mobile e2e injects question loading failure', mobileE2e, "page.route('**/api/questions'");
assertContains('mobile e2e keeps question failure as server error', mobileE2e, 'status: 500');
assertContains('mobile e2e verifies disabled question failure action state', mobileE2e, 'toBeDisabled()');
assertContains('mobile e2e verifies disabled question failure action', mobileE2e, "toHaveText('题目加载失败')");
assertContains('mobile e2e covers manual short code validation', mobileE2e, 'manual-short-code-validation-e2e');
const manualShortCodeE2e = mobileE2e.match(/test\('home manual short code validates input accessibly'[\s\S]*?\n\}\);/)?.[0] || '';
assert('mobile e2e manual short code test found', manualShortCodeE2e.length > 0);
assertContains('mobile e2e verifies manual invalid aria state', mobileE2e, "toHaveAttribute('aria-invalid', 'true')");
assertContains('mobile e2e verifies manual short code status region', mobileE2e, '#manual-match-message');
assertContains('mobile e2e verifies manual short code message', mobileE2e, '请输入 6 到 7 位短码');
assertContains('mobile e2e verifies stale match invite is cleared', mobileE2e, "getByText('要和这张人格卡做双人匹配吗？')).toHaveCount(0)");
assertContains('mobile e2e verifies restored candidate short code', mobileE2e, 'toContainText(`短码 ${partner.shortCode}`)');
assertContains('mobile e2e delays stale candidate response', mobileE2e, 'delayedFirstCandidate');
assertContains('mobile e2e controls stale candidate response', mobileE2e, 'releaseFirstCandidatePromise');
assertContains('mobile e2e waits for stale candidate response', mobileE2e, 'firstCandidateContinuedPromise');
assertContains('mobile e2e waits for stale candidate API response', mobileE2e, 'staleCandidateResponsePromise');
assert('mobile e2e avoids fixed stale candidate sleep', !/waitForTimeout\(/.test(manualShortCodeE2e));

const matchPage = read('frontend/src/pages/MatchPage.vue');
assertContains('MatchPage announces loading state', matchPage, 'class="match-state-card" role="status" aria-live="polite"');
assertContains('MatchPage announces error state', matchPage, 'class="match-state-card error-state" role="alert" aria-live="polite"');
assertContains('MatchPage renders suggestions as steps', matchPage, 'class="suggestion-list"');
assertContains('MatchPage keeps compact suggestion step styles', matchPage, '.suggestion-step');
assertContains('MatchPage uses relationship reference cards instead of a compact legend', matchPage, 'class="reference-grid"');
assertContains('MatchPage normalizes backend headline whitespace', matchPage, 'matchHeadline');
assertContains('MatchPage removes spaces around relation headline connector', matchPage, "\\s*与\\s*");
assertContains('MatchPage removes full-width headline gaps', matchPage, '\\u00a0\\u3000');
assertContains('MatchPage balances relation headline wrapping', matchPage, 'text-wrap: balance');
assertContains('MatchPage has direct next short code input', matchPage, 'data-testid="match-next-code"');
assertContains('MatchPage routes next match without forcing home bounce', matchPage, 'router.push(`/match/${encodeURIComponent(code)}/${encodeURIComponent(currentShortCode)}`)');

const guidePage = read('frontend/src/pages/GuidePage.vue');
const guideMotto = guidePage.match(/<div class="vertical-motto"[\s\S]*?<\/div>/)?.[0] || '';
assert('GuidePage motto block found', guideMotto.length > 0);
assert('GuidePage motto is pure text', !/<(?:i|b)>/.test(guideMotto));
assertContains('GuidePage preserves synthetic match channel', guidePage, "attribution.channel === 'perf-test'");
assertContains('GuidePage synthetic match query keeps perf-test', guidePage, "channel: preserveSynthetic ? 'perf-test' : 'match'");
assertContains('GuidePage keeps manual short code message id', guidePage, 'id="manual-match-message"');
assertContains('GuidePage keeps manual short code message visible to status', guidePage, 'role="status"');
assertContains('GuidePage keeps manual short code live region', guidePage, 'aria-live="polite"');
assertContains('GuidePage keeps hidden empty manual message node', guidePage, ':class="{ empty: !clipboardMessage }"');
const guideMountedHook = guidePage.match(/onMounted\(\(\) => \{[\s\S]*?\n\}\);/)?.[0] || '';
assert('GuidePage tracks page view on mount', guideMountedHook.includes("track('PAGE_VIEW_HOME', '/')"));
assert('GuidePage waits for explicit clipboard detection click', !/detectClipboardShortCode/.test(guideMountedHook));
assertContains('GuidePage keeps manual clipboard detection button', guidePage, '@click="detectClipboardShortCode(true)"');
assertContains('GuidePage exposes manual short code invalid state', guidePage, ':aria-invalid="manualShortCode.length > 0 && !manualShortCodeValid"');
assertContains('GuidePage announces match invite', guidePage, 'class="match-invite" aria-label="双人匹配邀请" aria-live="polite"');
assertContains('GuidePage clears candidate on manual input', guidePage, '@input="resetManualMatchCandidate"');
assertContains('GuidePage clears stale manual match candidate', guidePage, 'matchCandidate.value = null');
assertContains('GuidePage resets dismissed manual match state', guidePage, 'matchDismissed.value = false');
assertContains('GuidePage guards stale manual lookup responses', guidePage, 'manualLookupSeq');
assertContains('GuidePage compares manual lookup sequence', guidePage, 'lookupSeq !== manualLookupSeq.value');
assertContains('GuidePage checks candidate response still matches input', guidePage, 'currentShortCode !== shortCode');
assertContains('GuidePage clears stale manual short code message while editing', guidePage, "clipboardMessage.value = ''");
assertContains('GuidePage shows invalid short code input subtly', guidePage, '.manual-match-entry input[aria-invalid="true"]');

const glyphSource = read('frontend/src/utils/shareCard.ts')
  .match(/function drawElementGlyph\([\s\S]*?\n}\n\nfunction drawFooter/)?.[0] || '';
assert('share card glyph source found', glyphSource.length > 0);
assert('share card element marks are not code-branch doodles', !/visual\.code\s*===/.test(glyphSource));
assert('share card element marks avoid hand-drawn paths', !/(bezierCurveTo|arc|moveTo|lineTo)\s*\(/.test(glyphSource));
assert('share card element marks are text only', !/roundRect|stroke|fillRect/.test(glyphSource));

const elementMark = read('frontend/src/components/ElementMark.vue');
assert('vue element mark is text only', !/<svg|<path|::before|::after/.test(elementMark));
assertContains('ElementMark keeps glyphs visually restrained', elementMark, 'font-size: 28px');
assertContains('ElementMark keeps compact glyphs visually restrained', elementMark, 'font-size: 22px');
const personaCard = read('frontend/src/components/PersonaCard.vue');
const elementLegend = read('frontend/src/components/ElementLegend.vue');
assert('PersonaCard removes visible header element glyph pair', !/persona-mark-pair|<ElementMark/.test(personaCard));
assertContains('PersonaCard uses primary decorative element pattern', personaCard, 'class="element-pattern water-pattern"');
assertContains('PersonaCard uses secondary decorative element pattern', personaCard, 'class="element-pattern earth-pattern"');
assertContains('PersonaCard centers identity header content', personaCard, 'place-items: center');
assertContains('PersonaCard tightens mobile identity eyebrow', personaCard, '.card-visual .eyebrow');
assertContains('ElementLegend mobile layout uses stable three-column first row', elementLegend, 'grid-template-columns: repeat(3, minmax(0, 1fr));');
assertContains('ElementLegend tiny mobile layout uses two-column rows', elementLegend, '@media (max-width: 420px)');
assert('ElementLegend no longer gives the fifth regular item full-width mobile emphasis', !/\.legend-item:last-child\s*\{[\s\S]*?grid-column:\s*1 \/ -1;/.test(elementLegend));
assertContains('MatchPage keeps desktop relation title restrained', matchPage, 'font-size: 40px');

const shareLinkBox = read('frontend/src/components/ShareLinkBox.vue');
const resultPage = read('frontend/src/pages/ResultPage.vue');
assert('ResultPage no longer renders a duplicate top save action', !/data-testid="result-primary-save-image"/.test(resultPage));
assert('ResultPage delegates the single save image action to ShareLinkBox', /<ShareLinkBox[\s\S]*?show-save-image[\s\S]*?@save-image="downloadShareImage"/.test(resultPage));
assert('Shared result CTA carries match code into the test flow', /campaign:\s*'result-banner',\s*matchCode:\s*result\.shortCode/.test(resultPage));
assert('Shared result footer CTA carries match code into the test flow', /campaign:\s*'result-footer',\s*matchCode:\s*result\.shortCode/.test(resultPage));
assert('mobile e2e verifies shared result CTA matchCode', /campaign=result-banner&matchCode=/.test(mobileE2e));
[
  'personaLabel',
  'starToneName',
  'structureTitle',
  'starToneExplanation',
  'dayMasterText',
  'primarySecondaryText',
  'accentText',
  'heavenText',
  'humanText',
  'starOfficerText',
  'strengthText',
  'growthAdvice',
].forEach((field) => assertContains(`ResultPage reads backend persona archetype field ${field}`, resultPage, field));
assert('ResultPage does not expose backend persona type ids', !/personaTypeId|命中类型/.test(resultPage));
assertContains('ResultPage renders star tone core title', resultPage, 'const coreSectionTitle = computed');
assertContains('ResultPage renders star tone explanation', resultPage, 'result.starToneExplanation');
assert('ResultPage does not render redundant generation basis panel', !/bottom-basis-panel|这张卡从哪里来|生成依据与免责声明/.test(resultPage));
assertContains('ResultPage renders day master core section', resultPage, "eyebrow: '日主'");
assertContains('ResultPage renders star officer as its own anchor section', resultPage, '<p class="eyebrow">星官</p>');
assert('ResultPage avoids duplicate stuck section naming', !/<p class="analysis-kicker">卡点<\/p>/.test(resultPage));
assert('ResultPage renders star officer explanation as separate paragraphs', /v-for="paragraph in starParagraphs"[\s\S]*class="identity-copy"/.test(resultPage));
assert('ResultPage sanitizes backend copy before rendering paragraphs', /sanitizeResultText\(current,\s*paragraph\.trim\(\)\)/.test(resultPage));
assert('ResultPage no longer keeps local persona label override table', !/labelOverrides|labelAdjectives|labelNouns/.test(resultPage));
assertContains('PersonaCard labels star tone concept', personaCard, "{{ result.starToneLabel || '星曜取象' }}");
assertContains('PersonaCard uses normalized star tone name as h1', personaCard, '<h1>{{ personaLabel }}</h1>');
assert('PersonaCard does not append star officer into h1', !/<h1>[\s\S]*starOfficerName/.test(personaCard));
assertContains('PersonaCard renders star tone hero summary', personaCard, 'props.result.heroSummary');
assertContains('PersonaCard renders star tone identity line', personaCard, 'props.result.identityLine');
assertContains('PersonaCard upgrades element roles into structured cards', personaCard, 'class="element-role primary-role"');
assertContains('PersonaCard keeps role descriptions user-facing', personaCard, '第一反应');
assertContains('PersonaCard lays keywords out as a stable grid', personaCard, 'grid-template-columns: repeat(5, minmax(0, 1fr));');
assert('ShareLinkBox copy share link remains a secondary action', /data-testid="copy-share-link"[^>]*class="secondary share-secondary-action"/.test(shareLinkBox));
assert('ShareLinkBox keeps match code and share-link copy actions visible', /data-testid="copy-match-code"[\s\S]*data-testid="copy-share-link"/.test(shareLinkBox));
assertContains('ShareLinkBox explains image and short code purposes', shareLinkBox, '分享图适合直接转发，短码可用于双人匹配。');
assertContains('ShareLinkBox presents match code as a product card', shareLinkBox, 'class="match-code-card"');
assertContains('ShareLinkBox keeps retake action weak inside share module', shareLinkBox, 'class="retake-action"');
assert('ShareLinkBox no longer renders a visible result short-link label', !/打开结果短链/.test(shareLinkBox));
assert('ShareLinkBox no longer keeps a visible short-link formatter', !/displayShareUrl|compactShareUrl/.test(shareLinkBox));
assertContains('ShareLinkBox still copies attribution URL', shareLinkBox, 'navigator.clipboard.writeText(shareUrl.value)');
assertContains('ShareLinkBox announces share action status', shareLinkBox, 'class="tip" role="status" aria-live="polite"');
assert('ShareLinkBox secondary actions use a stable grid', /\.secondary-action-grid\s*\{[\s\S]*?grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\);/.test(shareLinkBox));
assert('ShareLinkBox tiny mobile native share spans the first row', /@media \(max-width:\s*380px\)[\s\S]*button\[data-testid="native-share"\][\s\S]*grid-column:\s*1 \/ -1;/.test(shareLinkBox));
assert('ShareLinkBox mobile copy actions stay in grouped secondary actions', /class="secondary-action-grid"[\s\S]*data-testid="copy-match-code"[\s\S]*data-testid="copy-share-link"/.test(shareLinkBox));

assert('MatchPage replaces decorative compact legend with relationship reference copy', /aria-label="双人关系参照"[\s\S]*?把五行差异翻译成相处节奏/.test(matchPage) && !/ElementLegend/.test(matchPage));

const adminDashboard = read('frontend/src/pages/AdminDashboard.vue');
assertContains('AdminDashboard clears invalid admin token', adminDashboard, "localStorage.removeItem('wuxing_admin_token')");
assertContains('AdminDashboard clears invalid token input', adminDashboard, "token.value = ''");
assertContains('AdminDashboard resets stale admin data', adminDashboard, 'resetAdminData()');
assertContains('AdminDashboard guards stale external runtime request', adminDashboard, 'runtimeRequestSeq');
assertContains('AdminDashboard guards stale visit runtime request', adminDashboard, 'visitEventRuntimeRequestSeq');
assertContains('AdminDashboard has localized invalid token message', adminDashboard, '管理 token 无效，请重新输入。');
assertContains('AdminDashboard announces admin error status', adminDashboard, 'role="alert" aria-live="polite"');
assertContains('AdminDashboard announces busy status', adminDashboard, 'class="muted admin-busy" role="status" aria-live="polite"');
assertContains('AdminDashboard has mobile report toggle', adminDashboard, 'data-testid="admin-mobile-report-toggle"');
assertContains('AdminDashboard gives short link detail links a 44px target class', adminDashboard, 'class="detail-link"');
assertContains('AdminDashboard detail links meet touch target height', adminDashboard, 'min-height: 44px');
assertContains('AdminDashboard has mobile short link card list', adminDashboard, 'data-testid="admin-shortlink-mobile-list"');
assertContains('AdminDashboard keeps mobile short link metrics readable', adminDashboard, 'class="shortlink-mobile-metrics"');
assertContains('AdminDashboard keeps desktop short link table separate', adminDashboard, 'class="table-wrap shortlink-table-wrap"');
assertContains('AdminDashboard force-refreshes overview after filter actions', adminDashboard, 'load(true)');
assertContains('AdminDashboard expands collapsed mobile reports before evidence scroll', adminDashboard, 'async function locateEvidence');
assertContains('AdminDashboard locates hidden mobile report content after next tick', adminDashboard, "target?.closest('.mobile-report-content')");
assertContains('AdminDashboard opens nested mobile report group before evidence scroll', adminDashboard, "target.closest('.mobile-report-group')");
assertContains('AdminDashboard keeps mobile report group state responsive', adminDashboard, "window.matchMedia('(max-width: 760px)')");
assertContains('AdminDashboard keeps evidence drill-down focused on one mobile report group', adminDashboard, 'openMobileReportGroup(reportGroupKey, compactReportGroups.value)');
assertContains('AdminDashboard keeps manual mobile report group toggles focused', adminDashboard, 'openMobileReportGroup(key, true)');
assertContains('AdminDashboard exclusive mobile report group opens only the target core group', adminDashboard, "core: key === 'core'");
assertContains('AdminDashboard groups mobile report around key metrics', adminDashboard, 'data-report-group="core"');
assertContains('AdminDashboard groups mobile report around trend and runtime', adminDashboard, 'data-report-group="trend"');
assertContains('AdminDashboard groups mobile report around attribution and short links', adminDashboard, 'data-report-group="attribution"');
assertContains('AdminDashboard exposes mobile core report group test id', adminDashboard, 'data-testid="admin-mobile-report-group-core"');
assertContains('AdminDashboard exposes mobile trend report group test id', adminDashboard, 'data-testid="admin-mobile-report-group-trend"');
assertContains('AdminDashboard exposes mobile attribution report group test id', adminDashboard, 'data-testid="admin-mobile-report-group-attribution"');
assertContains('AdminDashboard prevents native details toggle for controlled mobile groups', adminDashboard, '@click.prevent="toggleMobileReportGroup');
assertContains('AdminDashboard labels mobile report key group', adminDashboard, '指标与链路');
assertContains('AdminDashboard labels mobile report attribution group', adminDashboard, '归因与短链');
assert(
  'AdminDashboard keeps daily trend table inside trend report group before attribution group',
  adminDashboard.indexOf('data-report-group="trend"') < adminDashboard.indexOf('<h2>日趋势</h2>')
    && adminDashboard.indexOf('<h2>日趋势</h2>') < adminDashboard.indexOf('data-report-group="attribution"'),
);
assertContains('AdminDashboard finds the actual vertical scroll host for evidence links', adminDashboard, 'function findVerticalScrollHost');
assertContains('AdminDashboard checks scrollable vertical host dimensions', adminDashboard, 'element.scrollHeight > element.clientHeight + 1');
assertContains('AdminDashboard falls back to explicit page scroll for evidence links', adminDashboard, 'window.scrollTo');
assertContains('AdminDashboard has dedicated evidence scroll helper', adminDashboard, 'function scrollEvidenceIntoView');
assertContains('AdminDashboard binds evidence links to locateEvidence', adminDashboard, '@click="locateEvidence($event, item.evidenceId)"');
assertContains('mobile e2e opens trend report group before expecting growth funnel', mobileE2e, "getByTestId('admin-mobile-report-group-trend').click()");
assertContains('mobile e2e opens attribution report group before expecting Top Channel', mobileE2e, "getByTestId('admin-mobile-report-group-attribution').click()");
assert(
  'AdminDashboard mobile filter controls keep readable typography',
  /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.filter-bar input:not\(\[type='checkbox'\]\),\s*\n\s*\.filter-bar select\s*\{[\s\S]*?font-size:\s*14px;[\s\S]*?font-weight:\s*850;/.test(adminDashboard),
);
assert(
  'AdminDashboard mobile synthetic toggle avoids tiny text',
  /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.filter-bar \.toggle-row\s*\{[\s\S]*?font-size:\s*13px;/.test(adminDashboard),
);
const adminShortLinkDetail = read('frontend/src/pages/AdminShortLinkDetail.vue');
assertContains('AdminDashboard keeps localized start date placeholder', adminDashboard, 'data-placeholder="选择开始日期"');
assertContains('AdminDashboard keeps localized end date placeholder', adminDashboard, 'data-placeholder="选择结束日期"');
assertContains('AdminDashboard keeps date format aria label', adminDashboard, 'aria-label="开始日期，格式 YYYY-MM-DD"');
assertContains('AdminDashboard keeps end date format aria label', adminDashboard, 'aria-label="结束日期，格式 YYYY-MM-DD"');
assertContains('AdminDashboard keeps localized keyword placeholder', adminDashboard, 'placeholder="输入短码或结果 ID"');
assert('AdminDashboard quick date buttons keep 44px target', /\.quick-range-bar button\s*\{[\s\S]*?min-height:\s*44px;/.test(adminDashboard));
assert('AdminDashboard mobile report toggle keeps 44px target', /\.mobile-report-gate button\s*\{[\s\S]*?min-height:\s*44px;/.test(adminDashboard));
assert('AdminDashboard mobile report group summary keeps 48px target', /\.mobile-report-group > summary\s*\{[\s\S]*?min-height:\s*48px;/.test(adminDashboard));
assert('AdminDashboard mobile text links keep 44px target', /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.text-link\s*\{[\s\S]*?min-height:\s*44px;/.test(adminDashboard));
assert('AdminDashboard hides wide short link table on mobile', /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.shortlink-table-wrap\s*\{[\s\S]*?display:\s*none;/.test(adminDashboard));
assert('AdminDashboard shows mobile short link cards on mobile', /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.shortlink-mobile-list\s*\{[\s\S]*?display:\s*grid;/.test(adminDashboard));
assert('AdminDashboard mobile pager uses two stable columns', /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.pager-footer > div\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);/.test(adminDashboard));
assertContains('AdminShortLinkDetail keeps localized start date placeholder', adminShortLinkDetail, 'data-placeholder="选择开始日期"');
assertContains('AdminShortLinkDetail keeps localized end date placeholder', adminShortLinkDetail, 'data-placeholder="选择结束日期"');
assertContains('AdminShortLinkDetail keeps date format aria label', adminShortLinkDetail, 'aria-label="开始日期，格式 YYYY-MM-DD"');
assertContains('AdminShortLinkDetail keeps end date format aria label', adminShortLinkDetail, 'aria-label="结束日期，格式 YYYY-MM-DD"');
assert('AdminShortLinkDetail compact return keeps 44px target', /\.compact-return\s*\{[\s\S]*?min-height:\s*44px;/.test(adminShortLinkDetail));
assert('AdminShortLinkDetail pager select keeps 44px target', /\.detail-pager select\s*\{[\s\S]*?min-height:\s*44px;/.test(adminShortLinkDetail));
assert('AdminShortLinkDetail scope chips can wrap long dynamic text', /\.scope-line span\s*\{[\s\S]*?max-width:\s*100%;[\s\S]*?overflow-wrap:\s*anywhere;[\s\S]*?white-space:\s*normal;/.test(adminShortLinkDetail));
assertContains('AdminShortLinkDetail marks mobile pager edge actions', adminShortLinkDetail, 'class="secondary edge-page-action"');
assert('AdminShortLinkDetail hides first/last pager buttons on mobile', /\.detail-pager\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);[\s\S]*?\.detail-pager\s+\.edge-page-action\s*\{[\s\S]*?display:\s*none;/.test(adminShortLinkDetail));
assertContains('global styles keep date shell class', globalStyle, '.date-input-shell.empty::after');
assertContains('global styles hide native empty date text', globalStyle, '.date-input-shell.empty input::-webkit-datetime-edit');
assertContains('global styles cap localized date placeholder width', globalStyle, 'max-width: calc(100% - 48px)');
assert('global styles keep inline debug summary touch target', /\.inline-debug summary\s*\{[\s\S]*?display:\s*inline-flex;[\s\S]*?min-height:\s*44px;/.test(globalStyle));
assertContains('ShortLinkVisit supports nullable external client hash', frontendTypes, 'clientIdHash: string | null');
assertContains('ShortLinkVisit supports nullable external ip hash', frontendTypes, 'ipHash: string | null');
assertContains('ShortLinkVisit supports nullable external user agent hash', frontendTypes, 'userAgentHash: string | null');
assertContains('AdminShortLinkDetail clears invalid admin token', adminShortLinkDetail, "localStorage.removeItem('wuxing_admin_token')");
assertContains('AdminShortLinkDetail has localized invalid token message', adminShortLinkDetail, '管理 token 无效，请返回后台重新登录。');
assertContains('AdminShortLinkDetail announces admin error status', adminShortLinkDetail, 'role="alert" aria-live="polite"');
assertContains('AdminShortLinkDetail announces empty state', adminShortLinkDetail, 'class="muted empty-state" role="status" aria-live="polite"');
assertContains('AdminShortLinkDetail renders mobile visit cards', adminShortLinkDetail, 'data-testid="shortlink-visit-card"');
assertContains('AdminShortLinkDetail formats visit time to seconds', adminShortLinkDetail, 'formatVisitTime');
assertContains('AdminShortLinkDetail maps external visit event type', adminShortLinkDetail, '外部短链访问');
assertContains('AdminShortLinkDetail renders empty hashes as dash', adminShortLinkDetail, "return '-'");
assertContains('AdminShortLinkDetail explains missing external attribution', adminShortLinkDetail, '外部平台未返回');
assertContains('mobile e2e verifies missing external attribution copy', mobileE2e, '外部平台未返回');
assertContains('AdminShortLinkDetail uses index-stable keys for nullable external records', adminShortLinkDetail, 'function visitRecordKey');
assertContains('AdminShortLinkDetail forwards statSource to visit detail API', adminShortLinkDetail, 'statSource,');
assertContains('AdminShortLinkDetail avoids mixing detail source with synthetic toggle', adminShortLinkDetail, '测试流量开关只影响 perf-test 排除');
assertContains('AdminShortLinkDetail clarifies visit detail source', adminShortLinkDetail, '明细来源');
assertContains('AdminShortLinkDetail wraps long mobile visit fields', adminShortLinkDetail, 'overflow-wrap: anywhere');
assert('AdminShortLinkDetail hides wide table on mobile', /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.table-wrap\s*\{[\s\S]*?display:\s*none;/.test(adminShortLinkDetail));
assert('AdminShortLinkDetail shows visit cards on mobile', /@media \(max-width:\s*760px\)\s*\{[\s\S]*?\.visit-card-list\s*\{[\s\S]*?display:\s*grid;/.test(adminShortLinkDetail));

assert('fresh MySQL schema no longer has duplicate visit_event ALTER statements', !/ALTER TABLE visit_event/.test(backendSchema));
assert('fresh MySQL schema no longer has duplicate legacy create-index statements', !/CREATE INDEX idx_visit_event|CREATE INDEX idx_short_link_status_created_at/.test(backendSchema));
assertContains('quality gate checks fresh schema duplicate DDL', qualityCheckScript, 'Verify fresh MySQL schema has no duplicate legacy DDL');
assertContains('quality gate runs fresh MySQL schema smoke test', qualityCheckScript, 'scripts/mysql-schema-smoke-test.sh');
assertContains('AdminStatService caps computed statSource scans', backendAdminStatService, 'SOURCE_FILTER_SCAN_LIMIT = 500');
assertContains('AdminStatService rejects over-limit computed statSource scans', backendAdminStatService, 'statSource filter scans at most');
assertContains('AdminStatService uses strict external stats for explicit source filters', backendAdminStatService, 'fetchStatsStrict');
assertContains('AdminStatService reads external detail records for explicit statSource', backendAdminStatService, '"external".equals(normalizedSource)');
assertContains('AdminStatService returns empty page when explicit external detail is unavailable', backendAdminStatService, 'return new PageVO<>(normalizedPage, normalizedPageSize, 0, java.util.List.of())');
assertContains('ExternalShortLinkStatsAdapter strict mode rejects disabled external stats', externalStatsAdapter, 'external short link stats unavailable: disabled');
assertContains('ExternalShortLinkStatsAdapter strict mode rejects domain mismatch', externalStatsAdapter, 'external short link stats unavailable: domain mismatch');
assertContains('ExternalShortLinkStatsAdapterTest covers disabled strict stats', externalStatsAdapterTest, 'shouldFailStrictExternalStatsWhenExternalStatsAreDisabled');
assertContains('ExternalShortLinkStatsAdapterTest covers strict domain mismatch', externalStatsAdapterTest, 'shouldFailStrictExternalStatsWhenDomainDoesNotMatch');
assertContains('ExternalShortLinkStatsAdapterTest covers nullable external fingerprints', externalStatsAdapterTest, 'shouldKeepMissingExternalAccessRecordFingerprintsNullable');
assertContains('ExternalShortLinkStatsAdapter exposes strict stats fetch', externalStatsAdapter, 'fetchStatsStrict');

const frontendUiFiles = listFiles('frontend/src', (filePath) => /\.(vue|ts)$/.test(filePath));
assertContains('ResultPage announces loading state', resultPage, 'class="result-state-card" role="status" aria-live="polite"');
assertContains('ResultPage announces share image status', resultPage, 'class="muted" role="status" aria-live="polite"');
assertContains('ResultPage announces error state', resultPage, 'class="result-state-card error-state" role="alert" aria-live="polite"');
assertContains('ResultPage treats external share channel as shared entry', resultPage, "route.query.channel === 'share'");
assertContains('ExternalShortLinkProvider creates share landing origin URL', externalProvider, '?channel=share&campaign=result-card');
assertContains('ExternalShortLinkProvider stores share landing original path', externalProvider, 'setOriginalPath("/result/" + resultId + "?channel=share&campaign=result-card")');
assertContains('external shortlink smoke verifies share channel redirect', externalSmokeScript, 'channel=share');
assertContains('external shortlink smoke verifies result-card campaign redirect', externalSmokeScript, 'campaign=result-card');
assertContains('external shortlink smoke verifies bare redirect short code attribution', externalSmokeScript, 'sc=${short_code}');
assertContains('external shortlink smoke verifies attributed redirect separately', externalSmokeScript, 'shared_redirect_code');
assertContains('external shortlink smoke reports attributed redirect', externalSmokeScript, 'sharedRedirect=${shared_location}');
assertContains('external shortlink smoke creates a synthetic perf-test visit', externalSmokeScript, 'channel=perf-test&campaign=external-shortlink-smoke');
assertContains('external shortlink smoke reports synthetic redirect', externalSmokeScript, 'syntheticRedirect=${synthetic_location}');
assertContains('external shortlink smoke verifies visit detail statSource', externalSmokeScript, 'statSource=${detail_source}');
assertContains('external shortlink smoke verifies includeSynthetic detail semantics', externalSmokeScript, 'includeSynthetic=true');
assertContains('external shortlink smoke asserts includeSynthetic includes perf-test locally', externalSmokeScript, 'includeSynthetic=true should include the synthetic perf-test visit');
assertContains('external shortlink smoke asserts visit page shape', externalSmokeScript, 'assert_visit_page');
assertContains('external shortlink smoke rejects wrong local detail source', externalSmokeScript, '"default visits" "local"');
assertContains('external shortlink smoke rejects wrong explicit detail source', externalSmokeScript, '"${detail_source} visits" "$detail_source"');
assertContains('external shortlink smoke allows includeSynthetic fallback sources', externalSmokeScript, '"includeSynthetic visits" "local,external"');
const notFoundPage = read('frontend/src/pages/NotFoundPage.vue');
assertContains('NotFoundPage has status region', notFoundPage, 'role="status" aria-labelledby="not-found-title"');
assertContains('NotFoundPage explains short link failure context', notFoundPage, '短链已失效');
assertContains('NotFoundPage has copy current address recovery action', notFoundPage, 'copyCurrentAddress');
assertContains('NotFoundPage offers retest action', notFoundPage, '重新测一张');
for (const filePath of frontendUiFiles) {
  const text = read(filePath);
  assert('frontend source avoids 10px font size', !/font-size:\s*10px\b/.test(text), filePath);
  for (const match of text.matchAll(/letter-spacing:\s*([^;]+);/g)) {
    assert('letter spacing stays zero', match[1].trim() === '0', `${filePath} has ${match[1].trim()}`);
  }
  assert('font size avoids viewport scaling', !/font-size:\s*(clamp\(|[^;]*(?:vw|vh))/.test(text), filePath);
}

if (failures.length > 0) {
  console.error('Frontend contract verification failed:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(JSON.stringify({
  ok: true,
  distFiles: distFiles.length,
  sourceFiles: sourceFiles.length,
  checked: [
    'api-routes',
    'match-entry',
    'share-entry',
    'api-base-url-contract',
    'api-url-runtime-shape',
    'backend-cors-preflight-contract',
    'api-request-dto-contract',
    'non-json-gateway-error-contract',
    'campaign-attribution-contract',
    'share-attribution-contract',
    'question-testid-contract',
    'stable-e2e-testid-contract',
    'question-option-state-contract',
    'test-flow-machine-runtime-contract',
    'test-flow-layout-contract',
    'test-page-state-a11y-contract',
    'guide-manual-short-code-contract',
    'guide-manual-short-code-race-e2e-contract',
    'match-page-a11y-density-contract',
    'not-found-state-contract',
    'admin-status-a11y-contract',
    'admin-date-placeholder-contract',
    'admin-touch-target-contract',
    'admin-chip-wrap-contract',
    'admin-mobile-filter-typography-contract',
    'admin-mobile-report-collapse-contract',
    'admin-mobile-report-evidence-contract',
    'admin-mobile-report-showcase-contract',
    'ci-browser-e2e-contract',
    'share-action-hierarchy-contract',
    'share-action-mobile-density-contract',
    'share-fallback-e2e-contract',
    'share-download-png-e2e-contract',
    'native-share-success-e2e-contract',
    'admin-invalid-token-e2e-contract',
    'admin-detail-invalid-token-e2e-contract',
    'mobile-main-flow-visual-contract',
    'mobile-home-showcase-layout-contract',
    'showcase-control-text-fit-contract',
    'desktop-result-showcase-contract',
    'desktop-shortlink-detail-showcase-contract',
    'match-showcase-contract',
    'showcase-artifact-contract',
    'all-showcase-png-geometry-contract',
    'external-share-landing-contract',
    'share-status-a11y-contract',
    'e2e-perf-test-seed',
    'text-only-element-marks',
    'typography-constraints',
  ],
}, null, 2));
