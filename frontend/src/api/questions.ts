import { request } from './request';
import type { Question } from './types';

export function fetchQuestions() {
  return request<Question[]>('/api/questions');
}
