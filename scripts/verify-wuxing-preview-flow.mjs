import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const html = fs.readFileSync(path.join(root, 'outputs', 'wuxing-frontend-flow-preview.html'), 'utf8');
const scriptText = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)]
  .map((match) => match[1])
  .join('\n');

class ClassList {
  constructor(element) {
    this.element = element;
    this.values = new Set((element.className || '').split(/\s+/).filter(Boolean));
  }

  add(value) {
    this.values.add(value);
    this.sync();
  }

  remove(value) {
    this.values.delete(value);
    this.sync();
  }

  toggle(value, force) {
    const shouldAdd = force === undefined ? !this.values.has(value) : Boolean(force);
    if (shouldAdd) {
      this.values.add(value);
    } else {
      this.values.delete(value);
    }
    this.sync();
    return shouldAdd;
  }

  contains(value) {
    return this.values.has(value);
  }

  sync() {
    this.element.className = [...this.values].join(' ');
  }
}

class FakeElement {
  constructor({ tag = 'div', className = '', dataset = {}, textContent = '' } = {}) {
    this.tagName = tag.toUpperCase();
    this.className = className;
    this.classList = new ClassList(this);
    this.dataset = { ...dataset };
    this.attributes = {};
    this.children = [];
    this.listeners = {};
    this.textContent = textContent;
    this.value = '';
    this.style = {};
  }

  addEventListener(type, handler) {
    this.listeners[type] ||= [];
    this.listeners[type].push(handler);
  }

  click() {
    for (const handler of this.listeners.click || []) {
      handler({ currentTarget: this, target: this });
    }
  }

  dispatchInput(value) {
    this.value = value;
    for (const handler of this.listeners.input || []) {
      handler({ currentTarget: this, target: this });
    }
  }

  dispatchBlur() {
    for (const handler of this.listeners.blur || []) {
      handler({ currentTarget: this, target: this });
    }
  }

  setAttribute(name, value) {
    this.attributes[name] = String(value);
  }

  getAttribute(name) {
    return this.attributes[name] ?? null;
  }

  appendChild(child) {
    this.children.push(child);
    return child;
  }

  remove() {
    this.removed = true;
  }

  select() {
    this.selected = true;
  }

  get innerHTML() {
    return this._innerHTML || '';
  }

  set innerHTML(value) {
    this._innerHTML = value;
    this.children = [];
    if (value.includes('data-month=')) {
      const matches = [...value.matchAll(/data-month="(\d+)"/g)];
      this.children = matches.map((match) => new FakeElement({
        tag: 'button',
        className: 'month-button',
        dataset: { month: match[1], testid: `month-${match[1]}` },
        textContent: `${match[1]}月`,
      }));
    }
    if (value.includes('data-option=')) {
      const matches = [...value.matchAll(/class="([^"]*option-row[^"]*)"[\s\S]*?data-option="([A-D])"[\s\S]*?<span class="option-text">([^<]+)<\/span>/g)];
      this.children = matches.map((match) => new FakeElement({
        tag: 'button',
        className: match[1],
        dataset: { option: match[2], testid: `option-${match[2]}` },
        textContent: match[3],
      }));
    }
  }

  querySelector(selector) {
    return this.querySelectorAll(selector)[0] || null;
  }

  querySelectorAll(selector) {
    if (selector === '[data-option]') {
      return this.children.filter((child) => child.dataset.option);
    }
    if (selector === '[data-month]') {
      return this.children.filter((child) => child.dataset.month);
    }
    return [];
  }
}

