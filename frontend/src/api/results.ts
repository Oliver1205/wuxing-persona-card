import { request } from './request';
import type { CreateResultRequest, ResultDetail } from './types';

export function createResult(payload: CreateResultRequest) {
  return request<ResultDetail>('/api/results', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchResult(resultId: string) {
  return request<ResultDetail>(`/api/results/${encodeURIComponent(resultId)}`);
}
