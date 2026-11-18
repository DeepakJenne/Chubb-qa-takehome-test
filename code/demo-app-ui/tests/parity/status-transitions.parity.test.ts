import { describe, expect, it, vi } from 'vitest';

// The generated API client is built at install time; only its enum is needed here.
vi.mock('@/src/lib/api/bff-client', () => ({
  ClaimStatus: {
    SUBMITTED: 'SUBMITTED',
    UNDER_REVIEW: 'UNDER_REVIEW',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
    CLOSED: 'CLOSED',
  },
}));

import { VALID_TRANSITIONS } from '@/src/features/admin/utils/claim-status-transitions';
import { loadSharedCases, type StatusTransitionContract } from './shared-cases';

const contract = loadSharedCases<StatusTransitionContract>('status-transitions.json');

describe('UI status transitions match the shared backend contract (A-09)', () => {
  it.each(contract.statuses)('%s offers exactly the transitions the backend allows', (status) => {
    const offeredByUi = [...VALID_TRANSITIONS[status as keyof typeof VALID_TRANSITIONS]].sort();
    const allowedByBackend = [...contract.allowed[status]].sort();

    expect(offeredByUi).toEqual(allowedByBackend);
  });

  it('covers every status the backend knows about', () => {
    expect(Object.keys(VALID_TRANSITIONS).sort()).toEqual([...contract.statuses].sort());
  });
});
