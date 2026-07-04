import { recordEvent } from '../api/events';
import {
  recordAnalyticsEvent,
  sendAnalyticsBeacon,
  sendAnalyticsHeartbeat,
  startAnalyticsSession,
} from '../api/analytics';
import type { Router } from 'vue-router';

const HEARTBEAT_INTERVAL_MS = 30000;
let analyticsInitialized = false;
let heartbeatTimer: number | undefined;
let latestPath = '/';

export function track(eventType: string, pagePath?: string, resultId?: string | null, shortCode?: string | null) {
  recordEvent({ eventType, pagePath, resultId, shortCode }).catch(() => {
    // Tracking failure must not block the user's main flow.
  });
}

export function initAnalytics(router: Router) {
  if (analyticsInitialized || typeof window === 'undefined') {
    return;
  }
  analyticsInitialized = true;
  latestPath = currentPath();
  startAnalyticsSession(latestPath).catch(ignoreTrackingError);
  recordAnalyticsEvent({ eventName: 'page_view', path: latestPath }).catch(ignoreTrackingError);

  router.afterEach((to) => {
    latestPath = to.fullPath || to.path || currentPath();
    recordAnalyticsEvent({ eventName: 'page_view', path: latestPath }).catch(ignoreTrackingError);
    sendAnalyticsHeartbeat(latestPath).catch(ignoreTrackingError);
  });

  heartbeatTimer = window.setInterval(() => {
    sendAnalyticsHeartbeat(latestPath).catch(ignoreTrackingError);
  }, HEARTBEAT_INTERVAL_MS);

  window.addEventListener('pagehide', () => {
    if (heartbeatTimer) {
      window.clearInterval(heartbeatTimer);
      heartbeatTimer = undefined;
    }
    sendAnalyticsBeacon(latestPath);
  });
}

function currentPath() {
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

function ignoreTrackingError() {
  // Analytics must never block the user's main flow.
}
