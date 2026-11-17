import { useEffect, useState } from 'react'
import { api, isLogged, logout } from './api'
import Login from './pages/Login'
import Devis, { DevisList } from './pages/Devis'
import QuoteBuilder from './pages/QuoteBuilder'
import QuotesList from './pages/QuotesList'
import Admin from './pages/Admin'

type Tab = 'new' | 'quotes' | 'auto' | 'oldquotes' | 'admin'

export default function App() {
  const [logged, setLogged] = useState(isLogged())
  const [me, setMe] = useState<{ username: string; admin: boolean } | null>(null)
  const [tab, setTab] = useState<Tab>('new')

  useEffect(() => {
    if (!logged) return
    api.get<{ username: string; admin: boolean }>('/api/auth/me').then(setMe).catch(() => { logout(); setLogged(false) })
  }, [logged])

  if (!logged || !me) return <Login onLogin={() => setLogged(true)} />
  const tabs: [Tab, string][] = [['new', 'Nouveau devis'], ['quotes', 'Devis émis'], ['auto', 'Calcul auto (test)'], ['oldquotes', 'Anciens devis auto'],
    ...(me.admin ? [['admin', 'Administration'] as [Tab, string]] : [])]
  return (
    <div className="max-w-7xl mx-auto p-4">
      <header className="flex items-center gap-4 mb-4">
        <h1 className="text-xl font-bold">Devis meubles sur mesure</h1>
        <nav className="flex gap-2 flex-1 flex-wrap">
          {tabs.map(([k, l]) => <button key={k} className={tab === k ? 'btn' : 'btn2'} onClick={() => setTab(k)}>{l}</button>)}
        </nav>
        <span className="text-sm text-stone-500">{me.username}</span>
        <button className="btn2" onClick={() => { logout(); setLogged(false); setMe(null) }}>Quitter</button>
      </header>
      {tab === 'new' && <Devis />}
      {tab === 'quotes' && <DevisList />}
      {tab === 'auto' && <QuoteBuilder />}
      {tab === 'oldquotes' && <QuotesList />}
      {tab === 'admin' && me.admin && <Admin />}
    </div>
  )
}
