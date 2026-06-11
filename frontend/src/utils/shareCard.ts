import type { ResultDetail } from '../api/types';

const CARD_WIDTH = 900;
const CARD_HEIGHT = 1200;
const CONTENT_X = 88;
const CONTENT_WIDTH = CARD_WIDTH - CONTENT_X * 2;

type ElementPalette = {
  primary: string;
  secondary: string;
  soft: string;
  ink: string;
};

const elementPalettes: Record<string, ElementPalette> = {
  METAL: { primary: '#5c6670', secondary: '#dfe5e2', soft: '#f7f4e8', ink: '#24302f' },
  WOOD: { primary: '#5e8d63', secondary: '#dcebd9', soft: '#eef6e8', ink: '#1f3a2a' },
  WATER: { primary: '#486f92', secondary: '#dce8f1', soft: '#e9f2f8', ink: '#20364a' },
  FIRE: { primary: '#b66045', secondary: '#f5d9cc', soft: '#fff1e8', ink: '#4b2820' },
  EARTH: { primary: '#9d7a42', secondary: '#eadfc7', soft: '#f7efe1', ink: '#4a3920' },
};

const defaultPalette: ElementPalette = {
  primary: '#2f6f5e',
  secondary: '#dcebd9',
  soft: '#edf7f2',
  ink: '#24302f',
};

export function downloadResultShareCard(result: ResultDetail) {
  const canvas = document.createElement('canvas');
  canvas.width = CARD_WIDTH;
  canvas.height = CARD_HEIGHT;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('当前浏览器不支持生成分享图');
  }

  drawBackground(ctx);
  drawTitle(ctx, result);
  drawElementBlock(ctx, result);
  drawKeywords(ctx, result);
  drawTexts(ctx, result);
  drawFooter(ctx, result);

  const link = document.createElement('a');
  link.href = canvas.toDataURL('image/png');
  link.download = `wuxing-${result.resultId}.png`;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

function drawBackground(ctx: CanvasRenderingContext2D) {
  const gradient = ctx.createLinearGradient(0, 0, CARD_WIDTH, CARD_HEIGHT);
  gradient.addColorStop(0, '#f8f5eb');
  gradient.addColorStop(0.58, '#e6f2ee');
  gradient.addColorStop(1, '#f4eadc');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);

  ctx.fillStyle = 'rgba(215, 155, 67, 0.14)';
  circle(ctx, 112, 112, 76);
  ctx.fill();
  ctx.fillStyle = 'rgba(47, 111, 94, 0.12)';
  circle(ctx, 782, 1024, 118);
  ctx.fill();

  ctx.fillStyle = 'rgba(255, 255, 255, 0.82)';
  roundRect(ctx, 48, 48, CARD_WIDTH - 96, CARD_HEIGHT - 96, 28);
  ctx.fill();
}

function drawTitle(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  ctx.fillStyle = '#7b5d35';
  ctx.font = '800 26px sans-serif';
  ctx.fillText('五行人格卡 · 传统文化元素人格测试', CONTENT_X, 126);

  ctx.fillStyle = '#24302f';
  ctx.font = '900 60px sans-serif';
  drawSingleLineText(ctx, `${result.primaryElementName}${result.secondaryElementName}型${result.keywords[0] ?? '人格'}`, CONTENT_X, 210, CONTENT_WIDTH);

  ctx.fillStyle = '#596764';
  ctx.font = '700 28px sans-serif';
  ctx.fillText(result.starOfficerName, CONTENT_X, 258);
}

function drawElementBlock(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const palette = getPalette(result.primaryElement);
  const gradient = ctx.createLinearGradient(CONTENT_X, 300, CONTENT_X + CONTENT_WIDTH, 500);
  gradient.addColorStop(0, '#24302f');
  gradient.addColorStop(1, palette.primary);
  ctx.fillStyle = gradient;
  roundRect(ctx, CONTENT_X, 300, CONTENT_WIDTH, 220, 26);
  ctx.fill();

  ctx.fillStyle = '#ffffff';
  ctx.font = '900 96px sans-serif';
  ctx.fillText(result.primaryElementName, 138, 436);

  ctx.font = '800 34px sans-serif';
  ctx.fillText(`${result.primaryPercent}% ${result.primaryElementName}`, 318, 374);
  ctx.fillStyle = 'rgba(255, 255, 255, 0.78)';
  ctx.fillText(`${result.secondaryPercent}% ${result.secondaryElementName}`, 318, 430);

  ctx.fillStyle = 'rgba(255, 255, 255, 0.18)';
  roundRect(ctx, 138, 468, 624, 18, 9);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  roundRect(ctx, 138, 468, Math.max(24, Math.round(624 * result.primaryPercent / 100)), 18, 9);
  ctx.fill();

  ctx.fillStyle = palette.soft;
  roundRect(ctx, 600, 330, 150, 52, 26);
  ctx.fill();
  ctx.fillStyle = palette.ink;
  ctx.font = '800 22px sans-serif';
  ctx.fillText('主五行', 638, 364);
}

