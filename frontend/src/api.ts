// Identifiants gardés en mémoire de session (sessionStorage) : jamais écrits dans le code.
let auth = sessionStorage.getItem('auth') || ''

export function setCredentials(user: string, pass: string) {
  auth = 'Basic ' + btoa(unescape(encodeURIComponent(user + ':' + pass)))
  sessionStorage.setItem('auth', auth)
}
export function logout() { auth = ''; sessionStorage.removeItem('auth') }
export const isLogged = () => !!auth

async function request(method: string, url: string, body?: unknown): Promise<Response> {
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: auth },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!res.ok) {
    let msg = res.statusText
    try { msg = (await res.json()).message ?? msg } catch { /* corps vide */ }
    throw new Error(res.status === 401 ? 'Identifiants invalides' : msg)
  }
  return res
}
export const api = {
  get: async <T,>(u: string) => (await request('GET', u)).json() as Promise<T>,
  post: async <T,>(u: string, b: unknown) => (await request('POST', u, b)).json() as Promise<T>,
  put: async <T,>(u: string, b: unknown) => (await request('PUT', u, b)).json() as Promise<T>,
  del: async (u: string) => { await request('DELETE', u) },
  blob: async (u: string) => (await request('GET', u)).blob(),
}
