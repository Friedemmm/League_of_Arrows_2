<template>
  <div class="page-wrapper admin-page">
    <div class="container" style="max-width:1100px;">

      <div class="admin-page-header">
        <button class="btn-back" id="btn-back-spatial" @click="$router.push('/dashboard')">
          <span class="material-icons">arrow_back</span> Volver
        </button>
        <div class="header-row">
          <div>
            <h1 class="page-title">
              <span class="material-icons page-title-icon">satellite_alt</span>
              Componente Espacial
            </h1>
            <p class="page-subtitle">Visualización PostGIS — zonas, blancos, posiciones y análisis ambiental.</p>
          </div>
          <div class="form-group" style="min-width:260px;margin-bottom:0;">
            <label class="form-label" for="spatial-tournament">Torneo</label>
            <select id="spatial-tournament" class="form-input" v-model.number="tournamentId">
              <option :value="null" disabled>— Selecciona un torneo —</option>
              <option v-for="t in tournaments" :key="t.tournamentId" :value="t.tournamentId">{{ t.name }}</option>
            </select>
          </div>
        </div>
        <hr class="page-rule" />
      </div>

      <!-- TABS -->
      <div class="mode-tabs">
        <button class="mode-tab" :class="{ active: activeTab === 'campo' }" id="tab-spatial-campo" @click="activeTab = 'campo'">
          <span class="material-icons">map</span> Mapa del Campo
        </button>
        <button class="mode-tab" :class="{ active: activeTab === 'posicion' }" id="tab-spatial-posicion" @click="activeTab = 'posicion'">
          <span class="material-icons">location_on</span> Posición Arquero
        </button>
        <button class="mode-tab" :class="{ active: activeTab === 'ambiental' }" id="tab-spatial-ambiental" @click="activeTab = 'ambiental'">
          <span class="material-icons">query_stats</span> Análisis Ambiental
        </button>
      </div>

      <div v-if="!tournamentId" class="lol-card" style="text-align:center;padding:2.5rem 1rem;">
        <span class="material-icons" style="font-size:2rem;opacity:0.3;">place</span>
        <p class="text-muted" style="margin-top:0.5rem;">Selecciona un torneo para visualizar su campo.</p>
      </div>

      <!-- ══ TAB: MAPA DEL CAMPO ══ -->
      <div v-else-if="activeTab === 'campo'" class="lol-card" id="spatial-campo-tab">
        <div class="draw-mode-toolbar">
          <button class="btn btn-sm" :class="drawMode === 'campo' ? 'btn-gold' : 'btn-ghost'" @click="setDrawMode('campo')">
            <span class="material-icons btn-icon" style="font-size:0.9rem;">terrain</span> Campo Oficial
          </button>
          <button class="btn btn-sm" :class="drawMode === 'zona' ? 'btn-gold' : 'btn-ghost'" @click="setDrawMode('zona')">
            <span class="material-icons btn-icon" style="font-size:0.9rem;">crop_free</span> Zona de Competencia
          </button>
          <button class="btn btn-sm" :class="drawMode === 'blanco' ? 'btn-gold' : 'btn-ghost'" @click="setDrawMode('blanco')">
            <span class="material-icons btn-icon" style="font-size:0.9rem;">my_location</span> Blanco
          </button>
        </div>

        <div id="spatial-campo-map" style="height: 420px; width: 100%; border-radius: 8px;margin-bottom:1rem;"></div>

        <Transition name="slide-up">
          <div class="alert alert-error" v-if="campoError" role="alert">
            <span class="material-icons alert-icon">error</span>
            <div class="alert-body"><span class="alert-msg">{{ campoError }}</span></div>
          </div>
        </Transition>
        <Transition name="slide-up">
          <div class="alert alert-success" v-if="campoSuccess" role="status">
            <span class="material-icons alert-icon">check_circle</span>
            <div class="alert-body"><span class="alert-msg">{{ campoSuccess }}</span></div>
          </div>
        </Transition>

        <div class="grid-2">
          <!-- ── Campo oficial ── -->
          <div v-if="drawMode === 'campo'">
            <h4 class="text-gold" style="margin-bottom:0.5rem;">
              <span class="material-icons" style="vertical-align:middle;font-size:1.1rem;">terrain</span> Campo Oficial
            </h4>
            <p class="hint-text" style="color:var(--text-muted);">
              Haz click en el mapa para agregar vértices del polígono (mínimo 3). Un torneo tiene un solo campo;
              guardar reemplaza el existente.
            </p>
            <p v-if="fieldFeature" class="text-secondary" style="font-size:0.8rem;">
              Campo actual: <strong class="text-gold">{{ fieldFeature.properties.name }}</strong>
            </p>
            <div class="form-group">
              <label class="form-label" for="field-name-input">Nombre del campo</label>
              <input id="field-name-input" class="form-input" v-model="fieldName" placeholder="Ej. Campo Nacional de Tiro con Arco" />
            </div>
            <p class="hint-text">{{ drawPoints.length }} vértice(s) definidos</p>
            <div class="flex gap-1">
              <button class="btn btn-gold btn-sm" :disabled="savingCampo || drawPoints.length < 3 || !fieldName.trim()" @click="saveField">
                <span class="material-icons btn-icon" style="font-size:0.9rem;">save</span> Guardar Campo
              </button>
              <button class="btn btn-ghost btn-sm" :disabled="drawPoints.length === 0" @click="clearDrawPoints">Limpiar</button>
            </div>
          </div>

          <!-- ── Zona de competencia ── -->
          <div v-if="drawMode === 'zona'">
            <h4 class="text-gold" style="margin-bottom:0.5rem;">
              <span class="material-icons" style="vertical-align:middle;font-size:1.1rem;">crop_free</span> Nueva Zona de Competencia
            </h4>
            <p class="hint-text" style="color:var(--text-muted);">
              Haz click en el mapa para agregar vértices del polígono. Mínimo 3 puntos.
            </p>
            <ul v-if="zoneFeatures.length > 0" class="inscribed-list" style="margin-bottom:0.8rem;">
              <li v-for="z in zoneFeatures" :key="z.properties.id_zone" class="inscribed-item">
                <span class="material-icons" style="font-size:1rem;color:#c89b3c;flex-shrink:0;">layers</span>
                <span class="inscribed-name">{{ z.properties.name }}</span>
                <button class="btn btn-danger btn-sm icon-btn" style="margin-left:auto;" @click="deleteZone(z.properties.id_zone)" title="Eliminar zona">
                  <span class="material-icons" style="font-size:0.9rem;">delete</span>
                </button>
              </li>
            </ul>
            <div class="form-group">
              <label class="form-label" for="zone-name-input">Nombre de la zona</label>
              <input id="zone-name-input" class="form-input" v-model="zoneName" placeholder="Ej. Campo Principal" />
            </div>
            <p class="hint-text">{{ drawPoints.length }} vértice(s) definidos</p>
            <div class="flex gap-1">
              <button class="btn btn-gold btn-sm" :disabled="savingCampo || drawPoints.length < 3 || !zoneName.trim()" @click="saveZone">
                <span class="material-icons btn-icon" style="font-size:0.9rem;">save</span> Crear Zona
              </button>
              <button class="btn btn-ghost btn-sm" :disabled="drawPoints.length === 0" @click="clearDrawPoints">Limpiar</button>
            </div>
          </div>

          <!-- ── Blancos ── -->
          <div v-if="drawMode === 'blanco'">
            <h4 class="text-gold" style="margin-bottom:0.5rem;">
              <span class="material-icons" style="vertical-align:middle;font-size:1.1rem;">my_location</span> Nuevo Blanco
            </h4>
            <p class="hint-text" style="color:var(--text-muted);">Haz click en el mapa para colocar un blanco (punto único).</p>
            <ul v-if="targetFeatures.length > 0" class="inscribed-list" style="margin-bottom:0.8rem;">
              <li v-for="tg in targetFeatures" :key="tg.properties.id_target" class="inscribed-item">
                <span class="material-icons" style="font-size:1rem;color:#5b8dd6;flex-shrink:0;">gps_fixed</span>
                <span class="inscribed-name">{{ tg.properties.label }} ({{ tg.properties.required_distance_m }} m)</span>
                <button class="btn btn-danger btn-sm icon-btn" style="margin-left:auto;" @click="deleteTarget(tg.properties.id_target)" title="Eliminar blanco">
                  <span class="material-icons" style="font-size:0.9rem;">delete</span>
                </button>
              </li>
            </ul>
            <div class="form-group">
              <label class="form-label" for="target-label-input">Etiqueta</label>
              <input id="target-label-input" class="form-input" v-model="targetLabel" placeholder="Ej. Blanco A" />
            </div>
            <div class="form-group">
              <label class="form-label" for="target-distance-input">Distancia normativa (m)</label>
              <input id="target-distance-input" class="form-input" type="number" min="0" v-model.number="targetDistance" />
            </div>
            <div class="form-group">
              <label class="form-label" for="target-category-input">Categoría</label>
              <select id="target-category-input" class="form-input" v-model.number="targetCategoryId">
                <option :value="null" disabled>— Categoría —</option>
                <option v-for="c in categories" :key="c.id_category ?? c.idCategory" :value="c.id_category ?? c.idCategory">{{ c.name }}</option>
              </select>
            </div>
            <div class="flex gap-1">
              <button class="btn btn-gold btn-sm"
                :disabled="savingCampo || !drawPoint || !targetLabel.trim() || !targetCategoryId || !targetDistance"
                @click="saveTarget">
                <span class="material-icons btn-icon" style="font-size:0.9rem;">save</span> Crear Blanco
              </button>
              <button class="btn btn-ghost btn-sm" :disabled="!drawPoint" @click="clearDrawPoints">Limpiar</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ══ TAB: POSICIÓN ARQUERO ══ -->
      <div v-else-if="activeTab === 'posicion'" class="lol-card" id="spatial-posicion-tab">
        <div class="grid-2">
          <div>
            <h4 class="text-gold" style="margin-bottom:0.5rem;">
              <span class="material-icons" style="vertical-align:middle;font-size:1.1rem;">gps_fixed</span> Registrar Posición GPS
            </h4>
            <p class="hint-text" style="color:var(--text-muted);margin-bottom:1rem;">
              Selecciona los datos y haz click en el mapa para ubicar al arquero.
            </p>

            <Transition name="slide-up">
              <div class="alert alert-error" v-if="posError" role="alert">
                <span class="material-icons alert-icon">error</span>
                <div class="alert-body"><span class="alert-msg">{{ posError }}</span></div>
              </div>
            </Transition>
            <Transition name="slide-up">
              <div class="alert alert-success" v-if="posSuccess" role="status">
                <span class="material-icons alert-icon">check_circle</span>
                <div class="alert-body"><span class="alert-msg">{{ posSuccess }}</span></div>
              </div>
            </Transition>

            <div class="form-group">
              <label class="form-label" for="pos-archer">Arquero</label>
              <select id="pos-archer" class="form-input" v-model.number="posArcherId">
                <option :value="null" disabled>— Selecciona un arquero —</option>
                <option v-for="a in archers" :key="a.archerId" :value="a.archerId">{{ a.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label" for="pos-round">Ronda</label>
              <div v-if="loadingRounds" class="loading-center" style="padding:0.5rem 0;"><div class="spinner"></div></div>
              <select v-else id="pos-round" class="form-input" v-model.number="posRoundId" :disabled="!posArcherId">
                <option :value="null" disabled>
                  {{ posArcherId ? '— Selecciona una ronda —' : 'Selecciona primero un arquero' }}
                </option>
                <option v-for="r in archerRounds" :key="r.roundId" :value="r.roundId">
                  Ronda {{ r.roundNumber }} (#{{ r.roundId }})
                </option>
              </select>
              <p v-if="posArcherId && !loadingRounds && archerRounds.length === 0" class="hint-text">
                Este arquero no tiene rondas registradas en este torneo.
              </p>
            </div>
            <div class="form-group">
              <label class="form-label" for="pos-target">Blanco asignado (opcional)</label>
              <select id="pos-target" class="form-input" v-model.number="posTargetId">
                <option :value="null">— Sin blanco asignado —</option>
                <option v-for="tg in targetFeatures" :key="tg.properties.id_target" :value="tg.properties.id_target">
                  {{ tg.properties.label }} ({{ tg.properties.required_distance_m }} m)
                </option>
              </select>
            </div>

            <p class="hint-text" style="margin-bottom:0.5rem;">
              <span class="material-icons" style="font-size:0.9rem;vertical-align:middle;">touch_app</span>
              Haz click en el mapa para fijar su posición.
            </p>
            <p v-if="posPoint" class="coords-badge">
              <span class="material-icons" style="font-size:0.9rem;">place</span>
              {{ posPoint.lat.toFixed(6) }}, {{ posPoint.lng.toFixed(6) }}
            </p>

            <div class="flex gap-1" style="margin-top:0.8rem;">
              <button class="btn btn-gold" :disabled="savingPos || !posArcherId || !posRoundId || !posPoint" @click="registerPosition">
                <span class="material-icons btn-icon">send</span>
                {{ savingPos ? 'Registrando...' : 'Registrar Posición' }}
              </button>
              <button class="btn btn-ghost" @click="clearPosition">Limpiar</button>
            </div>

            <div class="validation-info">
              <p><span class="material-icons" style="font-size:0.85rem;vertical-align:middle;">verified</span> El backend valida automáticamente:</p>
              <ul>
                <li>Geocerca (arquero dentro del campo oficial)</li>
                <li>Zona de competencia (dentro de la zona activa)</li>
                <li>Distancia de seguridad (mínimo 5 m entre arqueros)</li>
                <li>Distancia normativa del blanco (si se asigna uno)</li>
              </ul>
            </div>
          </div>

          <div id="spatial-position-map" style="height: 480px; width: 100%; border-radius: 8px;"></div>
        </div>
      </div>

      <!-- ══ TAB: ANÁLISIS AMBIENTAL ══ -->
      <div v-else-if="activeTab === 'ambiental'" class="lol-card" id="spatial-ambiental-tab">
        <h4 class="text-gold" style="margin-bottom:0.8rem;">
          <span class="material-icons" style="vertical-align:middle;font-size:1.1rem;">query_stats</span>
          Correlación condición ambiental vs. precisión
        </h4>
        <div v-if="loadingEnv" class="loading-center" style="padding:2rem;"><div class="spinner"></div></div>
        <div v-else-if="envData.length === 0" class="text-muted" style="text-align:center;padding:1.5rem 0;font-size:0.85rem;">
          No hay datos ambientales suficientes para este torneo (se necesitan rondas con posición, blanco asignado y zonas ambientales definidas).
        </div>
        <div v-else class="lol-table-wrapper">
          <table class="lol-table">
            <thead>
              <tr>
                <th>Condición</th>
                <th>Intensidad</th>
                <th>Rondas evaluadas</th>
                <th>Precisión promedio</th>
                <th>Correlación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in envData" :key="i">
                <td>{{ row.zoneType }}</td>
                <td><span class="badge badge-blue">{{ row.intensity }}</span></td>
                <td>{{ row.roundsEvaluated }}</td>
                <td>{{ row.avgPrecision }}</td>
                <td>{{ row.correlationCoefficient !== null ? row.correlationCoefficient.toFixed(3) : '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'
import api from '@/api/axios'
import { getTournaments } from '@/api/tournaments'
import { getArchers } from '@/api/archers'

L.Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl })

const DEFAULT_CENTER = [-33.456, -70.666]
const DEFAULT_ZOOM = 15

const tournaments = ref([])
const archers     = ref([])
const categories  = ref([])
const tournamentId = ref(null)
const activeTab     = ref('campo')

// Datos espaciales del torneo activo (compartidos entre pestañas)
const fieldFeature   = ref(null)
const zoneFeatures    = ref([])
const targetFeatures  = ref([])

// ── Pestaña "Mapa del Campo" ──────────────────────────────────────────
const campoMap   = ref(null)
const contextLayerCampo = ref(null)
const drawLayer  = ref(null)
const drawMode   = ref('campo') // 'campo' | 'zona' | 'blanco'
const drawPoints = ref([])       // para campo/zona (polígono)
const drawPoint  = ref(null)     // para blanco (punto único)
const fieldName  = ref('')
const zoneName   = ref('')
const targetLabel = ref('')
const targetDistance = ref(null)
const targetCategoryId = ref(null)
const campoError   = ref('')
const campoSuccess = ref('')
const savingCampo  = ref(false)

// ── Pestaña "Posición Arquero" ────────────────────────────────────────
const positionMap = ref(null)
const contextLayerPos = ref(null)
const posMarker    = ref(null)
const posArcherId  = ref(null)
const posRoundId   = ref(null)
const posTargetId  = ref(null)
const posPoint     = ref(null)
const posError     = ref('')
const posSuccess    = ref('')
const savingPos     = ref(false)
const archerRounds  = ref([])
const loadingRounds = ref(false)

// ── Pestaña "Análisis Ambiental" ──────────────────────────────────────
const envData     = ref([])
const loadingEnv  = ref(false)

async function loadInitialData() {
  try {
    const [tourRes, archerRes, catRes] = await Promise.all([
      getTournaments(),
      getArchers(),
      api.get('/categories'),
    ])
    tournaments.value = tourRes.data
    archers.value     = archerRes.data
    categories.value  = catRes.data
  } catch (e) {
    console.error('[AdminSpatial] error cargando datos base:', e.message)
  }
}
loadInitialData()

async function loadSpatialData(id) {
  fieldFeature.value  = null
  zoneFeatures.value  = []
  targetFeatures.value = []
  try {
    const [fieldRes, zoneRes, targetRes] = await Promise.all([
      api.get(`/fields/tournament/${id}`),
      api.get(`/competition-zones/tournament/${id}`),
      api.get(`/targets/tournament/${id}`),
    ])
    fieldFeature.value   = (fieldRes.data.features || [])[0] || null
    zoneFeatures.value   = zoneRes.data.features || []
    targetFeatures.value = targetRes.data.features || []
  } catch (e) {
    console.error('[AdminSpatial] error cargando geometría del torneo:', e.message)
  }
}

watch(tournamentId, async (id) => {
  resetCampoState()
  resetPositionState()
  if (id) await loadSpatialData(id)
  await nextTick()
  if (activeTab.value === 'campo') initCampoMap()
  if (activeTab.value === 'posicion') initPositionMap()
  if (activeTab.value === 'ambiental') loadEnvironmental(id)
})

watch(activeTab, async (tab) => {
  await nextTick()
  if (tab === 'campo') initCampoMap()
  if (tab === 'posicion') initPositionMap()
  if (tab === 'ambiental') loadEnvironmental(tournamentId.value)
})

onBeforeUnmount(() => {
  if (campoMap.value) campoMap.value.remove()
  if (positionMap.value) positionMap.value.remove()
})

// ── Capas de contexto compartidas (campo + zonas + blancos) ──────────
function drawContextLayers(map, layerRefHolder) {
  if (layerRefHolder.value) {
    map.removeLayer(layerRefHolder.value)
    layerRefHolder.value = null
  }
  const group = L.featureGroup()
  if (fieldFeature.value) {
    L.geoJSON({ type: 'Feature', ...fieldFeature.value }, {
      style: { color: '#5b8dd6', weight: 2, fillOpacity: 0.03, dashArray: '3 3' },
    }).addTo(group)
  }
  zoneFeatures.value.forEach(z => {
    L.geoJSON({ type: 'Feature', ...z }, {
      style: { color: '#c89b3c', weight: 2, fillColor: '#c89b3c', fillOpacity: 0.12 },
    }).addTo(group)
  })
  targetFeatures.value.forEach(tg => {
    L.geoJSON({ type: 'Feature', ...tg }, {
      pointToLayer: (feat, latlng) => L.circleMarker(latlng, {
        radius: 6, color: '#e8b23c', fillColor: '#e8b23c', fillOpacity: 0.9, weight: 1,
      }).bindTooltip(feat.properties.label || 'Blanco'),
    }).addTo(group)
  })
  group.addTo(map)
  layerRefHolder.value = group

  const bounds = group.getBounds ? group.getBounds() : null
  if (bounds && bounds.isValid && bounds.isValid()) {
    map.invalidateSize(false)
    const targetZoom = Math.min(18, map.getBoundsZoom(bounds, false))
    map.setView(bounds.getCenter(), targetZoom, { animate: false })
  }
}

// ── Mapa del Campo (dibujo) ────────────────────────────────────────────
function initCampoMap() {
  if (campoMap.value) { campoMap.value.remove(); campoMap.value = null }
  const el = document.getElementById('spatial-campo-map')
  if (!el) return
  campoMap.value = L.map('spatial-campo-map', { zoomAnimation: false }).setView(DEFAULT_CENTER, DEFAULT_ZOOM)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '© OpenStreetMap contributors',
  }).addTo(campoMap.value)
  drawContextLayers(campoMap.value, contextLayerCampo)

  campoMap.value.on('click', (e) => {
    if (drawMode.value === 'blanco') {
      drawPoint.value = e.latlng
    } else {
      drawPoints.value.push(e.latlng)
    }
    redrawDrawLayer()
  })
}

function redrawDrawLayer() {
  if (!campoMap.value) return
  if (drawLayer.value) {
    campoMap.value.removeLayer(drawLayer.value)
    drawLayer.value = null
  }
  if (drawMode.value === 'blanco') {
    if (drawPoint.value) {
      drawLayer.value = L.marker(drawPoint.value).addTo(campoMap.value)
    }
    return
  }
  const pts = drawPoints.value
  if (pts.length === 0) return
  if (pts.length < 3) {
    drawLayer.value = L.polyline(pts, { color: '#c89b3c', weight: 2, dashArray: '4 4' }).addTo(campoMap.value)
  } else {
    drawLayer.value = L.polygon(pts, { color: '#c89b3c', weight: 2, fillColor: '#c89b3c', fillOpacity: 0.2 }).addTo(campoMap.value)
  }
}

function setDrawMode(mode) {
  drawMode.value = mode
  clearDrawPoints()
}

function clearDrawPoints() {
  drawPoints.value = []
  drawPoint.value = null
  redrawDrawLayer()
}

function resetCampoState() {
  drawPoints.value = []
  drawPoint.value = null
  fieldName.value = ''
  zoneName.value = ''
  targetLabel.value = ''
  targetDistance.value = null
  targetCategoryId.value = null
  campoError.value = campoSuccess.value = ''
}

function ringFromPoints(points) {
  const ring = points.map(p => [p.lng, p.lat])
  ring.push(ring[0])
  return ring
}

async function saveField() {
  campoError.value = campoSuccess.value = ''
  savingCampo.value = true
  try {
    await api.post('/fields', {
      idTournament: tournamentId.value,
      name: fieldName.value.trim(),
      geometry: { type: 'Polygon', coordinates: [ringFromPoints(drawPoints.value)] },
    })
    campoSuccess.value = 'Campo guardado correctamente.'
    fieldName.value = ''
    clearDrawPoints()
    await loadSpatialData(tournamentId.value)
    drawContextLayers(campoMap.value, contextLayerCampo)
  } catch (e) {
    campoError.value = e.response?.data?.error || 'Error al guardar el campo.'
  } finally { savingCampo.value = false }
}

async function saveZone() {
  campoError.value = campoSuccess.value = ''
  savingCampo.value = true
  try {
    await api.post('/competition-zones', {
      idTournament: tournamentId.value,
      name: zoneName.value.trim(),
      geometry: { type: 'Polygon', coordinates: [ringFromPoints(drawPoints.value)] },
    })
    campoSuccess.value = 'Zona creada correctamente.'
    zoneName.value = ''
    clearDrawPoints()
    await loadSpatialData(tournamentId.value)
    drawContextLayers(campoMap.value, contextLayerCampo)
  } catch (e) {
    campoError.value = e.response?.data?.error || 'Error al crear la zona.'
  } finally { savingCampo.value = false }
}

async function deleteZone(idZone) {
  campoError.value = campoSuccess.value = ''
  try {
    await api.delete(`/competition-zones/${idZone}`)
    campoSuccess.value = 'Zona eliminada.'
    await loadSpatialData(tournamentId.value)
    drawContextLayers(campoMap.value, contextLayerCampo)
  } catch (e) {
    campoError.value = e.response?.data?.error || 'Error al eliminar la zona.'
  }
}

async function saveTarget() {
  campoError.value = campoSuccess.value = ''
  savingCampo.value = true
  try {
    await api.post('/targets', {
      idTournament: tournamentId.value,
      idCategory: targetCategoryId.value,
      label: targetLabel.value.trim(),
      requiredDistanceM: targetDistance.value,
      geometry: { type: 'Point', coordinates: [drawPoint.value.lng, drawPoint.value.lat] },
    })
    campoSuccess.value = 'Blanco creado correctamente.'
    targetLabel.value = ''
    targetDistance.value = null
    targetCategoryId.value = null
    clearDrawPoints()
    await loadSpatialData(tournamentId.value)
    drawContextLayers(campoMap.value, contextLayerCampo)
  } catch (e) {
    campoError.value = e.response?.data?.error || 'Error al crear el blanco.'
  } finally { savingCampo.value = false }
}

async function deleteTarget(idTarget) {
  campoError.value = campoSuccess.value = ''
  try {
    await api.delete(`/targets/${idTarget}`)
    campoSuccess.value = 'Blanco eliminado.'
    await loadSpatialData(tournamentId.value)
    drawContextLayers(campoMap.value, contextLayerCampo)
  } catch (e) {
    campoError.value = e.response?.data?.error || 'Error al eliminar el blanco.'
  }
}

// ── Posición Arquero ───────────────────────────────────────────────────
function initPositionMap() {
  if (positionMap.value) { positionMap.value.remove(); positionMap.value = null }
  const el = document.getElementById('spatial-position-map')
  if (!el) return
  positionMap.value = L.map('spatial-position-map', { zoomAnimation: false }).setView(DEFAULT_CENTER, DEFAULT_ZOOM)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '© OpenStreetMap contributors',
  }).addTo(positionMap.value)
  drawContextLayers(positionMap.value, contextLayerPos)

  positionMap.value.on('click', (e) => {
    posPoint.value = e.latlng
    if (posMarker.value) positionMap.value.removeLayer(posMarker.value)
    posMarker.value = L.marker(e.latlng).addTo(positionMap.value)
  })
}

