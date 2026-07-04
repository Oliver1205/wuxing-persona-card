#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const catalogPath = path.resolve(root, process.argv[2] || 'docs/persona-star-tone-catalog-20260703.md');
const reportPath = path.resolve(root, process.argv[3] || 'docs/persona-star-tone-production-audit-20260703.md');

const expectedCount = 120;
const trueZiweiTransforms = ['化禄', '化权', '化科', '化忌'];
const forbiddenNameChars = ['灾', '厄', '病', '死', '孤', '败', '凶', '煞', '忌', '刑', '亡'];
const staleLabels = ['水岸的灯', '水有岸，灯才会亮', '酷的沙砾', '静的沙砾'];
const publicForbiddenTerms = [
  'personaTypeId',
  'internalRationale',
  '审阅索引',
  '内部索引',
  '内部说明',
  '后台字段',
  '内部说明',
  'debug',
  'registry',
  'match route',
  'shortUrl 字段名',
  'WATER',
  'WOOD',
  'FIRE',
  'METAL',
  'EARTH',
  'dominant',
  'balanced',
  'score',
  'ratio',
  '紫微命盘',
  '紫微主星',
  '命宫主星',
  '真实紫微',
  '你的紫微斗数结果',
  '巨门化忌',
  '太阳化权',
  '天机化禄',
  '文昌化科',
  '/5',
  '2/5',
];
const suspiciousDoubleChars = ['被被', '让让', '会会', '先先', '再再', '它它', '你你', '的的', '和和', '地地', '得得'];
const repeatedTemplatePhrases = [
  '它不会把你改成另一种人',
  '而是让第一反应更有出口',
  '不是单线条',
  '先有第一反应，再有第二层校准',
  '多出一处能被记住的光',
  '不是随便出手，而是会用自己的节奏把事情推到更合适的位置',
  '真正需要的不是压掉这种第一反应，而是给它一个更清楚的出口',
];

const suffixMeaningRegistry = {
  含澜: { family: '水象' },
  入渊: { family: '水象' },
  藏思: { family: '水象' },
  流照: { family: '水象' },
  含光: { family: '火象' },
  藏明: { family: '火象' },
  化照: { family: '火象' },
  启明: { family: '火象' },
  开阳: { family: '火象' },
  成垣: { family: '土象' },
  定衡: { family: '土象' },
  化承: { family: '土象' },
  守中: { family: '土象' },
  化衡: { family: '土象' },
  照鉴: { family: '金象' },
  成律: { family: '金象' },
  凝锋: { family: '金象' },
  定章: { family: '金象' },
  开机: { family: '木象' },
  生章: { family: '木象' },
  生枝: { family: '木象' },
  舒荣: { family: '木象' },
};

const failures = [];

function fail(message) {
  failures.push(message);
}

function read(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function countOccurrences(text, phrase) {
  if (!phrase) {
    return 0;
  }
  return text.split(phrase).length - 1;
}

function lineValue(section, label) {
  const match = section.match(new RegExp(`^- ${label}：(.+)$`, 'm'));
  return match?.[1]?.trim() || '';
}

function blockValue(section, title) {
  const marker = `**${title}**`;
  const start = section.indexOf(marker);
  if (start < 0) {
    return '';
  }
  const rest = section.slice(start + marker.length).trim();
  const next = rest.search(/\n\*\*[^*]+?\*\*/);
  const value = next >= 0 ? rest.slice(0, next) : rest;
  return value
    .replace(/\*\*成长建议：\*\*[\s\S]*$/m, '')
    .trim();
}

function growthAdvice(section) {
  const start = section.indexOf('**成长建议：**');
  if (start < 0) {
    return [];
  }
  return section.slice(start)
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => /^- \*\*.+?\*\*：/.test(line))
    .map((line) => {
      const match = line.match(/^- \*\*(.+?)\*\*：(.+)$/);
      return { title: match?.[1] || '', text: match?.[2] || '' };
    });
}

function splitSentences(text) {
  return (text.match(/[^。！？；]+[。！？；]?/g) || [])
    .map((sentence) => sentence.trim())
    .filter((sentence) => sentence.length > 8);
}

