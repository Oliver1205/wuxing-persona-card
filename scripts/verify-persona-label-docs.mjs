#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const docsDir = path.join(root, 'docs');
const targetFiles = fs.readdirSync(docsDir)
  .filter((file) => /^persona-label-review.*\.md$/.test(file))
  .sort();

if (targetFiles.length === 0) {
  console.error('No persona label review documents found.');
  process.exit(1);
}

const forbiddenTerms = [
  'personaTypeId',
  'dominant',
  'balanced',
  'RelationKind',
  'primary',
  'secondary',
  'accent',
  '命中类型',
  '后台字段',
  '/5',
  '2/5',
  'WATER',
  'WOOD',
  'FIRE',
  'METAL',
  'EARTH',
];

const failures = [];

function fail(file, message) {
  failures.push(`${file}: ${message}`);
}

function cellValues(line) {
  return line.split('|')
    .map((cell) => cell.trim())
    .filter(Boolean);
}

for (const file of targetFiles) {
  const text = fs.readFileSync(path.join(docsDir, file), 'utf8');
  const rows = text.split('\n').filter((line) => /^\| \d{3} /.test(line));
  if (rows.length !== 120) {
    fail(file, `expected 120 rows, found ${rows.length}`);
    continue;
  }

  rows.forEach((line, index) => {
    const expectedNumber = String(index + 1).padStart(3, '0');
    const cells = cellValues(line);
    if (cells[0] !== expectedNumber) {
      fail(file, `row ${expectedNumber} has non-continuous number: ${cells[0] ?? '(empty)'}`);
    }
  });

  const labels = rows.map((line) => {
    const cells = cellValues(line);
    return cells[cells.length - 1] ?? '';
  });
  const duplicates = labels.filter((label, index) => labels.indexOf(label) !== index);
  if (duplicates.length > 0) {
    fail(file, `duplicate labels: ${[...new Set(duplicates)].join(', ')}`);
  }

  labels.forEach((label, index) => {
    const codePoints = [...label];
    const deCount = codePoints.filter((char) => char === '的').length;
    if (codePoints.length !== 4 || deCount !== 1 || !/^\p{Script=Han}+的\p{Script=Han}+$/u.test(label)) {
      fail(file, `row ${String(index + 1).padStart(3, '0')} invalid label shape: ${label}`);
    }
  });

  forbiddenTerms.forEach((term) => {
    if (text.includes(term)) {
      fail(file, `forbidden backend-facing term found: ${term}`);
    }
  });
}

if (failures.length > 0) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log(`Verified ${targetFiles.length} persona label review document(s).`);
