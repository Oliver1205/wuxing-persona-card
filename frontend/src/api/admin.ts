import { request } from './request';
import type { AdminOverview, PageResult, ShortLinkListItem, ShortLinkVisit } from './types';

export interface AdminDateFilter {
  startDate?: string;
  endDate?: string;
}

export function fetchAdminOverview(token: string, filter: AdminDateFilter = {}) {
  return request<AdminOverview>(`/api/admin/overview${toQuery(filter)}`, { adminToken: token });
}

export function fetchAdminShortLinks(token: string, page = 1, pageSize = 20, filter: AdminDateFilter = {}) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  appendDateFilter(params, filter);
  return request<PageResult<ShortLinkListItem>>(
    `/api/admin/short-links?${params.toString()}`,
    { adminToken: token },
  );
}

export function fetchShortLinkVisits(
  token: string,
  shortCode: string,
  page = 1,
  pageSize = 20,
  filter: AdminDateFilter = {},
) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  appendDateFilter(params, filter);
  return request<PageResult<ShortLinkVisit>>(
    `/api/admin/short-links/${encodeURIComponent(shortCode)}/visits?${params.toString()}`,
    { adminToken: token },
  );
}

function toQuery(filter: AdminDateFilter) {
  const params = new URLSearchParams();
  appendDateFilter(params, filter);
  const query = params.toString();
  return query ? `?${query}` : '';
}

function appendDateFilter(params: URLSearchParams, filter: AdminDateFilter) {
  if (filter.startDate) {
    params.set('startDate', filter.startDate);
  }
  if (filter.endDate) {
    params.set('endDate', filter.endDate);
  }
}
