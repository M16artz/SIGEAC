import {test} from 'node:test';
import assert from 'node:assert/strict';
import {unwrapApiResponse} from './api-response.mjs';

test('domain data stays nested after removing the envelope', () => {
  const career = {id: 'career', data: {code: 'C'}};
  assert.deepEqual(unwrapApiResponse({success: true, message: 'OK', data: career, errors: null, meta: {}}), career);
});
test('paged views regain metadata without changing source counters', () => {
  assert.deepEqual(unwrapApiResponse({success: true, message: 'OK', data: {sources: [1], totalSources: 4}, errors: null,
    meta: {page: 1, size: 1}}), {sources: [1], totalSources: 4, page: 1, size: 1});
});
test('errors retain machine codes and validation details', () => {
  const error = {code: 'VALIDATION_FAILED', details: {name: 'required'}};
  assert.deepEqual(unwrapApiResponse({success: false, message: 'Invalid', data: null, errors: error, meta: {}}), error);
});
test('protocol documents and no-content remain unchanged', () => {
  assert.equal(unwrapApiResponse(null), null);
  assert.deepEqual(unwrapApiResponse({openapi: '3.1.0'}), {openapi: '3.1.0'});
});
