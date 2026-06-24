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
    color: '#b8872d',
    soft: '#f7ecd4',
  },
  {
    code: 'WOOD',
    name: '木',
    keywords: ['生长', '舒展', '灵动'],
    color: '#2e735e',
    soft: '#e4f1ea',
  },
  {
    code: 'WATER',
    name: '水',
    keywords: ['润泽', '沉静', '蓄藏'],
    color: '#2c6894',
    soft: '#e4eef5',
  },
  {
    code: 'FIRE',
    name: '火',
    keywords: ['热烈', '上扬', '明朗'],
    color: '#c45f3c',
    soft: '#f8e5da',
  },
  {
    code: 'EARTH',
    name: '土',
    keywords: ['承载', '滋养', '收成'],
    color: '#8a663e',
    soft: '#efe4cf',
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
