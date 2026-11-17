import { useState } from 'react'
import { Result } from '../types'
import CutPlan from './CutPlan'

const fmt = (n: number, cur = '') => n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + (cur ? ' ' + cur : '')

export default function ResultView({ r }: { r: Result }) {
  const [open, setOpen] = useState<string | null>(null)
  const c = r.costs
  return (
    <div className="space-y-4">
      {r.warnings.length > 0 && (
        <div className="card border-amber-400 bg-amber-50">
          <h3 className="font-semibold mb-1">Points d'attention ({r.warnings.length})</h3>
          <ul className="list-disc ml-5 text-sm space-y-0.5">{r.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
        </div>
      )}
      <div className="card">
        <h3 className="font-semibold mb-2">Détail des coûts</h3>
        <table className="w-full text-sm">
          <tbody>
            {r.panels.map(p => (
              <tr key={p.materialCode} className="border-b">
                <td className="py-1">Panneau {p.name} ({p.thicknessMm} mm) : {p.sheets} feuille(s), perte {p.wastePercent.toFixed(1)} %, ~{p.cuts} coupes</td>
                <td className="text-right">{fmt(p.cost)}</td>
              </tr>))}
            {r.edge && <tr className="border-b"><td className="py-1">Chant {r.edge.name} : {r.edge.netMeters.toFixed(2)} m nets, {r.edge.purchasedMeters.toFixed(2)} m à prévoir ({r.edge.strips} bandes)</td><td className="text-right">{fmt(r.edge.cost)}</td></tr>}
            {r.hardware.map(h => <tr key={h.code} className="border-b"><td className="py-1">{h.name} x {h.qty}</td><td className="text-right">{fmt(h.total)}</td></tr>)}
            <tr className="border-b"><td className="py-1">Main-d'œuvre : {r.labor.hours.toFixed(2)} h</td><td className="text-right">{fmt(c.labor)}</td></tr>
            <tr className="border-b"><td className="py-1">Finition : {r.finishAreaM2.toFixed(2)} m²</td><td className="text-right">{fmt(c.finish)}</td></tr>
            <tr className="font-medium"><td className="py-1">Coût de production</td><td className="text-right">{fmt(c.productionCost)}</td></tr>
            <tr><td className="py-1">Marge ({c.markupPercent} %)</td><td className="text-right">{fmt(c.markup)}</td></tr>
            <tr><td className="py-1">Transport</td><td className="text-right">{fmt(c.transport)}</td></tr>
            <tr><td className="py-1">Installation</td><td className="text-right">{fmt(c.installation)}</td></tr>
            <tr className="font-semibold border-t"><td className="py-1">Total HT</td><td className="text-right">{fmt(c.totalHt)}</td></tr>
            {c.taxes.map(t => <tr key={t.code}><td className="py-1">{t.label} ({t.ratePercent} %)</td><td className="text-right">{fmt(t.amount)}</td></tr>)}
            <tr className="font-bold text-base border-t"><td className="py-1">Total TTC</td><td className="text-right">{fmt(c.totalTtc, r.currency)}</td></tr>
          </tbody>
        </table>
      </div>
      <div className="card">
        <h3 className="font-semibold mb-2">Pièces à débiter</h3>
        <div className="overflow-x-auto"><table className="w-full text-sm">
          <thead><tr className="text-left text-stone-500"><th>Pièce</th><th>Qté</th><th>Fini a x b (mm)</th><th>Débit a x b (mm)</th><th>Matériau</th></tr></thead>
          <tbody>{r.parts.map((p, i) => <tr key={i} className="border-t"><td>{p.label}</td><td>{p.qty}</td>
            <td>{p.a.toFixed(1)} x {p.b.toFixed(1)}</td><td>{p.coreA.toFixed(1)} x {p.coreB.toFixed(1)}</td><td>{p.materialCode} ({p.thicknessMm})</td></tr>)}</tbody>
        </table></div>
      </div>
      {r.shelfChecks.length > 0 && (
        <div className="card"><h3 className="font-semibold mb-1">Contrôle de flèche des étagères</h3>
          {r.shelfChecks.map((s, i) => <p key={i} className={`text-sm ${s.ok ? '' : 'text-red-700'}`}>
            Portée {s.spanMm.toFixed(0)} mm : flèche {s.deflectionMm.toFixed(2)} mm (admissible {s.limitMm.toFixed(2)} mm) - portée max indicative {s.maxSpanMm.toFixed(0)} mm</p>)}
        </div>)}
      <div className="card">
        <h3 className="font-semibold mb-2">Plans de débit</h3>
        {r.panels.map(p => (
          <div key={p.materialCode} className="mb-2">
            <button className="btn2" onClick={() => setOpen(open === p.materialCode ? null : p.materialCode)}>{p.name} - {p.sheets} feuille(s)</button>
            {open === p.materialCode && <div className="mt-2"><CutPlan panel={p} /></div>}
          </div>))}
      </div>
    </div>
  )
}
