import { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api'

interface Article { code: string; name: string; category: string; unit: string; unitPrice: number; approachCoef: number }
type Kind = 'SURFACE' | 'CHANT' | 'FORMULA' | 'FIXED' | 'FREE'
interface Line { articleCode: string; quantity: number | ''; kind: Kind; manual: boolean; tindex?: number }
interface Piece { name: string; panelSlot: string | null; qty: number; dim1: number; dim2: number; surfaceM2: number; chantM: number }
interface Comp {
  template: string; name: string; widthMm: number | ''; depthMm: number | ''; heightMm: number | ''; qty: number
  lines: Line[]; pieces: Piece[]; showPieces: boolean; error?: string
}
interface Expand { pieces: Piece[]; lines: { index: number; articleCode: string; slot: string | null; kind: Kind; quantity: number }[]; chantMeters: number }
interface Result {
  name: string; quantity: number; currency: string
  components: { name: string; qty: number; unitTotal: number; total: number }[]
  materialsPerUnit: number; materialsTotal: number; laborHours: number; hourlyRate: number; laborCost: number
  productionCost: number; markupPercent: number; markup: number; transport: number; installation: number; totalHt: number
  taxes: { code: string; label: string; ratePercent: number; amount: number }[]; totalTtc: number; warnings: string[]
}
interface Summary { id: number; reference: string; customerName: string; createdAt: string; totalHt: number; totalTtc: number; currency: string }

const CATS: Record<string, string> = { PANNEAU: 'Panneaux', CHANT: 'Chant', COLLE: 'Colle', QUINCAILLERIE: 'Quincaillerie', AUTRE: 'Autre' }
const fmt = (n: number) => n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const fmtQ = (n: number) => n.toLocaleString('fr-FR', { maximumFractionDigits: 4 })
const num = (v: number | '') => (v === '' ? 0 : v)
const lineAmount = (a: Article | undefined, q: number | '') => a ? Math.round(num(q) * a.unitPrice * a.approachCoef * 100) / 100 : 0
const isAuto = (k: Kind) => k === 'SURFACE' || k === 'CHANT'

async function download(url: string, name: string) {
  const b = await api.blob(url); const a = document.createElement('a')
  a.href = URL.createObjectURL(b); a.download = name; a.click()
}

/** Fusionne le calcul du serveur avec les choix de l'utilisateur (article remplacé, quantité saisie à la main, lignes ajoutées). */
function merge(c: Comp, r: Expand): Comp {
  const server = r.lines.map(s => {
    const old = c.lines.find(l => l.tindex === s.index)
    const keepManual = !!old?.manual && !isAuto(s.kind)
    return { articleCode: old?.articleCode || s.articleCode, quantity: keepManual ? old!.quantity : s.quantity,
      kind: s.kind, manual: keepManual, tindex: s.index } as Line
  })
  const extras = c.lines.filter(l => l.tindex === undefined)
  return { ...c, lines: [...server, ...extras], pieces: r.pieces, error: undefined }
}

export default function Devis() {
  const [arts, setArts] = useState<Article[]>([]); const [tpls, setTpls] = useState<string[]>([])
  const [name, setName] = useState('Meuble haut'); const [qty, setQty] = useState(1); const [hours, setHours] = useState<number | ''>('')
  const [dims, setDims] = useState<{ w: number | ''; p: number | ''; h: number | '' }>({ w: 500, p: 330, h: 900 })
  const [comps, setComps] = useState<Comp[]>([]); const [tplPick, setTplPick] = useState('')
  const [res, setRes] = useState<Result | null>(null); const [err, setErr] = useState(''); const [busy, setBusy] = useState(false)
  const [customer, setCustomer] = useState(''); const [contact, setContact] = useState(''); const [saved, setSaved] = useState<Summary | null>(null)
  const lastKeys = useRef<string[]>([])

  useEffect(() => {
    api.get<{ articles: Article[]; templates: string[] }>('/api/bom/catalog').then(c => { setArts(c.articles); setTpls(c.templates) }).catch(e => setErr(e.message))
  }, [])
  const byCode = useMemo(() => Object.fromEntries(arts.map(a => [a.code, a])), [arts])

  async function refresh(i: number, c: Comp) {
    try {
      const r = await api.post<Expand>('/api/bom/expand', { template: c.template, widthMm: num(c.widthMm), depthMm: num(c.depthMm), heightMm: num(c.heightMm), qty: c.qty })
      setComps(cs => cs.map((x, j) => j === i ? merge(x, r) : x))
    } catch (e) { setComps(cs => cs.map((x, j) => j === i ? { ...x, error: (e as Error).message } : x)) }
  }

  // Recalcul automatique (surfaces, chants, quincaillerie par défaut) quand les mesures ou la quantité changent
  const dimKey = comps.map(c => [c.template, c.widthMm, c.depthMm, c.heightMm, c.qty].join('|')).join(';')
  useEffect(() => {
    const t = setTimeout(() => {
      comps.forEach((c, i) => {
        const key = [c.template, c.widthMm, c.depthMm, c.heightMm, c.qty].join('|')
        if (c.template && lastKeys.current[i] !== key) { lastKeys.current[i] = key; refresh(i, c) } else if (!c.template) lastKeys.current[i] = key
      })
    }, 350)
    return () => clearTimeout(t)
  }, [dimKey])

  const upComp = (i: number, p: Partial<Comp>) => setComps(cs => cs.map((c, j) => j === i ? { ...c, ...p } : c))
  const upLine = (i: number, k: number, p: Partial<Line>) => setComps(cs => cs.map((c, j) => j !== i ? c : { ...c, lines: c.lines.map((l, m) => m === k ? { ...l, ...p } : l) }))
  const addComponent = () => {
    setComps(cs => [...cs, { template: tplPick, name: tplPick || 'Composant', widthMm: dims.w, depthMm: dims.p, heightMm: dims.h, qty: 1, lines: [], pieces: [], showPieces: false }])
    setTplPick('')
  }
  const removeComp = (i: number) => { lastKeys.current = []; setComps(cs => cs.filter((_, j) => j !== i)) }
  const resetLine = (i: number, k: number) => {
    const c = comps[i]; const next = { ...c, lines: c.lines.map((l, m) => m === k ? { ...l, manual: false } : l) }
    upComp(i, next); refresh(i, next)
  }

  const payload = () => ({ name, quantity: qty, laborHours: hours === '' ? null : hours,
    components: comps.map(c => ({ name: c.name, qty: c.qty, widthMm: c.widthMm === '' ? null : c.widthMm,
      depthMm: c.depthMm === '' ? null : c.depthMm, heightMm: c.heightMm === '' ? null : c.heightMm,
      lines: c.lines.filter(l => l.articleCode).map(l => ({ articleCode: l.articleCode, quantity: num(l.quantity) })) })) })

  async function calculate() {
    setBusy(true); setErr(''); setSaved(null)
    try { setRes(await api.post<Result>('/api/bom/calculate', payload())) } catch (e) { setErr((e as Error).message); setRes(null) }
    setBusy(false)
  }
  async function save() {
    setBusy(true); setErr('')
    try {
      const s = await api.post<Summary>('/api/bom/quotes', { customerName: customer, customerContact: contact, input: payload() })
      setSaved(s); await download(`/api/bom/quotes/${s.id}/pdf`, `${s.reference}.pdf`)
    } catch (e) { setErr((e as Error).message) }
    setBusy(false)
  }

  const compTotal = (c: Comp) => c.lines.reduce((s, l) => s + lineAmount(byCode[l.articleCode], l.quantity), 0)
  const grouped = Object.keys(CATS).map(k => [k, arts.filter(a => a.category === k)] as const).filter(([, l]) => l.length)
  const dimInput = (label: string, v: number | '', on: (n: number | '') => void) => (
    <label className="text-sm">{label} mm<input className="inp" type="number" min={0} value={v} onChange={e => on(e.target.value === '' ? '' : Number(e.target.value))} /></label>)

  return (
    <div className="space-y-4">
      <div className="card space-y-3">
        <div className="grid sm:grid-cols-4 gap-3 items-end">
          <label className="text-sm sm:col-span-2">Meuble<input className="inp" value={name} onChange={e => setName(e.target.value)} /></label>
          <label className="text-sm">Quantité de meubles<input className="inp" type="number" min={1} value={qty} onChange={e => setQty(Math.max(1, Number(e.target.value)))} /></label>
          <label className="text-sm">Main-d'œuvre (h / meuble)<input className="inp" type="number" min={0} step="any" value={hours} onChange={e => setHours(e.target.value === '' ? '' : Number(e.target.value))} /></label>
        </div>
        <div className="grid grid-cols-3 gap-3 max-w-xl">
          {dimInput('LARG', dims.w, n => setDims(d => ({ ...d, w: n })))}{dimInput('PROF', dims.p, n => setDims(d => ({ ...d, p: n })))}{dimInput('HAUT', dims.h, n => setDims(d => ({ ...d, h: n })))}
        </div>
        <p className="text-xs text-stone-500">Ces mesures du meuble pré-remplissent chaque nouveau composant (modifiables ensuite composant par composant).</p>
      </div>

      {comps.map((c, i) => (
        <div key={i} className="card space-y-2">
          <div className="grid grid-cols-2 sm:grid-cols-6 gap-2 items-end">
            <label className="text-sm sm:col-span-2">Composant<input className="inp" value={c.name} onChange={e => upComp(i, { name: e.target.value })} /></label>
            {dimInput('LARG', c.widthMm, n => upComp(i, { widthMm: n }))}{dimInput('PROF', c.depthMm, n => upComp(i, { depthMm: n }))}{dimInput('HAUT', c.heightMm, n => upComp(i, { heightMm: n }))}
            <label className="text-sm">QTE<input className="inp" type="number" min={1} value={c.qty} onChange={e => upComp(i, { qty: Math.max(1, Number(e.target.value)) })} /></label>
          </div>
          {c.error && <p className="text-red-600 text-sm">{c.error}</p>}
          <table className="w-full text-sm">
            <thead><tr className="text-left text-stone-500"><th>Article</th><th className="w-36">Surf / Qté</th><th>Unité</th><th className="text-right">Prix U</th><th className="text-right">Coef.</th><th className="text-right">Montant</th><th /></tr></thead>
            <tbody>{c.lines.map((l, k) => { const a = byCode[l.articleCode]; return (
              <tr key={k} className="border-t">
                <td className="py-1 pr-2"><select className="inp" value={l.articleCode} onChange={e => upLine(i, k, { articleCode: e.target.value })}>
                  <option value="">- choisir un article -</option>
                  {grouped.map(([cat, list]) => <optgroup key={cat} label={CATS[cat]}>{list.map(x => <option key={x.code} value={x.code}>{x.name}</option>)}</optgroup>)}
                  {l.articleCode && !a && <option value={l.articleCode}>{l.articleCode} (inactif)</option>}
                </select></td>
                <td className="pr-2">{isAuto(l.kind)
                  ? <span className="inline-flex items-center gap-1"><span className="font-medium">{fmtQ(num(l.quantity))}</span><span className="text-xs text-stone-500 bg-stone-100 rounded px-1">auto</span></span>
                  : <span className="inline-flex items-center gap-1">
                      <input className="inp" type="number" min={0} step="any" value={l.quantity} onChange={e => upLine(i, k, { quantity: e.target.value === '' ? '' : Number(e.target.value), manual: l.kind !== 'FREE' })} />
                      {l.manual && <button className="btn2" title="Revenir à la valeur calculée" onClick={() => resetLine(i, k)}>↺</button>}
                    </span>}</td>
                <td>{a?.unit}</td><td className="text-right">{a ? a.unitPrice.toLocaleString('fr-FR', { maximumFractionDigits: 4 }) : ''}</td><td className="text-right">{a ? a.approachCoef : ''}</td>
                <td className="text-right">{a ? fmt(lineAmount(a, l.quantity)) : ''}</td>
                <td className="text-right"><button className="btn2" onClick={() => upComp(i, { lines: c.lines.filter((_, m) => m !== k) })}>x</button></td></tr>) })}</tbody>
          </table>
          <div className="flex flex-wrap items-center gap-2">
            <button className="btn2" onClick={() => upComp(i, { lines: [...c.lines, { articleCode: '', quantity: '', kind: 'FREE', manual: false }] })}>+ Ajouter une ligne</button>
            {c.pieces.length > 0 && <button className="btn2" onClick={() => upComp(i, { showPieces: !c.showPieces })}>{c.showPieces ? 'Masquer' : 'Voir'} le détail des pièces</button>}
            <button className="btn2" onClick={() => removeComp(i)}>Supprimer le composant</button>
            <span className="ml-auto font-medium text-sm">Total composant : {fmt(compTotal(c))}</span>
          </div>
          {c.showPieces && (
            <table className="w-full text-xs bg-stone-50 rounded">
              <thead><tr className="text-left text-stone-500"><th className="p-1">Pièce</th><th>Qté</th><th className="text-right">H/L mm</th><th className="text-right">P mm</th><th className="text-right">Surface m²</th><th className="text-right">Chant m</th></tr></thead>
              <tbody>{c.pieces.map((p, m) => <tr key={m} className="border-t"><td className="p-1">{p.name}</td><td>{fmtQ(p.qty)}</td>
                <td className="text-right">{fmtQ(p.dim1)}</td><td className="text-right">{fmtQ(p.dim2)}</td><td className="text-right">{p.surfaceM2.toFixed(4)}</td><td className="text-right">{p.chantM.toFixed(3)}</td></tr>)}</tbody>
            </table>)}
        </div>))}

      <div className="card flex flex-wrap items-center gap-2">
        <select className="inp w-56" value={tplPick} onChange={e => setTplPick(e.target.value)}>
          <option value="">Composant vide</option>{tpls.map(t => <option key={t} value={t}>Gabarit : {t}</option>)}</select>
        <button className="btn2" onClick={addComponent}>+ Ajouter un composant</button>
        <button className="btn ml-auto" disabled={busy || comps.length === 0} onClick={calculate}>{busy ? 'Calcul...' : 'Calculer le devis'}</button>
      </div>

      {err && <div className="card border-red-400 text-red-700 text-sm">{err}</div>}
      {res && (
        <div className="space-y-3">
          {res.warnings.length > 0 && <div className="card border-amber-400 bg-amber-50 text-sm"><ul className="list-disc ml-5">{res.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul></div>}
          <div className="card">
            <table className="w-full text-sm"><tbody>
              {res.components.map((c, i) => <tr key={i} className="border-b"><td className="py-1">{c.name}</td><td className="text-right">{fmt(c.total)}</td></tr>)}
              <tr className="font-medium"><td className="py-1">Matériaux HT (par meuble)</td><td className="text-right">{fmt(res.materialsPerUnit)}</td></tr>
              {res.quantity > 1 && <tr><td className="py-1">Matériaux x {res.quantity} meubles</td><td className="text-right">{fmt(res.materialsTotal)}</td></tr>}
              <tr><td className="py-1">Main-d'œuvre</td><td className="text-right">{fmt(res.laborCost)}</td></tr>
              <tr className="font-medium"><td className="py-1">Coût de production</td><td className="text-right">{fmt(res.productionCost)}</td></tr>
              <tr><td className="py-1">Marge ({res.markupPercent} %)</td><td className="text-right">{fmt(res.markup)}</td></tr>
              <tr><td className="py-1">Transport</td><td className="text-right">{fmt(res.transport)}</td></tr>
              <tr><td className="py-1">Installation</td><td className="text-right">{fmt(res.installation)}</td></tr>
              <tr className="font-semibold border-t"><td className="py-1">Total HT</td><td className="text-right">{fmt(res.totalHt)}</td></tr>
              {res.taxes.map(t => <tr key={t.code}><td className="py-1">{t.label} ({t.ratePercent} %)</td><td className="text-right">{fmt(t.amount)}</td></tr>)}
              <tr className="font-bold text-base border-t"><td className="py-1">Total TTC</td><td className="text-right">{fmt(res.totalTtc)} {res.currency}</td></tr>
            </tbody></table>
          </div>
          <div className="card grid sm:grid-cols-3 gap-2 items-end">
            <label className="text-sm">Client<input className="inp" value={customer} onChange={e => setCustomer(e.target.value)} /></label>
            <label className="text-sm">Contact<input className="inp" value={contact} onChange={e => setContact(e.target.value)} /></label>
            <button className="btn" disabled={busy || !customer} onClick={save}>Enregistrer + PDF client</button>
            {saved && <div className="sm:col-span-3 text-sm text-green-700 flex gap-3 items-center">Devis {saved.reference} enregistré.
              <button className="btn2" onClick={() => download(`/api/bom/quotes/${saved.id}/pdf?detail=true`, `REVIENT-${saved.reference}.pdf`)}>Fiche de revient (interne)</button></div>}
          </div>
        </div>)}
    </div>
  )
}

export function DevisList() {
  const [rows, setRows] = useState<Summary[]>([]); const [err, setErr] = useState('')
  useEffect(() => { api.get<Summary[]>('/api/bom/quotes').then(setRows).catch(e => setErr(e.message)) }, [])
  return (
    <div className="card">{err && <p className="text-red-600 text-sm">{err}</p>}
      <table className="w-full text-sm">
        <thead><tr className="text-left text-stone-500"><th>Référence</th><th>Client</th><th>Date</th><th className="text-right">HT</th><th className="text-right">TTC</th><th /></tr></thead>
        <tbody>{rows.map(q => <tr key={q.id} className="border-t"><td>{q.reference}</td><td>{q.customerName}</td>
          <td>{new Date(q.createdAt).toLocaleDateString('fr-FR')}</td><td className="text-right">{fmt(q.totalHt)}</td><td className="text-right">{fmt(q.totalTtc)} {q.currency}</td>
          <td className="text-right space-x-2"><button className="btn2" onClick={() => download(`/api/bom/quotes/${q.id}/pdf`, `${q.reference}.pdf`)}>PDF client</button>
            <button className="btn2" onClick={() => download(`/api/bom/quotes/${q.id}/pdf?detail=true`, `REVIENT-${q.reference}.pdf`)}>Revient</button></td></tr>)}</tbody></table></div>
  )
}
