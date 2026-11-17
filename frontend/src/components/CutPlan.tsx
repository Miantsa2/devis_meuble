import { Result } from '../types'

/** Dessin SVG du plan de débit : une feuille par bloc, pièces numérotées. */
export default function CutPlan({ panel }: { panel: Result['panels'][number] }) {
  const L = panel.sheetLengthMm, W = panel.sheetWidthMm, scale = 480 / L
  return (
    <div className="space-y-3">
      {panel.layout.map(sheet => (
        <div key={sheet.index}>
          <div className="text-xs text-stone-500 mb-1">Feuille {sheet.index} - {sheet.placements.length} pièces - {sheet.cuts} coupes (estimation)</div>
          <svg width={L * scale} height={W * scale} className="border border-stone-400 bg-amber-50 max-w-full">
            {sheet.placements.map((p, i) => (
              <g key={i}>
                <rect x={p.x * scale} y={p.y * scale} width={p.w * scale} height={p.h * scale} fill="#d6c3a5" stroke="#3b2a1e" strokeWidth={0.7} />
                {p.w * scale > 28 && p.h * scale > 12 && (
                  <text x={p.x * scale + 3} y={p.y * scale + 11} fontSize={8} fill="#3b2a1e">{p.label} {Math.round(p.w)}x{Math.round(p.h)}</text>
                )}
              </g>
            ))}
          </svg>
        </div>
      ))}
    </div>
  )
}