function clearPosition() {
  posPoint.value = null
  posError.value = posSuccess.value = ''
  if (posMarker.value && positionMap.value) {
    positionMap.value.removeLayer(posMarker.value)
    posMarker.value = null
  }
}

function resetPositionState() {
  posArcherId.value = null
  posRoundId.value = null
  posTargetId.value = null
  archerRounds.value = []
  clearPosition()
}

// Al elegir arquero, cargamos SUS rondas para ESTE torneo — evita que el
// admin escriba a mano un id_round que en realidad pertenezca a otro torneo
// (eso hacía que la geocerca validara contra el campo equivocado).
watch(posArcherId, async (archerId) => {
  posRoundId.value = null
  archerRounds.value = []
  if (!archerId || !tournamentId.value) return
  loadingRounds.value = true
  try {
    const res = await api.get('/rounds', { params: { tournamentId: tournamentId.value, archerId } })
    archerRounds.value = res.data || []
  } catch (e) {
    console.error('[AdminSpatial] error cargando rondas del arquero:', e.message)
  } finally { loadingRounds.value = false }
})

async function registerPosition() {
  posError.value = posSuccess.value = ''
  savingPos.value = true
  try {
    const res = await api.put(`/rounds/${posRoundId.value}/position`, {
      lon: posPoint.value.lng,
      lat: posPoint.value.lat,
      targetId: posTargetId.value,
    })
    posSuccess.value = res.data?.message || 'Posición registrada correctamente.'
  } catch (e) {
    posError.value = e.response?.data?.error || 'Error de validación PostGIS al registrar la posición.'
  } finally { savingPos.value = false }
}