function drawKeywords(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  let x = CONTENT_X;
  let y = 590;
  const palette = getPalette(result.primaryElement);
  ctx.font = '800 26px sans-serif';
  for (const keyword of result.keywords) {
    const width = ctx.measureText(keyword).width + 42;
    if (x + width > CONTENT_X + CONTENT_WIDTH) {
      x = CONTENT_X;
      y += 58;
    }
    ctx.fillStyle = palette.soft;
    roundRect(ctx, x, y - 34, width, 44, 22);
    ctx.fill();
    ctx.fillStyle = palette.ink;
    ctx.fillText(keyword, x + 21, y - 4);
    x += width + 14;
  }
}

function drawTexts(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const startY = 704;
  ctx.fillStyle = '#24302f';
  ctx.font = '800 30px sans-serif';
  ctx.fillText('性格亮点', CONTENT_X, startY);
  drawWrappedText(ctx, result.strengthText, CONTENT_X, startY + 48, CONTENT_WIDTH, 34, 3);

  ctx.font = '800 30px sans-serif';
  ctx.fillText('相处优势', CONTENT_X, startY + 190);
  drawWrappedText(ctx, result.relationshipText, CONTENT_X, startY + 238, CONTENT_WIDTH, 34, 3);
}

function drawFooter(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const palette = getPalette(result.primaryElement);
  ctx.fillStyle = '#fff7e8';
  roundRect(ctx, CONTENT_X, 1036, CONTENT_WIDTH, 72, 20);
  ctx.fill();

  ctx.fillStyle = '#6d4f29';
  ctx.font = '800 23px sans-serif';
  ctx.fillText('保存分享图发朋友圈，复制短链发私聊', CONTENT_X + 26, 1081);
  ctx.fillStyle = palette.primary;
  ctx.font = '900 23px sans-serif';
  ctx.fillText('朋友打开也能测一张', 576, 1081);

  ctx.fillStyle = '#596764';
  ctx.font = '600 18px sans-serif';
  drawSingleLineText(ctx, result.shortUrl, CONTENT_X, 1144, CONTENT_WIDTH);
  ctx.font = '500 18px sans-serif';
  drawSingleLineText(ctx, '娱乐性人格解读，不构成现实决策建议。', CONTENT_X, 1174, CONTENT_WIDTH);
}

function drawWrappedText(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
  maxLines: number,
) {
  let line = '';
  let lineCount = 0;
  for (const char of text) {
    const nextLine = line + char;
    if (ctx.measureText(nextLine).width > maxWidth && line) {
      ctx.fillText(line, x, y + lineCount * lineHeight);
      line = char;
      lineCount += 1;
      if (lineCount >= maxLines) {
        return;
      }
    } else {
      line = nextLine;
    }
  }
  if (line && lineCount < maxLines) {
    ctx.fillText(line, x, y + lineCount * lineHeight);
  }
}

function drawSingleLineText(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
) {
  if (ctx.measureText(text).width <= maxWidth) {
    ctx.fillText(text, x, y);
    return;
  }

  let clipped = text;
  while (clipped.length > 1 && ctx.measureText(`${clipped}...`).width > maxWidth) {
    clipped = clipped.slice(0, -1);
  }
  ctx.fillText(`${clipped}...`, x, y);
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}

function circle(ctx: CanvasRenderingContext2D, x: number, y: number, radius: number) {
  ctx.beginPath();
  ctx.arc(x, y, radius, 0, Math.PI * 2);
  ctx.closePath();
}

function getPalette(elementCode: string) {
  return elementPalettes[elementCode] ?? defaultPalette;
}
