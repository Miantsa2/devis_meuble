export interface Option { code: string; name: string; category?: string; thicknessMm?: number; slideLengthMm?: number }
export interface Catalog {
  furnitureTypes: { code: string; label: string }[]
  panels: Option[]; edgeBands: Option[]; hardware: Option[]
}
export interface Spec {
  typeCode: string; widthMm: number; heightMm: number; depthMm: number; quantity: number
  carcassMaterial: string; backMaterial: string; frontMaterial: string; edgeBand: string
  hasBack: boolean; dividers: number; shelvesPerCompartment: number; doors: number; drawers: number
  drawerFrontHeightMm: number; hasPlinth: boolean; plinthHeightMm: number
  hingeCode: string; slideCode: string; shelfPinCode: string; handleCode: string; connectorCode: string
  shelfLoadKgPerM2: number | null; extras: { code: string; qty: number }[]
}
export interface Placement { label: string; x: number; y: number; w: number; h: number }
export interface Result {
  currency: string; units: number
  parts: { role: string; label: string; qty: number; a: number; b: number; coreA: number; coreB: number; thicknessMm: number; materialCode: string }[]
  panels: { materialCode: string; name: string; thicknessMm: number; sheets: number; netAreaM2: number; sheetsAreaM2: number
    wastePercent: number; cuts: number; unitPrice: number; cost: number; sheetLengthMm: number; sheetWidthMm: number
    layout: { index: number; placements: Placement[]; cuts: number }[] }[]
  edge: { name: string; netMeters: number; appliedMeters: number; purchasedMeters: number; strips: number; unitPrice: number; cost: number } | null
  hardware: { code: string; name: string; qty: number; unitPrice: number; total: number }[]
  labor: { hours: number; cost: number; lines: { code: string; label: string; quantity: number; minutesEach: number; totalMinutes: number; cost: number }[] }
  finishAreaM2: number
  shelfChecks: { spanMm: number; deflectionMm: number; limitMm: number; maxSpanMm: number; ok: boolean }[]
  costs: { panels: number; edge: number; hardware: number; labor: number; finish: number; productionCost: number
    markupPercent: number; markup: number; transport: number; installation: number; totalHt: number
    taxes: { code: string; label: string; ratePercent: number; amount: number }[]; totalTtc: number }
  warnings: string[]; rulesToValidate: number
}
export interface QuoteSummary { id: number; reference: string; customerName: string; createdAt: string; totalHt: number; totalTtc: number; currency: string }
