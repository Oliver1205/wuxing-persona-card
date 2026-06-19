import { getClientId } from '../utils/clientId';
import { getAttribution } from '../utils/attribution';
import { getSessionId } from '../utils/sessionId';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export function apiUrl(path: string) {
  const requestPath = path.startsWith('/') ? path : `/${path}`;
  const baseUrl = API_BASE_URL.replace(/\/+$/, '');
  return baseUrl ? `${baseUrl}${requestPath}` : requestPath;
}

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
  const requestUrl = apiUrl(path);
  const response = await fetch(requestUrl, {
    ...options,
    headers,
  });
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    const responseText = await response.text();
    const responseSnippet = responseText.replace(/\s+/g, ' ').slice(0, 160);
    throw new Error(
      response.ok
        ? `接口返回格式异常：${requestUrl} 未返回 JSON`
        : `接口请求失败：${requestUrl} 返回 ${response.status}${response.statusText ? ` ${response.statusText}` : ''}${responseSnippet ? `，响应片段：${responseSnippet}` : ''}`,
    );
  }
  const payload = (await response.json()) as ApiResponse<T>;
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `Request failed: ${response.status}`);
  }
  return payload.data;
}
