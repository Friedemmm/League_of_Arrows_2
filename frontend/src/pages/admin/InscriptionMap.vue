<template>
  <div class="map-container">
    <div id="inscription-map" style="height: 500px; width: 100%; border-radius: 8px;"></div>
    
    <div class="coordinates-display" v-if="selectedPosition">
      <strong>Posición seleccionada:</strong> 
      Lat: {{ selectedPosition.lat.toFixed(5) }}, Lng: {{ selectedPosition.lng.toFixed(5) }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css'; 
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';

L.Icon.Default.mergeOptions({
  iconRetinaUrl,
  iconUrl,
  shadowUrl,
});

// Referencias reactivas
const map = ref(null);
const marker = ref(null);
const selectedPosition = ref(null);

// Emitir las coordenadas al componente padre (el formulario de inscripción)
const emit = defineEmits(['position-selected']);

onMounted(() => {
  initMap();
});

onBeforeUnmount(() => {
  if (map.value) {
    map.value.remove();
  }
});

const initMap = () => {
  map.value = L.map('inscription-map').setView([-33.456, -70.666], 15);

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
</style>