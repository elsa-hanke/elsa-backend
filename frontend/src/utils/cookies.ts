export function setCookie(name: string, value: string, maxAge: number) {
  document.cookie = `${name}=${value}; max-age=${maxAge}`
}

export function getCookie(name: string) {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`) as string[] | undefined
  return parts && parts.length === 2 ? parts?.pop()?.split(';').shift() : undefined
}
