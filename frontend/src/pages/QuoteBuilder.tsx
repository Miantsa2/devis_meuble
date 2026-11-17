import { useEffect, useState } from 'react'
import { api } from '../api'
import { Catalog, Result, Spec } from '../types'
import ResultView from '../components/ResultView'

const EMPTY: Spec = {
  typeCode: 'MEUBLE_HAUT', widthMm: 800, heightMm: 720, depthMm: 330, quantity: 1,
  carcassMaterial: '', backMaterial: '', frontMaterial: '', edgeBand: '',
  hasBack: true, dividers: 0, shelvesPerCompartment: 1, doors: 2, drawers: 0, drawerFrontHeightMm: 180,
  hasPlinth: false, plinthHeightMm: 100, hingeCode: '', slideCode: '', shelfPinCode: '', handleCode: '', connectorCode: '',
  shelfLoadKgPerM2: null, extras: [],
}

export default function QuoteBuilder() {
  const [cat, setCat] = useState<Catalog | null>(null)
  const [spec, setSpec] = useState<Spec>(EMPTY)
  const [res, setRes] = useState<Result | null>(null)
  const [err, setErr] = useState(''); const [busy, setBusy] = useState(false)
  const [customer, setCustomer] = useState(''); const [contact, setContact] = useState(''); const [saved, setSaved] = useState('')

  useEffect(() => { api.get<Catalog>('/api/catalog').then(setCat).catch(e => setErr(e.message)) }, [])

  const set = <K extends keyof Spec>(k: K, v: Spec[K]) => setSpec(s => ({ ...s, [k]: v }))
  const num = (k: keyof Spec) => (e: React.ChangeEvent<HTMLInputElement>) => set(k, (e.target.value === '' ? 0 : Number(e.target.value)) as never)
  const payload = () => ({ ...spec, backMaterial: spec.hasBack ? spec.backMaterial : null,
    frontMaterial: spec.frontMaterial || null, edgeBand: spec.edgeBand || null,
    slideCode: spec.slideCode || null, handleCode: spec.handleCode || null, shelfPinCode: spec.shelfPinCode || null,
    hingeCode: spec.hingeCode || null, connectorCode: spec.connectorCode || null })

  async function calculate() {
    setBusy(true); setErr(''); setSaved('')
    try { setRes(await api.post<Result>('/api/quotes/calculate', payload())) } catch (e) { setErr((e as Error).message); setRes(null) }
    setBusy(false)
  }
  async function save() {
    setBusy(true); setErr('')
    try {
      const s = await api.post<{ id: number; reference: string }>('/api/quotes', { customerName: customer, customerContact: contact, spec: payload() })
      const blob = await api.blob(`/api/quotes/${s.id}/pdf`)
      const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `${s.reference}.pdf`; a.click()
      setSaved(`Devis ${s.reference} enregistré et PDF téléchargé.`)
    } catch (e) { setErr((e as Error).message) }
    setBusy(false)
  }

  if (!cat) return <p>{err || 'Chargement du catalogue...'}</p>
  const sel = (k: keyof Spec, items: { code: string; name: string }[], label: string, optional = false) => (
    <label className="text-sm block">{label}
      <select className="inp" value={spec[k] as string} onChange={e => set(k, e.target.value as never)}>
        <option value="">{optional ? '- aucun -' : '- choisir -'}</option>
        {items.map(o => <option key={o.code} value={o.code}>{o.name}</option>)}
      </select></label>)
  const n = (k: keyof Spec, label: string) => (
    <label className="text-sm block">{label}<input className="inp" type="number" min={0} value={spec[k] as number} onChange={num(k)} /></label>)
  const hw = (c: string) => cat.hardware.filter(h => h.category === c)

  return (
    <div className="grid md:grid-cols-[380px_1fr] gap-4">
      <div className="card space-y-3 h-fit">
        <h2 className="font-semibold">Meuble</h2>
        <label className="text-sm block">Type
          <select className="inp" value={spec.typeCode} onChange={e => set('typeCode', e.target.value)}>
            {cat.furnitureTypes.filter(t => t.code !== 'DEFAULT').map(t => <option key={t.code} value={t.code}>{t.label}</option>)}</select></label>
        <div className="grid grid-cols-3 gap-2">{n('widthMm', 'Largeur mm')}{n('heightMm', 'Hauteur mm')}{n('depthMm', 'Profondeur mm')}</div>
        {n('quantity', 'Quantité')}
        <h2 className="font-semibold pt-2">Matériaux</h2>
        {sel('carcassMaterial', cat.panels, 'Caisson')}
        <label className="text-sm flex gap-2 items-center"><input type="checkbox" checked={spec.hasBack} onChange={e => set('hasBack', e.target.checked)} />Fond</label>
        {spec.hasBack && sel('backMaterial', cat.panels, 'Panneau de fond')}
        {sel('frontMaterial', cat.panels, 'Façades (sinon caisson)', true)}
        {sel('edgeBand', cat.edgeBands, 'Chant', true)}
        <h2 className="font-semibold pt-2">Structure</h2>
        <div className="grid grid-cols-2 gap-2">
          {n('dividers', 'Séparations verticales')}{n('shelvesPerCompartment', 'Étagères / compartiment')}
          {n('doors', 'Portes')}{n('drawers', 'Tiroirs')}
        </div>
        {spec.drawers > 0 && n('drawerFrontHeightMm', 'Hauteur façade tiroir mm')}
        <label className="text-sm flex gap-2 items-center"><input type="checkbox" checked={spec.hasPlinth} onChange={e => set('hasPlinth', e.target.checked)} />Plinthe</label>
        {spec.hasPlinth && n('plinthHeightMm', 'Hauteur plinthe mm')}
        <label className="text-sm block">Charge par étagère (kg/m², facultatif)
          <input className="inp" type="number" min={0} value={spec.shelfLoadKgPerM2 ?? ''} onChange={e => set('shelfLoadKgPerM2', e.target.value === '' ? null : Number(e.target.value))} /></label>
        <h2 className="font-semibold pt-2">Quincaillerie</h2>
        {spec.doors > 0 && sel('hingeCode', hw('HINGE'), 'Charnière')}
        {spec.drawers > 0 && sel('slideCode', hw('SLIDE'), 'Coulisse')}
        {spec.shelvesPerCompartment > 0 && sel('shelfPinCode', hw('SHELF_PIN'), 'Taquet')}
        {sel('handleCode', hw('HANDLE'), 'Poignée', true)}
        {sel('connectorCode', hw('CONNECTOR'), 'Connecteur d\'assemblage', true)}
        <button className="btn w-full" disabled={busy} onClick={calculate}>{busy ? 'Calcul...' : 'Calculer'}</button>
      </div>
      <div className="space-y-4">
        {err && <div className="card border-red-400 text-red-700 text-sm">{err}</div>}
        {!res && !err && <div className="card text-stone-500 text-sm">Renseignez le meuble puis cliquez sur « Calculer ».</div>}
        {res && <>
          <div className="card grid sm:grid-cols-3 gap-2 items-end">
            <label className="text-sm">Client<input className="inp" value={customer} onChange={e => setCustomer(e.target.value)} /></label>
            <label className="text-sm">Contact<input className="inp" value={contact} onChange={e => setContact(e.target.value)} /></label>
            <button className="btn" disabled={busy || !customer} onClick={save}>Enregistrer + PDF</button>
            {saved && <p className="text-green-700 text-sm sm:col-span-3">{saved}</p>}
          </div>
          <ResultView r={res} />
        </>}
      </div>
    </div>
  )
}
