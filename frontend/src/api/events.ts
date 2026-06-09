import { request } from './request';

export function recordEvent(payload: {
  eventType: string;
  pagePath?: string;
  resultId?: string | null;
  shortCode?: string | null;
}) {
  return request<void>('/api/events', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
