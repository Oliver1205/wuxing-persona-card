<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import {
  aggregateAdminAnalytics,
  exportAdminShortLinks,
  fetchAdminOverview,
  fetchAdminShortLinks,
  fetchExternalShortLinkRuntime,
  fetchVisitEventRuntime,
} from '../api/admin';
import type { AdminDateFilter } from '../api/admin';
import { useRoute } from 'vue-router';
import type {
  AdminOverview,
  AnalyticsAggregation,
  ExternalShortLinkRuntime,
  PageResult,
  ShortLinkListItem,
  VisitEventRuntime,
} from '../api/types';
import StatCard from '../components/StatCard.vue';

type ActionTone = 'good' | 'watch' | 'danger' | 'cold';
type MobileReportGroupKey = 'core' | 'trend' | 'attribution';

interface ActionItem {
  level: string;
  title: string;
  detail: string;
  action: string;
  tone: ActionTone;
  evidenceId: string;
  evidenceLabel: string;
}

interface RadarItem {
  label: string;
  value: string;
  score: number;
  detail: string;
  tone: ActionTone;
}

interface JourneyStep {
  label: string;
  eventType: string;
  count: number;
  retentionLabel: string;
  dropLabel: string;
  note: string;
  tone: ActionTone;
}

interface EvidenceItem {
  id: string;
  label: string;
  value: string;
  note: string;
  tone: ActionTone;
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;
const quickDateRanges = [
  { label: '今日', days: 1 },
  { label: '近 7 天', days: 7 },
  { label: '近 14 天', days: 14 },
  { label: '全部', days: null },
];
const pageSizeOptions = [10, 20, 50];

const route = useRoute();
const token = ref(localStorage.getItem('wuxing_admin_token') || '');
const startDate = ref(String(route.query.startDate || ''));
const endDate = ref(String(route.query.endDate || ''));
const keyword = ref(String(route.query.keyword || ''));
const statSource = ref<'local' | 'external' | ''>(route.query.statSource === 'local' || route.query.statSource === 'external' ? route.query.statSource : '');
const includeSynthetic = ref(route.query.includeSynthetic === 'true');
const shortLinkPage = ref(1);
const shortLinkPageSize = ref(20);
const overview = ref<AdminOverview | null>(null);
const comparisonOverview = ref<AdminOverview | null>(null);
const shortLinks = ref<PageResult<ShortLinkListItem> | null>(null);
const runtime = ref<ExternalShortLinkRuntime | null>(null);
const visitEventRuntime = ref<VisitEventRuntime | null>(null);
const aggregation = ref<AnalyticsAggregation | null>(null);
const error = ref('');
const runtimeWarning = ref('');
const visitEventRuntimeWarning = ref('');
const loading = ref(false);
const runtimeChecking = ref(false);
const visitEventRuntimeChecking = ref(false);
const exporting = ref(false);
const aggregating = ref(false);
const briefingCopied = ref(false);
const mobileReportOpen = ref(false);
const compactReportGroups = ref(false);
const mobileReportGroups = ref<Record<MobileReportGroupKey, boolean>>({
  core: true,
  trend: false,
  attribution: false,
});
const lastLoadedAt = ref('');
let runtimeRequestSeq = 0;
let visitEventRuntimeRequestSeq = 0;
let compactReportMedia: MediaQueryList | null = null;
const busy = computed(() => loading.value || exporting.value || aggregating.value);
const dailyTrendMax = computed(() => {
  const rows = overview.value?.dailyTrends ?? [];
  return Math.max(1, ...rows.flatMap((item) => [item.pv, item.resultCreated, item.shortLinkVisits]));
});
const dailyTrendRows = computed(() => {
  const rows = overview.value?.dailyTrends ?? [];
  return rows.map((item, index) => {
    const previous = rows[index - 1];
    return {
      ...item,
      pvDelta: previous ? item.pv - previous.pv : null,
      resultDelta: previous ? item.resultCreated - previous.resultCreated : null,
      shortLinkVisitDelta: previous ? item.shortLinkVisits - previous.shortLinkVisits : null,
    };
  });
});
const funnelMax = computed(() => {
  const rows = overview.value?.funnelSteps ?? [];
  return Math.max(1, ...rows.map((item) => item.count));
});
const elementComboMax = computed(() => maxCount(overview.value?.popularElementCombos ?? []));
const channelMax = computed(() => maxCount(overview.value?.topChannels ?? []));
const campaignMax = computed(() => maxCount(overview.value?.topCampaigns ?? []));
const recentShortLinkMax = computed(() => {
  const rows = overview.value?.recentShortLinks ?? [];
  return Math.max(1, ...rows.map((item) => item.pv));
});
const shortLinkTotalPages = computed(() => Math.max(1, Math.ceil((shortLinks.value?.total ?? 0) / shortLinkPageSize.value)));
const shortLinkStartIndex = computed(() => {
  if (!shortLinks.value?.total) {
    return 0;
  }
  return (shortLinkPage.value - 1) * shortLinkPageSize.value + 1;
});
const shortLinkEndIndex = computed(() => {
  const total = shortLinks.value?.total ?? 0;
  return Math.min(total, shortLinkPage.value * shortLinkPageSize.value);
});
const dataScopeStatus = computed(() => {
  const rangeLabel = startDate.value || endDate.value
    ? `${startDate.value || '起始不限'} 至 ${endDate.value || '今日'}`
    : '全部日期';
  const trafficLabel = includeSynthetic.value ? '包含 perf-test 测试流量' : '默认排除 perf-test';
  const sourceLabel = overview.value ? metricSourceLabel(overview.value.metricSource) : '待加载';
  const aggregateLabel = overview.value?.aggregatedThroughDate
    ? `聚合至 ${overview.value.aggregatedThroughDate}`
    : '实时事件口径';
  const refreshedLabel = lastLoadedAt.value ? `刷新于 ${lastLoadedAt.value}` : '尚未刷新';
  return {
    rangeLabel,
    trafficLabel,
    sourceLabel,
    aggregateLabel,
    refreshedLabel,
  };
});
const sourceMix = computed(() => {
  const records = shortLinks.value?.records ?? [];
  const local = records.filter((item) => item.statSource === 'local').length;
  const external = records.filter((item) => item.statSource === 'external').length;
  const total = Math.max(1, records.length);
  return {
    local,
    external,
    localPercent: Math.round((local * 1000) / total) / 10,
    externalPercent: Math.round((external * 1000) / total) / 10,
  };
});
const shortLinkEvidenceCards = computed(() => {
  const records = shortLinks.value?.records ?? [];
  const pagePv = records.reduce((sum, item) => sum + item.pv, 0);
  const created = overview.value?.shortLinkCreated ?? 0;
  const visits = overview.value?.shortLinkVisits ?? 0;
  const sourceFilterLabel = statSource.value === 'external'
    ? '只看外部'
    : statSource.value === 'local'
      ? '只看本地'
      : '本地 + 外部';
  return [
    {
      label: '短链总量',
      value: String(shortLinks.value?.total ?? 0),
      note: '当前筛选范围',
    },
    {
      label: '回流访问',
      value: String(visits),
      note: `平均 ${perShortLinkLabel(averagePerItem(visits, created))}`,
    },
    {
      label: '当前页 PV',
      value: String(pagePv),
      note: `${shortLinkStartIndex.value}-${shortLinkEndIndex.value} 条明细`,
    },
    {
      label: '统计口径',
      value: overview.value ? metricSourceLabel(overview.value.metricSource) : '待加载',
      note: sourceFilterLabel,
    },
  ];
});
const shareActionTypes = ['SHORT_LINK_COPY', 'SAVE_SHARE_IMAGE_SUCCESS', 'NATIVE_SHARE_SUCCESS'];

const evidenceIndex = computed<EvidenceItem[]>(() => {
  if (!overview.value) {
    return [];
  }
  const data = overview.value;
  const shareActions = funnelCount(data, shareActionTypes);
  const visitsPerShortLink = averagePerItem(data.shortLinkVisits, data.shortLinkCreated);
  const runtimeToneValue = visitEventRuntime.value ? runtimeTone(visitEventRuntime.value.healthStatus) : 'watch';
  return [
    {
      id: 'journey-section',
      label: '转化证据',
      value: rateLabel(data.completionRate, { compact: true }),
      note: `${data.resultCreated}/${data.startClicks} 结果生成`,
      tone: data.completionRate > 100 ? 'watch' : data.completionRate >= 45 ? 'good' : 'watch',
    },
    {
      id: 'trend-section',
      label: '趋势证据',
      value: `${data.dailyTrends.length} 天`,
      note: data.dailyTrends.length ? 'PV/结果/回流同屏对比' : '暂无趋势样本',
      tone: data.dailyTrends.length ? 'good' : 'cold',
    },
    {
      id: 'attribution-section',
      label: '来源证据',
      value: data.topChannels[0]?.name ?? '暂无',
      note: `分享动作 ${shareActions}，Campaign ${data.topCampaigns.length} 类`,
      tone: shareActions > 0 || data.topChannels.length ? 'good' : 'watch',
    },
    {
      id: 'shortlink-section',
      label: '短链证据',
      value: perShortLinkLabel(visitsPerShortLink),
      note: `${shortLinks.value?.total ?? 0} 条短链可分页查看`,
      tone: visitsPerShortLink > 10 ? 'watch' : data.shortLinkVisits > 0 ? 'good' : 'cold',
    },
    {
      id: 'runtime-section',
      label: '运行态证据',
      value: visitEventRuntime.value ? runtimeHealthLabel(visitEventRuntime.value.healthStatus) : '待读取',
      note: visitEventRuntime.value ? `队列 ${visitEventRuntime.value.queueSize}/${visitEventRuntime.value.queueCapacity}` : '刷新后看队列与写入失败',
      tone: runtimeToneValue,
    },
  ];
});

function funnelCount(data: AdminOverview, eventTypes: string[]) {
  const allowed = new Set(eventTypes);
  return data.funnelSteps
    .filter((item) => allowed.has(item.eventType))
    .reduce((sum, item) => sum + item.count, 0);
}

const operationInsights = computed(() => {
  if (!overview.value) {
    return [];
  }
  const data = overview.value;
  const shareActions = funnelCount(data, shareActionTypes);
  const sharePanelViews = funnelCount(data, ['SHARE_PANEL_VIEW']);
  const shareActionRate = numberPercent(shareActions, data.resultCreated);
  const visitsPerShortLink = averagePerItem(data.shortLinkVisits, data.shortLinkCreated);
  const submitGap = Math.max(0, data.testSubmits - data.resultCreated);
  const abnormalCompletion = data.completionRate > 100;
  const abnormalReturn = visitsPerShortLink > 10;
  return [
    {
      label: '转化判断',
      title: abnormalCompletion ? '存在压测或接口直调样本' : data.completionRate >= 45 ? '测算链路基本顺畅' : '开始到完成仍有优化空间',
      detail: abnormalCompletion
        ? `完成率 ${data.completionRate}% 已超过 100%，说明结果生成数高于开始点击数，需先排除压测、补数或接口直调样本。`
        : `完成率 ${data.completionRate}%，重点看题卡停留、提交失败和移动端按钮位置。`,
      tone: abnormalCompletion ? 'watch' : data.completionRate >= 45 ? 'good' : 'watch',
    },
    {
      label: '分享判断',
      title: shareActions > 0 ? '已有真实分享动作' : sharePanelViews > 0 ? '结果页已曝光分享区' : '真实分享动作样本不足',
      detail: shareActions > 0
        ? `复制、保存图或系统分享共 ${shareActions} 次，约占结果生成 ${shareActionRate}%。`
        : `分享区曝光 ${sharePanelViews} 次；短链生成是系统绑定能力，不等同用户主动分享。`,
      tone: shareActions > 0 ? 'good' : 'watch',
    },
    {
      label: '回流判断',
      title: abnormalReturn ? '回流强度被压测放大' : visitsPerShortLink > 0 ? '短链已有回流信号' : '短链传播样本仍不足',
      detail: abnormalReturn
        ? `平均 ${perShortLinkLabel(visitsPerShortLink)}，更像压测或重复访问样本，分析传播前应先看渠道和时间段。`
        : `平均 ${perShortLinkLabel(visitsPerShortLink)}，适合继续看渠道和活动归因。`,
      tone: abnormalReturn ? 'watch' : visitsPerShortLink > 0 ? 'good' : 'cold',
    },
    {
      label: '异常判断',
      title: submitGap > 0 ? '存在提交成功与结果生成落差' : '提交与结果生成口径一致',
      detail: submitGap > 0 ? `约 ${submitGap} 次提交未转成结果，需排查接口错误或重复点击。` : '当前未看到明显结果生成缺口。',
      tone: submitGap > 0 ? 'watch' : 'good',
    },
  ];
});
const focusMetrics = computed(() => {
  if (!overview.value) {
    return [];
  }
  const data = overview.value;
  const shareActions = funnelCount(data, shareActionTypes);
  const sharePanelViews = funnelCount(data, ['SHARE_PANEL_VIEW']);
  const visitsPerShortLink = averagePerItem(data.shortLinkVisits, data.shortLinkCreated);
  const completionAbnormal = data.completionRate > 100;
  return [
    {
      label: '测算完成',
      value: rateLabel(data.completionRate),
      note: completionAbnormal ? `${data.resultCreated} 个结果 / ${data.startClicks} 次开始 · 口径异常` : `${data.resultCreated} 个结果 / ${data.startClicks} 次开始`,
      tone: completionAbnormal ? 'watch' : data.completionRate >= 55 ? 'good' : data.completionRate >= 25 ? 'watch' : 'cold',
    },
    {
      label: '分享入口',
      value: String(shareActions),
      note: `${sharePanelViews} 次分享区曝光 · ${data.shortLinkCreated} 条系统短链`,
      tone: shareActions > 0 ? 'good' : sharePanelViews > 0 ? 'watch' : 'cold',
    },
    {
      label: '回流强度',
      value: perShortLinkLabel(visitsPerShortLink),
      note: `${data.shortLinkVisits} 次访问 / ${data.shortLinkCreated} 条短链`,
      tone: visitsPerShortLink > 10 ? 'watch' : data.shortLinkVisits > 0 ? 'good' : 'watch',
    },
    {
      label: '数据口径',
      value: metricSourceLabel(data.metricSource),
      note: data.syntheticTrafficExcluded
        ? isolationLevelLabel(data.syntheticIsolationLevel)
        : data.aggregatedThroughDate ? `已聚合至 ${data.aggregatedThroughDate}` : '使用实时事件',
      tone: data.metricSource === 'live_event' ? 'watch' : 'good',
    },
  ];
});
const syntheticImpact = computed(() => {
  if (!overview.value || !comparisonOverview.value) {
    return null;
  }
  const current = overview.value;
  const comparison = comparisonOverview.value;
  const real = current.syntheticTrafficExcluded ? current : comparison.syntheticTrafficExcluded ? comparison : null;
  const all = current.syntheticTrafficExcluded ? comparison : !comparison.syntheticTrafficExcluded ? comparison : current;
  if (!real || !all) {
    return null;
  }
  const items = [
    impactItem('PV', all.totalPv, real.totalPv),
    impactItem('结果', all.resultCreated, real.resultCreated),
    impactItem('系统短链', all.shortLinkCreated, real.shortLinkCreated),
    impactItem('回流访问', all.shortLinkVisits, real.shortLinkVisits),
  ];
  const totalDelta = items.reduce((sum, item) => sum + item.delta, 0);
  return {
    title: includeSynthetic.value ? '当前含测试流量' : '默认按事件排除测试流量',
    note: totalDelta > 0
      ? `本筛选范围内识别到 ${totalDelta} 条测试增量，按 channel=perf-test 隔离；这不是实体层强隔离。`
      : '本筛选范围内暂未观察到 perf-test 测试增量；当前仍是事件口径隔离。',
    items,
    hasImpact: totalDelta > 0,
  };
});
const operationRadar = computed<RadarItem[]>(() => {
  if (!overview.value) {
    return [];
  }
  const data = overview.value;
  const shareActions = funnelCount(data, shareActionTypes);
  const shareActionRate = numberPercent(shareActions, data.resultCreated);
  const visitsPerShortLink = averagePerItem(data.shortLinkVisits, data.shortLinkCreated);
  const completionScore = data.completionRate > 100 ? 55 : clampScore(data.completionRate);
  const shareScore = clampScore(shareActionRate);
  const returnScore = visitsPerShortLink > 10 ? 72 : clampScore(visitsPerShortLink * 35);
  const trustPenalty = (syntheticImpact.value?.hasImpact ? 18 : 0)
    + (data.completionRate > 100 ? 20 : 0)
    + (data.syntheticTrafficExcluded ? 0 : 8);
  const trustBase = data.metricSource === 'live_event' ? 68 : 82;
  const trustScore = clampScore(trustBase - trustPenalty);

  return [
    {
      label: '完成力',
      value: rateLabel(data.completionRate, { compact: true }),
      score: completionScore,
      detail: data.completionRate > 100 ? '口径异常，先排除压测或接口直调。' : '结果生成相对开始点击的转化强度。',
      tone: data.completionRate > 100 ? 'watch' : scoreTone(completionScore),
    },
    {
      label: '分享意愿',
      value: `${shareActions} 次`,
      score: shareScore,
      detail: `复制、保存图或系统分享约占结果生成 ${rateLabel(shareActionRate)}。`,
      tone: scoreTone(shareScore),
    },
    {
      label: '回流热度',
      value: perShortLinkLabel(visitsPerShortLink),
      score: returnScore,
      detail: visitsPerShortLink > 10 ? '样本可能被压测或重复访问放大。' : '每条系统短链带来的平均访问强度。',
      tone: visitsPerShortLink > 10 ? 'watch' : scoreTone(returnScore),
    },
    {
      label: '口径可信',
      value: metricSourceLabel(data.metricSource),
      score: trustScore,
      detail: data.syntheticTrafficExcluded ? '默认排除 perf-test，仍需留意实体层隔离边界。' : '当前包含测试流量，汇报时需要注明口径。',
      tone: scoreTone(trustScore),
    },
  ];
});
const journeySteps = computed<JourneyStep[]>(() => {
  const rows = overview.value?.funnelSteps ?? [];
  return rows.map((item, index) => {
    const previous = index > 0 ? rows[index - 1] : null;
    const inverted = previous ? item.count > previous.count : false;
    const retentionRate = previous && previous.count > 0
      ? Math.round((item.count * 1000) / previous.count) / 10
      : 100;
    const dropCount = previous ? Math.max(0, previous.count - item.count) : 0;
    const tone = !previous
      ? 'good'
      : inverted
        ? 'watch'
        : retentionRate >= 70
          ? 'good'
          : retentionRate >= 40
            ? 'watch'
            : 'cold';

    return {
      label: item.label,
      eventType: item.eventType,
      count: item.count,
      retentionLabel: previous ? rateLabel(retentionRate, { compact: true }) : '起点',
      dropLabel: previous ? `-${dropCount}` : '-',
      note: journeyStepNote(item.label, previous?.label, retentionRate, inverted),
      tone,
    };
  });
});
const actionItems = computed<ActionItem[]>(() => {
  if (!overview.value) {
    return [];
  }
  const data = overview.value;
  const shareActions = funnelCount(data, shareActionTypes);
  const sharePanelViews = funnelCount(data, ['SHARE_PANEL_VIEW']);
  const visitsPerShortLink = averagePerItem(data.shortLinkVisits, data.shortLinkCreated);
  const items: ActionItem[] = [];

  if (visitEventRuntime.value?.healthStatus === 'danger') {
      items.push({
        level: 'P0',
        title: '访问事件写入需要立即处理',
        detail: visitEventRuntime.value.healthMessage,
        action: `先看队列 ${queueUsageLabel(visitEventRuntime.value)}、丢弃 ${visitEventRuntime.value.droppedAsyncEvents}、失败 ${visitEventRuntime.value.batchWriteFailures}`,
        tone: 'danger',
        evidenceId: 'runtime-section',
        evidenceLabel: '运行态',
      });
    } else if (visitEventRuntime.value?.healthStatus === 'watch') {
      items.push({
        level: 'P1',
        title: '访问事件运行态需要持续观察',
        detail: visitEventRuntime.value.healthMessage,
        action: `刷新运行态，确认 ${visitEventRuntime.value.asyncMode} 模式下队列没有继续升高`,
        tone: 'watch',
        evidenceId: 'runtime-section',
        evidenceLabel: '运行态',
      });
    }

  if (data.completionRate > 100) {
      items.push({
        level: 'P1',
        title: '完成率超过 100%，先校准数据口径',
        detail: `${data.resultCreated} 个结果 / ${data.startClicks} 次开始，通常来自压测、接口直调或补数。`,
        action: '切换“包含测试流量”，再按渠道和日期缩小范围',
        tone: 'watch',
        evidenceId: 'journey-section',
        evidenceLabel: '转化链路',
      });
    } else if (data.startClicks >= 10 && data.completionRate < 30) {
      items.push({
        level: 'P1',
        title: '测算完成率偏低',
        detail: `完成率 ${data.completionRate}%，说明开始测试后有明显掉点。`,
        action: '优先看测试页题卡、移动端底部按钮和提交错误日志',
        tone: 'watch',
        evidenceId: 'journey-section',
        evidenceLabel: '转化链路',
      });
    }

  if (sharePanelViews > 0 && shareActions === 0) {
      items.push({
        level: 'P2',
        title: '分享区有曝光，但真实分享动作不足',
        detail: `${sharePanelViews} 次分享区曝光，没有复制短链、保存图或系统分享事件。`,
        action: '优先检查结果页分享按钮可见性、文案吸引力和移动端点击区域',
        tone: 'watch',
        evidenceId: 'journey-section',
        evidenceLabel: '转化链路',
      });
    } else if (shareActions > 0) {
      items.push({
        level: 'OK',
        title: '真实分享动作已经被记录',
        detail: `当前筛选范围内有 ${shareActions} 次真实分享动作。`,
        action: '继续按渠道观察短链回流和二次测试点击',
        tone: 'good',
        evidenceId: 'attribution-section',
        evidenceLabel: '来源排行',
      });
    }

  if (visitsPerShortLink > 10) {
      items.push({
        level: 'P2',
        title: '短链回流强度异常偏高',
        detail: `平均 ${perShortLinkLabel(visitsPerShortLink)}，可能被压测或重复访问放大。`,
        action: '先排除 perf-test，再看短链访问明细里的设备、IP 和时间段',
        tone: 'watch',
        evidenceId: 'shortlink-section',
        evidenceLabel: '短链明细',
      });
    } else if (data.shortLinkVisits > 0) {
      items.push({
        level: 'OK',
        title: '短链回流已经出现',
        detail: `${data.shortLinkVisits} 次访问分布在 ${data.shortLinkCreated} 条系统短链上。`,
        action: '继续观察 Top Channel、Top Campaign 和最近短链',
        tone: 'good',
        evidenceId: 'shortlink-section',
        evidenceLabel: '短链明细',
      });
    }

  if (syntheticImpact.value?.hasImpact) {
      items.push({
        level: 'P2',
        title: '当前范围存在测试流量差异',
        detail: syntheticImpact.value.note,
        action: '汇报真实运营数据时说明这是事件 channel 过滤',
        tone: 'watch',
        evidenceId: 'synthetic-section',
        evidenceLabel: '口径差异',
      });
    }

  if (runtime.value?.externalMode && runtime.value.reachable === false) {
      items.push({
        level: 'P1',
        title: '外部短链服务不可达',
        detail: runtime.value.message,
        action: runtime.value.fallbackToInternal ? '确认已降级到内部短链' : '需要恢复外部服务或手动切回内部模式',
        tone: 'danger',
        evidenceId: 'external-shortlink-section',
        evidenceLabel: '外部短链',
      });
    }

  if (!items.length) {
      items.push({
        level: 'OK',
        title: '核心口径暂未发现明显异常',
        detail: '访问、完成、分享、回流和运行态均处于可继续观察状态。',
        action: '下一步可以按日期和渠道看内容实验效果',
        tone: 'good',
        evidenceId: 'trend-section',
        evidenceLabel: '趋势视图',
      });
    }

  return items.slice(0, 6);
});
const operationBriefing = computed(() => {
  if (!overview.value) {
    return null;
  }
  const data = overview.value;
  const topAction = actionItems.value[0];
  const latest = dailyTrendRows.value[dailyTrendRows.value.length - 1];
  const topChannel = data.topChannels[0]?.name ?? '暂无';
  const topCampaign = data.topCampaigns[0]?.name ?? '暂无';
  const topCombo = data.popularElementCombos[0]?.name ?? '暂无';
  const runtimeLine = visitEventRuntime.value
    ? `${runtimeHealthLabel(visitEventRuntime.value.healthStatus)}，队列 ${visitEventRuntime.value.queueSize}/${visitEventRuntime.value.queueCapacity}，丢弃 ${visitEventRuntime.value.droppedAsyncEvents}，写入失败 ${visitEventRuntime.value.batchWriteFailures}`
    : '尚未读取访问事件运行态';
  const lines = [
    `口径：${dataScopeStatus.value.rangeLabel}，${dataScopeStatus.value.trafficLabel}，来源 ${dataScopeStatus.value.sourceLabel}，${dataScopeStatus.value.aggregateLabel}，${dataScopeStatus.value.refreshedLabel}。`,
    `核心：PV ${data.totalPv}，UV ${data.totalUv}，完成率 ${rateLabel(data.completionRate)}，结果 ${data.resultCreated}，短链回流 ${data.shortLinkVisits}。`,
    `传播：分享动作 ${funnelCount(data, shareActionTypes)}，系统短链 ${data.shortLinkCreated}，平均 ${perShortLinkLabel(averagePerItem(data.shortLinkVisits, data.shortLinkCreated))}。`,
    latest
      ? `趋势：最新 ${latest.date}，ΔPV ${deltaLabel(latest.pvDelta)}，Δ结果 ${deltaLabel(latest.resultDelta)}，Δ回流 ${deltaLabel(latest.shortLinkVisitDelta)}。`
      : '趋势：当前筛选范围暂无日趋势数据。',
    `结构：Top Channel ${topChannel}，Top Campaign ${topCampaign}，热门人格 ${topCombo}。`,
    topAction
      ? `优先动作：${topAction.level} ${topAction.title}，${topAction.action}。`
      : '优先动作：暂无明显异常，继续观察渠道、Campaign 和短链明细。',
    `运行态：${runtimeLine}。`,
  ];
  return {
    lines,
    text: `五行人格数据中台复盘\n${lines.map((line) => `- ${line}`).join('\n')}`,
  };
});

onMounted(() => {
  setupCompactReportGroups();
  if (token.value) {
    load();
  }
});

onUnmounted(() => {
  compactReportMedia?.removeEventListener('change', syncCompactReportMode);
});

function setupCompactReportGroups() {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return;
  }
  compactReportMedia = window.matchMedia('(max-width: 760px)');
  syncCompactReportMode(compactReportMedia);
  compactReportMedia.addEventListener('change', syncCompactReportMode);
}

