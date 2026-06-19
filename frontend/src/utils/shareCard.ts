import type { ResultDetail } from '../api/types';
import { elementVisualByCode } from './elementVisuals';

const CARD_WIDTH = 900;
const CARD_HEIGHT = 1200;
const CONTENT_X = 88;
const CONTENT_WIDTH = CARD_WIDTH - CONTENT_X * 2;
const POSTER_RIGHT_X = 604;

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
  drawElementSpectrum(ctx, result);
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
  gradient.addColorStop(0, '#f8f3e9');
  gradient.addColorStop(0.62, '#fbf7ee');
  gradient.addColorStop(1, '#edf3ee');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, CARD_WIDTH, CARD_HEIGHT);

  ctx.fillStyle = 'rgba(177, 211, 209, 0.6)';
  ctx.beginPath();
  ctx.moveTo(0, 1048);
  ctx.bezierCurveTo(156, 946, 272, 1098, 412, 1014);
  ctx.bezierCurveTo(564, 922, 700, 986, 900, 880);
  ctx.lineTo(900, 1200);
  ctx.lineTo(0, 1200);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = 'rgba(151, 204, 205, 0.42)';
  ctx.beginPath();
  ctx.moveTo(0, 1096);
  ctx.bezierCurveTo(170, 1014, 264, 1160, 442, 1078);
  ctx.bezierCurveTo(612, 1000, 710, 1042, 900, 960);
  ctx.lineTo(900, 1200);
  ctx.lineTo(0, 1200);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
  roundRect(ctx, 48, 48, CARD_WIDTH - 96, CARD_HEIGHT - 96, 22);
  ctx.fill();
}

function drawTitle(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  ctx.fillStyle = '#bf8918';
  ctx.font = '800 24px sans-serif';
  ctx.fillText('五行人格卡', CONTENT_X, 124);

  ctx.fillStyle = '#202725';
  ctx.font = '650 60px serif';
  drawSingleLineText(ctx, `${result.primaryElementName}${result.secondaryElementName} · ${result.starOfficerName}`, CONTENT_X, 210, CONTENT_WIDTH);

  ctx.fillStyle = '#596764';
  ctx.font = '700 27px sans-serif';
  drawSingleLineText(ctx, result.keywords.slice(0, 4).join(' · '), CONTENT_X, 262, CONTENT_WIDTH);
}

function drawElementBlock(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const palette = getPalette(result.primaryElement);
  ctx.fillStyle = 'rgba(255, 255, 255, 0.88)';
  roundRect(ctx, CONTENT_X, 318, CONTENT_WIDTH, 246, 22);
  ctx.fill();
  ctx.strokeStyle = 'rgba(36, 48, 47, 0.1)';
  ctx.lineWidth = 2;
  roundRect(ctx, CONTENT_X, 318, CONTENT_WIDTH, 246, 22);
  ctx.stroke();

  drawElementGlyph(ctx, result.primaryElement, result.primaryElementName, CONTENT_X + 64, 358, 104);
  drawElementGlyph(ctx, result.secondaryElement, result.secondaryElementName, CONTENT_X + 202, 378, 82);

  const primaryVisual = elementVisualByCode(result.primaryElement, result.primaryElementName);
  const secondaryVisual = elementVisualByCode(result.secondaryElement, result.secondaryElementName);
  ctx.fillStyle = '#24302f';
  ctx.font = '800 28px sans-serif';
  ctx.fillText(`主${result.primaryElementName}`, CONTENT_X + 332, 386);
  ctx.fillStyle = '#596764';
  ctx.font = '700 22px sans-serif';
  ctx.fillText(primaryVisual.keywords.join(' / '), CONTENT_X + 332, 424);
  ctx.fillStyle = '#24302f';
  ctx.font = '800 28px sans-serif';
  ctx.fillText(`辅${result.secondaryElementName}`, CONTENT_X + 332, 474);
  ctx.fillStyle = '#596764';
  ctx.font = '700 22px sans-serif';
  ctx.fillText(secondaryVisual.keywords.join(' / '), CONTENT_X + 332, 512);

  ctx.fillStyle = '#fff8e8';
  roundRect(ctx, POSTER_RIGHT_X, 340, 160, 56, 28);
  ctx.fill();
  ctx.strokeStyle = 'rgba(191, 137, 24, 0.28)';
  roundRect(ctx, POSTER_RIGHT_X, 340, 160, 56, 28);
  ctx.stroke();
  ctx.fillStyle = '#123253';
  ctx.font = '800 22px sans-serif';
  ctx.fillText('专属短码', POSTER_RIGHT_X + 34, 374);

  ctx.fillStyle = palette.soft;
  roundRect(ctx, POSTER_RIGHT_X, 418, 160, 74, 18);
  ctx.fill();
  ctx.fillStyle = palette.ink;
  ctx.font = '900 30px sans-serif';
  ctx.fillText(result.shortCode, POSTER_RIGHT_X + 24, 464);
  ctx.fillStyle = '#596764';
  ctx.font = '700 18px sans-serif';
  ctx.fillText('朋友打开也能测', POSTER_RIGHT_X + 18, 496);
}

