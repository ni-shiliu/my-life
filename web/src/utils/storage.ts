const ACCESS_TOKEN_KEY = 'ml_access_token'

export function getToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setToken(accessToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export function clearToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}
