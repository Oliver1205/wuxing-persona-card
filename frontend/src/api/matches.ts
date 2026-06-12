import { request } from './request';
import type { CreateMatchRequest, MatchCandidate, MatchResult } from './types';

export function fetchMatchCandidate(shortCode: string) {
  return request<MatchCandidate>(`/api/matches/candidates/${encodeURIComponent(shortCode)}`);
}

export function createMatch(payload: CreateMatchRequest) {
  return request<MatchResult>('/api/matches', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function fetchMatch(partnerShortCode: string, currentShortCode: string) {
  return request<MatchResult>(
    `/api/matches/${encodeURIComponent(partnerShortCode)}/${encodeURIComponent(currentShortCode)}`,
  );
}