function syncCompactReportMode(event: MediaQueryList | MediaQueryListEvent) {
  compactReportGroups.value = event.matches;
}

function reportGroupOpen(key: MobileReportGroupKey) {
  return !compactReportGroups.value || mobileReportGroups.value[key];
}

function toggleMobileReportGroup(key: MobileReportGroupKey) {
  if (!compactReportGroups.value) {
    return;
  }
  if (mobileReportGroups.value[key]) {
    setMobileReportGroup(key, false);
    return;
  }
  openMobileReportGroup(key, true);
}

function openMobileReportGroup(key: MobileReportGroupKey, exclusive = false) {
  if (!exclusive) {
    setMobileReportGroup(key, true);
    return;
  }
  mobileReportGroups.value = {
    core: key === 'core',
    trend: key === 'trend',
    attribution: key === 'attribution',
  };
}

function setMobileReportGroup(key: MobileReportGroupKey, open: boolean) {
  if (mobileReportGroups.value[key] === open) {
    return;
  }
  mobileReportGroups.value = {
    ...mobileReportGroups.value,
    [key]: open,
  };
}

async function load(forceRefresh = false) {
  if (busy.value) {
    return;
  }
  const adminToken = token.value.trim();
  if (!adminToken) {
    resetAdminData();
    error.value = '请输入管理 token';
    return;
  }
  error.value = '';
  runtimeWarning.value = '';
  visitEventRuntimeWarning.value = '';
  loading.value = true;
  try {
    const [overviewData, shortLinkData] = await Promise.all([
      fetchAdminOverview(adminToken, { ...dateFilter(), forceRefresh }),
      fetchAdminShortLinks(adminToken, shortLinkPage.value, shortLinkPageSize.value, shortLinkFilter()),
    ]);
    token.value = adminToken;
    localStorage.setItem('wuxing_admin_token', adminToken);
    overview.value = normalizeOverview(overviewData);
    shortLinks.value = normalizeShortLinks(shortLinkData);
    comparisonOverview.value = await fetchComparisonOverview();
    lastLoadedAt.value = formatClock(new Date());
  } catch (err) {
    const message = err instanceof Error ? err.message : '后台数据加载失败';
    const invalidToken = message.includes('admin token is invalid');
    error.value = invalidToken ? '管理 token 无效，请重新输入。' : message;
    if (invalidToken) {
      localStorage.removeItem('wuxing_admin_token');
      token.value = '';
      resetAdminData();
    }
  } finally {
    loading.value = false;
  }
  if (!error.value) {
    void resetAdminHorizontalScroll();
    void refreshRuntime(false);
    void refreshVisitEventRuntime();
  }
}

