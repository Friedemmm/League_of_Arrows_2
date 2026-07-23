<template>
  <div class="scoring-map-wrap">

    <!-- Toolbar superior -->
    <div class="map-toolbar">
      <!-- Badge de categoría y distancia -->
      <div class="category-badge" v-if="categoryName">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
        {{ displayCategoryName }} · <strong>{{ laneDistanceM }} m</strong>
      </div>

      <!-- Instrucción inicial -->
      <span class="map-hint" v-if="!conePos">Haz click en el campo para colocar el cono del arquero</span>

      <!-- Controles de rotación (solo cuando hay cono y NO está bloqueado) -->
      <template v-if="conePos && !locked">
        <div class="rotation-controls">
          <button type="button" class="rot-btn" @click="rotate(-15)" title="Rotar izquierda 15°">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M7.11 8.53L5.7 7.11C4.8 8.27 4.24 9.61 4.07 11h2.02c.14-.87.49-1.72 1.02-2.47zM6.09 13H4.07c.17 1.39.72 2.73 1.62 3.89l1.41-1.42c-.52-.75-.87-1.59-1.01-2.47zm1.01 5.32c1.16.9 2.51 1.44 3.9 1.61V17.9c-.87-.15-1.71-.49-2.46-1.03L7.1 18.32zM13 4.07V1L8.45 5.55 13 10V6.09c2.84.48 5 2.94 5 5.91s-2.16 5.43-5 5.91v2.02c3.95-.49 7-3.85 7-7.93s-3.05-7.44-7-7.93z"/></svg>
          </button>
          <span class="bearing-display">{{ bearing }}°</span>
          <button type="button" class="rot-btn" @click="rotate(+15)" title="Rotar derecha 15°">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02c-2.84-.48-5-2.94-5-5.91s2.16-5.43 5-5.91V10l4.55-4.45zm4.41 6.45h-2.02c-.14.87-.49 1.72-1.02 2.47l1.41 1.42c.9-1.16 1.45-2.5 1.63-3.89zm-1.63 5.31l-1.41-1.42c-.75.53-1.59.88-2.47 1.02v2.02c1.39-.17 2.74-.71 3.88-1.62z"/></svg>
          </button>
        </div>

        <button type="button" class="map-mode-btn btn-clear" @click="clearAll">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z"/></svg>
          Limpiar
        </button>
      </template>

      <!-- Indicador de posición bloqueada -->
      <div class="lock-badge" v-if="locked && conePos">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
        Posición fija
      </div>
    </div>

    <!-- Mapa -->
    <div :id="mapId" class="scoring-map" :class="{ 'map-locked': locked }"></div>

    <!-- Barra de estado -->
    <div class="map-status-bar">
      <div class="status-item" :class="conePos ? 'status-ok' : 'status-empty'">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
        <span v-if="conePos">Cono: {{ conePos.lat.toFixed(5) }}, {{ conePos.lng.toFixed(5) }}</span>
        <span v-else>Sin cono</span>
      </div>
      <div class="status-item" :class="targetPos ? 'status-ok' : 'status-empty'">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="3"/><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z"/></svg>
        <span v-if="targetPos">Blanco: {{ targetPos.lat.toFixed(5) }}, {{ targetPos.lng.toFixed(5) }}</span>
        <span v-else>Blanco (auto)</span>
      </div>
      <div class="status-item status-dist" v-if="distance !== null">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H3V8h18v8z"/></svg>
        <strong>{{ distance.toFixed(1) }} m</strong>
        <span v-if="categoryName">/ {{ laneDistanceM }} m norma</span>
      </div>
    </div>

    <!-- Advertencias -->
    <div v-if="outOfFieldWarning" class="map-alert alert-danger">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
      {{ outOfFieldWarning }}
    </div>
    <div v-else-if="proximityWarning" class="map-alert alert-warn">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
      {{ proximityWarning }}
    </div>
    <div v-if="!hasZone" class="map-alert alert-info">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>
      Sin campo definido para este torneo. Define el campo en Gestión de Torneos.
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import iconUrl       from 'leaflet/dist/images/marker-icon.png'
import shadowUrl     from 'leaflet/dist/images/marker-shadow.png'
import api from '@/api/axios'

