export interface ElementVisual {
  code: string;
  name: string;
  keywords: string[];
  color: string;
  soft: string;
}

export const elementVisuals: ElementVisual[] = [
  {
    code: 'METAL',
    name: '金',
    keywords: ['收束', '淬炼', '清醒'],
    color: '#bf8918',
    soft: '#f8f0dc',
  },
  {
    code: 'WOOD',
    name: '木',
    keywords: ['生长', '舒展', '灵动'],
    color: '#2f705e',
    soft: '#e7f2ed',
  },
  {
    code: 'WATER',
    name: '水',
    keywords: ['润泽', '沉静', '蓄藏'],
    color: '#1e5f9f',
    soft: '#e4eef7',
  },
  {
    code: 'FIRE',
    name: '火',
    keywords: ['热烈', '上扬', '明朗'],
    color: '#b84d35',
    soft: '#f8e6de',
  },
  {
    code: 'EARTH',
    name: '土',
    keywords: ['承载', '滋养', '收成'],
    color: '#7b5c32',
    soft: '#eee5d3',
  },
];

export const elementVisualMap = elementVisuals.reduce<Record<string, ElementVisual>>((map, item) => {
  map[item.code] = item;
  map[item.name] = item;
  return map;
}, {});

export function elementVisualByCode(code: string, name?: string): ElementVisual {
  return elementVisualMap[code] ?? (name ? elementVisualMap[name] : undefined) ?? {
    code,
    name: name ?? code,
    keywords: ['观察', '平衡', '生长'],
    color: '#2f705e',
    soft: '#e7f2ed',
  };
}