// ── Análisis Ambiental ─────────────────────────────────────────────────
async function loadEnvironmental(id) {
  if (!id) { envData.value = []; return }
  loadingEnv.value = true
  try {
    const res = await api.get(`/tournaments/${id}/environmental-correlation`)
    envData.value = res.data || []
  } catch (e) {
    console.error('[AdminSpatial] error cargando análisis ambiental:', e.message)
    envData.value = []
  } finally { loadingEnv.value = false }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/icon?family=Material+Icons');
.admin-page { padding: calc(var(--header-height, 70px) + 2rem) 0 4rem; min-height: 100vh; }
.header-row { display:flex; justify-content:space-between; align-items:flex-end; gap:1rem; flex-wrap:wrap; }
.btn-back { display:inline-flex;align-items:center;gap:0.3rem;background:none;border:none;color:var(--text-muted);cursor:pointer;font-family:'Cinzel',serif;font-size:0.7rem;text-transform:uppercase;letter-spacing:0.1em;padding:0;margin-bottom:1rem;transition:color 0.2s; }
.btn-back:hover { color: var(--lol-gold); }
.btn-back .material-icons { font-size: 1rem; }
.page-title { font-size:1.5rem;font-family:'Cinzel',serif;display:flex;align-items:center;gap:0.5rem;margin-bottom:0.3rem; }
.page-title-icon { font-size:1.3rem;color:var(--lol-gold); }
.page-subtitle { font-size:0.82rem;color:var(--text-muted);margin:0; }
.page-rule { margin:1rem 0 1.5rem; }
.btn-icon { display:inline-flex;align-items:center;gap:0.3rem; }
.mode-tabs { display:flex;gap:0.5rem;margin-bottom:1.2rem; }
.mode-tab { display:flex;align-items:center;gap:0.4rem;padding:0.55rem 1.2rem;font-family:'Cinzel',serif;font-size:0.72rem;font-weight:700;text-transform:uppercase;letter-spacing:0.1em;background:transparent;border:1px solid var(--lol-border,rgba(200,155,60,0.2));color:var(--text-muted);cursor:pointer;transition:all 0.2s; }
.mode-tab .material-icons { font-size:1rem; }
.mode-tab:hover { border-color:var(--lol-gold-dark);color:var(--lol-gold-light); }
.mode-tab.active { background:rgba(200,155,60,0.1);border-color:var(--lol-gold);color:var(--lol-gold-bright); }
.draw-mode-toolbar { display:flex;gap:0.5rem;margin-bottom:0.8rem;flex-wrap:wrap; }
.hint-text { font-size:0.78rem;color:var(--text-muted);margin:0.3rem 0; }
.grid-2 { display:grid;grid-template-columns:1fr 1fr;gap:1.5rem; }
.coords-badge { display:inline-flex;align-items:center;gap:0.3rem;font-family:monospace;font-size:0.8rem;color:var(--lol-gold-light);background:rgba(200,155,60,0.08);border:1px solid rgba(200,155,60,0.2);border-radius:4px;padding:0.3rem 0.6rem; }
.validation-info { margin-top:1.2rem;font-size:0.76rem;color:var(--text-muted);background:rgba(255,255,255,0.02);border-radius:6px;padding:0.7rem 0.9rem; }
.validation-info ul { margin:0.3rem 0 0 1.2rem;padding:0; }
.validation-info li { margin-bottom:0.15rem; }
@media (max-width: 800px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