L.Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl })

// ── Props & Emits ─────────────────────────────────────────────────────────
const props = defineProps({
  tournamentId: { type: [Number, String], default: null },
  mapId:        { type: String, default: 'scoring-map' },
  otherCones:   { type: Array,  default: () => [] },
  categoryName: { type: String, default: '' },
  initialCone:  { type: Object, default: null },
  locked:       { type: Boolean, default: false },  // true = position fixed, no clicking
})
const emit = defineEmits(['cone-selected', 'target-selected', 'distance-changed'])

// ── Category → distance mapping (World Archery outdoor standard) ──────────
const CATEGORY_DISTANCES = {
  recurvo:   70, recurve:   70,
  compuesto: 50, compound:  50,
  curvo:     50, 'bare bow': 50, barebow: 50, desnudo: 50, arco_desnudo: 50,
  longbow:   50, largo:     50,
  instintivo: 25,
}
const laneDistanceM = computed(() => {
  if (!props.categoryName) return 18
  const key = props.categoryName.toLowerCase().trim()
  for (const [k, v] of Object.entries(CATEGORY_DISTANCES)) {
    if (key.includes(k)) return v
  }
  return 18  // indoor default
})
const displayCategoryName = computed(() => {
  if (!props.categoryName) return ''
  const n = props.categoryName
  return n.charAt(0).toUpperCase() + n.slice(1).toLowerCase()
})

// ── State ─────────────────────────────────────────────────────────────────
const conePos          = ref(null)   // { lat, lng } — apex
const targetPos        = ref(null)   // { lat, lng } — base midpoint (auto or manual)
const distance         = ref(null)   // metres
const hasZone          = ref(false)
const bearing          = ref(0)      // degrees from north (0 = north, 90 = east)
const adjustingTarget  = ref(false)  // fine-tune target mode
const outOfFieldWarning  = ref('')
const proximityWarning   = ref('')

// ── Leaflet objects ───────────────────────────────────────────────────────
let map               = null
let coneMarker        = null
let targetMarker      = null
let lanePoly          = null   // the shooting-lane triangle
let distanceLine      = null   // dashed line apex→target
let zoneLayer         = null
let otherConeGroup    = null
let fieldPolygonRings = []     // for point-in-polygon

// ── Geometry helpers ──────────────────────────────────────────────────────
function movePoint(lat, lng, bearingDeg, distM) {
  const R  = 6371000
  const b  = (bearingDeg % 360) * Math.PI / 180
  const φ1 = lat * Math.PI / 180
  const λ1 = lng * Math.PI / 180
  const φ2 = Math.asin(
    Math.sin(φ1) * Math.cos(distM / R) +
    Math.cos(φ1) * Math.sin(distM / R) * Math.cos(b)
  )
  const λ2 = λ1 + Math.atan2(
    Math.sin(b) * Math.sin(distM / R) * Math.cos(φ1),
    Math.cos(distM / R) - Math.sin(φ1) * Math.sin(φ2)
  )
  return { lat: φ2 * 180 / Math.PI, lng: λ2 * 180 / Math.PI }
}

function haversine(lat1, lng1, lat2, lng2) {
  const R  = 6371000
  const d1 = (lat2 - lat1) * Math.PI / 180
  const d2 = (lng2 - lng1) * Math.PI / 180
  const a  = Math.sin(d1/2)**2 + Math.cos(lat1*Math.PI/180)*Math.cos(lat2*Math.PI/180)*Math.sin(d2/2)**2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// Ray-casting point-in-polygon (GeoJSON ring: [[lng,lat], ...])
function pointInPolygon(lat, lng, rings) {
  if (!rings || rings.length === 0) return true
  const ring = rings[0]
  let inside = false
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i]
    const [xj, yj] = ring[j]
    if (((yi > lat) !== (yj > lat)) && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi))
      inside = !inside
  }
  return inside
}

