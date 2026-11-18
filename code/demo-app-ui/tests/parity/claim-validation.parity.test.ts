import { describe, expect, it } from 'vitest';

import { step1Schema, step2Schema } from '@/src/features/claims/schemas/claim-schemas';
import {
  buildDate,
  buildText,
  loadSharedCases,
  type ValidationCaseSpec,
  type ValidationContract,
} from './shared-cases';

const contract = loadSharedCases<ValidationContract>('validation-cases.json');

const VALID_STEP1 = {
  incidentDate: new Date(Date.now() - 24 * 60 * 60 * 1000),
  incidentLocation: '12 Harbour Road, Singapore',
  claimAmount: 1500,
};
const VALID_DESCRIPTION = 'Water damage to the basement after a pipe burst';

const accepted = (success: boolean) => (success ? 'accepts' : 'rejects');

/**
 * Same boundary table the API integration tests use (C-08). A failure means the UI and the API
 * disagree about what a valid claim is.
 *
 * Known candidate: text padded with spaces. The API trims before measuring length; the UI schema
 * does not, so padded input passes the form and then fails on submit.
 */
describe('claim form validation matches the API contract', () => {
  describe.each<[string, ValidationCaseSpec[], (spec: ValidationCaseSpec) => boolean]>([
    [
      'claimAmount',
      contract.claimAmount,
      (spec) => step1Schema.safeParse({ ...VALID_STEP1, claimAmount: spec.value }).success,
    ],
    [
      'incidentLocation',
      contract.incidentLocation,
      (spec) => step1Schema.safeParse({ ...VALID_STEP1, incidentLocation: buildText(spec) }).success,
    ],
    [
      'incidentDate',
      contract.incidentDate,
      (spec) => step1Schema.safeParse({ ...VALID_STEP1, incidentDate: buildDate(spec) }).success,
    ],
    [
      'description',
      contract.description,
      (spec) => step2Schema.safeParse({ description: buildText(spec) }).success,
    ],
  ])('%s', (_field, specs, isAcceptedByUi) => {
    it.each(specs)('$name', (spec) => {
      const uiAccepts = isAcceptedByUi(spec);

      expect(uiAccepts, `UI ${accepted(uiAccepts)} this input but the API expects valid=${spec.valid}`)
        .toBe(spec.valid);
    });
  });

  it('accepts a fully valid claim', () => {
    expect(step1Schema.safeParse(VALID_STEP1).success).toBe(true);
    expect(step2Schema.safeParse({ description: VALID_DESCRIPTION }).success).toBe(true);
  });
});
