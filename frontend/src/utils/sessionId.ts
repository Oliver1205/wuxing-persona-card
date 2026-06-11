import { createAnonymousId } from './clientId';

const SESSION_ID_KEY = 'wuxing_session_id';

export function getSessionId(): string {
  const existing = sessionStorage.getItem(SESSION_ID_KEY);
  if (existing) {
    return existing;
  }
  const id = createAnonymousId();
  sessionStorage.setItem(SESSION_ID_KEY, id);
  return id;
}
