import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const htmlPath = path.join(root, 'outputs', 'wuxing-frontend-flow-preview.html');
const html = fs.readFileSync(htmlPath, 'utf8');
const shareCardPath = path.join(root, 'frontend', 'src', 'utils', 'shareCard.ts');
const shareCard = fs.readFileSync(shareCardPath, 'utf8');
const elementMarkPath = path.join(root, 'frontend', 'src', 'components', 'ElementMark.vue');
const elementMark = fs.readFileSync(elementMarkPath, 'utf8');
const visualRendererPath = path.join(root, 'outputs', 'render-wuxing-question-previews.py');
const visualRenderer = fs.existsSync(visualRendererPath) ? fs.readFileSync(visualRendererPath, 'utf8') : '';

const failures = [];

function check(name, condition) {
  if (!condition) {
    failures.push(name);
  }
}

function pngSize(filePath) {
  const bytes = fs.readFileSync(filePath);
  const signature = bytes.subarray(0, 8).toString('hex');
  if (signature !== '89504e470d0a1a0a') {
    throw new Error(`${filePath} is not a PNG`);
  }
  return {
    width: bytes.readUInt32BE(16),
    height: bytes.readUInt32BE(20),
  };
}

function checkPng(name, relativePath, expectedWidth, expectedHeight) {
  const filePath = path.join(root, relativePath);
  check(`${name} exists`, fs.existsSync(filePath));
  if (!fs.existsSync(filePath)) {
    return;
  }
  const size = pngSize(filePath);
  check(`${name} width`, size.width === expectedWidth);
  check(`${name} height`, size.height === expectedHeight);
}

const scriptText = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)]
  .map((match) => match[1])
  .join('\n');

check('script parses', (() => {
  try {
    new Function(scriptText);
    return true;
  } catch {
    return false;
  }
})());

