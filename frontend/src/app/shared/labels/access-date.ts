/** IdentityAdministrationService encodes an omitted validUntil with this exact sentinel. */
export function accessDate(value: string | null | undefined, indefinite = 'Sin fecha de finalización'): string {
  if (value == null || value === '9999-12-31T00:00:00Z' || value === '9999-12-31T00:00:00.000Z') return indefinite;
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:T|$)/.exec(value);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : 'Fecha no disponible';
}
