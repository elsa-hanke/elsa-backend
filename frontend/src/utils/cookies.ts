export function setCookie(name: string, value: string, maxAge: number) {
  document.cookie = `${name}=${value}; max-age=${maxAge}`
}

export function getCookie(name: string) {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`) as string[] | undefined
  // Use index [1] (first occurrence) to match the cookie the server reads via
  // WebUtils.getCookie(), which also returns the first cookie with that name.
  // This handles duplicate XSRF-TOKEN cookies (e.g. a stale one scoped to a
  // subdomain alongside the current one scoped to the parent domain).
  return parts && parts.length >= 2 ? parts[1].split(';').shift() : undefined
}