async function locateEvidence(event: MouseEvent, evidenceId: string) {
  event.preventDefault();
  const target = document.getElementById(evidenceId);
  if (target?.closest('.mobile-report-content')) {
    mobileReportOpen.value = true;
    const reportGroup = target.closest('.mobile-report-group') as HTMLDetailsElement | null;
    const reportGroupKey = reportGroup?.dataset.reportGroup as MobileReportGroupKey | undefined;
    if (reportGroupKey) {
      openMobileReportGroup(reportGroupKey, compactReportGroups.value);
    }
    if (reportGroup) {
      reportGroup.open = true;
    }
    await nextTick();
  }
  scrollEvidenceIntoView(evidenceId);
  if (window.location.hash !== `#${evidenceId}`) {
    window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}#${evidenceId}`);
  }
}

function scrollEvidenceIntoView(evidenceId: string) {
  const target = document.getElementById(evidenceId);
  if (!target) {
    return;
  }
  const scrollHost = findVerticalScrollHost(target);
  if (scrollHost) {
    const hostRect = scrollHost.getBoundingClientRect();
    const targetRect = target.getBoundingClientRect();
    scrollHost.scrollTo({
      top: scrollHost.scrollTop + targetRect.top - hostRect.top - 12,
      behavior: 'smooth',
    });
    return;
  }
  const targetRect = target.getBoundingClientRect();
  window.scrollTo({
    top: window.scrollY + targetRect.top - 12,
    behavior: 'smooth',
  });
}

function findVerticalScrollHost(target: HTMLElement) {
  let element = target.parentElement;
  while (element) {
    const style = window.getComputedStyle(element);
    const canScroll = element.scrollHeight > element.clientHeight + 1;
    const scrollsY = ['auto', 'scroll', 'overlay'].includes(style.overflowY);
    if (canScroll && scrollsY) {
      return element;
    }
    element = element.parentElement;
  }
  return null;
}

function resetAdminData() {
  runtimeRequestSeq += 1;
  visitEventRuntimeRequestSeq += 1;
  overview.value = null;
  comparisonOverview.value = null;
  shortLinks.value = null;
  runtime.value = null;
  visitEventRuntime.value = null;
  aggregation.value = null;
  runtimeChecking.value = false;
  visitEventRuntimeChecking.value = false;
  runtimeWarning.value = '';
  visitEventRuntimeWarning.value = '';
  lastLoadedAt.value = '';
}

function applyFilter() {
  if (busy.value) {
    return;
  }
  shortLinkPage.value = 1;
  load(true);
}

function clearDateFilter() {
  if (busy.value) {
    return;
  }
  startDate.value = '';
  endDate.value = '';
  keyword.value = '';
  statSource.value = '';
  includeSynthetic.value = false;
  shortLinkPage.value = 1;
  load(true);
}

function applyQuickRange(days: number | null) {
  if (busy.value) {
    return;
  }
  if (days === null) {
    startDate.value = '';
    endDate.value = '';
  } else {
    const today = new Date();
    const start = new Date(today.getTime() - (days - 1) * MS_PER_DAY);
    startDate.value = formatInputDate(start);
    endDate.value = formatInputDate(today);
  }
  shortLinkPage.value = 1;
  load(true);
}

function changeShortLinkPageSize() {
  if (busy.value) {
    return;
  }
  shortLinkPage.value = 1;
  load(true);
}

function goShortLinkPage(page: number) {
  if (busy.value) {
    return;
  }
  const nextPage = Math.max(1, Math.min(shortLinkTotalPages.value, page));
  if (nextPage === shortLinkPage.value) {
    return;
  }
  shortLinkPage.value = nextPage;
  load(true);
}

async function checkExternalRuntime() {
  if (busy.value || runtimeChecking.value) {
    return;
  }
  await refreshRuntime(true);
}

async function refreshRuntime(forceRefresh: boolean) {
  const requestToken = token.value.trim();
  if (!requestToken) {
    runtime.value = null;
    runtimeWarning.value = '';
    return;
  }
  const requestSeq = ++runtimeRequestSeq;
  error.value = '';
  runtimeWarning.value = '';
  runtimeChecking.value = true;
  try {
    const runtimeData = await fetchExternalShortLinkRuntime(requestToken, forceRefresh);
    if (requestSeq === runtimeRequestSeq && token.value.trim() === requestToken && !error.value) {
      runtime.value = runtimeData;
    }
  } catch (err) {
    if (requestSeq === runtimeRequestSeq && token.value.trim() === requestToken) {
      runtime.value = null;
      runtimeWarning.value = err instanceof Error ? '不影响核心数据，可在调试区手动检查。' : '不影响核心数据，可稍后重试。';
    }
  } finally {
    if (requestSeq === runtimeRequestSeq) {
      runtimeChecking.value = false;
    }
  }
}

async function refreshVisitEventRuntime() {
  if (visitEventRuntimeChecking.value) {
    return;
  }
  const requestToken = token.value.trim();
  if (!requestToken) {
    visitEventRuntime.value = null;
    visitEventRuntimeWarning.value = '';
    return;
  }
  const requestSeq = ++visitEventRuntimeRequestSeq;
  visitEventRuntimeWarning.value = '';
  visitEventRuntimeChecking.value = true;
  try {
    const runtimeData = await fetchVisitEventRuntime(requestToken);
    if (requestSeq === visitEventRuntimeRequestSeq && token.value.trim() === requestToken && !error.value) {
      visitEventRuntime.value = runtimeData;
    }
  } catch (err) {
    if (requestSeq === visitEventRuntimeRequestSeq && token.value.trim() === requestToken) {
      visitEventRuntime.value = null;
      visitEventRuntimeWarning.value = err instanceof Error ? '不影响核心数据，可稍后刷新运行态。' : '不影响核心数据，可稍后重试。';
    }
  } finally {
    if (requestSeq === visitEventRuntimeRequestSeq) {
      visitEventRuntimeChecking.value = false;
    }
  }
}

async function refreshAggregation() {
  if (busy.value) {
    return;
  }
  error.value = '';
  aggregating.value = true;
  try {
    aggregation.value = await aggregateAdminAnalytics(token.value, dateFilter());
    overview.value = normalizeOverview(await fetchAdminOverview(token.value, { ...dateFilter(), forceRefresh: true }));
    comparisonOverview.value = await fetchComparisonOverview();
    lastLoadedAt.value = formatClock(new Date());
  } catch (err) {
    error.value = err instanceof Error ? err.message : '增长聚合刷新失败';
  } finally {
    aggregating.value = false;
  }
}

