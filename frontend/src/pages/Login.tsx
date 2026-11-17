import { useState } from 'react'
import { api, setCredentials, logout } from '../api'

export default function Login({ onLogin }: { onLogin: () => void }) {
  const [u, setU] = useState(''); const [p, setP] = useState(''); const [err, setErr] = useState('')
  async function submit() {
    setCredentials(u, p)
    try { await api.get('/api/auth/me'); onLogin() } catch (e) { logout(); setErr((e as Error).message) }
  }
  return (
    <div className="max-w-sm mx-auto mt-24 card space-y-3">
      <h1 className="text-lg font-bold">Connexion</h1>
      <input className="inp" placeholder="Identifiant" value={u} onChange={e => setU(e.target.value)} />
      <input className="inp" type="password" placeholder="Mot de passe" value={p} onChange={e => setP(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && submit()} />
      {err && <p className="text-red-600 text-sm">{err}</p>}
      <button className="btn w-full" onClick={submit}>Se connecter</button>
    </div>
  )
}