function makeDocument() {
  const elements = [];
  const add = (key, element) => {
    element.key = key;
    elements.push(element);
    return element;
  };

  const stage = add('.stage', new FakeElement({ className: 'stage' }));
  const toast = add('.toast', new FakeElement({ className: 'toast' }));
  const pages = ['home', 'birth', 'question', 'result', 'share'].map((page, index) => add(`[data-page="${page}"]`, new FakeElement({
    className: `page${index === 0 ? ' active' : ''}`,
    dataset: { page },
  })));

  const goButtons = [
    ['birth', 'start-test'],
    ['result', 'view-sample'],
    ['question', 'enter-question'],
    ['home', 'back-home-from-birth'],
    ['share', 'go-share'],
    ['home', 'restart-test'],
    ['result', 'back-result-from-share'],
  ].map(([go, testid]) => add(`[data-testid="${testid}"]`, new FakeElement({ tag: 'button', dataset: { go, testid } })));

  const copyButtons = ['copy-result-link', 'copy-share-link'].map((testid) => add(`[data-testid="${testid}"]`, new FakeElement({
    tag: 'button',
    dataset: { action: 'copy', testid },
  })));
  const detailButton = add('[data-testid="result-element-detail"]', new FakeElement({
    tag: 'button',
    dataset: { action: 'detail', testid: 'result-element-detail' },
  }));
  const overviewButton = add('[data-testid="result-overview"]', new FakeElement({
    tag: 'button',
    dataset: { action: 'overview', testid: 'result-overview' },
  }));
  const saveButton = add('[data-testid="save-share-image"]', new FakeElement({
    tag: 'button',
    dataset: { action: 'save', testid: 'save-share-image' },
  }));

  const yearMinus = add('[data-testid="year-minus"]', new FakeElement({ tag: 'button', dataset: { yearStep: '-1', testid: 'year-minus' } }));
  const yearPlus = add('[data-testid="year-plus"]', new FakeElement({ tag: 'button', dataset: { yearStep: '1', testid: 'year-plus' } }));
  const yearInput = add('[data-testid="year-input"]', new FakeElement({ tag: 'input', dataset: { yearInput: '', testid: 'year-input' } }));
  const yearRange = add('[data-testid="year-range"]', new FakeElement({ tag: 'input', dataset: { yearRange: '', testid: 'year-range' } }));
  const yearText = add('[data-year-text]', new FakeElement({ tag: 'strong', textContent: '2002' }));
  const birthSummary = add('[data-birth-summary]', new FakeElement({ tag: 'span' }));
  const monthGrid = add('[data-testid="month-grid"]', new FakeElement({ dataset: { monthGrid: '', testid: 'month-grid' } }));
  const optionalToggle = add('[data-testid="optional-toggle"]', new FakeElement({ tag: 'button', dataset: { optionalToggle: '', testid: 'optional-toggle' } }));
  const optionalPanel = add('[data-testid="optional-panel"]', new FakeElement({ className: 'optional-panel', dataset: { optionalPanel: '', testid: 'optional-panel' } }));
  const optionalSummary = add('[data-optional-summary]', new FakeElement({ tag: 'span' }));
  const optionalDays = ['', '1', '15', '28'].map((day) => add(`[data-optional-day="${day}"]`, new FakeElement({
    tag: 'button',
    className: 'chip-button',
    dataset: { optionalDay: day },
    textContent: day ? `${day}日` : '不透露',
  })));
  const optionalTimes = ['', 'morning', 'afternoon', 'night'].map((time) => add(`[data-optional-time="${time}"]`, new FakeElement({
    tag: 'button',
    className: 'chip-button',
    dataset: { optionalTime: time },
    textContent: time || '不透露',
  })));

  const questionCard = add('[data-testid="question-card"]', new FakeElement({ className: 'dynamic-panel question-card', dataset: { testid: 'question-card' } }));
  const questionNumber = add('[data-question-number]', new FakeElement({ tag: 'strong' }));
  const questionTitle = add('[data-question-title]', new FakeElement({ tag: 'h1' }));
  const optionList = add('[data-testid="option-list"]', new FakeElement({ dataset: { optionList: '', testid: 'option-list' } }));
  const questionStatus = add('[data-question-status]', new FakeElement({ tag: 'span' }));
  const questionPrev = add('[data-testid="question-prev"]', new FakeElement({ tag: 'button', dataset: { questionPrev: '', testid: 'question-prev' }, textContent: '基础信息' }));
  const questionNext = add('[data-testid="question-next"]', new FakeElement({ tag: 'button', dataset: { questionNext: '', testid: 'question-next' }, textContent: '下一题' }));

  const body = new FakeElement({ tag: 'body' });
  const anchors = [];

  const document = {
    activeElement: null,
    body,
    execCommand(command) {
      return command === 'copy';
    },
    createElement(tag) {
      const element = new FakeElement({ tag });
      if (tag === 'a') {
        anchors.push(element);
      }
      return element;
    },
    querySelector(selector) {
      if (selector === '.stage') return stage;
      if (selector === '.toast') return toast;
      if (selector === '[data-year-text]') return yearText;
      if (selector === '[data-year-input]') return yearInput;
      if (selector === '[data-year-range]') return yearRange;
      if (selector === '[data-birth-summary]') return birthSummary;
      if (selector === '[data-month-grid]') return monthGrid;
      if (selector === '[data-optional-toggle]') return optionalToggle;
      if (selector === '[data-optional-panel]') return optionalPanel;
      if (selector === '[data-optional-summary]') return optionalSummary;
      if (selector === '[data-question-number]') return questionNumber;
      if (selector === '[data-question-title]') return questionTitle;
      if (selector === '[data-option-list]') return optionList;
      if (selector === '[data-question-status]') return questionStatus;
      if (selector === '[data-question-prev]') return questionPrev;
      if (selector === '[data-question-next]') return questionNext;
      if (selector === '[data-action="save"]') return saveButton;
      if (selector === '[data-action="detail"]') return detailButton;
      if (selector === '[data-action="overview"]') return overviewButton;
      const testId = selector.match(/^\[data-testid="([^"]+)"\]$/)?.[1];
      if (testId) return elements.find((element) => element.dataset.testid === testId) || null;
      const progress = selector.match(/^\[data-progress="([^"]+)"\]$/)?.[1];
      if (progress) {
        return elements.find((element) => element.dataset.progress === progress) || null;
      }
      return null;
    },
    querySelectorAll(selector) {
      if (selector === '.page') return pages;
      if (selector === '[data-go]') return goButtons;
      if (selector === '[data-year-step]') return [yearMinus, yearPlus];
      if (selector === '[data-month]') return monthGrid.children.filter((child) => child.dataset.month);
      if (selector === '[data-optional-day]') return optionalDays;
      if (selector === '[data-optional-time]') return optionalTimes;
      if (selector === '[data-action="copy"]') return copyButtons;
      if (selector === '[data-option]') return optionList.children.filter((child) => child.dataset.option);
      return [];
    },
    _elements: elements,
    _anchors: anchors,
  };

  add('[data-progress="birth"]', new FakeElement({ dataset: { progress: 'birth' } }));
  add('[data-progress="question"]', new FakeElement({ dataset: { progress: 'question' } }));

  return document;
}

