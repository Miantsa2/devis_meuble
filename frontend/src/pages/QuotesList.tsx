import { useEffect, useState } from 'react'
import { api } from '../api'
import { QuoteSummary, Result } from '../types'
import ResultView from '../components/ResultView'

export default function QuotesList() {
  const [rows, setRows] = useState<QuoteSummary[]>([]); const [sel, setSel] = useState<Result | null>(null); const [err, setErr] = useState('')
  useEffect(() => { api.get<QuoteSummary[]>('/api/quotes').then(setRows).catch(e => setErr(e.message)) }, [])
  async function pdf(q: QuoteSummary) {
    const b = await api.blob(`/api/quotes/${q.id}/pdf`); const a = document.createElement('a')
    a.href = URL.createObjectURL(b); a.download = `${q.reference}.pdf`; a.click()
  }
  return (
    <div className="space-y-4">
      {err && <p className="text-red-600">{err}</p>}
      <div className="card"><table className="w-full text-sm">
        <thead><tr className="text-left text-stone-500"><th>Référence</th><th>Client</th><th>Date</th><th className="text-right">HT</th><th className="text-right">TTC</th><th /></tr></thead>
        <tbody>{rows.map(q => <tr key={q.id} className="border-t">
          <td>{q.reference}</td><td>{q.customerName}</td><td>{new Date(q.createdAt).toLocaleDateString('fr-FR')}</td>
          <td className="text-right">{q.totalHt.toFixed(2)}</td><td className="text-right">{q.totalTtc.toFixed(2)} {q.currency}</td>
          <td className="text-right space-x-2"><button className="btn2" onClick={async () => setSel(await api.get<Result>(`/api/quotes/${q.id}`))}>Détail</button>
            <button className="btn2" onClick={() => pdf(q)}>PDF</button></td></tr>)}</tbody></table></div>
      {sel && <ResultView r={sel} />}
    </div>
  )
}
