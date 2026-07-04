import { apiUrl, request } from './request';
import { getAttribution } from '../utils/attribution';
import { getClientId } from '../utils/clientId';
import { getSessionId } from '../utils/sessionId';
import type { MetricTimeseries, RecentMetricEvent, RealtimeMetrics } from './types';

interface SessionPayload {
  visitorId: string;
  sessionId: string;
  path: string;
}

export function startAnalyticsSession(path: string) {
  return request<void>('/api/analytics/session/start', {
    method: 'POST',
    body: JSON.stringify(sessionPayload(path)),
  });
}

export function sendAnalyticsHeartbeat(path: string) {
  return request<void>('/api/analytics/heartbeat', {
    method: 'POST',
    body: JSON.stringify(sessionPayload(path)),
  });
}

export function endAnalyticsSession(path: string) {
  return request<void>('/api/analytics/session/end', {
    method: 'POST',
    body: JSON.stringify(sessionPayload(path)),
  });
}

export function recordAnalyticsEvent(payload: {
  eventName: string;
  path?: string;
  resultId?: string | null;
  shortCode?: string | null;
}) {
  return request<void>('/api/analytics/event', {
    method: 'POST',
    body: JSON.stringify({
      visitorId: getClientId(),
      sessionId: getSessionId(),
      path: payload.path ?? window.location.pathname,
      eventName: payload.eventName,
      resultId: payload.resultId,
      shortCode: payload.shortCode,
    }),
  });
}

export function sendAnalyticsBeacon(path: string) {
  if (!navigator.sendBeacon) {
    void endAnalyticsSession(path);
    return;
  }
  const attribution = getAttribution();
  const blob = new Blob([JSON.stringify({
    ...sessionPayload(path),
    channel: attribution.channel,
    campaign: attribution.campaign,
  })], { type: 'application/json' });
  navigator.sendBeacon(apiUrl('/api/analytics/session/end'), blob);
}

export function fetchRealtimeMetrics(token: string) {
  return request<RealtimeMetrics>('/api/admin/metrics/realtime', { adminToken: token });
}

export function fetchMetricTimeseries(token: string, range = '1h') {
  return request<MetricTimeseries>(
    `/api/admin/metrics/timeseries?range=${encodeURIComponent(range)}`,
    { adminToken: token },
  );
}

export function fetchRecentMetricEvents(token: string, range = '24h') {
  return request<RecentMetricEvent[]>(
    `/api/admin/metrics/events?range=${encodeURIComponent(range)}`,
    { adminToken: token },
  );
}

function sessionPayload(path: string): SessionPayload {
  return {
    visitorId: getClientId(),
    sessionId: getSessionId(),
    path,
  };
}