const document = makeDocument();
const window = {
  scrollTo() {},
  clearTimeout,
  setTimeout(callback) {
    callback();
    return 0;
  },
};
const navigator = {
  clipboard: {
    text: '',
    async writeText(text) {
      this.text = text;
    },
  },
};
const location = { search: '' };

function installGlobal(name, value) {
  Object.defineProperty(globalThis, name, {
    value,
    configurable: true,
    writable: true,
  });
}

installGlobal('document', document);
installGlobal('window', window);
installGlobal('navigator', navigator);
installGlobal('location', location);
installGlobal('clearTimeout', clearTimeout);
installGlobal('setTimeout', window.setTimeout);

new Function(scriptText)();

function byTestId(testId) {
  const element = document.querySelector(`[data-testid="${testId}"]`);
  if (!element) {
    throw new Error(`Missing ${testId}`);
  }
  return element;
}

function activePage() {
  return document.querySelectorAll('.page').find((page) => page.classList.contains('active'))?.dataset.page;
}

function assert(name, condition) {
  if (!condition) {
    throw new Error(name);
  }
}

assert('starts on home', activePage() === 'home');
byTestId('start-test').click();
assert('goes to birth', activePage() === 'birth');
byTestId('year-plus').click();
assert('year plus updates', document.querySelector('[data-year-text]').textContent === 2003 || document.querySelector('[data-year-text]').textContent === '2003');
byTestId('year-input').dispatchInput('1998');
byTestId('year-input').dispatchBlur();
assert('year input updates', document.querySelector('[data-year-text]').textContent === 1998 || document.querySelector('[data-year-text]').textContent === '1998');
byTestId('year-range').dispatchInput('2005');
assert('year range updates', document.querySelector('[data-year-text]').textContent === 2005 || document.querySelector('[data-year-text]').textContent === '2005');
document.querySelector('[data-month-grid]').querySelector('[data-month]').click();
assert('month click updates summary', document.querySelector('[data-birth-summary]').textContent.includes('1 月'));
byTestId('optional-toggle').click();
assert('optional panel opens', byTestId('optional-panel').classList.contains('open'));
assert('optional toggle marks expanded', byTestId('optional-toggle').getAttribute('aria-expanded') === 'true');
document.querySelectorAll('[data-optional-day]').find((button) => button.dataset.optionalDay === '15').click();
document.querySelectorAll('[data-optional-time]').find((button) => button.dataset.optionalTime === 'afternoon').click();
assert('optional summary updates', document.querySelector('[data-optional-summary]').textContent.includes('15日') && document.querySelector('[data-optional-summary]').textContent.includes('下午'));
assert('optional day aria selected', document.querySelectorAll('[data-optional-day]').find((button) => button.dataset.optionalDay === '15').getAttribute('aria-pressed') === 'true');
byTestId('optional-toggle').click();
assert('optional panel closes', !byTestId('optional-panel').classList.contains('open'));
byTestId('enter-question').click();
assert('goes to question', activePage() === 'question');
assert('question starts at one', document.querySelector('[data-question-number]').textContent === 1 || document.querySelector('[data-question-number]').textContent === '1');
assert('first prev label is basic info', byTestId('question-prev').textContent === '基础信息');
assert(
  'question starts without a preselected option',
  document.querySelector('[data-option-list]').querySelectorAll('[data-option]').every((option) => !option.classList.contains('selected')),
);
assert('unanswered next is marked not ready', byTestId('question-next').dataset.ready === 'false');
assert('unanswered next has aria disabled', byTestId('question-next').getAttribute('aria-disabled') === 'true');
byTestId('question-next').click();
assert('unanswered next stays on q1', document.querySelector('[data-question-number]').textContent === 1 || document.querySelector('[data-question-number]').textContent === '1');

