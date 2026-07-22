<template>
  <div class="zone-map-container">
    <div class="zone-map-toolbar">
      <button type="button" class="btn btn-ghost btn-sm" :disabled="points.length === 0" @click="undoPoint">
        <span class="material-icons" style="font-size:1rem;">undo</span> Deshacer punto
      </button>
      <button type="button" class="btn btn-ghost btn-sm" :disabled="points.length === 0" @click="resetPoints">
        <span class="material-icons" style="font-size:1rem;">restart_alt</span> Reiniciar
      </button>
      <span class="zone-map-hint">
        {{ points.length < 3
          ? `Haz click en el mapa para marcar los vértices del polígono (van ${points.length}, mínimo 3).`
          : `Polígono con ${points.length} vértices listo para guardar.` }}
      </span>
    </div>
    <div :id="mapId" style="height: 420px; width: 100%; border-radius: 8px;"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';
import api from '@/api/axios';

L.Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl });

const props = defineProps({
  tournamentId: { type: [Number, String], default: null },
  mapId: { type: String, default: 'zone-draw-map' },
  // Endpoint que devuelve el GeoJSON existente para /tournament/{id}, ej. '/competition-zones' o '/fields'
  loadEndpoint: { type: String, default: '/competition-zones' },
  drawColor: { type: String, default: '#c89b3c' },
});
const emit = defineEmits(['polygon-changed']);

const map = ref(null);
const existingZonesLayer = ref(null);
const drawLayer = ref(null);
const points = ref([]); // [{lat, lng}, ...]

onMounted(() => {
  initMap();
  if (props.tournamentId) loadExisting(props.tournamentId);
});

onBeforeUnmount(() => {
  if (map.value) map.value.remove();
});

watch(() => props.tournamentId, (newId) => {
  resetPoints();
  if (newId) loadExisting(newId);
});

function initMap() {
  map.value = L.map(props.mapId, { zoomAnimation: false }).setView([-33.456, -70.666], 15);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors',
  }).addTo(map.value);

  map.value.on('click', (e) => {
    points.value.push(e.latlng);
    redrawPolygon();
    emitChange();
  });
}

function redrawPolygon() {
  if (drawLayer.value) {
    map.value.removeLayer(drawLayer.value);
    drawLayer.value = null;
  }
  if (points.value.length === 0) return;
  if (points.value.length < 3) {
    drawLayer.value = L.polyline(points.value, { color: props.drawColor, weight: 2, dashArray: '4 4' }).addTo(map.value);
  } else {
    drawLayer.value = L.polygon(points.value, {
      color: props.drawColor, weight: 2, fillColor: props.drawColor, fillOpacity: 0.2,
    }).addTo(map.value);
  }
}

function undoPoint() {
  points.value.pop();
  redrawPolygon();
  emitChange();
}

function resetPoints() {
  points.value = [];
  redrawPolygon();
  emitChange();
}

function emitChange() {
  if (points.value.length < 3) {
    emit('polygon-changed', null);
    return;
  }
  // GeoJSON requiere el anillo cerrado (primer punto == último)
  const ring = points.value.map(p => [p.lng, p.lat]);
  ring.push(ring[0]);
  emit('polygon-changed', { type: 'Polygon', coordinates: [ring] });
}

// Muestra el/los polígono(s) ya existente(s) del torneo (referencia, no editables acá)
async function loadExisting(tournamentId) {
  if (existingZonesLayer.value) {
    map.value.removeLayer(existingZonesLayer.value);
    existingZonesLayer.value = null;
  }
  try {
    const res = await api.get(`${props.loadEndpoint}/tournament/${tournamentId}`);
    const geojson = res.data;
    if (!geojson.features || geojson.features.length === 0) return;
    existingZonesLayer.value = L.geoJSON(geojson, {
      style: { color: '#5b8dd6', weight: 2, fillColor: '#5b8dd6', fillOpacity: 0.1, dashArray: '3 3' },
    }).addTo(map.value);
    map.value.invalidateSize(false);
    const bounds = existingZonesLayer.value.getBounds();
    const targetZoom = Math.min(18, map.value.getBoundsZoom(bounds, false));
    map.value.setView(bounds.getCenter(), targetZoom, { animate: false });
  } catch (e) {
    console.error('[CompetitionZoneMap] error cargando geometría existente:', e.message);
  }
}
</script>

<style scoped>
.zone-map-container {
  margin: 10px 0;
}
.zone-map-toolbar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.5rem;
  flex-wrap: wrap;
}
.zone-map-hint {
  font-size: 0.78rem;
  color: var(--text-muted);
}
</style>