async function exportCsv() {
  if (busy.value || !shortLinks.value) {
    return;
  }
  error.value = '';
  exporting.value = true;
  try {
    const blob = await exportAdminShortLinks(token.value, shortLinkFilter());
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `wuxing-short-links-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '短链导出失败';
  } finally {
    exporting.value = false;
  }
}

async function copyOperationBriefing() {
  if (!operationBriefing.value || busy.value) {
    return;
  }
  try {
    await writeClipboard(operationBriefing.value.text);
    briefingCopied.value = true;
    window.setTimeout(() => {
      briefingCopied.value = false;
    }, 1600);
  } catch {
    error.value = '复盘摘要复制失败，请手动选中文本复制。';
  }
}

function dateFilter(): AdminDateFilter {
  return {
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    includeSynthetic: includeSynthetic.value,
  };
}

function comparisonDateFilter(): AdminDateFilter {
  return {
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    includeSynthetic: !includeSynthetic.value,
  };
}

async function fetchComparisonOverview() {
  try {
    return normalizeOverview(await fetchAdminOverview(token.value, comparisonDateFilter()));
  } catch {
    return null;
  }
}

function shortLinkFilter() {
  return {
    ...dateFilter(),
    includeSynthetic: includeSynthetic.value || undefined,
    keyword: keyword.value.trim() || undefined,
    statSource: statSource.value || undefined,
  };
}

function normalizeOverview(data: AdminOverview): AdminOverview {
  return {
    ...data,
    totalPv: data.totalPv ?? 0,
    totalUv: data.totalUv ?? 0,
    totalUip: data.totalUip ?? 0,
    homeViews: data.homeViews ?? 0,
    startClicks: data.startClicks ?? 0,
    testSubmits: data.testSubmits ?? 0,
    resultCreated: data.resultCreated ?? 0,
    shortLinkCreated: data.shortLinkCreated ?? 0,
    shortLinkVisits: data.shortLinkVisits ?? 0,
    completionRate: data.completionRate ?? 0,
    syntheticTrafficExcluded: data.syntheticTrafficExcluded ?? false,
    syntheticIsolationLevel: data.syntheticIsolationLevel ?? (data.syntheticTrafficExcluded ? 'event_channel' : 'all_traffic'),
    syntheticIsolationNote: data.syntheticIsolationNote ?? '',
    metricSource: data.metricSource ?? 'live_event',
    aggregatedThroughDate: data.aggregatedThroughDate ?? null,
    dailyTrends: data.dailyTrends ?? [],
    funnelSteps: data.funnelSteps ?? [],
    topChannels: data.topChannels ?? [],
    topCampaigns: data.topCampaigns ?? [],
    popularElementCombos: data.popularElementCombos ?? [],
    popularStarOfficers: data.popularStarOfficers ?? [],
    recentResults: data.recentResults ?? [],
    recentShortLinks: data.recentShortLinks ?? [],
  };
}

function normalizeShortLinks(data: PageResult<ShortLinkListItem>): PageResult<ShortLinkListItem> {
  return {
    ...data,
    total: data.total ?? 0,
    records: data.records ?? [],
  };
}

function detailQuery() {
  const query: Record<string, string> = {};
  if (startDate.value) {
    query.startDate = startDate.value;
  }
  if (endDate.value) {
    query.endDate = endDate.value;
  }
  if (includeSynthetic.value) {
    query.includeSynthetic = 'true';
  }
  if (keyword.value.trim()) {
    query.keyword = keyword.value.trim();
  }
  if (statSource.value) {
    query.statSource = statSource.value;
  }
  return query;
}

function statSourceLabel(source: ShortLinkListItem['statSource']) {
  return source === 'external' ? '外部' : '本地';
}

function runtimeReachableLabel(value: boolean | null) {
  if (value === true) {
    return '可达';
  }
  if (value === false) {
    return '不可达';
  }
  return '未探测';
}

function runtimeHealthLabel(value: VisitEventRuntime['healthStatus']) {
  if (value === 'danger') {
    return '危险';
  }
  if (value === 'watch') {
    return '关注';
  }
  return '正常';
}

function runtimeTone(value: VisitEventRuntime['healthStatus']): ActionTone {
  if (value === 'danger') {
    return 'danger';
  }
  if (value === 'watch') {
    return 'watch';
  }
  return 'good';
}

function metricSourceLabel(value: AdminOverview['metricSource'] | ShortLinkListItem['metricSource']) {
  if (value === 'daily_metric') {
    return '日聚合';
  }
  if (value === 'external') {
    return '外部统计';
  }
  if (value === 'mixed') {
    return '聚合 + 实时';
  }
  return '实时事件';
}

function isolationLevelLabel(value: string) {
  if (value === 'event_channel') {
    return '按事件 channel 排除';
  }
  if (value === 'all_traffic') {
    return '全量口径';
  }
  return '测试口径';
}

function percent(numerator: number, denominator: number, options: { compact?: boolean } = {}) {
  if (!denominator) {
    return '0%';
  }
  return rateLabel(Math.round((numerator * 1000) / denominator) / 10, options);
}

function numberPercent(numerator: number, denominator: number) {
  if (!denominator) {
    return 0;
  }
  return Math.round((numerator * 1000) / denominator) / 10;
}

function averagePerItem(numerator: number, denominator: number) {
  if (!denominator) {
    return 0;
  }
  return Math.round((numerator * 10) / denominator) / 10;
}

function perShortLinkLabel(value: number) {
  return `${value} 次/链接`;
}

function queueUsageLabel(value: VisitEventRuntime) {
  if (!value.queueCapacity) {
    return '0%';
  }
  return `${Math.round((value.queueSize * 1000) / value.queueCapacity) / 10}%`;
}

function maxCount(rows: { count: number }[]) {
  return Math.max(1, ...rows.map((item) => item.count));
}

function clampScore(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)));
}

function scoreTone(value: number): ActionTone {
  if (value >= 75) {
    return 'good';
  }
  if (value >= 45) {
    return 'watch';
  }
  return 'cold';
}

function journeyStepNote(label: string, previousLabel: string | undefined, retentionRate: number, inverted: boolean) {
  if (!previousLabel) {
    return `${label} 是本轮漏斗起点，用来判断后续每一步的保留情况。`;
  }
  if (inverted) {
    return `${label} 高于 ${previousLabel}，通常意味着补数、接口直调或事件口径不完全同源。`;
  }
  if (retentionRate >= 70) {
    return `${previousLabel} 到 ${label} 保留较好，当前不是最优先掉点。`;
  }
  if (retentionRate >= 40) {
    return `${previousLabel} 到 ${label} 有可见流失，适合结合页面停留和按钮位置继续看。`;
  }
  return `${previousLabel} 到 ${label} 流失明显，是优先排查的体验断点。`;
}

function barWidth(value: number, max: number) {
  return `${Math.max(2, Math.round((value * 1000) / Math.max(1, max)) / 10)}%`;
}

function rateLabel(value: number, options: { compact?: boolean } = {}) {
  if (value > 1000 && options.compact) {
    return '>1000%';
  }
  return `${value}%`;
}

function rateTone(value: number) {
  if (value > 1000) {
    return 'rate-danger';
  }
  if (value > 100) {
    return 'rate-watch';
  }
  return 'rate-normal';
}

function impactItem(label: string, allValue: number, realValue: number) {
  const delta = Math.max(0, allValue - realValue);
  return {
    label,
    delta,
    value: `+${delta}`,
    note: delta > 0 ? `占全量 ${percent(delta, allValue)}` : '无测试增量',
    tone: delta > 0 ? 'watch' : 'good',
  };
}

function formatDateTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function formatInputDate(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatClock(value: Date) {
  const hours = String(value.getHours()).padStart(2, '0');
  const minutes = String(value.getMinutes()).padStart(2, '0');
  const seconds = String(value.getSeconds()).padStart(2, '0');
  return `${formatInputDate(value)} ${hours}:${minutes}:${seconds}`;
}

function deltaLabel(value: number | null) {
  if (value === null) {
    return '-';
  }
  if (value > 0) {
    return `+${value}`;
  }
  return String(value);
}

function deltaTone(value: number | null) {
  if (value === null || value === 0) {
    return 'delta-flat';
  }
  return value > 0 ? 'delta-up' : 'delta-down';
}

async function writeClipboard(value: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const textarea = document.createElement('textarea');
  textarea.value = value;
  textarea.setAttribute('readonly', 'true');
  textarea.style.position = 'fixed';
  textarea.style.left = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  textarea.remove();
}

async function resetAdminHorizontalScroll() {
  await nextTick();
  requestAnimationFrame(() => {
    const container = document.querySelector<HTMLElement>('.admin-desktop-page');
    if (container) {
      container.scrollLeft = 0;
    }
  });
}
</script>

<template>
  <main class="page admin-desktop-page">
    <section class="shell stack admin-page">
      <div class="panel stack admin-hero">
        <div class="admin-hero-head">
          <div>
            <p class="eyebrow">运营观察台</p>
            <h2>数据中台</h2>
            <p class="muted">电脑端运营看板，优先观察访问、完成、分享和回流；窄屏会自动折叠为可阅读布局。</p>
          </div>
          <span class="admin-state" :class="{ active: overview }">{{ overview ? '已连接' : '待登录' }}</span>
        </div>
        <div class="admin-token">
          <input v-model="token" type="password" placeholder="输入管理 token" :disabled="busy" />
          <button data-testid="admin-login-button" type="button" :disabled="busy" @click="load()">{{ loading ? '加载中...' : '进入后台' }}</button>
        </div>
        <div class="quick-range-bar" aria-label="快捷日期筛选">
          <span>快捷筛选</span>
          <button
            v-for="item in quickDateRanges"
            :key="item.label"
            class="secondary"
            type="button"
            :disabled="busy"
            @click="applyQuickRange(item.days)"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="filter-bar" :aria-busy="busy">
          <label>
            开始日期
            <span class="date-input-shell" :class="{ empty: !startDate }" data-placeholder="选择开始日期">
              <input v-model="startDate" type="date" aria-label="开始日期，格式 YYYY-MM-DD" :disabled="busy" />
            </span>
          </label>
          <label>
            结束日期
            <span class="date-input-shell" :class="{ empty: !endDate }" data-placeholder="选择结束日期">
              <input v-model="endDate" type="date" aria-label="结束日期，格式 YYYY-MM-DD" :disabled="busy" />
            </span>
          </label>
          <label>
            短码 / 结果
            <input v-model="keyword" type="search" placeholder="输入短码或结果 ID" :disabled="busy" />
          </label>
          <label>
            来源
            <select v-model="statSource" :disabled="busy">
              <option value="">全部</option>
              <option value="local">本地</option>
              <option value="external">外部</option>
            </select>
          </label>
          <label class="toggle-row">
            <input v-model="includeSynthetic" type="checkbox" :disabled="busy" />
            <span>
              包含测试流量
              <small>未勾选则排除 perf-test</small>
            </span>
          </label>
          <button type="button" :disabled="busy" @click="applyFilter">应用筛选</button>
          <button class="secondary" type="button" :disabled="busy" @click="clearDateFilter">清空</button>
          <button data-testid="admin-export-csv" class="secondary" type="button" :disabled="busy || !shortLinks" @click="exportCsv">
            {{ exporting ? '导出中...' : '导出 CSV' }}
          </button>
        </div>
        <div class="scope-status" aria-label="当前数据口径">
          <span>{{ dataScopeStatus.rangeLabel }}</span>
          <span>{{ dataScopeStatus.trafficLabel }}</span>
          <span>来源：{{ dataScopeStatus.sourceLabel }}</span>
          <span>{{ dataScopeStatus.aggregateLabel }}</span>
          <span>{{ dataScopeStatus.refreshedLabel }}</span>
        </div>
        <p v-if="busy" class="muted admin-busy" role="status" aria-live="polite">正在处理当前请求，请稍候。</p>
        <p v-else-if="runtimeChecking" class="muted admin-busy" role="status" aria-live="polite">正在检查外部短链调试信息，核心数据可继续查看。</p>
        <p v-if="runtimeWarning" class="muted admin-busy" role="status" aria-live="polite">外部短链调试信息暂不可用：{{ runtimeWarning }}</p>
        <p v-if="visitEventRuntimeWarning" class="muted admin-busy" role="status" aria-live="polite">
          访问事件运行态暂不可用：{{ visitEventRuntimeWarning }}
        </p>
        <p v-if="error" class="error-text" role="alert" aria-live="polite">{{ error }}</p>
      </div>

      <template v-if="overview">
        <div class="focus-grid" aria-label="运营核心观察">
          <article
            v-for="item in focusMetrics"
            :key="item.label"
            class="focus-card"
            :class="`tone-${item.tone}`"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.note }}</small>
          </article>
        </div>

        <nav v-if="evidenceIndex.length" class="panel evidence-nav" aria-label="证据索引">
          <a
            v-for="item in evidenceIndex"
            :key="item.id"
            class="evidence-link"
            :class="`tone-${item.tone}`"
            :href="`#${item.id}`"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.note }}</small>
          </a>
        </nav>

        <div
          v-if="syntheticImpact"
          id="synthetic-section"
          class="impact-band"
          :class="{ quiet: !syntheticImpact.hasImpact }"
          aria-label="测试流量影响诊断"
        >
          <div class="impact-copy">
            <span>口径差异</span>
            <strong>{{ syntheticImpact.title }}</strong>
            <p>{{ syntheticImpact.note }}</p>
          </div>
          <div class="impact-items">
            <span
              v-for="item in syntheticImpact.items"
              :key="item.label"
              class="impact-item"
              :class="`tone-${item.tone}`"
            >
              <small>{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
              <em>{{ item.note }}</em>
            </span>
          </div>
        </div>

        <section class="panel stack action-panel" aria-label="风险与行动建议">
          <div class="section-head">
            <div>
              <h2>风险与行动建议</h2>
              <p class="muted">把当前筛选范围内的口径、分享、回流和运行态信号合并成处理优先级。</p>
            </div>
            <span class="chart-legend">{{ actionItems.length }} 条建议</span>
          </div>
          <div class="action-list">
            <div
              v-for="item in actionItems"
              :key="`${item.level}-${item.title}`"
              class="action-row"
              :class="`tone-${item.tone}`"
            >
              <span class="action-level">{{ item.level }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.detail }}</p>
              </div>
              <div class="action-next">
                <em>{{ item.action }}</em>
                <a class="text-link" :href="`#${item.evidenceId}`" @click="locateEvidence($event, item.evidenceId)">
                  定位{{ item.evidenceLabel }}
                </a>
              </div>
            </div>
          </div>
        </section>

        <section v-if="operationBriefing" class="panel stack briefing-panel" aria-label="复盘摘要">
          <div class="section-head">
            <div>
              <h2>复盘摘要</h2>
              <p class="muted">按当前筛选范围自动整理，可直接用于阶段汇报或压测复盘。</p>
            </div>
            <button class="secondary" type="button" :disabled="busy" @click="copyOperationBriefing">
              {{ briefingCopied ? '已复制' : '复制摘要' }}
            </button>
          </div>
          <div class="briefing-lines">
            <p v-for="line in operationBriefing.lines" :key="line">{{ line }}</p>
          </div>
        </section>

        <div class="mobile-report-shell" :class="{ open: mobileReportOpen }">
          <div class="mobile-report-gate" aria-label="详细报表折叠入口">
            <div>
              <span>详细报表</span>
              <strong>趋势、漏斗、排行和短链列表</strong>
              <p>移动端先看核心判断，需要排查时再展开完整运营数据。</p>
            </div>
            <button
              data-testid="admin-mobile-report-toggle"
              class="secondary"
              type="button"
              :aria-expanded="mobileReportOpen"
              @click="mobileReportOpen = !mobileReportOpen"
            >
              {{ mobileReportOpen ? '收起' : '展开' }}
            </button>
          </div>
          <div class="mobile-report-content">
            <details
              class="mobile-report-group"
              data-report-group="core"
              :open="reportGroupOpen('core')"
            >
              <summary data-testid="admin-mobile-report-group-core" @click.prevent="toggleMobileReportGroup('core')">
                <span>指标与链路</span>
                <small>核心指标、运营判断和转化诊断</small>
              </summary>
              <div class="mobile-report-group-body">
        <div class="stats-grid" aria-label="关键指标口径">
          <StatCard label="总 PV" :value="overview.totalPv" note="当前筛选范围内的事件总次数" />
          <StatCard label="总 UV" :value="overview.totalUv" note="按匿名 clientId hash 去重" />
          <StatCard label="总 UIP" :value="overview.totalUip" note="按脱敏 IP hash 去重" />
          <StatCard label="首页访问" :value="overview.homeViews" note="用户进入产品首屏的次数" />
          <StatCard label="开始点击" :value="overview.startClicks" note="点击开始测试的行为次数" />
          <StatCard label="提交次数" :value="overview.testSubmits" note="测试页发起提交的尝试次数" />
          <StatCard label="结果生成" :value="overview.resultCreated" note="后端成功生成结果的次数" />
          <StatCard label="短链生成" :value="overview.shortLinkCreated" note="结果绑定专属短码的次数" />
          <StatCard label="短链访问" :value="overview.shortLinkVisits" note="/s/{code} 被打开的回流次数" />
          <StatCard
            label="完成率"
            :value="rateLabel(overview.completionRate)"
            :note="overview.completionRate > 100 ? '结果生成数 / 开始点击数 · 需排除压测或接口直调' : '结果生成数 / 开始点击数'"
          />
        </div>

        <div class="ops-grid" aria-label="运营诊断">
          <article
            v-for="item in operationInsights"
            :key="item.label"
            class="ops-card"
            :class="`tone-${item.tone}`"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.detail }}</p>
          </article>
        </div>

        <section class="panel stack radar-panel" aria-label="运营雷达">
          <div class="section-head">
            <div>
              <h2>运营雷达</h2>
              <p class="muted">把完成、分享、回流和数据口径压成 0-100 观察值，方便电脑端快速扫盘。</p>
            </div>
            <span class="chart-legend">0-100 观察值</span>
          </div>
          <div class="radar-grid">
            <article
              v-for="item in operationRadar"
              :key="item.label"
              class="radar-item"
              :class="`tone-${item.tone}`"
            >
              <div class="radar-copy">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <em>{{ item.score }}</em>
              </div>
              <div class="radar-track">
                <i :style="{ width: `${item.score}%` }"></i>
              </div>
              <p>{{ item.detail }}</p>
            </article>
          </div>
        </section>

        <section id="journey-section" class="panel stack journey-panel" aria-label="转化链路诊断">
          <div class="section-head">
            <div>
              <h2>转化链路诊断</h2>
              <p class="muted">按相邻步骤看保留率和流失数，快速定位从进入、开始、提交到分享回流的断点。</p>
            </div>
            <span class="chart-legend">相邻步骤保留</span>
          </div>
          <div v-if="journeySteps.length" class="journey-grid">
            <article
              v-for="item in journeySteps"
              :key="item.eventType"
              class="journey-card"
              :class="`tone-${item.tone}`"
            >
              <div class="journey-topline">
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </div>
              <div class="journey-meta">
                <small>保留 {{ item.retentionLabel }}</small>
                <small>流失 {{ item.dropLabel }}</small>
              </div>
              <p>{{ item.note }}</p>
              <code>{{ item.eventType }}</code>
            </article>
          </div>
          <p v-else class="muted empty-state">暂无漏斗数据。</p>
        </section>
              </div>
            </details>

            <details
              class="mobile-report-group"
              data-report-group="trend"
              :open="reportGroupOpen('trend')"
            >
              <summary data-testid="admin-mobile-report-group-trend" @click.prevent="toggleMobileReportGroup('trend')">
                <span>趋势与运行态</span>
                <small>趋势图、漏斗表和服务健康信息</small>
              </summary>
              <div class="mobile-report-group-body">
        <div class="visual-grid">
          <section id="trend-section" class="panel stack chart-panel">
            <div class="section-head">
              <div>
                <h2>趋势视图</h2>
                <p class="muted">PV、结果生成和短链访问的同屏对比。</p>
              </div>
              <span class="chart-legend">PV / 结果 / 回流</span>
            </div>
            <div v-if="overview.dailyTrends.length" class="trend-chart" aria-label="日趋势图">
              <div v-for="item in overview.dailyTrends" :key="item.date" class="trend-day">
                <div class="trend-bars">
                  <span class="bar pv" :style="{ height: barWidth(item.pv, dailyTrendMax) }"></span>
                  <span class="bar result" :style="{ height: barWidth(item.resultCreated, dailyTrendMax) }"></span>
                  <span class="bar share" :style="{ height: barWidth(item.shortLinkVisits, dailyTrendMax) }"></span>
                </div>
                <small>{{ item.date.slice(5) }}</small>
              </div>
            </div>
            <p v-else class="muted empty-state">暂无趋势数据。</p>
          </section>

          <section id="funnel-section" class="panel stack chart-panel">
            <h2>漏斗强弱</h2>
            <p class="muted">用横条直观看每一步掉点，适合快速判断哪一段最需要优化。</p>
            <div v-if="overview.funnelSteps.length" class="funnel-bars">
              <div v-for="item in overview.funnelSteps" :key="item.eventType" class="funnel-bar-row">
                <span>{{ item.label }}</span>
                <div class="funnel-track">
                  <i :style="{ width: barWidth(item.count, funnelMax) }"></i>
                </div>
                <strong>{{ item.count }}</strong>
                <small :class="rateTone(item.conversionRate)">{{ rateLabel(item.conversionRate, { compact: true }) }}</small>
              </div>
            </div>
          </section>

          <section class="panel stack chart-panel">
            <h2>当前页统计来源结构</h2>
            <p class="muted">按当前短链列表页区分本地统计和外部短链统计，避免混淆数据口径。</p>
            <div class="source-meter">
              <span :style="{ width: `${sourceMix.localPercent}%` }">本地 {{ sourceMix.local }}</span>
              <span :style="{ width: `${sourceMix.externalPercent}%` }">外部 {{ sourceMix.external }}</span>
            </div>
            <div class="source-notes">
              <span>本地 {{ sourceMix.localPercent }}%</span>
              <span>外部 {{ sourceMix.externalPercent }}%</span>
            </div>
          </section>
        </div>

        <details v-if="runtime" id="external-shortlink-section" class="panel stack debug-panel">
          <summary>
            <span>外部短链调试信息</span>
            <small>
              {{ runtime.mode }} · {{ runtimeReachableLabel(runtime.reachable) }} ·
              {{ runtime.fallbackToInternal ? '可自动降级' : '不降级' }}
            </small>
          </summary>
          <div class="section-head">
            <p class="muted">用于排查外部短链服务连通性，日常运营优先看上方核心指标。</p>
            <button class="secondary" type="button" :disabled="busy || runtimeChecking" @click="checkExternalRuntime">
              {{ runtimeChecking ? '检查中...' : '检查' }}
            </button>
          </div>
          <div class="runtime-grid">
            <span>模式：{{ runtime.mode }}</span>
            <span>统计：{{ runtime.statsEnabled ? '开启' : '关闭' }}</span>
            <span>降级：{{ runtime.fallbackToInternal ? '开启' : '关闭' }}</span>
            <span>可达性：{{ runtimeReachableLabel(runtime.reachable) }}</span>
            <span>域名：{{ runtime.domain }}</span>
            <span>分组：{{ runtime.groupId }}</span>
            <span>状态码：{{ runtime.httpStatus ?? '-' }}</span>
            <span>信息：{{ runtime.message }}</span>
          </div>
        </details>

        <details v-if="visitEventRuntime" id="runtime-section" class="panel stack debug-panel">
          <summary>
            <span>访问事件运行态</span>
            <small>
              {{ runtimeHealthLabel(visitEventRuntime.healthStatus) }} ·
              {{ visitEventRuntime.asyncMode }} ·
              本地队列 {{ visitEventRuntime.queueSize }}/{{ visitEventRuntime.queueCapacity }} ·
              {{ visitEventRuntime.workerAlive ? 'writer 正常' : 'writer 异常' }}
            </small>
          </summary>
          <div class="section-head">
            <p class="muted">
              <strong class="health-pill" :class="`health-${visitEventRuntime.healthStatus}`">
                {{ runtimeHealthLabel(visitEventRuntime.healthStatus) }}
              </strong>
              {{ visitEventRuntime.healthMessage }}
            </p>
            <button
              class="secondary"
              type="button"
              :disabled="busy || visitEventRuntimeChecking"
              @click="refreshVisitEventRuntime"
            >
              {{ visitEventRuntimeChecking ? '刷新中...' : '刷新' }}
            </button>
          </div>
          <div class="runtime-grid">
            <span>模式：{{ visitEventRuntime.asyncMode }}</span>
            <span>队列：{{ visitEventRuntime.queueSize }}/{{ visitEventRuntime.queueCapacity }}</span>
            <span>批量阈值：{{ visitEventRuntime.drainLimit }}</span>
            <span>已落库：{{ visitEventRuntime.totalFlushedEvents }}</span>
            <span>最近批量：{{ visitEventRuntime.lastBatchSize }}</span>
            <span>丢弃事件：{{ visitEventRuntime.droppedAsyncEvents }}</span>
            <span>写入失败：{{ visitEventRuntime.batchWriteFailures }}</span>
            <span>最近落库：{{ visitEventRuntime.lastFlushAt ?? '-' }}</span>
            <span>MQ 可用：{{ visitEventRuntime.rocketMqAvailable ? '是' : '否' }}</span>
            <span>MQ Topic：{{ visitEventRuntime.rocketMqTopic }}</span>
            <span>MQ 发布：{{ visitEventRuntime.rocketMqPublishedEvents }}</span>
            <span>MQ 失败：{{ visitEventRuntime.rocketMqPublishFailures }}</span>
            <span>MQ 回退：{{ visitEventRuntime.rocketMqFallbackEvents }}</span>
            <span>Shadow 本地：{{ visitEventRuntime.rocketMqShadowLocalEvents }}</span>
            <span>本地回退：{{ visitEventRuntime.rocketMqFallbackToLocal ? '开启' : '关闭' }}</span>
            <span>MQ 主消费：{{ visitEventRuntime.rocketMqConsumerEnabled ? '开启' : '未开启' }}</span>
            <span>MQ 落库就绪：{{ visitEventRuntime.rocketMqConsumerPersistenceReady ? '是' : '否' }}</span>
          </div>
        </details>

        <div class="panel stack">
          <h2>增长漏斗</h2>
          <p class="muted">按当前日期筛选统计，转化率为相邻步骤转化。</p>
          <div class="table-wrap">
            <table class="compact-table">
              <thead>
                <tr>
                  <th>步骤</th>
                  <th>次数</th>
                  <th>转化率</th>
                  <th>埋点代码</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in overview.funnelSteps" :key="item.eventType">
                  <td>{{ item.label }}</td>
                  <td>{{ item.count }}</td>
                  <td>
                    <span class="rate-chip" :class="rateTone(item.conversionRate)">
                      {{ rateLabel(item.conversionRate, { compact: true }) }}
                    </span>
                  </td>
                  <td><code class="debug-code">{{ item.eventType }}</code></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel stack">
          <div class="section-head">
            <div>
              <h2>日趋势</h2>
              <p class="muted">
                来源：{{ metricSourceLabel(overview.metricSource) }}
                <span v-if="overview.aggregatedThroughDate">，已聚合至 {{ overview.aggregatedThroughDate }}</span>
              </p>
            </div>
            <button class="secondary" type="button" :disabled="busy" @click="refreshAggregation">
              {{ aggregating ? '聚合中...' : '刷新聚合' }}
            </button>
          </div>
          <p v-if="aggregation" class="muted">
            最近聚合：{{ aggregation.startDate }} 至 {{ aggregation.endDate }}，
            {{ aggregation.daysAggregated }} 天，短链明细 {{ aggregation.shortLinkRowsAggregated }} 行。
          </p>
          <div class="table-wrap">
            <table class="compact-table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>PV</th>
                  <th>ΔPV</th>
                  <th>结果</th>
                  <th>Δ结果</th>
                  <th>短链生成</th>
                  <th>短链访问</th>
                  <th>Δ回流</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in dailyTrendRows" :key="item.date">
                  <td>{{ item.date }}</td>
                  <td>{{ item.pv }}</td>
                  <td><span class="delta-chip" :class="deltaTone(item.pvDelta)">{{ deltaLabel(item.pvDelta) }}</span></td>
                  <td>{{ item.resultCreated }}</td>
                  <td><span class="delta-chip" :class="deltaTone(item.resultDelta)">{{ deltaLabel(item.resultDelta) }}</span></td>
                  <td>{{ item.shortLinkCreated }}</td>
                  <td>{{ item.shortLinkVisits }}</td>
                  <td>
                    <span class="delta-chip" :class="deltaTone(item.shortLinkVisitDelta)">
                      {{ deltaLabel(item.shortLinkVisitDelta) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
              </div>
            </details>

            <details
              class="mobile-report-group"
              data-report-group="attribution"
              :open="reportGroupOpen('attribution')"
            >
              <summary data-testid="admin-mobile-report-group-attribution" @click.prevent="toggleMobileReportGroup('attribution')">
                <span>归因与短链</span>
                <small>渠道排行、结果分布和短链列表</small>
              </summary>
              <div class="mobile-report-group-body">
        <div class="panel stack">
          <div class="section-head">
            <div>
              <h2>五行人格分布</h2>
              <p class="muted">观察用户最终落在哪些主副元素组合，判断内容是否足够均衡。</p>
            </div>
            <span class="chart-legend">按结果生成统计</span>
          </div>
          <div v-if="overview.popularElementCombos.length" class="combo-visuals">
            <div v-for="item in overview.popularElementCombos" :key="item.name" class="combo-row">
              <span>{{ item.name }}</span>
              <div class="combo-track">
                <i :style="{ width: barWidth(item.count, elementComboMax) }"></i>
              </div>
              <strong>{{ item.count }}</strong>
            </div>
          </div>
          <p v-else class="muted empty-state">暂无五行组合数据。</p>
        </div>

        <div id="attribution-section" class="insight-grid">
          <div class="panel stack">
            <h2>Top Channel</h2>
            <div v-if="overview.topChannels.length" class="rank-bars">
              <div class="rank-row visual-rank" v-for="item in overview.topChannels" :key="item.name">
                <span>{{ item.name }}</span>
                <i :style="{ width: barWidth(item.count, channelMax) }"></i>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无渠道数据</p>
          </div>

          <div class="panel stack">
            <h2>Top Campaign</h2>
            <div v-if="overview.topCampaigns.length" class="rank-bars">
              <div class="rank-row visual-rank" v-for="item in overview.topCampaigns" :key="item.name">
                <span>{{ item.name }}</span>
                <i :style="{ width: barWidth(item.count, campaignMax) }"></i>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无活动数据</p>
          </div>

          <div class="panel stack">
            <h2>热门五行组合</h2>
            <div v-if="overview.popularElementCombos.length">
              <div class="rank-row" v-for="item in overview.popularElementCombos" :key="item.name">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无数据</p>
          </div>

          <div class="panel stack">
            <h2>热门星官</h2>
            <div v-if="overview.popularStarOfficers.length">
              <div class="rank-row" v-for="item in overview.popularStarOfficers" :key="item.name">
                <span>{{ item.name }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <p v-else class="muted">暂无数据</p>
          </div>

          <div class="panel stack">
            <h2>最近结果</h2>
            <div v-if="overview.recentResults.length" class="table-wrap">
              <table class="compact-table">
                <thead>
                  <tr>
                    <th>结果</th>
                    <th>组合</th>
                    <th>星官</th>
                    <th>创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in overview.recentResults" :key="item.resultId">
                    <td>{{ item.resultId }}</td>
                    <td>{{ item.elementCombo }}</td>
                    <td>{{ item.starOfficerName }}</td>
                    <td>{{ formatDateTime(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="muted empty-state">当前筛选范围内暂无结果生成记录。</p>
          </div>

          <div class="panel stack">
            <h2>最近短链</h2>
            <div v-if="overview.recentShortLinks.length" class="table-wrap">
              <table class="compact-table">
                <thead>
                  <tr>
                    <th>短码</th>
                    <th>结果</th>
                    <th>PV</th>
                    <th>来源</th>
                    <th>口径</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in overview.recentShortLinks" :key="item.shortCode">
                    <td>{{ item.shortCode }}</td>
                    <td>{{ item.resultId }}</td>
                    <td>
                      <span class="mini-pv">
                        <i :style="{ width: barWidth(item.pv, recentShortLinkMax) }"></i>
                        {{ item.pv }}
                      </span>
                    </td>
                    <td>{{ statSourceLabel(item.statSource) }}</td>
                    <td>{{ metricSourceLabel(item.metricSource) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="muted empty-state">当前筛选范围内暂无短链生成记录。</p>
          </div>
        </div>

        <div id="shortlink-section" class="panel stack">
          <div class="section-head short-link-head">
            <div>
              <h2>短链列表</h2>
              <p class="muted">
                当前筛选共 {{ shortLinks?.total ?? 0 }} 条，正在查看
                {{ shortLinkStartIndex }}-{{ shortLinkEndIndex }} 条，来源筛选会分页扫描完整范围。
              </p>
            </div>
            <div class="pager-tools">
              <label>
                每页
                <select v-model.number="shortLinkPageSize" :disabled="busy" @change="changeShortLinkPageSize">
                  <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
                </select>
              </label>
              <span>第 {{ shortLinkPage }} / {{ shortLinkTotalPages }} 页</span>
              <button
                class="secondary"
                type="button"
                :disabled="busy || shortLinkPage <= 1"
                @click="goShortLinkPage(shortLinkPage - 1)"
              >
                上一页
              </button>
              <button
                class="secondary"
                type="button"
                :disabled="busy || shortLinkPage >= shortLinkTotalPages"
                @click="goShortLinkPage(shortLinkPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>
          <div class="shortlink-evidence-strip" aria-label="短链跳转证据摘要">
            <article v-for="item in shortLinkEvidenceCards" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.note }}</small>
            </article>
          </div>
          <div v-if="shortLinks?.records.length" class="shortlink-records">
            <div class="table-wrap shortlink-table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>短码</th>
                    <th>短链</th>
                    <th>结果</th>
                    <th>组合</th>
                    <th>星官</th>
                    <th>PV</th>
                    <th>UV</th>
                    <th>UIP</th>
                    <th>来源</th>
                    <th>口径</th>
                    <th>详情</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in shortLinks?.records" :key="item.shortCode">
                    <td>{{ item.shortCode }}</td>
                    <td>
                      <a class="shortlink-url" :href="item.shortUrl" target="_blank" rel="noopener noreferrer">
                        打开
                      </a>
                    </td>
                    <td>{{ item.resultId }}</td>
                    <td>{{ item.elementCombo }}</td>
                    <td>{{ item.starOfficerName }}</td>
                    <td>{{ item.pv }}</td>
                    <td>{{ item.uv }}</td>
                    <td>{{ item.uip }}</td>
                    <td>{{ statSourceLabel(item.statSource) }}</td>
                    <td>{{ metricSourceLabel(item.metricSource) }}</td>
                    <td>
                      <RouterLink
                        class="detail-link"
                        :to="{ path: `/admin/short-links/${item.shortCode}`, query: detailQuery() }"
                      >
                        查看
                      </RouterLink>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="shortlink-mobile-list" data-testid="admin-shortlink-mobile-list">
              <article v-for="item in shortLinks?.records" :key="`mobile-${item.shortCode}`" class="shortlink-mobile-card">
                <div class="shortlink-mobile-card-head">
                  <div>
                    <span class="shortlink-code">{{ item.shortCode }}</span>
                    <small>{{ item.resultId }}</small>
                  </div>
                  <RouterLink
                    class="detail-link shortlink-mobile-detail"
                    :to="{ path: `/admin/short-links/${item.shortCode}`, query: detailQuery() }"
                  >
                    查看
                  </RouterLink>
                </div>
                <div class="shortlink-mobile-metrics" aria-label="短链访问指标">
                  <span><strong>{{ item.pv }}</strong><small>PV</small></span>
                  <span><strong>{{ item.uv }}</strong><small>UV</small></span>
                  <span><strong>{{ item.uip }}</strong><small>UIP</small></span>
                </div>
                <dl class="shortlink-mobile-meta">
                  <div>
                    <dt>短链</dt>
                    <dd>
                      <a class="shortlink-url" :href="item.shortUrl" target="_blank" rel="noopener noreferrer">
                        {{ item.shortUrl }}
                      </a>
                    </dd>
                  </div>
                  <div>
                    <dt>命盘</dt>
                    <dd>{{ item.elementCombo || '未记录' }} · {{ item.starOfficerName || '未记录' }}</dd>
                  </div>
                  <div>
                    <dt>来源</dt>
                    <dd>{{ statSourceLabel(item.statSource) }}</dd>
                  </div>
                  <div>
                    <dt>口径</dt>
                    <dd>{{ metricSourceLabel(item.metricSource) }}</dd>
                  </div>
                </dl>
              </article>
            </div>
          </div>
          <p v-else class="muted empty-state">没有匹配当前筛选条件的短链。</p>
          <div v-if="shortLinks?.records.length" class="pager-footer">
            <span>{{ shortLinkStartIndex }}-{{ shortLinkEndIndex }} / {{ shortLinks.total }}</span>
            <div>
              <button
                class="secondary"
                type="button"
                :disabled="busy || shortLinkPage <= 1"
                @click="goShortLinkPage(1)"
              >
                首页
              </button>
              <button
                class="secondary"
                type="button"
                :disabled="busy || shortLinkPage <= 1"
                @click="goShortLinkPage(shortLinkPage - 1)"
              >
                上一页
              </button>
              <button
                class="secondary"
                type="button"
                :disabled="busy || shortLinkPage >= shortLinkTotalPages"
                @click="goShortLinkPage(shortLinkPage + 1)"
              >
                下一页
              </button>
              <button
                class="secondary"
                type="button"
                :disabled="busy || shortLinkPage >= shortLinkTotalPages"
                @click="goShortLinkPage(shortLinkTotalPages)"
              >
                末页
              </button>
            </div>
          </div>
        </div>
              </div>
            </details>
          </div>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.admin-desktop-page {
  overflow-x: auto;
  padding: 24px;
}

.admin-page {
  gap: 18px;
  width: min(1440px, 100%);
  min-width: 1120px;
  max-width: none;
}

.admin-hero {
  overflow: hidden;
  border-color: rgba(36, 48, 47, 0.1);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(237, 247, 242, 0.86)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.08), rgba(215, 155, 67, 0.1));
}

.admin-hero-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
}

.admin-hero {
  display: grid;
  grid-template-columns: minmax(340px, 0.72fr) minmax(0, 1fr);
  gap: 18px;
}

.admin-hero-head,
.admin-token,
.quick-range-bar,
.filter-bar,
.scope-status,
.admin-busy,
.error-text {
  grid-column: 1 / -1;
}

.eyebrow {
  margin: 0 0 6px;
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.admin-hero h2 {
  margin: 0;
  color: #24302f;
  font-size: 30px;
}

.admin-state {
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 999px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.72);
  color: #6a7774;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.admin-state.active {
  border-color: rgba(47, 111, 94, 0.22);
  background: #edf7f2;
  color: #2f6f5e;
}

.admin-token,
.filter-bar {
  display: grid;
  gap: 10px;
}

.admin-token {
  grid-template-columns: minmax(220px, 1fr) auto;
}

.quick-range-bar,
.scope-status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.quick-range-bar {
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.62);
}

.quick-range-bar span {
  color: #596764;
  font-size: 12px;
  font-weight: 950;
}

.quick-range-bar button {
  min-height: 44px;
  padding: 0 12px;
}

.scope-status {
  border-top: 1px solid rgba(36, 48, 47, 0.08);
  padding-top: 10px;
}

.scope-status span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  border: 1px solid rgba(47, 111, 94, 0.12);
  border-radius: 999px;
  padding: 0 10px;
  background: rgba(237, 247, 242, 0.72);
  color: #40514e;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.filter-bar {
  grid-template-columns: 180px 180px minmax(220px, 1fr) 150px 132px repeat(3, auto);
  align-items: end;
}

.filter-bar label {
  display: grid;
  gap: 6px;
  color: #596764;
  font-size: 12px;
  font-weight: 850;
}

.filter-bar .toggle-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.78);
  color: #40514e;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.filter-bar .toggle-row input {
  width: 16px;
  height: 16px;
}

