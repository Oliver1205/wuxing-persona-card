import { apiUrl, request } from './request';
import type {
  AdminOverview,
  AnalyticsAggregation,
  ExternalShortLinkRuntime,
  PageResult,
  ShortLinkListItem,
  ShortLinkVisit,
  VisitEventRuntime,
} from './types';

export interface AdminDateFilter {
  startDate?: string;
  endDate?: string;
  includeSynthetic?: boolean;
}

export interface AdminOverviewFilter extends AdminDateFilter {
  forceRefresh?: boolean;
}

export interface AdminShortLinkFilter extends AdminDateFilter {
  keyword?: string;
  statSource?: 'local' | 'external' | '';
}

export interface AdminShortLinkVisitFilter extends AdminDateFilter {
  statSource?: 'local' | 'external' | '';
}

export function fetchAdminOverview(token: string, filter: AdminOverviewFilter = {}) {
  return request<AdminOverview>(`/api/admin/overview${toQuery(filter)}`, { adminToken: token });
}

export function fetchAdminShortLinks(token: string, page = 1, pageSize = 20, filter: AdminShortLinkFilter = {}) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  appendShortLinkFilter(params, filter);
  return request<PageResult<ShortLinkListItem>>(
    `/api/admin/short-links?${params.toString()}`,
    { adminToken: token },
  );
}

export async function exportAdminShortLinks(token: string, filter: AdminShortLinkFilter = {}) {
  const response = await fetch(apiUrl(`/api/admin/short-links/export${toShortLinkQuery(filter)}`), {
    headers: {
      'X-Admin-Token': token,
    },
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new Error(payload?.message || '短链导出失败');
  }
  return response.blob();
}

export function fetchExternalShortLinkRuntime(token: string, probe = false) {
  return request<ExternalShortLinkRuntime>(
    `/api/admin/external-shortlink/status?probe=${String(probe)}`,
    { adminToken: token },
  );
}

export function fetchVisitEventRuntime(token: string) {
  return request<VisitEventRuntime>('/api/admin/visit-events/runtime', { adminToken: token });
}

export function aggregateAdminAnalytics(token: string, filter: AdminDateFilter = {}) {
  return request<AnalyticsAggregation>(
    `/api/admin/analytics/aggregate${toQuery(filter)}`,
    { method: 'POST', adminToken: token },
  );
}

export function fetchShortLinkVisits(
  token: string,
  shortCode: string,
  page = 1,
  pageSize = 20,
  filter: AdminShortLinkVisitFilter = {},
) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  appendDateFilter(params, filter);
  if (filter.statSource) {
    params.set('statSource', filter.statSource);
  }
  return request<PageResult<ShortLinkVisit>>(
    `/api/admin/short-links/${encodeURIComponent(shortCode)}/visits?${params.toString()}`,
    { adminToken: token },
  );
}

function toQuery(filter: AdminOverviewFilter) {
  const params = new URLSearchParams();
  appendDateFilter(params, filter);
  if (filter.forceRefresh) {
    params.set('forceRefresh', 'true');
  }
  const query = params.toString();
  return query ? `?${query}` : '';
}

function toShortLinkQuery(filter: AdminShortLinkFilter) {
  const params = new URLSearchParams();
  appendShortLinkFilter(params, filter);
  const query = params.toString();
  return query ? `?${query}` : '';
}

function appendShortLinkFilter(params: URLSearchParams, filter: AdminShortLinkFilter) {
  appendDateFilter(params, filter);
  if (filter.keyword) {
    params.set('keyword', filter.keyword);
  }
  if (filter.statSource) {
    params.set('statSource', filter.statSource);
  }
}

function appendDateFilter(params: URLSearchParams, filter: AdminDateFilter) {
  if (filter.startDate) {
    params.set('startDate', filter.startDate);
  }
  if (filter.endDate) {
    params.set('endDate', filter.endDate);
  }
  if (filter.includeSynthetic) {
    params.set('includeSynthetic', 'true');
  }
}
