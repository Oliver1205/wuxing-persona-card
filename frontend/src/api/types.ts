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

export interface CreateMatchRequest extends CreateResultRequest {
  partnerShortCode: string;
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

export interface MatchCandidate {
  shortCode: string;
  resultId: string;
  displayName: string;
  primaryElementName: string;
  secondaryElementName: string;
  keywords: string[];
  createdAt: string;
}

export interface MatchResult {
  matchId: string;
  partnerShortCode: string;
  currentShortCode: string;
  partnerResult: ResultDetail;
  currentResult: ResultDetail;
  compatibilityScore: number;
  relationLabel: string;
  headline: string;
  summary: string;
  strengths: string[];
  suggestions: string[];
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
  syntheticTrafficExcluded: boolean;
  syntheticIsolationLevel: string;
  syntheticIsolationNote: string;
  metricSource: 'live_event' | 'daily_metric' | 'mixed';
  aggregatedThroughDate: string | null;
  dailyTrends: DailyMetric[];
  funnelSteps: FunnelStep[];
  topChannels: NameCount[];
  topCampaigns: NameCount[];
  popularElementCombos: NameCount[];
  popularStarOfficers: NameCount[];
  recentResults: RecentResult[];
  recentShortLinks: ShortLinkListItem[];
}

export interface AnalyticsAggregation {
  startDate: string;
  endDate: string;
  daysAggregated: number;
  shortLinkRowsAggregated: number;
  aggregatedAt: string;
}

export interface FunnelStep {
  eventType: string;
  label: string;
  count: number;
  conversionRate: number;
}

export interface ExternalShortLinkRuntime {
  mode: string;
  externalMode: boolean;
  statsEnabled: boolean;
  fallbackToInternal: boolean;
  baseUrl: string;
  domain: string;
  groupId: string;
  reachable: boolean | null;
  httpStatus: number | null;
  message: string;
  checkedAt: string;
}

export interface VisitEventRuntime {
  queueSize: number;
  queueCapacity: number;
  drainLimit: number;
  droppedAsyncEvents: number;
  totalFlushedEvents: number;
  lastFlushAt: string | null;
  lastBatchSize: number;
  batchWriteFailures: number;
  workerAlive: boolean;
  asyncMode: 'local' | 'rocketmq';
  rocketMqAvailable: boolean;
  rocketMqFallbackToLocal: boolean;
  rocketMqTopic: string;
  rocketMqPublishedEvents: number;
  rocketMqPublishFailures: number;
  rocketMqFallbackEvents: number;
  rocketMqShadowLocalEvents: number;
  rocketMqConsumerEnabled: boolean;
  rocketMqConsumerPersistenceReady: boolean;
  healthStatus: 'ok' | 'watch' | 'danger';
  healthMessage: string;
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
  metricSource: 'live_event' | 'daily_metric' | 'external';
  lastVisitAt: string | null;
}

export interface ShortLinkVisit {
  createdAt: string;
  eventType: string;
  clientIdHash: string | null;
  ipHash: string | null;
  userAgentHash: string | null;
  channel: string | null;
  campaign: string | null;
  deviceType: string | null;
  referer: string | null;
  statSource: 'local' | 'external';
}

export interface PageResult<T> {
  page: number;
  pageSize: number;
  total: number;
  records: T[];
}