.filter-bar .toggle-row span {
  display: grid;
  gap: 2px;
  line-height: 1.2;
}

.filter-bar .toggle-row small {
  color: #7a8783;
  font-size: 11px;
  font-weight: 800;
}

.focus-grid,
.stats-grid,
.insight-grid,
.ops-grid,
.visual-grid,
.runtime-grid {
  display: grid;
  gap: 12px;
}

.focus-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.evidence-nav {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  border-color: rgba(36, 48, 47, 0.08);
  padding: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(250, 252, 249, 0.94)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.05), rgba(215, 155, 67, 0.05));
}

.evidence-link {
  display: grid;
  gap: 5px;
  min-height: 92px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.82);
  text-align: left;
  text-decoration: none;
  box-shadow: none;
}

.evidence-link:hover {
  transform: translateY(-1px);
}

.evidence-link span {
  color: #596764;
  font-size: 12px;
  font-weight: 950;
}

.evidence-link strong {
  color: #24302f;
  font-size: 20px;
  line-height: 1.15;
}

.evidence-link small {
  color: #6a7774;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.35;
}

.evidence-link.tone-good {
  border-color: rgba(47, 111, 94, 0.18);
}

.evidence-link.tone-watch {
  border-color: rgba(215, 155, 67, 0.22);
}

