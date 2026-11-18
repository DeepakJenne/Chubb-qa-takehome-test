import { readFileSync } from 'node:fs';
import path from 'node:path';

/** Repo-level contract files shared with the backend integration tests. */
const SHARED_DIRECTORY = path.resolve(__dirname, '../../../../qa/shared');

export const loadSharedCases = <T>(fileName: string): T =>
  JSON.parse(readFileSync(path.join(SHARED_DIRECTORY, fileName), 'utf-8')) as T;

export interface StatusTransitionContract {
  statuses: string[];
  allowed: Record<string, string[]>;
}

export interface ValidationCaseSpec {
  name: string;
  valid: boolean;
  value?: number;
  length?: number;
  leadingSpaces?: number;
  daysFromToday?: number;
}

export interface ValidationContract {
  claimAmount: ValidationCaseSpec[];
  description: ValidationCaseSpec[];
  incidentLocation: ValidationCaseSpec[];
  incidentDate: ValidationCaseSpec[];
}

const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

export const buildText = ({ length = 0, leadingSpaces = 0 }: ValidationCaseSpec): string =>
  ' '.repeat(leadingSpaces) + 'a'.repeat(length);

export const buildDate = ({ daysFromToday = 0 }: ValidationCaseSpec): Date =>
  new Date(Date.now() + daysFromToday * MILLIS_PER_DAY);