// Compute the 3 base points of the shooting lane triangle
function computeLaneTriangle(apexLat, apexLng, brng, distM) {
  const HALF_BASE_M = 3   // 3m each side → 6m total base width
  const baseMid  = movePoint(apexLat, apexLng, brng, distM)
  const baseLeft = movePoint(baseMid.lat, baseMid.lng, brng + 90, HALF_BASE_M)
  const baseRight= movePoint(baseMid.lat, baseMid.lng, brng - 90, HALF_BASE_M)
  return { baseMid, baseLeft, baseRight }
}

// ── SVG Icons (Material Icons don't work inside Leaflet divIcon) ──────────
const APEX_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="36" height="44" viewBox="0 0 36 44">
  <defs><filter id="g1"><feGaussianBlur stdDeviation="2" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>
  <g filter="url(#g1)">
    <path d="M18 2C12 2 7 7 7 13c0 8 11 29 11 29s11-21 11-29c0-6-5-11-11-11z" fill="#00e676"/>
    <circle cx="18" cy="13" r="5" fill="white" opacity="0.9"/>
    <circle cx="18" cy="13" r="2.5" fill="#00c853"/>
  </g>
</svg>`

const TARGET_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 34 34">
  <defs><filter id="g2"><feGaussianBlur stdDeviation="1.5" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>
  <g filter="url(#g2)">
    <circle cx="17" cy="17" r="13" fill="none" stroke="#e84057" stroke-width="2.5"/>
    <circle cx="17" cy="17" r="8"  fill="none" stroke="#e84057" stroke-width="1.8"/>
    <circle cx="17" cy="17" r="3"  fill="#e84057"/>
    <line x1="17" y1="2"  x2="17" y2="9"  stroke="#e84057" stroke-width="2"/>
    <line x1="17" y1="25" x2="17" y2="32" stroke="#e84057" stroke-width="2"/>
    <line x1="2"  y1="17" x2="9"  y2="17" stroke="#e84057" stroke-width="2"/>
    <line x1="25" y1="17" x2="32" y2="17" stroke="#e84057" stroke-width="2"/>
  </g>
</svg>`

const OTHER_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="34" viewBox="0 0 28 34">
  <path d="M14 2C9 2 4.5 6.5 4.5 11.5c0 7 9.5 20.5 9.5 20.5s9.5-13.5 9.5-20.5C23.5 6.5 19 2 14 2z" fill="#82b1ff" opacity="0.85"/>
  <circle cx="14" cy="11.5" r="4" fill="white" opacity="0.85"/>