const answerPlan = ['B', 'C', 'A', 'D', 'D'];
for (let index = 0; index < answerPlan.length; index += 1) {
  const code = answerPlan[index];
  document.querySelector('[data-option-list]').querySelectorAll('[data-option]').find((option) => option.dataset.option === code).click();
  assert(`q${index + 1} selected`, document.querySelector('[data-question-status]').textContent.includes('已选'));
  assert(`q${index + 1} next is marked ready`, byTestId('question-next').dataset.ready === 'true');
  assert(
    `q${index + 1} only selected ${code}`,
    document.querySelector('[data-option-list]').querySelectorAll('[data-option]')
      .filter((option) => option.classList.contains('selected'))
      .map((option) => option.dataset.option)
      .join(',') === code,
  );
  byTestId('question-next').click();
  if (index === 0) {
    assert('moves to q2 before prev check', String(document.querySelector('[data-question-number]').textContent) === '2');
    byTestId('question-prev').click();
    assert('previous returns to q1', String(document.querySelector('[data-question-number]').textContent) === '1');
    assert(
      'q1 answer is preserved after previous',
      document.querySelector('[data-option-list]').querySelectorAll('[data-option]')
        .filter((option) => option.classList.contains('selected'))
        .map((option) => option.dataset.option)
        .join(',') === code,
    );
    byTestId('question-next').click();
  }
  if (index < answerPlan.length - 1) {
    assert(`moves to q${index + 2}`, String(document.querySelector('[data-question-number]').textContent) === String(index + 2));
  }
}
assert('goes to result after q5', activePage() === 'result');
byTestId('copy-result-link').click();
assert('result copy writes clipboard', navigator.clipboard.text.includes('wuxing.example'));
byTestId('result-element-detail').click();
assert('detail action gives feedback', document.querySelector('.toast').textContent.includes('逐项解读'));
byTestId('result-overview').click();
assert('overview action gives feedback', document.querySelector('.toast').textContent.includes('互动总览'));
byTestId('go-share').click();
assert('goes to share', activePage() === 'share');
byTestId('copy-share-link').click();
assert('copy writes clipboard', navigator.clipboard.text.includes('wuxing.example'));
byTestId('save-share-image').click();
assert('save creates download anchor', document._anchors.some((anchor) => anchor.download === 'wuxing-persona-share.png'));

console.log(JSON.stringify({
  ok: true,
  start: 'home',
  end: activePage(),
  answered: answerPlan.length,
  copied: navigator.clipboard.text,
}, null, 2));
