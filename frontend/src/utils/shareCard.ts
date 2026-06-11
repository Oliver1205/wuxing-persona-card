import type { ResultDetail } from '../api/types';

const CARD_WIDTH = 900;
const CARD_HEIGHT = 1200;

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

  ctx.fillStyle = 'rgba(255, 255, 255, 0.72)';
  roundRect(ctx, 54, 54, CARD_WIDTH - 108, CARD_HEIGHT - 108, 28);
  ctx.fill();
}

function drawTitle(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  ctx.fillStyle = '#7b5d35';
  ctx.font = '700 28px sans-serif';
  ctx.fillText('五行人格卡', 100, 132);

  ctx.fillStyle = '#24302f';
  ctx.font = '900 58px sans-serif';
  ctx.fillText(`${result.primaryElementName}${result.secondaryElementName}型${result.keywords[0] ?? '人格'}`, 100, 212);

  ctx.fillStyle = '#596764';
  ctx.font = '700 28px sans-serif';
  ctx.fillText(result.starOfficerName, 100, 258);
}

function drawElementBlock(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  ctx.fillStyle = '#2f6f5e';
  roundRect(ctx, 100, 300, 700, 190, 24);
  ctx.fill();

  ctx.fillStyle = '#ffffff';
  ctx.font = '900 88px sans-serif';
  ctx.fillText(result.primaryElementName, 148, 420);

  ctx.font = '800 34px sans-serif';
  ctx.fillText(`${result.primaryPercent}% ${result.primaryElementName}`, 320, 370);
  ctx.fillText(`${result.secondaryPercent}% ${result.secondaryElementName}`, 320, 424);

  ctx.fillStyle = 'rgba(255, 255, 255, 0.24)';
  roundRect(ctx, 148, 448, 604, 16, 8);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  roundRect(ctx, 148, 448, Math.max(20, Math.round(604 * result.primaryPercent / 100)), 16, 8);
  ctx.fill();
}

function drawKeywords(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  let x = 100;
  let y = 560;
  ctx.font = '700 26px sans-serif';
  for (const keyword of result.keywords) {
    const width = ctx.measureText(keyword).width + 42;
    if (x + width > 800) {
      x = 100;
      y += 58;
    }
    ctx.fillStyle = '#f1eadc';
    roundRect(ctx, x, y - 34, width, 44, 22);
    ctx.fill();
    ctx.fillStyle = '#6d4f29';
    ctx.fillText(keyword, x + 21, y - 4);
    x += width + 14;
  }
}

function drawTexts(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  ctx.fillStyle = '#24302f';
  ctx.font = '800 30px sans-serif';
  ctx.fillText('性格亮点', 100, 700);
  drawWrappedText(ctx, result.strengthText, 100, 750, 700, 34, 3);

  ctx.font = '800 30px sans-serif';
  ctx.fillText('相处优势', 100, 890);
  drawWrappedText(ctx, result.relationshipText, 100, 940, 700, 34, 3);
}

function drawFooter(ctx: CanvasRenderingContext2D, result: ResultDetail) {
  ctx.fillStyle = '#596764';
  ctx.font = '500 22px sans-serif';
  drawWrappedText(ctx, '传统文化元素启发下的娱乐性人格解读，不构成现实决策建议。', 100, 1082, 700, 30, 2);
  ctx.font = '700 20px sans-serif';
  drawWrappedText(ctx, result.shortUrl, 100, 1140, 700, 26, 2);
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
