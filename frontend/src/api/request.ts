import { getClientId } from '../utils/clientId';

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
