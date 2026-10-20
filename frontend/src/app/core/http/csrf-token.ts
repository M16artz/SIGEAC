const STORAGE_KEY = 'sec.csrf-token';

export function storeCsrfToken(token: string): void {
  if (typeof sessionStorage !== 'undefined') sessionStorage.setItem(STORAGE_KEY, token);
}

export function clearCsrfToken(): void {
  if (typeof sessionStorage !== 'undefined') sessionStorage.removeItem(STORAGE_KEY);
}

export function readCsrfToken(): string | undefined {
  if (typeof sessionStorage !== 'undefined') {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) return stored;
  }
  return readCookie('XSRF-TOKEN');
}

function readCookie(name: string): string | undefined {
  if (typeof document === 'undefined') return undefined;
  const prefix = `${encodeURIComponent(name)}=`;
  const item = document.cookie
    .split(';')
    .map((value) => value.trim())
    .find((value) => value.startsWith(prefix));
  return item ? decodeURIComponent(item.slice(prefix.length)) : undefined;
}
