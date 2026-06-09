import { request } from './request';
import type { AdminOverview, PageResult, ShortLinkListItem, ShortLinkVisit } from './types';

export function fetchAdminOverview(token: string) {
  return request<AdminOverview>('/api/admin/overview', { adminToken: token });
}

export function fetchAdminShortLinks(token: string, page = 1, pageSize = 20) {
  return request<PageResult<ShortLinkListItem>>(
    `/api/admin/short-links?page=${page}&pageSize=${pageSize}`,
    { adminToken: token },
  );
}

export function fetchShortLinkVisits(token: string, shortCode: string, page = 1, pageSize = 20) {
  return request<PageResult<ShortLinkVisit>>(
    `/api/admin/short-links/${encodeURIComponent(shortCode)}/visits?page=${page}&pageSize=${pageSize}`,
    { adminToken: token },
  );
}
