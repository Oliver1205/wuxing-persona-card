import { recordEvent } from '../api/events';

export function track(eventType: string, pagePath?: string, resultId?: string | null, shortCode?: string | null) {
  recordEvent({ eventType, pagePath, resultId, shortCode }).catch(() => {
    // Tracking failure must not block the user's main flow.
  });
}
