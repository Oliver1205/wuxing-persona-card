export interface Option {
  optionCode: string;
  optionText: string;
  elementCode: string;
  elementName: string;
}

export interface Question {
  questionCode: string;
  title: string;
  options: Option[];
}

export interface Answer {
  questionCode: string;
  optionCode: string;
}

export interface CreateResultRequest {
  birthYear: number;
  birthMonth: number;
  birthDay: number | null;
  birthTimeRange: string | null;
  answers: Answer[];
}

export interface ResultDetail {
  resultId: string;
  primaryElement: string;
  primaryElementName: string;
  primaryPercent: number;
  secondaryElement: string;
  secondaryElementName: string;
  secondaryPercent: number;
  allElementScores: Record<string, number>;
  starOfficerCode: string;
  starOfficerName: string;
  keywords: string[];
  layoutExplanation: string;
  strengthText: string;
  relationshipText: string;
  cardImageKey: string;
  shortCode: string;
  shortUrl: string;
  createdAt: string;
}

export interface AdminOverview {
  totalPv: number;
  totalUv: number;
  totalUip: number;
  homeViews: number;
  startClicks: number;
  testSubmits: number;
  resultCreated: number;
  shortLinkCreated: number;
  shortLinkVisits: number;
  completionRate: number;
  dailyTrends: DailyMetric[];
  popularElementCombos: NameCount[];
  popularStarOfficers: NameCount[];
  recentResults: RecentResult[];
  recentShortLinks: ShortLinkListItem[];
}

export interface DailyMetric {
  date: string;
  pv: number;
  resultCreated: number;
  shortLinkCreated: number;
  shortLinkVisits: number;
}

export interface NameCount {
  name: string;
  count: number;
}

export interface RecentResult {
  resultId: string;
  elementCombo: string;
  starOfficerName: string;
  createdAt: string;
}

export interface ShortLinkListItem {
  shortCode: string;
  shortUrl: string;
  resultId: string;
  elementCombo: string;
  starOfficerName: string;
  createdAt: string;
  pv: number;
  uv: number;
  uip: number;
  statSource: 'local' | 'external';
  lastVisitAt: string | null;
}

export interface ShortLinkVisit {
  createdAt: string;
  eventType: string;
  clientIdHash: string;
  ipHash: string;
  userAgentHash: string;
  referer: string | null;
  statSource: 'local' | 'external';
}

export interface PageResult<T> {
  page: number;
  pageSize: number;
  total: number;
  records: T[];
}