function parseEntries(text) {
  return text.split(/^### /m).slice(1).map((raw) => {
    const [heading, ...bodyLines] = raw.split('\n');
    const body = bodyLines.join('\n');
    const headingMatch = heading.match(/^(.+?)（(.+)）$/);
    const detail = headingMatch?.[2] || '';
    const detailParts = detail.split('/').map((part) => part.trim());
    const name = headingMatch?.[1]?.trim() || heading.trim();
    const personaTypeId = (body.match(/- 内部索引：`([^`]+)`/) || [])[1] || '';
    const structureTitle = lineValue(body, '核心标题');
    const heroSummary = lineValue(body, '顶部短句');
    const explanation = lineValue(body, '取象解释');
    const keywords = lineValue(body, '关键词');
    const dayMaster = blockValue(body, '日主框架');
    const relation = blockValue(body, '主从关系');
    const accent = blockValue(body, '点睛元素');
    const heaven = blockValue(body, '天 · 内心世界');
    const human = blockValue(body, '人 · 外部感受');
    const advice = growthAdvice(body);
    return {
      name,
      body,
      personaTypeId,
      primary: detailParts[0] || '',
      secondary: detailParts[1] || '',
      accentElement: (detailParts[2] || '').replace(/^点睛/, ''),
      mode: detailParts[3] || '',
      structureTitle,
      heroSummary,
      explanation,
      keywords,
      dayMaster,
      relation,
      accent,
      heaven,
      human,
      advice,
    };
  });
}

function publicText(entry) {
  return [
    entry.structureTitle,
    entry.heroSummary,
    entry.explanation,
    entry.keywords,
    entry.dayMaster,
    entry.relation,
    entry.accent,
    entry.heaven,
    entry.human,
    ...entry.advice.flatMap((advice) => [advice.title, advice.text]),
  ].join('\n');
}

function validateEntry(entry, namesSeen) {
  const issues = [];
  const nameChars = [...entry.name];
  if (nameChars.length !== 4 || !/^\p{Script=Han}{4}$/u.test(entry.name)) {
    issues.push('星曜取象名不是四个汉字');
  }
  if (entry.name.includes('的')) {
    issues.push('星曜取象名包含“的”');
  }
  if (namesSeen.has(entry.name)) {
    issues.push('星曜取象名重复');
  }
  namesSeen.add(entry.name);
  for (const char of forbiddenNameChars) {
    if (entry.name.includes(char)) {
      issues.push(`星曜取象名含禁用字：${char}`);
    }
  }
  for (const phrase of trueZiweiTransforms) {
    if (entry.name.includes(phrase)) {
      issues.push(`星曜取象名疑似真实四化：${phrase}`);
    }
  }
  const suffix = nameChars.slice(2).join('');
  const suffixMeaning = suffixMeaningRegistry[suffix];
  if (!suffixMeaning) {
    issues.push(`后缀未进入 suffixMeaningRegistry：${suffix}`);
  } else if (!entry.explanation.includes(suffixMeaning.family)) {
    issues.push(`后缀解释未标明${suffixMeaning.family}`);
  }
  if (!entry.structureTitle) {
    issues.push('structureTitle 为空');
  }
  if (!entry.heroSummary) {
    issues.push('heroSummary 为空');
  }
  if (!entry.explanation) {
    issues.push('starToneExplanation 为空');
  }
  if (entry.advice.length < 3) {
    issues.push('成长建议少于 3 条');
  }
  const visible = publicText(entry);
  [...publicForbiddenTerms, ...staleLabels].forEach((term) => {
    if (visible.includes(term)) {
      issues.push(`用户文案包含禁用词：${term}`);
    }
  });
  suspiciousDoubleChars.forEach((term) => {
    if (visible.includes(term)) {
      issues.push(`疑似重复字：${term}`);
    }
  });
  repeatedTemplatePhrases.forEach((phrase) => {
    if (countOccurrences(visible, phrase) > 1) {
      issues.push(`同条结果重复模板句：${phrase}`);
    }
  });
  ['定衡取木', '成垣取火', '成律取水', '被被目标点亮'].forEach((term) => {
    if (visible.includes(term)) {
      issues.push(`已知错误仍存在：${term}`);
    }
  });
  [
    entry.dayMaster,
    entry.relation,
    entry.accent,
    entry.heaven,
    entry.human,
    ...entry.advice.map((advice) => advice.text),
  ].forEach((block) => {
    const seen = new Set();
    splitSentences(block).forEach((sentence) => {
      if (seen.has(sentence)) {
        issues.push(`同段重复句：${sentence}`);
      }
      seen.add(sentence);
    });
  });
  if (!entry.relation.includes(entry.primary) || !entry.relation.includes(entry.secondary)) {
    issues.push('主从关系没有同时提到主元素和副元素');
  }
  if (!entry.accent.includes(entry.accentElement)) {
    issues.push('点睛元素解释没有提到点睛元素');
  }
  return issues;
}

function chooseSamples(entries) {
  const samples = [];
  const primaryCounts = new Map();
  const coveredModes = new Set();
  const coveredAccents = new Set();
  for (const entry of entries) {
    const primaryCount = primaryCounts.get(entry.primary) || 0;
    const helpsPrimary = primaryCount < 3;
    const helpsMode = !coveredModes.has(entry.mode);
    const helpsAccent = !coveredAccents.has(entry.accentElement);
    if (samples.length < 15 && (helpsPrimary || helpsMode || helpsAccent)) {
      samples.push(entry);
      primaryCounts.set(entry.primary, primaryCount + 1);
      coveredModes.add(entry.mode);
      coveredAccents.add(entry.accentElement);
    }
  }
  for (const entry of entries) {
    if (samples.length >= 15) {
      break;
    }
    if (!samples.includes(entry)) {
      samples.push(entry);
    }
  }
  return samples;
}

function buildReport(entries, entryIssues) {
  const passed = entries.filter((entry) => entryIssues.get(entry).length === 0).length;
  const samples = chooseSamples(entries);
  const now = new Date().toISOString();
  const lines = [];
  lines.push('# 五行人格卡星曜取象上线前内容审阅报告');
  lines.push('');
  lines.push(`生成时间：${now}`);
  lines.push('');
  lines.push('## 总览');
  lines.push('');
  lines.push(`- 目录文件：${path.relative(root, catalogPath)}`);
  lines.push(`- 星曜取象总数：${entries.length}`);
  lines.push(`- 自动校验通过：${passed}`);
  lines.push(`- 自动校验需复审：${entries.length - passed}`);
  lines.push('');
  lines.push('## 当前指定样例');
  lines.push('');
  const target = entries.find((entry) => entry.personaTypeId === 'WATER-EARTH-FIRE-dominant');
  if (target) {
    lines.push(`- personaTypeId：${target.personaTypeId}`);
    lines.push(`- 星曜取象：${target.name}`);
    lines.push(`- 核心标题：${target.structureTitle}`);
    lines.push(`- 顶部短句：${target.heroSummary}`);
    lines.push(`- 关键词：${target.keywords}`);
  }
  lines.push('');
  lines.push('## 15 条抽样审阅');
  lines.push('');
  lines.push('| starToneName | 主元素 | 副元素 | 点睛 | mode | structureTitle | heroSummary | 主从关系摘要 | 点睛摘要 | 成长建议标题 | 模板味 | 是否可上线 |');
  lines.push('| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |');
  samples.forEach((entry) => {
    const issues = entryIssues.get(entry);
    lines.push(`| ${entry.name} | ${entry.primary} | ${entry.secondary} | ${entry.accentElement} | ${entry.mode} | ${entry.structureTitle} | ${entry.heroSummary} | ${compact(entry.relation)} | ${compact(entry.accent)} | ${entry.advice.map((advice) => advice.title).join('、')} | ${issues.length ? '需复审' : '低'} | ${issues.length ? '否' : '是'} |`);
  });
  lines.push('');
  lines.push('## 全量 120 类命名质量复检');
  lines.push('');
  lines.push('| personaTypeId | primaryElement | secondaryElement | accentElement | mode | starToneName | structureTitle | heroSummary | 命名是否合格 | 是否需要人工复审 | 问题说明 |');
  lines.push('| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |');
  entries.forEach((entry) => {
    const issues = entryIssues.get(entry);
    lines.push(`| ${entry.personaTypeId} | ${entry.primary} | ${entry.secondary} | ${entry.accentElement} | ${entry.mode} | ${entry.name} | ${entry.structureTitle} | ${entry.heroSummary} | ${issues.length ? '否' : '是'} | ${issues.length ? '是' : '否'} | ${issues.join('；') || '自动校验通过'} |`);
  });
  lines.push('');
  return lines.join('\n');
}

function compact(text) {
  return text.replace(/\s+/g, ' ').slice(0, 76);
}

if (!fs.existsSync(catalogPath)) {
  console.error(`Catalog not found: ${catalogPath}`);
  process.exit(1);
}

const catalog = read(catalogPath);
const entries = parseEntries(catalog);
if (entries.length !== expectedCount) {
  fail(`expected ${expectedCount} entries, found ${entries.length}`);
}
const namesSeen = new Set();
const entryIssues = new Map();
entries.forEach((entry) => {
  const issues = validateEntry(entry, namesSeen);
  entryIssues.set(entry, issues);
  issues.forEach((issue) => fail(`${entry.personaTypeId || entry.name}: ${issue}`));
});

fs.mkdirSync(path.dirname(reportPath), { recursive: true });
fs.writeFileSync(reportPath, buildReport(entries, entryIssues), 'utf8');

if (failures.length > 0) {
  console.error(failures.join('\n'));
  console.error(`Report written: ${path.relative(root, reportPath)}`);
  process.exit(1);
}

console.log(`Validated ${entries.length} star tone result entries.`);
console.log(`Report written: ${path.relative(root, reportPath)}`);
