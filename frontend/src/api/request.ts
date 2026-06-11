import { getClientId } from '../utils/clientId';
import { getAttribution } from '../utils/attribution';
import { getSessionId } from '../utils/sessionId';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

interface RequestOptions extends RequestInit {
  adminToken?: string;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  headers.set('X-Client-Id', getClientId());
  headers.set('X-Session-Id', getSessionId());
  const attribution = getAttribution();
  if (attribution.channel) {
    headers.set('X-Channel', attribution.channel);
  }
  if (attribution.campaign) {
    headers.set('X-Campaign', attribution.campaign);
  }
  if (options.adminToken) {
    headers.set('X-Admin-Token', options.adminToken);
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });
  const payload = (await response.json()) as ApiResponse<T>;
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `Request failed: ${response.status}`);
  }
  return payload.data;
}
