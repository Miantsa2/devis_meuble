import { useState } from 'react'
import AdminTable from '../components/AdminTable'

const STATUS = ['DOCUMENTED', 'DESIGN_CHOICE', 'TO_VALIDATE']
const BASIS = ['PER_SHEET', 'PER_CUT', 'PER_PART', 'PER_EDGE_METER', 'PER_EDGE_STRIP', 'PER_HOLE', 'PER_UNIT', 'PER_DOOR', 'PER_DRAWER', 'PER_HINGE', 'PER_M2_FINISH']
const ART_CATS = ['PANNEAU', 'CHANT', 'COLLE', 'QUINCAILLERIE', 'AUTRE']
const UNITS = ['M2', 'ML', 'KG', 'L', 'PCE', 'U']
const CATS = ['HINGE', 'SLIDE', 'SHELF_PIN', 'HANDLE', 'CONNECTOR', 'OTHER']

export default function Admin() {
  const [tab, setTab] = useState('articles'); const [type, setType] = useState('DEFAULT')
  const tabs: [string, string][] = [['articles', 'Articles'], ['gabarits', 'Gabarits'], ['pieces', 'Pièces des gabarits'], ['lignes', 'Lignes des gabarits'], ['panels', 'Panneaux (calcul auto)'], ['edges', 'Chants'], ['hardware', 'Accessoires'], ['settings', 'Tarifs & marges'],
    ['taxes', 'Taxes'], ['labor', 'Temps de fabrication'], ['rules', 'Règles de fabrication'], ['thresholds', 'Seuils charnières'], ['types', 'Types de meubles']]
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">{tabs.map(([k, l]) => <button key={k} className={tab === k ? 'btn' : 'btn2'} onClick={() => setTab(k)}>{l}</button>)}</div>

      {tab === 'articles' && <AdminTable endpoint="/api/admin/articles" help="Catalogue unique : panneaux, chants, colles, quincaillerie. Montant d'une ligne = quantité saisie x prix unitaire x coefficient d'arrivée. Décoche « Actif » pour retirer un article des devis sans le supprimer."
        blank={{ code: '', name: '', category: 'QUINCAILLERIE', unit: 'PCE', unitPrice: 0, approachCoef: 1, active: true, note: '' }}
        cols={[{ key: 'code', label: 'Code' }, { key: 'name', label: 'Désignation', width: '240px' }, { key: 'category', label: 'Catégorie', type: 'select', options: ART_CATS },
          { key: 'unit', label: 'Unité', type: 'select', options: UNITS }, { key: 'unitPrice', label: 'Prix unitaire', type: 'number' },
          { key: 'approachCoef', label: 'Coef. arrivée', type: 'number' }, { key: 'active', label: 'Actif', type: 'bool' }, { key: 'note', label: 'Note', width: '200px' }]} />}

      {tab === 'gabarits' && <AdminTable endpoint="/api/admin/component-templates" help="Un gabarit = un type de composant (Caisson, Étagère, Porte mélaminé...). Chute de panneau et chute de chant en % (ex. 10 = +10 %), appliquées aux totaux calculés à partir des pièces. Le nom doit être identique dans « Pièces » et « Lignes »."
        blank={{ name: '', panelWastePercent: 10, chantWastePercent: 0 }}
        cols={[{ key: 'name', label: 'Gabarit', width: '200px' }, { key: 'panelWastePercent', label: 'Chute panneau %', type: 'number' }, { key: 'chantWastePercent', label: 'Chute chant %', type: 'number' }]} />}

      {tab === 'pieces' && <AdminTable endpoint="/api/admin/template-pieces" help="Pièces d'un gabarit, comme le second tableau du fichier Excel. Formules avec L = LARG, P = PROF, H = HAUT, Q = QTE du composant (ex. L-38, P-25, L/Q-1, 2*Q). Surface = dim 1 × dim 2 × quantité. Chant = quantité × (bords sur dim 1 × (dim 1 + surlongueur) + bords sur dim 2 × (dim 2 + surlongueur)). « Slot panneau » et « Slot chant » relient la pièce à la ligne de même slot dans « Lignes »."
        blank={{ templateName: '', position: 1, name: '', panelSlot: '', qtyFormula: 'Q', dim1Formula: 'H', dim2Formula: 'P', chantSlot: '', edgesDim1: 0, edgesDim2: 0 }}
        cols={[{ key: 'templateName', label: 'Gabarit', width: '150px' }, { key: 'position', label: 'Ordre', type: 'number' }, { key: 'name', label: 'Pièce', width: '150px' },
          { key: 'panelSlot', label: 'Slot panneau' }, { key: 'qtyFormula', label: 'Quantité' }, { key: 'dim1Formula', label: 'Dim 1 (H/L)' }, { key: 'dim2Formula', label: 'Dim 2 (P)' },
          { key: 'chantSlot', label: 'Slot chant' }, { key: 'edgesDim1', label: 'Bords dim 1', type: 'number' }, { key: 'edgesDim2', label: 'Bords dim 2', type: 'number' }]} />}

      {tab === 'lignes' && <AdminTable endpoint="/api/admin/template-lines" help="Lignes d'un gabarit. Panneaux et chants : indiquer le slot (surface et mètres calculés automatiquement à partir des pièces). Autres articles (quincaillerie, colle) : formule de quantité avec L, P, H, Q et CH (= mètres de chant du composant), ex. 2*Q, 8*Q, CH/50 ; sinon la quantité par défaut est utilisée. Dans un devis, l'article de chaque ligne reste remplaçable."
        blank={{ templateName: '', position: 1, articleCode: '', slot: '', quantityFormula: '', defaultQuantity: 0 }}
        cols={[{ key: 'templateName', label: 'Gabarit', width: '150px' }, { key: 'position', label: 'Ordre', type: 'number' }, { key: 'articleCode', label: 'Code article' },
          { key: 'slot', label: 'Slot' }, { key: 'quantityFormula', label: 'Formule quantité' }, { key: 'defaultQuantity', label: 'Quantité par défaut', type: 'number' }]} />}

      {tab === 'panels' && <AdminTable endpoint="/api/admin/panels" help="Un panneau = une référence (dimensions + épaisseur) avec son prix de feuille. Densité : estimation du poids des portes. Module d'élasticité (MPa) : contrôle de flèche."
        blank={{ code: '', name: '', family: '', thicknessMm: 18, lengthMm: 2440, widthMm: 1220, pricePerSheet: 0, grain: false, active: true }}
        cols={[{ key: 'code', label: 'Code' }, { key: 'name', label: 'Nom', width: '180px' }, { key: 'family', label: 'Famille' },
          { key: 'thicknessMm', label: 'Ép. mm', type: 'number' }, { key: 'lengthMm', label: 'Long. mm', type: 'number' }, { key: 'widthMm', label: 'Larg. mm', type: 'number' },
          { key: 'pricePerSheet', label: 'Prix / feuille', type: 'number' }, { key: 'grain', label: 'Fil', type: 'bool' },
          { key: 'densityKgM3', label: 'kg/m³', type: 'number' }, { key: 'youngModulusMpa', label: 'E (MPa)', type: 'number' }, { key: 'active', label: 'Actif', type: 'bool' }]} />}

      {tab === 'edges' && <AdminTable endpoint="/api/admin/edge-bands" blank={{ code: '', name: '', thicknessMm: 1, widthMm: 22, pricePerMeter: 0, active: true }}
        cols={[{ key: 'code', label: 'Code' }, { key: 'name', label: 'Nom', width: '200px' }, { key: 'thicknessMm', label: 'Ép. mm', type: 'number' },
          { key: 'widthMm', label: 'Larg. mm', type: 'number' }, { key: 'pricePerMeter', label: 'Prix / m', type: 'number' }, { key: 'active', label: 'Actif', type: 'bool' }]} />}

      {tab === 'hardware' && <AdminTable endpoint="/api/admin/hardware" help="Catégories reconnues par le calculateur : HINGE, SLIDE (longueur de coulisse requise), SHELF_PIN, HANDLE, CONNECTOR."
        blank={{ code: '', name: '', category: 'OTHER', unitPrice: 0, active: true }}
        cols={[{ key: 'code', label: 'Code' }, { key: 'name', label: 'Nom', width: '220px' }, { key: 'category', label: 'Catégorie', type: 'select', options: CATS },
          { key: 'unitPrice', label: 'Prix unitaire', type: 'number' }, { key: 'slideLengthMm', label: 'Long. coulisse mm', type: 'number' }, { key: 'active', label: 'Actif', type: 'bool' }]} />}

      {tab === 'settings' && <AdminTable endpoint="/api/admin/settings" idKey="id" blank={{ id: '', valueNumeric: 0, unit: '', description: '' }}
        help="Taux horaire, finition, transport, installation, marge (markup %) et devise. panel.costing_mode : 0 = feuilles entières, 1 = surface nette + perte."
        cols={[{ key: 'id', label: 'Clé' }, { key: 'valueNumeric', label: 'Valeur', type: 'number' }, { key: 'valueText', label: 'Texte' }, { key: 'unit', label: 'Unité' }, { key: 'description', label: 'Description', width: '320px' }]} />}

      {tab === 'taxes' && <AdminTable endpoint="/api/admin/taxes" blank={{ code: '', label: '', ratePercent: 0, active: true }}
        cols={[{ key: 'code', label: 'Code' }, { key: 'label', label: 'Libellé' }, { key: 'ratePercent', label: 'Taux %', type: 'number' }, { key: 'active', label: 'Actif', type: 'bool' }]} />}

      {tab === 'labor' && <AdminTable endpoint="/api/admin/labor" help="Temps de référence en minutes par unité de base. À mesurer sur des cas réels. Une ligne d'un type de meuble remplace la ligne DEFAULT de même code."
        blank={{ furnitureType: 'DEFAULT', code: '', label: '', basis: 'PER_UNIT', minutes: 0, validationStatus: 'TO_VALIDATE' }}
        cols={[{ key: 'furnitureType', label: 'Type' }, { key: 'code', label: 'Code' }, { key: 'label', label: 'Opération', width: '240px' },
          { key: 'basis', label: 'Base', type: 'select', options: BASIS }, { key: 'minutes', label: 'Minutes', type: 'number' },
          { key: 'validationStatus', label: 'Statut', type: 'select', options: STATUS }]} />}

      {tab === 'rules' && <div className="space-y-2">
        <label className="text-sm">Type de meuble : <input className="inp inline w-48" value={type} onChange={e => setType(e.target.value.toUpperCase())} /></label>
        <AdminTable endpoint="/api/admin/rules" filter={r => r.furnitureType === type} help="Règles de fabrication. DEFAULT s'applique à tous ; une règle du même nom sur un type la remplace. Statut TO_VALIDATE = valeur empirique à valider par le métier."
          blank={{ furnitureType: type, ruleKey: '', value: 0, unit: '', source: '', validationStatus: 'TO_VALIDATE', description: '' }}
          cols={[{ key: 'ruleKey', label: 'Règle', width: '220px' }, { key: 'value', label: 'Valeur', type: 'number' }, { key: 'unit', label: 'Unité' },
            { key: 'validationStatus', label: 'Statut', type: 'select', options: STATUS }, { key: 'source', label: 'Source', width: '260px' }, { key: 'description', label: 'Description', width: '260px' }]} /></div>}

      {tab === 'thresholds' && <AdminTable endpoint="/api/admin/thresholds" help="Nombre de charnières : le moteur retient le maximum entre la table par hauteur de porte (mm) et la table par poids (kg)."
        blank={{ kind: 'HINGE_HEIGHT', upTo: 0, count: 2, source: '', validationStatus: 'TO_VALIDATE' }}
        cols={[{ key: 'kind', label: 'Table', type: 'select', options: ['HINGE_HEIGHT', 'HINGE_WEIGHT'] }, { key: 'upTo', label: "Jusqu'à (mm ou kg)", type: 'number' },
          { key: 'count', label: 'Charnières', type: 'number' }, { key: 'validationStatus', label: 'Statut', type: 'select', options: STATUS }, { key: 'source', label: 'Source', width: '320px' }]} />}

      {tab === 'types' && <AdminTable endpoint="/api/admin/furniture-types" idKey="id" blank={{ id: '', label: '' }}
        cols={[{ key: 'id', label: 'Code' }, { key: 'label', label: 'Libellé', width: '300px' }]} />}
    </div>
  )
}