check('uses fixed image-height positioning', !/min-height:\s*100vh/.test(html));
check('preview font sizes avoid viewport scaling', !/font-size:\s*(clamp\(|[^;]*(?:vw|vh))/.test(html));
check('preview avoids rough symbol decorations', !/[□⌯]/.test(html));
check(
  'preview letter spacing stays zero',
  [...html.matchAll(/letter-spacing:\s*([^;]+);/g)].every((match) => match[1].trim() === '0'),
);
check(
  'preview text indent stays zero',
  [...html.matchAll(/text-indent:\s*([^;]+);/g)].every((match) => match[1].trim() === '0'),
);
check('question card is fixed-height', /\.question-card\s*\{[\s\S]*?height:\s*51\.35%/.test(html));
check('question card is opaque paper', /linear-gradient\(180deg,\s*#fffdf8,\s*#f8f3ea\)/.test(html));
check('birth card uses opaque interactive panel', /\.birth-card\s*\{[\s\S]*?linear-gradient\(180deg,\s*#fffdf8,\s*#f8f3ea\)/.test(html));
check('option rows do not reveal static answer underneath', html.includes('background: var(--panel-solid)'));
check('ghost action button does not reveal static button underneath', html.includes('background: var(--panel-solid);'));
check('old duplicated question heading layer removed', !html.includes('class="question-heading"'));
check('initial answers empty', /answers:\s*\{\}/.test(html));
check('no default A-D answer', !/answers:\s*\{[^}]*[A-D]/.test(html));
check('unanswered next is blocked', html.includes('请先选择一个选项'));
check('last question status mentions result', html.includes('可查看结果'));
check('first previous button says basic info', html.includes("基础信息' : '上一题"));
check('initial previous button says basic info', /data-testid="question-prev">基础信息<\/button>/.test(html));
check('initial previous button avoids old wording', !/data-testid="question-prev">上一步<\/button>/.test(html));
check('question options avoid template arrows', !html.includes('option-arrow') && !html.includes('›'));
check('question next exposes disabled semantics', html.includes("aria-disabled"));
check('question progress reuses step track', !html.includes('question-progress-title') && html.includes('data-progress="question"'));
check('question stems are short', !html.includes('和别人相处时，你更常像哪一种？') && !html.includes('完成一个项目时，你更愿意负责？'));
check('copy fallback exists', html.includes('document.execCommand'));
check('copy fallback handles rejected clipboard permission', /catch\s*\{[\s\S]*?fallbackCopyText/.test(html));
check('copy fallback avoids false success toast', html.includes("showToast(copied ? '分享链接已复制' : '请手动复制链接')"));
check('save share image exists', html.includes('downloadShareImage'));
check('visual preview renderer exists', fs.existsSync(visualRendererPath));
check('visual preview renderer avoids template arrows', !visualRenderer.includes('"›"') && !visualRenderer.includes("'›'"));
check('flow verifier exists', fs.existsSync(path.join(root, 'scripts', 'verify-wuxing-preview-flow.mjs')));
const designPatchPath = path.join(root, 'outputs', 'patch-design-final-element-marks.py');
check('design element mark patcher exists', fs.existsSync(designPatchPath));
const designPatch = fs.existsSync(designPatchPath) ? fs.readFileSync(designPatchPath, 'utf8') : '';
check('design element mark patcher covers home image', designPatch.includes('patch_home_image'));
check('design element mark patcher covers result image', designPatch.includes('patch_result_image'));
check('design element mark patcher covers share image', designPatch.includes('patch_share_image'));

const glyphSource = shareCard.match(/function drawElementGlyph\([\s\S]*?\n}\n\nfunction drawFooter/)?.[0] || '';
check('share card glyph source found', glyphSource.length > 0);
check('share card element marks are not hand-drawn by element code', !/visual\.code\s*===/.test(glyphSource));
check('share card element marks have no hand-drawn curves', !/(bezierCurveTo|arc|moveTo|lineTo)\s*\(/.test(glyphSource));
check('share card element marks are text only', !/roundRect|stroke|fillRect/.test(glyphSource));
check('vue element mark uses text only', !/<svg|<path|::before|::after/.test(elementMark));

const questionCount = (html.match(/title: '/g) || []).length;
const optionCount = (html.match(/\{ code: '[A-D]', text:/g) || []).length;
const fullTextCount = (html.match(/fullText:/g) || []).length;
check('has five questions', questionCount === 5);
check('has twenty A-D options', optionCount === 20);
check('has full option meanings', fullTextCount === 20);

[
  'start-test',
  'view-sample',
  'year-minus',
  'year-plus',
  'year-input',
  'year-range',
  'month-grid',
  'optional-toggle',
  'optional-panel',
  'enter-question',
  'question-card',
  'option-list',
  'question-status',
  'question-prev',
  'question-next',
  'go-share',
  'result-element-detail',
  'result-overview',
  'copy-result-link',
  'restart-test',
  'save-share-image',
  'copy-share-link',
  'back-result-from-share',
].forEach((testId) => {
  check(`test id ${testId}`, html.includes(`data-testid="${testId}"`));
});

check('runtime option test id template', html.includes('data-testid="option-${option.code}"'));
check('runtime month test id template', html.includes('data-testid="month-${month}"'));
check('runtime renders 12 months', html.includes('Array.from({ length: 12 }'));

[
  '01-home.png',
  '02-birth.png',
  '03-question.png',
  '04-result.png',
  '05-share.png',
].forEach((fileName) => {
  checkPng(`design ${fileName}`, path.join('design-final', fileName), 853, 1844);
});

checkPng('visual q1 preview', path.join('outputs', 'visual-check-question-q1.png'), 853, 1844);
checkPng('visual q5 preview', path.join('outputs', 'visual-check-question-q5-selected.png'), 853, 1844);

if (failures.length > 0) {
  console.error('Preview verification failed:');
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log(JSON.stringify({
  ok: true,
  html: path.relative(root, htmlPath),
  questions: questionCount,
  options: optionCount,
  fullTexts: fullTextCount,
  designImages: 5,
  visualPreviews: 2,
}, null, 2));