.evidence-link.tone-danger {
  border-color: rgba(184, 91, 72, 0.22);
}

.evidence-link.tone-cold {
  border-color: rgba(93, 120, 144, 0.18);
}

.focus-card {
  display: grid;
  gap: 8px;
  min-height: 126px;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 28px rgba(31, 48, 43, 0.08);
}

.focus-card span {
  color: #596764;
  font-size: 13px;
  font-weight: 900;
}

.focus-card strong {
  color: #24302f;
  font-size: 32px;
  line-height: 1;
}

.focus-card small {
  color: #6a7774;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.focus-card.tone-good {
  border-color: rgba(47, 111, 94, 0.24);
  background: linear-gradient(180deg, #ffffff, #edf7f2);
}

.focus-card.tone-watch {
  border-color: rgba(215, 155, 67, 0.28);
  background: linear-gradient(180deg, #ffffff, #fff7e8);
}

.focus-card.tone-cold {
  border-color: rgba(184, 91, 72, 0.24);
  background: linear-gradient(180deg, #ffffff, #fff0ed);
}

.impact-band {
  display: grid;
  grid-template-columns: minmax(260px, 0.36fr) minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
  border: 1px solid rgba(215, 155, 67, 0.24);
  border-radius: 8px;
  padding: 14px;
  background: #fffaf0;
}

.impact-band.quiet {
  border-color: rgba(47, 111, 94, 0.16);
  background: #f4fbf7;
}

.impact-copy {
  display: grid;
  gap: 5px;
}

.impact-copy span {
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.impact-copy strong {
  color: #24302f;
  font-size: 18px;
  line-height: 1.3;
}

.impact-copy p {
  margin: 0;
  color: #596764;
  font-size: 13px;
  font-weight: 760;
  line-height: 1.5;
}

.impact-items {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.impact-item {
  display: grid;
  gap: 4px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.78);
}

.impact-item small,
.impact-item em {
  color: #6a7774;
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
  line-height: 1.25;
}

.impact-item strong {
  color: #24302f;
  font-size: 22px;
  line-height: 1;
}

.impact-item.tone-watch strong {
  color: #a46518;
}

.impact-item.tone-good strong {
  color: #2f6f5e;
}

.action-panel {
  border-color: rgba(36, 48, 47, 0.1);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(250, 252, 249, 0.94)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.06), rgba(215, 155, 67, 0.06));
}

.action-list {
  display: grid;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
}

.action-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) minmax(260px, 0.52fr);
  gap: 14px;
  align-items: center;
  min-height: 76px;
  border-top: 1px solid rgba(36, 48, 47, 0.08);
  padding: 14px;
}

