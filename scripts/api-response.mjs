/** Adapt the public envelope to the existing acceptance scenario view models. */
export function unwrapApiResponse(body) {
  if (!body || typeof body.success !== 'boolean' || !('errors' in body) || !('meta' in body)) return body;
  if (!body.success) return body.errors;
  return body.data && !Array.isArray(body.data) && 'page' in body.meta
    ? {...body.data, ...body.meta} : body.data;
}