</svg>`

const apexIcon  = L.divIcon({ className: '', html: APEX_SVG,   iconSize:[36,44], iconAnchor:[18,44] })
const tgtIcon   = L.divIcon({ className: '', html: TARGET_SVG, iconSize:[34,34], iconAnchor:[17,17] })
const otherIcon = L.divIcon({ className: '', html: OTHER_SVG,  iconSize:[28,34], iconAnchor:[14,34] })

// ── Map lifecycle ─────────────────────────────────────────────────────────
onMounted(() => {
  map = L.map(props.mapId, { zoomAnimation: false }).setView([-33.456, -70.666], 15)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '© OpenStreetMap'
  }).addTo(map)
  otherConeGroup = L.layerGroup().addTo(map)
  map.on('click', onMapClick)
  if (props.tournamentId) loadZone(props.tournamentId)
})

onBeforeUnmount(() => { if (map) { map.remove(); map = null } })

watch(() => props.tournamentId, (id) => {
  clearAll()
  if (id) loadZone(id)
  else { clearZoneLayer(); fieldPolygonRings = [] }
})

watch(() => props.otherCones, renderOtherCones, { deep: true })

// Redraw lane when category distance changes
watch(laneDistanceM, () => { if (conePos.value) drawLane() })

// Pre-populate cone from parent (e.g. Tab 2 loading existing archer position)
watch(() => props.initialCone, (ic) => {
  if (!ic || !map) return
  // Set bearing first
  bearing.value = ic.bearing ?? 0
  // Simulate placing the cone at the given coordinates
  conePos.value = { lat: ic.lat, lng: ic.lng }
  outOfFieldWarning.value = ''
  if (coneMarker) coneMarker.setLatLng([ic.lat, ic.lng])
  else coneMarker = L.marker([ic.lat, ic.lng], { icon: apexIcon, zIndexOffset: 100 }).addTo(map)
  emit('cone-selected', { type: 'Point', coordinates: [ic.lng, ic.lat] })
  drawLane()
}, { deep: true })

// ── Click handler ─────────────────────────────────────────────────────────
function onMapClick(e) {
  if (props.locked) return   // position is fixed for this archer in this tournament
  const { lat, lng } = e.latlng

  // Normal mode: place/move apex
  if (hasZone.value && fieldPolygonRings.length > 0 && !pointInPolygon(lat, lng, fieldPolygonRings)) {
    outOfFieldWarning.value = '🚫 El cono del arquero debe estar dentro del campo de competencia (área dorada).'
    return
  }
  outOfFieldWarning.value = ''
  conePos.value = { lat, lng }

  // Place apex marker
  if (coneMarker) coneMarker.setLatLng([lat, lng])
  else coneMarker = L.marker([lat, lng], { icon: apexIcon, zIndexOffset: 100 }).addTo(map)

  emit('cone-selected', { type: 'Point', coordinates: [lng, lat] })
  checkProximity(lat, lng)
  drawLane()
}

// ── Draw shooting lane ────────────────────────────────────────────────────
function drawLane() {
  if (!conePos.value || !map) return
  const { lat, lng } = conePos.value
  const { baseMid, baseLeft, baseRight } = computeLaneTriangle(lat, lng, bearing.value, laneDistanceM.value)

  // Check if the auto-placed target (baseMid) is inside the field
  if (hasZone.value && fieldPolygonRings.length > 0 && !pointInPolygon(baseMid.lat, baseMid.lng, fieldPolygonRings)) {
    outOfFieldWarning.value = '🚫 El blanco queda fuera del campo con este ángulo. Rota el cono con los botones ↺ ↻ hasta que quede dentro.'
    // Remove lane and target if they were previously placed in wrong direction
    if (lanePoly)     { map.removeLayer(lanePoly);     lanePoly     = null }
    if (targetMarker) { map.removeLayer(targetMarker); targetMarker = null }
    if (distanceLine) { map.removeLayer(distanceLine); distanceLine = null }
    targetPos.value = null
    distance.value = null
    emit('target-selected', null)
    emit('distance-changed', null)
    return
  }
  outOfFieldWarning.value = ''

  // Draw or update triangle polygon
  const latlngs = [
    [lat, lng],
    [baseLeft.lat, baseLeft.lng],
    [baseRight.lat, baseRight.lng],
  ]
  if (lanePoly) { lanePoly.setLatLngs(latlngs) }
  else {
    lanePoly = L.polygon(latlngs, {
      color: '#00e676', weight: 1.5, fillColor: '#00e676', fillOpacity: 0.12,
      dashArray: '6 4',
    }).addTo(map)
  }

  // Auto-place target at base midpoint
  targetPos.value = baseMid
  placeTargetMarker(baseMid.lat, baseMid.lng)
  emit('target-selected', { type: 'Point', coordinates: [baseMid.lng, baseMid.lat] })

  updateDistanceLine()
  recalcDistance()
}

function placeTargetMarker(lat, lng) {
  if (targetMarker) targetMarker.setLatLng([lat, lng])
  else targetMarker = L.marker([lat, lng], { icon: tgtIcon, zIndexOffset: 90 }).addTo(map)
}

function updateDistanceLine() {
  if (!conePos.value || !targetPos.value || !map) return
  const latlngs = [
    [conePos.value.lat, conePos.value.lng],
    [targetPos.value.lat, targetPos.value.lng],
  ]
  if (distanceLine) { distanceLine.setLatLngs(latlngs) }
  else {
    distanceLine = L.polyline(latlngs, {
      color: '#c89b3c', weight: 1.5, dashArray: '4 4', opacity: 0.7
    }).addTo(map)
  }
}

// ── Rotation ──────────────────────────────────────────────────────────────
function rotate(deltaDeg) {
  bearing.value = ((bearing.value + deltaDeg) % 360 + 360) % 360
  if (conePos.value) drawLane()
}

// ── Distance ──────────────────────────────────────────────────────────────
function recalcDistance() {
  if (conePos.value && targetPos.value) {
    distance.value = haversine(
      conePos.value.lat, conePos.value.lng,
      targetPos.value.lat, targetPos.value.lng
    )
    emit('distance-changed', distance.value)
  } else {
    distance.value = null
    emit('distance-changed', null)
  }
}

// ── Proximity check ───────────────────────────────────────────────────────
function checkProximity(lat, lng) {
  proximityWarning.value = ''
  for (const other of props.otherCones) {
    const d = haversine(lat, lng, other.lat, other.lng)
    if (d < 5) {
      proximityWarning.value = `⚠️ Cono a solo ${d.toFixed(1)} m del arquero "${other.archerName || 'otro'}". Mínimo 5 m (ST_Distance).`
      return
    }
  }
}

// ── Zone loading ──────────────────────────────────────────────────────────
async function loadZone(tournamentId) {
  clearZoneLayer()
  try {
    const res = await api.get(`/competition-zones/tournament/${tournamentId}`)
    const geo = res.data
    if (!geo.features || geo.features.length === 0) { hasZone.value = false; return }

    // Store rings for point-in-polygon
    fieldPolygonRings = []
    geo.features.forEach(f => {
      if (f.geometry?.type === 'Polygon')      fieldPolygonRings.push(...f.geometry.coordinates)
      if (f.geometry?.type === 'MultiPolygon') f.geometry.coordinates.forEach(p => fieldPolygonRings.push(...p))
    })

    zoneLayer = L.geoJSON(geo, {
      style: { color: '#c89b3c', weight: 2.5, fillColor: '#c89b3c', fillOpacity: 0.15 },
      onEachFeature: (f, l) => l.bindTooltip(f.properties?.name || 'Campo', { sticky: true })
    }).addTo(map)
    hasZone.value = true
    map.invalidateSize(false)
    const bounds = zoneLayer.getBounds()
    map.setView(bounds.getCenter(), Math.min(18, map.getBoundsZoom(bounds, false)), { animate: false })
  } catch { hasZone.value = false }
}

function clearZoneLayer() {
  if (zoneLayer && map) { map.removeLayer(zoneLayer); zoneLayer = null }
  fieldPolygonRings = []
  hasZone.value = false
}

// ── Other cones ───────────────────────────────────────────────────────────
function renderOtherCones(cones) {
  if (!otherConeGroup) return
  otherConeGroup.clearLayers()
  for (const c of (cones || [])) {
    L.marker([c.lat, c.lng], { icon: otherIcon })
      .bindTooltip(`🔵 ${c.archerName || 'Otro arquero'}`)
      .addTo(otherConeGroup)
  }
}

// ── Clear ─────────────────────────────────────────────────────────────────
function clearAll() {
  if (map) {
    if (coneMarker)   { map.removeLayer(coneMarker);   coneMarker   = null }
    if (targetMarker) { map.removeLayer(targetMarker); targetMarker = null }
    if (lanePoly)     { map.removeLayer(lanePoly);     lanePoly     = null }
    if (distanceLine) { map.removeLayer(distanceLine); distanceLine = null }
  }
  conePos.value = null; targetPos.value = null; distance.value = null
  bearing.value = 0; adjustingTarget.value = false
  outOfFieldWarning.value = ''; proximityWarning.value = ''
  emit('cone-selected', null)
  emit('target-selected', null)
  emit('distance-changed', null)
}

defineExpose({
  clearAll,
  getBearing: () => bearing.value,
})
</script>

<style scoped>
.scoring-map-wrap { margin: 0.8rem 0; }

.map-toolbar {
  display: flex; align-items: center; gap: 0.5rem;
  margin-bottom: 0.5rem; flex-wrap: wrap;
}

.lock-badge {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.28rem 0.7rem; border-radius: 4px; font-size: 0.72rem;
  background: rgba(200,155,60,0.12); border: 1px solid rgba(200,155,60,0.4);
  color: var(--lol-gold); font-family: 'Cinzel', serif; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.07em;
}

.category-badge {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.28rem 0.7rem; border-radius: 4px; font-size: 0.75rem;
  background: rgba(200,155,60,0.12); border: 1px solid rgba(200,155,60,0.35);
  color: var(--lol-gold-light); font-family: 'Cinzel', serif;
}
.category-badge strong { color: var(--lol-gold-bright); }

.map-hint { font-size: 0.73rem; color: var(--text-muted); font-style: italic; }

.rotation-controls {
  display: inline-flex; align-items: center; gap: 0.25rem;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 4px; padding: 0.1rem 0.3rem;
}
.rot-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: 3px; border: none;
  background: transparent; color: var(--text-muted); cursor: pointer;
  transition: all 0.15s;
}
.rot-btn:hover { background: rgba(200,155,60,0.15); color: var(--lol-gold); }
.bearing-display {
  font-family: 'Cinzel', serif; font-size: 0.7rem; font-weight: 700;
  color: var(--lol-gold-light); min-width: 36px; text-align: center;
}

.map-mode-btn {
  display: inline-flex; align-items: center; gap: 0.3rem;
  padding: 0.32rem 0.75rem; font-family: 'Cinzel', serif; font-size: 0.68rem;
  font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em;
  background: transparent; border: 1px solid rgba(200,155,60,0.2);
  color: var(--text-muted); cursor: pointer; border-radius: 4px; transition: all 0.15s;
}
.map-mode-btn:hover  { border-color: var(--lol-gold-dark); color: var(--lol-gold); }
.map-mode-btn.active { background: rgba(200,155,60,0.12); border-color: var(--lol-gold); color: var(--lol-gold-bright); }
.btn-clear { border-color: rgba(232,64,87,0.25); color: #e84057; }
.btn-clear:hover { border-color: #e84057; background: rgba(232,64,87,0.08); }

.scoring-map {
  height: 440px; width: 100%; border-radius: 8px;
  border: 1px solid rgba(200,155,60,0.25);
}
.scoring-map.map-locked {
  cursor: not-allowed;
  border-color: rgba(200,155,60,0.45);
  box-shadow: inset 0 0 0 2px rgba(200,155,60,0.15);
}

.map-status-bar { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 0.5rem; }
.status-item {
  display: inline-flex; align-items: center; gap: 0.3rem;
  font-size: 0.73rem; padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid;
}
.status-empty { color: var(--text-muted); border-color: rgba(255,255,255,0.07); background: rgba(255,255,255,0.02); }
.status-ok    { color: #69f0ae; border-color: rgba(105,240,174,0.3); background: rgba(105,240,174,0.06); }
.status-dist  { color: var(--lol-gold-light); border-color: rgba(200,155,60,0.3); background: rgba(200,155,60,0.06); font-weight: 700; }

.map-alert {
  display: flex; align-items: flex-start; gap: 0.4rem;
  margin-top: 0.45rem; padding: 0.45rem 0.75rem; border-radius: 6px;
  font-size: 0.79rem; line-height: 1.4;
}
.alert-danger { background: rgba(232,64,87,0.1);  border: 1px solid rgba(232,64,87,0.4);  color: #e84057; }
.alert-warn   { background: rgba(255,152,0,0.1);  border: 1px solid rgba(255,152,0,0.4);  color: #ff9800; }
.alert-info   { background: rgba(100,150,255,0.08); border: 1px solid rgba(100,150,255,0.25); color: #82b1ff; }
</style>