function drawKeywords(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  let x = CONTENT_X;
  let y = 626;
  const palette = getPalette(result.primaryElement);
  ctx.font = '800 26px sans-serif';
  for (const keyword of result.keywords.slice(0, 4)) {
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

function drawElementSpectrum(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const entries = Object.entries(result.allElementScores)
    .sort((first, second) => second[1] - first[1])
    .slice(0, 5);
  const maxScore = Math.max(...entries.map(([, score]) => score), 1);
  const startY = 696;

  ctx.fillStyle = '#24302f';
  ctx.font = '800 27px sans-serif';
  ctx.fillText('五行能量分布', CONTENT_X, startY);

  entries.forEach(([elementCode, score], index) => {
    const palette = getPalette(elementCode);
    const y = startY + 42 + index * 42;
    const barWidth = Math.max(28, Math.round((score / maxScore) * 420));
    const label = elementName(elementCode);

    ctx.fillStyle = '#596764';
    ctx.font = '800 20px sans-serif';
    ctx.fillText(label, CONTENT_X, y + 19);

    ctx.fillStyle = '#eef2ed';
    roundRect(ctx, CONTENT_X + 78, y, 430, 18, 9);
    ctx.fill();
    ctx.fillStyle = palette.primary;
    roundRect(ctx, CONTENT_X + 78, y, barWidth, 18, 9);
    ctx.fill();

    ctx.fillStyle = '#596764';
    ctx.font = '700 18px sans-serif';
    ctx.fillText(String(score), CONTENT_X + 528, y + 18);
  });
}

function drawTexts(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const startY = 930;
  ctx.fillStyle = '#24302f';
  ctx.font = '800 28px sans-serif';
  ctx.fillText('为什么像你', CONTENT_X, startY);
  drawWrappedText(ctx, result.strengthText, CONTENT_X, startY + 42, CONTENT_WIDTH, 32, 2);
}

function drawElementGlyph(
  ctx: CanvasRenderingContext2D,
  elementCode: string,
  name: string,
  x: number,
  y: number,
  size: number,
) {
  const visual = elementVisualByCode(elementCode, name);
  const centerX = x + size / 2;
  const centerY = y + size / 2;
  ctx.save();
  ctx.fillStyle = visual.color;
  ctx.font = `900 ${Math.round(size * 0.58)}px serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(name, centerX, centerY + Math.round(size * 0.03));
  ctx.restore();
}

function drawFooter(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  const palette = getPalette(result.primaryElement);
  const footerY = 1034;
  const markX = CONTENT_X + CONTENT_WIDTH - 86;
  ctx.fillStyle = 'rgba(255, 248, 232, 0.94)';
  roundRect(ctx, CONTENT_X, footerY, CONTENT_WIDTH, 94, 20);
  ctx.fill();

  ctx.fillStyle = '#6d4f29';
  ctx.font = '800 22px sans-serif';
  drawSingleLineText(ctx, '保存分享图发朋友圈', CONTENT_X + 24, footerY + 38, CONTENT_WIDTH - 130);
  ctx.fillStyle = palette.primary;
  ctx.font = '900 22px sans-serif';
  drawSingleLineText(ctx, '复制分享链接发私聊，朋友打开也能测一张', CONTENT_X + 24, footerY + 70, CONTENT_WIDTH - 130);

  ctx.fillStyle = '#596764';
  ctx.font = '600 17px sans-serif';
  drawSingleLineText(ctx, result.shortUrl, CONTENT_X, 1184, CONTENT_WIDTH);
  drawShareMark(ctx, result.shortCode, markX, footerY + 10, palette);
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
      if (lineCount === maxLines - 1) {
        drawSingleLineText(ctx, `${line}...`, x, y + lineCount * lineHeight, maxWidth);
        return;
      }
      ctx.fillText(line, x, y + lineCount * lineHeight);
      line = char;
      lineCount += 1;
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

function drawShareMark(ctx: CanvasRenderingContext2D, shortCode: string, x: number, y: number, palette: ElementPalette) {
  ctx.fillStyle = '#ffffff';
  roundRect(ctx, x, y, 74, 74, 14);
  ctx.fill();

  ctx.fillStyle = palette.primary;
  const chars = shortCode || 'WUXING';
  for (let row = 0; row < 5; row += 1) {
    for (let col = 0; col < 5; col += 1) {
      const code = chars.charCodeAt((row * 5 + col) % chars.length);
      if ((code + row + col) % 3 !== 0) {
        roundRect(ctx, x + 11 + col * 11, y + 11 + row * 11, 7, 7, 2);
        ctx.fill();
      }
    }
  }
}

function elementName(elementCode: string) {
  const names: Record<string, string> = {
    METAL: '金',
    WOOD: '木',
    WATER: '水',
    FIRE: '火',
    EARTH: '土',
  };
  return names[elementCode] ?? elementCode;
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

function getPalette(elementCode: string) {
  return elementPalettes[elementCode] ?? defaultPalette;
}