.action-row:first-child {
  border-top: 0;
}

.action-level {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  min-height: 30px;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.08);
  color: #40514e;
  font-size: 12px;
  font-weight: 950;
}

.action-row strong {
  display: block;
  color: #24302f;
  font-size: 16px;
  line-height: 1.35;
}

.action-row p,
.action-row em {
  margin: 4px 0 0;
  color: #596764;
  font-size: 13px;
  font-style: normal;
  font-weight: 760;
  line-height: 1.45;
}

.action-row em {
  margin: 0;
  color: #40514e;
  font-weight: 850;
}

.action-next {
  display: grid;
  gap: 8px;
  justify-items: start;
}

.text-link {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  border: 0;
  padding: 0;
  background: transparent;
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 950;
  text-decoration: underline;
  text-underline-offset: 3px;
  box-shadow: none;
}

.text-link:hover {
  transform: none;
  color: #1f5c4e;
}

.action-row.tone-danger .action-level {
  background: rgba(184, 91, 72, 0.14);
  color: #b85b48;
}

.action-row.tone-watch .action-level {
  background: rgba(215, 155, 67, 0.16);
  color: #9b6d32;
}

.action-row.tone-good .action-level {
  background: rgba(47, 111, 94, 0.12);
  color: #2f6f5e;
}

.action-row.tone-cold .action-level {
  background: rgba(93, 120, 144, 0.14);
  color: #5d7890;
}

.briefing-panel {
  border-color: rgba(93, 120, 144, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 250, 0.94)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.06), rgba(93, 120, 144, 0.07));
}

.briefing-lines {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.briefing-lines p {
  min-height: 58px;
  margin: 0;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.82);
  color: #40514e;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
}

.stats-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

#synthetic-section,
#journey-section,
#trend-section,
#funnel-section,
#external-shortlink-section,
#runtime-section,
#attribution-section,
#shortlink-section {
  scroll-margin-top: 18px;
}

.ops-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.ops-card {
  display: grid;
  gap: 8px;
  min-height: 132px;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.ops-card span {
  color: #9b6d32;
  font-size: 12px;
  font-weight: 950;
}

.ops-card strong {
  color: #24302f;
  font-size: 17px;
  line-height: 1.35;
}

.ops-card p {
  margin: 0;
  color: #596764;
  font-size: 13px;
  font-weight: 760;
  line-height: 1.55;
}

.ops-card.tone-good {
  background: linear-gradient(180deg, #ffffff, #edf7f2);
}

.ops-card.tone-watch {
  background: linear-gradient(180deg, #ffffff, #fff7e8);
}

.ops-card.tone-cold {
  background: linear-gradient(180deg, #ffffff, #fff0ed);
}

.radar-panel {
  border-color: rgba(47, 111, 94, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 247, 0.94)),
    linear-gradient(90deg, rgba(93, 120, 144, 0.06), rgba(215, 155, 67, 0.06));
}

.radar-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.radar-item {
  display: grid;
  gap: 10px;
  border: 1px solid rgba(36, 48, 47, 0.09);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.86);
}

.radar-copy {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 10px;
  align-items: end;
}

.radar-copy span {
  grid-column: 1 / -1;
  color: #6a7774;
  font-size: 12px;
  font-weight: 950;
}

.radar-copy strong {
  color: #24302f;
  font-size: 20px;
  line-height: 1.2;
}

.radar-copy em {
  color: #596764;
  font-size: 18px;
  font-style: normal;
  font-weight: 950;
  line-height: 1;
}

.radar-track {
  overflow: hidden;
  height: 12px;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.08);
}

.radar-track i {
  display: block;
  min-width: 4px;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #5d7890, #2f6f5e);
}

.radar-item.tone-watch .radar-track i {
  background: linear-gradient(90deg, #d79b43, #e4bd70);
}

.radar-item.tone-cold .radar-track i {
  background: linear-gradient(90deg, #b85b48, #d98d7b);
}

.radar-item p {
  margin: 0;
  color: #596764;
  font-size: 12px;
  font-weight: 760;
  line-height: 1.45;
}

.journey-panel {
  border-color: rgba(93, 120, 144, 0.14);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 250, 251, 0.94)),
    linear-gradient(90deg, rgba(47, 111, 94, 0.05), rgba(93, 120, 144, 0.07));
}

.journey-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.journey-card {
  display: grid;
  gap: 10px;
  align-content: start;
  min-height: 172px;
  border: 1px solid rgba(36, 48, 47, 0.1);
  border-radius: 8px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.88);
}

.journey-topline {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: baseline;
}

.journey-topline span {
  color: #40514e;
  font-size: 13px;
  font-weight: 950;
  line-height: 1.25;
}

.journey-topline strong {
  color: #24302f;
  font-size: 24px;
  line-height: 1;
}

.journey-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.journey-meta small {
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 7px 8px;
  background: rgba(250, 252, 249, 0.8);
  color: #596764;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.2;
  white-space: nowrap;
}

.journey-card p {
  margin: 0;
  color: #596764;
  font-size: 12px;
  font-weight: 760;
  line-height: 1.45;
}

.journey-card code {
  align-self: end;
  color: #6a7774;
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.journey-card.tone-good {
  background: linear-gradient(180deg, #ffffff, #edf7f2);
}

.journey-card.tone-watch {
  background: linear-gradient(180deg, #ffffff, #fff7e8);
}

.journey-card.tone-cold {
  background: linear-gradient(180deg, #ffffff, #fff0ed);
}

.visual-grid {
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1.1fr) minmax(280px, 0.72fr);
  align-items: stretch;
}

.chart-panel {
  min-height: 292px;
}

.chart-legend {
  color: #6a7774;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  min-height: 196px;
  align-items: end;
  border-top: 1px solid rgba(36, 48, 47, 0.08);
  padding-top: 12px;
}

.trend-day {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.trend-day small {
  color: #6a7774;
  font-size: 11px;
  font-weight: 900;
  text-align: center;
}

.trend-bars {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3px;
  height: 150px;
  align-items: end;
}

.bar {
  min-height: 3px;
  border-radius: 6px 6px 2px 2px;
}

.bar.pv {
  background: #2f6f5e;
}

.bar.result {
  background: #d79b43;
}

.bar.share {
  background: #5d7890;
}

.funnel-bars,
.combo-visuals,
.rank-bars {
  display: grid;
  gap: 9px;
}

.funnel-bar-row,
.combo-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 52px 44px;
  gap: 10px;
  align-items: center;
  color: #40514e;
  font-size: 13px;
  font-weight: 850;
}

.combo-row {
  grid-template-columns: 120px minmax(0, 1fr) 48px;
}

.funnel-track,
.combo-track {
  overflow: hidden;
  height: 10px;
  border-radius: 999px;
  background: rgba(36, 48, 47, 0.08);
}

.funnel-track i,
.combo-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2f6f5e, #d79b43);
}

.funnel-bar-row strong,
.combo-row strong {
  color: #24302f;
  text-align: right;
}

.funnel-bar-row small {
  color: #6a7774;
  font-weight: 900;
  text-align: right;
}

.rate-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: 999px;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 950;
}

.rate-normal {
  color: #2f6f5e;
}

.rate-watch {
  color: #9b6d32;
}

.rate-danger {
  color: #b85b48;
}

.rate-chip.rate-normal {
  background: rgba(47, 111, 94, 0.1);
}

.rate-chip.rate-watch {
  background: rgba(215, 155, 67, 0.16);
}

.rate-chip.rate-danger {
  background: rgba(184, 91, 72, 0.14);
}

.delta-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  min-height: 24px;
  border-radius: 999px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 950;
}

.delta-up {
  background: rgba(47, 111, 94, 0.1);
  color: #2f6f5e;
}

.delta-down {
  background: rgba(184, 91, 72, 0.12);
  color: #b85b48;
}

.delta-flat {
  background: rgba(36, 48, 47, 0.07);
  color: #6a7774;
}

.source-meter {
  display: flex;
  overflow: hidden;
  min-height: 56px;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  background: rgba(36, 48, 47, 0.06);
}

.source-meter span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 0 8px;
  color: #fff;
  font-size: 12px;
  font-weight: 950;
  white-space: nowrap;
}

.source-meter span:first-child {
  background: #2f6f5e;
}

.source-meter span:last-child {
  background: #5d7890;
}

.source-notes {
  display: flex;
  justify-content: space-between;
  color: #596764;
  font-size: 12px;
  font-weight: 850;
}

