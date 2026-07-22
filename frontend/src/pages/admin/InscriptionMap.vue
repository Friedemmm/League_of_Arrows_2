<template>
  <div class="map-container">
    <div id="inscription-map" style="height: 500px; width: 100%; border-radius: 8px;"></div>

    <div class="zone-status" v-if="loadingZone">Cargando zona de competencia...</div>
    <div class="zone-status zone-status-error" v-else-if="zoneError">{{ zoneError }}</div>
    <div class="zone-status" v-else-if="!hasZone">
      Este torneo todavía no tiene una zona de competencia definida — puedes hacer click en cualquier
      punto, pero el backend puede rechazarlo.
    </div>

    <div class="coordinates-display" v-if="selectedPosition">
      <strong>Posición seleccionada:</strong>
      Lat: {{ selectedPosition.lat.toFixed(5) }}, Lng: {{ selectedPosition.lng.toFixed(5) }}
    </div>
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

L.Icon.Default.mergeOptions({
  iconRetinaUrl,
  iconUrl,
  shadowUrl,
});

const props = defineProps({
  tournamentId: { type: [Number, String], default: null },
});

// Referencias reactivas
const map = ref(null);
const marker = ref(null);
const zoneLayer = ref(null);
const selectedPosition = ref(null);
const loadingZone = ref(false);
const zoneError = ref('');
const hasZone = ref(false);

// Emitir las coordenadas al componente padre (el formulario de inscripción)
const emit = defineEmits(['position-selected']);

onMounted(() => {
  initMap();
  if (props.tournamentId) loadCompetitionZone(props.tournamentId);
});

onBeforeUnmount(() => {
  if (map.value) {
    map.value.remove();
  }
});

watch(() => props.tournamentId, (newId) => {
  clearSelection();
  if (newId) loadCompetitionZone(newId);
  else clearZone();
});

const initMap = () => {
  map.value = L.map('inscription-map', { zoomAnimation: false }).setView([-33.456, -70.666], 15);

  // Cargar el mapa base desde OpenStreetMap
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '© OpenStreetMap contributors'
  }).addTo(map.value);

  map.value.on('click', (e) => {
    const { lat, lng } = e.latlng;

    selectedPosition.value = { lat, lng };

    if (marker.value) {
      marker.value.setLatLng([lat, lng]);
    } else {
      marker.value = L.marker([lat, lng]).addTo(map.value);
    }

    emit('position-selected', {
      type: 'Point',
      coordinates: [lng, lat] // Nota: GeoJSON usa el orden [Longitud, Latitud]
    });
  });
};

function clearSelection() {
  selectedPosition.value = null;
  if (marker.value) {
    map.value.removeLayer(marker.value);
    marker.value = null;
  }
}

function clearZone() {
  if (zoneLayer.value) {
    map.value.removeLayer(zoneLayer.value);
    zoneLayer.value = null;
  }
  hasZone.value = false;
}

// Dibuja la zona de competencia real del torneo (el "cono" donde es válido
// pararse) en vez de dejar que el usuario adivine dónde puede hacer click.
async function loadCompetitionZone(tournamentId) {
  loadingZone.value = true;
  zoneError.value = '';
  clearZone();
  try {
    const res = await api.get(`/competition-zones/tournament/${tournamentId}`);
    const geojson = res.data;
    if (!geojson.features || geojson.features.length === 0) {
      hasZone.value = false;
      return;
    }
    zoneLayer.value = L.geoJSON(geojson, {
      style: {
        color: '#c89b3c',
        weight: 2,
        fillColor: '#c89b3c',
        fillOpacity: 0.15,
      },
    }).addTo(map.value);
    hasZone.value = true;
    // El contenedor puede haber cambiado de tamaño justo antes de esto (ej. al
    // aparecer tras un v-if); sin invalidateSize Leaflet calcula el zoom
    // objetivo con el tamaño cacheado viejo.
    map.value.invalidateSize(false);
    const bounds = zoneLayer.value.getBounds();
    const targetZoom = Math.min(18, map.value.getBoundsZoom(bounds, false));
    map.value.setView(bounds.getCenter(), targetZoom, { animate: false });
  } catch (e) {
    zoneError.value = 'No se pudo cargar la zona de competencia de este torneo.';
    hasZone.value = false;
  } finally {
    loadingZone.value = false;
  }
}
</script>

<style scoped>
.map-container {
  margin: 20px 0;
  border: 1px solid #ccc;
  padding: 10px;
  background-color: #f9f9f9;
}
.coordinates-display {
  margin-top: 10px;
  font-family: monospace;
  color: #333;
}
.zone-status {
  margin-top: 8px;
  font-size: 0.8rem;
  color: #555;
}
.zone-status-error {
  color: #b23b3b;
}
</style>
