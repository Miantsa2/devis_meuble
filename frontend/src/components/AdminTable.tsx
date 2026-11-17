import { useEffect, useState } from 'react'
import { api } from '../api'

export interface Col { key: string; label: string; type?: 'text' | 'number' | 'bool' | 'select'; options?: string[]; width?: string }
type Row = Record<string, any>

/** Grille d'édition générique : chaque ligne s'enregistre / se supprime individuellement. */
export default function AdminTable({ endpoint, cols, idKey = 'id', blank, filter, help }:
  { endpoint: string; cols: Col[]; idKey?: string; blank: Row; filter?: (r: Row) => boolean; help?: string }) {
  const [rows, setRows] = useState<Row[]>([]); const [err, setErr] = useState(''); const [ok, setOk] = useState('')
  const load = () => api.get<Row[]>(endpoint).then(setRows).catch(e => setErr(e.message))
  useEffect(() => { load() }, [endpoint])

  const edit = (i: number, k: string, v: unknown) => setRows(rs => rs.map((r, j) => j === i ? { ...r, [k]: v, _dirty: true } : r))
  async function save(r: Row, i: number) {
    setErr(''); setOk('')
    try {
      const { _dirty, _new, ...body } = r
      const saved = _new ? await api.post<Row>(endpoint, body) : await api.put<Row>(`${endpoint}/${encodeURIComponent(r[idKey])}`, body)
      setRows(rs => rs.map((x, j) => j === i ? saved : x)); setOk('Enregistré')
    } catch (e) { setErr((e as Error).message) }
  }
  async function del(r: Row, i: number) {
    if (r._new) return setRows(rs => rs.filter((_, j) => j !== i))
    if (!confirm('Supprimer cette ligne ?')) return
    try { await api.del(`${endpoint}/${encodeURIComponent(r[idKey])}`); setRows(rs => rs.filter((_, j) => j !== i)) } catch (e) { setErr((e as Error).message) }
  }
  const cell = (r: Row, i: number, c: Col) => {
    const lockedKey = c.key === idKey && !r._new && idKey !== 'id'
    if (c.type === 'bool') return <input type="checkbox" checked={!!r[c.key]} onChange={e => edit(i, c.key, e.target.checked)} />
    if (c.type === 'select') return <select className="inp" value={r[c.key] ?? ''} onChange={e => edit(i, c.key, e.target.value)}>{c.options!.map(o => <option key={o}>{o}</option>)}</select>
    return <input className="inp" type={c.type === 'number' ? 'number' : 'text'} step="any" disabled={lockedKey} value={r[c.key] ?? ''}
      onChange={e => edit(i, c.key, c.type === 'number' ? (e.target.value === '' ? null : Number(e.target.value)) : e.target.value)} />
  }
  return (
    <div className="card space-y-2">
      {help && <p className="text-sm text-stone-500">{help}</p>}
      {err && <p className="text-red-600 text-sm">{err}</p>}{ok && <p className="text-green-700 text-sm">{ok}</p>}
      <div className="overflow-x-auto"><table className="w-full text-sm">
        <thead><tr className="text-left text-stone-500">{cols.map(c => <th key={c.key} className="pr-2">{c.label}</th>)}<th /></tr></thead>
        <tbody>{rows.map((r, i) => (filter && !r._new && !filter(r)) ? null : (
          <tr key={i} className={`border-t ${r._dirty ? 'bg-amber-50' : ''}`}>
            {cols.map(c => <td key={c.key} className="pr-2 py-1" style={{ minWidth: c.width }}>{cell(r, i, c)}</td>)}
            <td className="whitespace-nowrap space-x-1"><button className="btn" disabled={!r._dirty && !r._new} onClick={() => save(r, i)}>OK</button>
              <button className="btn2" onClick={() => del(r, i)}>x</button></td></tr>))}</tbody></table></div>
      <button className="btn2" onClick={() => setRows(rs => [...rs, { ...blank, _new: true, _dirty: true }])}>+ Ajouter une ligne</button>
    </div>
  )
}