.insight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.insight-grid > .panel:nth-last-child(-n + 2) {
  grid-column: span 3;
}

.section-head,
.rank-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.short-link-head {
  align-items: end;
}

.shortlink-evidence-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.shortlink-evidence-strip article {
  min-width: 0;
  border: 1px solid rgba(36, 48, 47, 0.08);
  border-radius: 8px;
  padding: 13px 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(250, 252, 249, 0.82)),
    #fff;
}

.shortlink-evidence-strip span,
.shortlink-evidence-strip strong,
.shortlink-evidence-strip small {
  display: block;
}

.shortlink-evidence-strip span {
  color: #687572;
  font-size: 12px;
  font-weight: 900;
}

.shortlink-evidence-strip strong {
  overflow-wrap: anywhere;
  margin-top: 6px;
  color: #24302f;
  font-size: 22px;
  font-weight: 950;
  line-height: 1.12;
}

.shortlink-evidence-strip small {
  margin-top: 7px;
  color: #6a7774;
  font-size: 12px;
  font-weight: 820;
  line-height: 1.35;
}

.pager-tools,
.pager-footer,
.pager-footer > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager-tools {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pager-tools label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #596764;
  font-size: 12px;
  font-weight: 900;
}

.pager-tools select {
  min-width: 72px;
  height: 36px;
  border-radius: 8px;
  padding: 0 8px;
}

.pager-tools span,
.pager-footer span {
  color: #596764;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.pager-footer {
  justify-content: space-between;
  border-top: 1px solid rgba(36, 48, 47, 0.08);
  padding-top: 12px;
}

.rank-row {
  min-height: 36px;
  border-bottom: 1px solid rgba(36, 48, 47, 0.08);
  color: #40514e;
  font-weight: 800;
}

.rank-row:last-child {
  border-bottom: 0;
}

.rank-row strong {
  color: #24302f;
}

.visual-rank {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 56px;
  isolation: isolate;
}

.visual-rank i {
  position: absolute;
  inset: auto auto 0 0;
  z-index: -1;
  height: 40%;
  border-radius: 999px;
  background: rgba(47, 111, 94, 0.14);
}

.mini-pv {
  display: grid;
  grid-template-columns: 58px auto;
  gap: 8px;
  align-items: center;
  min-width: 94px;
}

.mini-pv i {
  display: block;
  height: 8px;
  border-radius: 999px;
  background: #2f6f5e;
}

.runtime-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  color: #40514e;
  font-size: 13px;
  font-weight: 800;
}

.health-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  margin-right: 8px;
  border: 1px solid rgba(36, 48, 47, 0.12);
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 12px;
  font-weight: 950;
  white-space: nowrap;
}

.health-ok {
  border-color: rgba(47, 111, 94, 0.25);
  background: #edf7f2;
  color: #2f6f5e;
}

.health-watch {
  border-color: rgba(215, 155, 67, 0.32);
  background: #fff7e8;
  color: #8a5f18;
}

.health-danger {
  border-color: rgba(184, 91, 72, 0.3);
  background: #fff0ed;
  color: #9b3f2c;
}

.debug-panel summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #24302f;
  font-weight: 950;
  cursor: pointer;
}

.debug-panel small {
  color: #6a7774;
  font-size: 12px;
}

.table-wrap {
  overflow-x: auto;
}

.shortlink-mobile-list {
  display: none;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border-bottom: 1px solid rgba(36, 48, 47, 0.08);
  padding: 10px 12px;
  color: #40514e;
  text-align: left;
  white-space: nowrap;
}

th {
  color: #24302f;
  font-size: 12px;
  font-weight: 950;
}

.compact-table th,
.compact-table td {
  padding: 9px 10px;
  font-size: 13px;
}

.detail-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  border-radius: 8px;
  color: #2f6f5e;
  font-weight: 900;
  text-decoration: none;
}

.detail-link:hover {
  background: rgba(47, 111, 94, 0.08);
}

.shortlink-url {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 44px;
  color: #2f6f5e;
  font-size: 12px;
  font-weight: 900;
  line-height: 1.45;
  overflow-wrap: anywhere;
  text-decoration: none;
}

.shortlink-url:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.shortlink-code {
  color: #24302f;
  font-size: 16px;
  font-weight: 950;
  letter-spacing: 0;
}

.debug-code {
  color: #6a4a22;
  font-size: 12px;
}

.empty-state,
.admin-busy {
  margin: 0;
}

.mobile-report-shell,
.mobile-report-content,
.mobile-report-group,
.mobile-report-group-body {
  display: contents;
}

.mobile-report-gate,
.mobile-report-group > summary {
  display: none;
}

@media (max-width: 1180px) {
  .admin-page {
    margin: 0;
    min-width: 1040px;
  }

  .filter-bar {
    grid-template-columns: 150px 150px minmax(180px, 1fr) 132px repeat(3, auto);
  }
}

@media (max-width: 760px) {
  .admin-desktop-page {
    overflow-x: hidden;
    padding: 10px;
  }

  .admin-page {
    width: 100%;
    min-width: 0;
    margin: 0 auto;
    gap: 10px;
  }

  .admin-page > .panel,
  .admin-hero {
    padding: 14px;
  }

  .admin-hero h2 {
    font-size: 24px;
  }

  .admin-hero,
  .admin-token,
  .filter-bar,
  .impact-band,
  .action-row,
  .briefing-lines,
  .visual-grid,
  .insight-grid,
  .runtime-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .admin-hero-head,
  .section-head,
  .short-link-head,
  .pager-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-hero-head {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .admin-state {
    justify-self: start;
  }

  .quick-range-bar,
  .scope-status,
  .pager-tools,
  .pager-footer > div {
    justify-content: flex-start;
  }

  .shortlink-evidence-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-range-bar,
  .scope-status {
    gap: 6px;
  }

  .quick-range-bar button {
    min-height: 44px;
    padding: 0 10px;
    font-size: 12px;
  }

  .filter-bar button,
  .admin-token button,
  .pager-footer button {
    width: 100%;
  }

  .pager-footer > div {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .filter-bar input:not([type='checkbox']),
  .filter-bar select {
    font-size: 14px;
    font-weight: 850;
  }

  .filter-bar .toggle-row {
    font-size: 13px;
  }

  .text-link {
    min-height: 44px;
  }

  .shortlink-table-wrap {
    display: none;
  }

  .shortlink-mobile-list {
    display: grid;
    gap: 10px;
  }

  .shortlink-mobile-card {
    display: grid;
    gap: 10px;
    border: 1px solid rgba(36, 48, 47, 0.1);
    border-radius: 8px;
    padding: 12px;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 246, 240, 0.9)),
      rgba(255, 255, 255, 0.92);
  }

  .shortlink-mobile-card-head {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
    align-items: center;
  }

  .shortlink-mobile-card-head small {
    display: block;
    overflow: hidden;
    margin-top: 3px;
    color: #6a7774;
    font-size: 11px;
    font-weight: 850;
    line-height: 1.35;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .shortlink-mobile-detail {
    border: 1px solid rgba(47, 111, 94, 0.16);
    background: #f3fbf7;
  }

  .shortlink-mobile-metrics {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }

  .shortlink-mobile-metrics span {
    min-width: 0;
    border-radius: 8px;
    padding: 8px 9px;
    background: rgba(47, 111, 94, 0.07);
  }

  .shortlink-mobile-metrics strong,
  .shortlink-mobile-metrics small {
    display: block;
    line-height: 1.2;
  }

  .shortlink-mobile-metrics strong {
    color: #24302f;
    font-size: 17px;
    font-weight: 950;
  }

  .shortlink-mobile-metrics small {
    margin-top: 2px;
    color: #6a7774;
    font-size: 11px;
    font-weight: 900;
  }

  .shortlink-mobile-meta {
    display: grid;
    gap: 6px;
    margin: 0;
  }

  .shortlink-mobile-meta div {
    display: grid;
    grid-template-columns: 42px minmax(0, 1fr);
    gap: 8px;
    align-items: start;
  }

  .shortlink-mobile-meta dt {
    color: #87908e;
    font-size: 11px;
    font-weight: 900;
  }

  .shortlink-mobile-meta dd {
    overflow-wrap: anywhere;
    margin: 0;
    color: #40514e;
    font-size: 12px;
    font-weight: 850;
    line-height: 1.45;
  }

  .focus-grid,
  .evidence-nav,
  .impact-items,
  .stats-grid,
  .ops-grid,
  .radar-grid,
  .journey-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .focus-card {
    min-height: 92px;
    gap: 6px;
    padding: 12px;
  }

  .focus-card strong {
    font-size: 24px;
  }

  .focus-card small {
    font-size: 11px;
    line-height: 1.35;
  }

  .evidence-nav {
    display: none;
  }

  .mobile-report-shell {
    display: block;
  }

  .mobile-report-gate {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin: 0;
    padding: 14px;
    border: 1px solid rgba(36, 48, 47, 0.1);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.92);
    box-shadow: 0 10px 24px rgba(37, 48, 47, 0.06);
  }

  .mobile-report-gate span {
    display: block;
    margin-bottom: 4px;
    color: #2f6f5e;
    font-size: 12px;
    font-weight: 950;
  }

  .mobile-report-gate strong {
    display: block;
    color: #24302f;
    font-size: 16px;
    line-height: 1.25;
  }

  .mobile-report-gate p {
    margin: 6px 0 0;
    color: #6b7875;
    font-size: 12px;
    line-height: 1.45;
  }

  .mobile-report-gate button {
    flex: 0 0 auto;
    min-width: 76px;
    min-height: 44px;
  }

  .mobile-report-content {
    display: none;
  }

  .mobile-report-shell.open .mobile-report-content {
    display: block;
    margin-top: 10px;
  }

  .mobile-report-content > * + * {
    margin-top: 10px;
  }

  .mobile-report-group {
    display: block;
    overflow: hidden;
    border: 1px solid rgba(36, 48, 47, 0.1);
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 10px 22px rgba(37, 48, 47, 0.05);
  }

  .mobile-report-group + .mobile-report-group {
    margin-top: 10px;
  }

  .mobile-report-group > summary {
    display: flex;
    min-height: 48px;
    cursor: pointer;
    list-style: none;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 12px 14px;
    color: #24302f;
    font-weight: 950;
  }

  .mobile-report-group > summary::-webkit-details-marker {
    display: none;
  }

  .mobile-report-group > summary::after {
    content: '+';
    display: grid;
    width: 24px;
    height: 24px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 999px;
    background: rgba(47, 111, 94, 0.1);
    color: #2f6f5e;
    font-size: 16px;
    line-height: 1;
  }

  .mobile-report-group[open] > summary::after {
    content: '-';
  }

  .mobile-report-group > summary span,
  .mobile-report-group > summary small {
    display: block;
  }

  .mobile-report-group > summary span {
    min-width: 76px;
    font-size: 15px;
    line-height: 1.25;
  }

  .mobile-report-group > summary small {
    margin-top: 2px;
    color: #6b7875;
    font-size: 11px;
    font-weight: 800;
    line-height: 1.35;
  }

  .mobile-report-group-body {
    display: block;
    padding: 0 10px 10px;
  }

  .mobile-report-group-body > * + * {
    margin-top: 10px;
  }

  .stats-grid :deep(.stat-card) {
    gap: 4px;
    min-height: 78px;
    padding: 11px;
  }

  .stats-grid :deep(.stat-card strong) {
    font-size: 21px;
    line-height: 1.1;
  }

  .stats-grid :deep(.stat-card small) {
    display: none;
  }

  .ops-card,
  .radar-item,
  .journey-card {
    gap: 7px;
    min-height: auto;
    padding: 12px;
  }

  .ops-card strong {
    font-size: 15px;
    line-height: 1.3;
  }

  .ops-card p,
  .radar-item p,
  .journey-card p {
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    font-size: 12px;
    line-height: 1.42;
  }

  .radar-copy strong,
  .journey-topline strong {
    font-size: 20px;
  }

  .journey-meta {
    grid-template-columns: minmax(0, 1fr);
  }

  .journey-meta small {
    padding: 6px 8px;
  }

  .evidence-link {
    min-height: 82px;
  }

  .impact-band {
    gap: 12px;
  }

  .action-row {
    min-height: auto;
  }

  .action-level {
    width: fit-content;
  }

  .action-next {
    justify-items: stretch;
  }

  .insight-grid > .panel:nth-last-child(-n + 2) {
    grid-column: auto;
  }

  .table-wrap table {
    min-width: 760px;
  }
}

@media (max-width: 430px) {
  .ops-grid,
  .radar-grid,
  .journey-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .focus-grid,
  .evidence-nav,
  .impact-items,
  .shortlink-evidence-strip,
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shortlink-evidence-strip {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
