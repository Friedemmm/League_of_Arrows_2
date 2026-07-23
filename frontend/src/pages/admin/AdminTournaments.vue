<template>
  <div class="page-wrapper admin-page">
    <div class="container">

      <!-- Page header -->
      <div class="admin-page-header">
        <button class="btn-back" id="btn-back-tournaments" @click="$router.push('/dashboard')">
          <span class="material-icons">arrow_back</span> Back
        </button>
        <div class="header-row">
          <div>
            <h1 class="page-title">
              <span class="material-icons page-title-icon">emoji_events</span>
              Gestionar Eventos
            </h1>
            <p class="page-subtitle">Crea y administra torneos: pasados, presentes y futuros.</p>
          </div>
          <button class="btn btn-gold" id="btn-add-tournament" @click="openCreate">
            <span class="material-icons btn-icon">add_circle</span> Nuevo Torneo
          </button>
        </div>
        <hr class="page-rule" />
      </div>

      <div v-if="loading" class="loading-center"><div class="spinner"></div></div>

      <div v-else class="lol-table-wrapper">
        <table class="lol-table" id="tournaments-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Categoría</th>
              <th>Inicio</th>
              <th>Fin</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in tournaments" :key="t.tournamentId" :id="`tournament-row-${t.tournamentId}`">
              <td class="text-muted">#{{ t.tournamentId }}</td>
              <td class="fw-bold">{{ t.name }}</td>
              <td><span class="badge badge-blue">{{ t.categoryName || '—' }}</span></td>
              <td>{{ formatDate(t.startDate) }}</td>
              <td>{{ formatDate(t.endDate) }}</td>
              <td>
                <span class="badge" :class="statusClass(t)">{{ statusLabel(t) }}</span>
              </td>
              <td>
                <div class="flex gap-1">
                  <!-- Ver inscritos -->
                  <button class="btn btn-ghost btn-sm icon-btn"
                    :id="`btn-view-inscribed-${t.tournamentId}`"
                    @click="openViewInscribed(t)"
                    title="Ver inscritos">
                    <span class="material-icons">group</span>
                  </button>
                  <!-- Inscribir arquero -->
                  <button class="btn btn-gold btn-sm icon-btn"
                    :id="`btn-inscribe-tournament-${t.tournamentId}`"
                    @click="openInscribe(t)"
                    title="Inscribir arquero">
                    <span class="material-icons">person_add</span>
                  </button>
                  <!-- Desinscribir arquero -->
                  <button class="btn btn-danger btn-sm icon-btn"
                    :id="`btn-unregister-tournament-${t.tournamentId}`"
                    @click="openUnregister(t)"
                    title="Desinscribir arquero">
                    <span class="material-icons">person_remove</span>
                  </button>
                  <!-- Zona de competencia -->
                  <button class="btn btn-ghost btn-sm icon-btn"
                    :id="`btn-zone-tournament-${t.tournamentId}`"
                    @click="openZoneModal(t)"
                    title="Zona de competencia">
                    <span class="material-icons">crop_free</span>
                  </button>
                  <button class="btn btn-ghost btn-sm icon-btn"
                    :id="`btn-edit-tournament-${t.tournamentId}`" @click="openEdit(t)">
                    <span class="material-icons">edit</span>
                  </button>
                  <button class="btn btn-danger btn-sm icon-btn"
                    :id="`btn-delete-tournament-${t.tournamentId}`" @click="confirmDelete(t)">
                    <span class="material-icons">delete</span>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && tournaments.length === 0">
              <td colspan="7" class="text-center text-muted" style="padding:2rem;">No se encontraron torneos.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── View Inscribed Modal ── -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showViewModal" @click.self="showViewModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>
                <span class="material-icons" style="vertical-align:middle;margin-right:0.4rem;color:var(--lol-gold);">group</span>
                Inscritos en {{ viewingT?.name }}
              </h3>
              <button class="modal-close" @click="showViewModal = false">
                <span class="material-icons">close</span>
              </button>
            </div>
            <div class="modal-body">
              <div v-if="viewInscribed.length === 0" class="text-muted" style="text-align:center;padding:1.5rem 0;font-size:0.85rem;">
                No hay arqueros inscritos en este torneo.
              </div>
              <ul v-else class="inscribed-list">
                <li v-for="entry in viewInscribed" :key="entry.inscriptionId" class="inscribed-item">
                  <span class="material-icons" style="font-size:1rem;color:var(--lol-gold);flex-shrink:0;">person</span>
                  <span class="inscribed-name">{{ entry.archerName }}</span>
                </li>
              </ul>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showViewModal = false">Cerrar</button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- ── Inscribe Archer Modal ── -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showInscribeModal" @click.self="showInscribeModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>
                <span class="material-icons" style="vertical-align:middle;margin-right:0.4rem;color:var(--lol-gold);">person_add</span>
                Inscribir Arquero
              </h3>
              <button class="modal-close" @click="showInscribeModal = false">
                <span class="material-icons">close</span>
              </button>
            </div>
            <div class="modal-body">
              <Transition name="slide-up">
                <div class="alert alert-error" v-if="inscribeError" role="alert">
                  <span class="material-icons alert-icon">error</span>
                  <div class="alert-body">
                    <span class="alert-title">Error al inscribir</span>
                    <span class="alert-msg">{{ inscribeError }}</span>
                  </div>
                </div>
              </Transition>
              <Transition name="slide-up">
                <div class="alert alert-success" v-if="inscribeSuccess" role="status">
                  <span class="material-icons alert-icon">check_circle</span>
                  <div class="alert-body">
                    <span class="alert-title">Inscripción exitosa</span>
                    <span class="alert-msg">{{ inscribeSuccess }}</span>
                  </div>
                </div>
              </Transition>

              <p class="text-secondary" style="margin-bottom:1rem;">
                Torneo: <strong class="text-gold">{{ inscribingT?.name }}</strong>
                <span v-if="inscribingT?.categoryName" class="badge badge-blue" style="margin-left:0.5rem;">{{ inscribingT.categoryName }}</span>
              </p>

              <div class="form-group">
                <label class="form-label" for="inscribe-archer-select">Seleccionar Arquero</label>
                <select id="inscribe-archer-select" class="form-input" v-model.number="inscribeArcherId">
                  <option :value="null" disabled>— Selecciona un arquero —</option>
                  <option
                    v-for="a in archers"
                    :key="a.archerId"
                    :value="a.archerId"
                    :class="{ 'option-incompatible': inscribingT?.categoryId && a.categoryId !== inscribingT.categoryId }"
                  >
                    {{ a.name }}
                    <template v-if="a.categoryName"> ({{ a.categoryName }})</template>
                    <template v-if="inscribingT?.categoryId && a.categoryId !== inscribingT.categoryId"> ⚠️ Categoría distinta</template>
                  </option>
                </select>
              </div>

              <!-- Alerta de incompatibilidad de categoría -->
              <Transition name="slide-up">
                <div class="alert alert-warning" v-if="categoryMismatch" role="alert">
                  <span class="material-icons alert-icon">category</span>
                  <div class="alert-body">
                    <span class="alert-title">Categoría incompatible</span>
                    <span class="alert-msg">
                      <strong>{{ categoryMismatchArcherName }}</strong> pertenece a la categoría
                      <strong>"{{ categoryMismatchArcherCat }}"</strong>, pero este torneo es de categoría
                      <strong>"{{ inscribingT?.categoryName }}"</strong>.
                      No se puede inscribir.
                    </span>
                  </div>
                </div>
              </Transition>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showInscribeModal = false">Cancelar</button>
              <button class="btn btn-gold" id="btn-confirm-inscribe" @click="doInscribe"
                :disabled="saving || !inscribeArcherId || categoryMismatch">
                <span class="material-icons btn-icon">how_to_reg</span>
                {{ saving ? 'Inscribiendo...' : 'Inscribir' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- ── Unregister Archer Modal ── -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showUnregisterModal" @click.self="showUnregisterModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>
                <span class="material-icons" style="vertical-align:middle;margin-right:0.4rem;color:#e84057;">person_remove</span>
                Desinscribir Arquero
              </h3>
              <button class="modal-close" @click="showUnregisterModal = false">
                <span class="material-icons">close</span>
              </button>
            </div>
            <div class="modal-body">
              <Transition name="slide-up">
                <div class="alert alert-error" v-if="unregisterError" role="alert">
                  <span class="material-icons alert-icon">error</span>
                  <div class="alert-body">
                    <span class="alert-title">Error al desinscribir</span>
                    <span class="alert-msg">{{ unregisterError }}</span>
                  </div>
                </div>
              </Transition>
              <Transition name="slide-up">
                <div class="alert alert-success" v-if="unregisterSuccess" role="status">
                  <span class="material-icons alert-icon">check_circle</span>
                  <div class="alert-body">
                    <span class="alert-title">Desinscripción exitosa</span>
                    <span class="alert-msg">{{ unregisterSuccess }}</span>
                  </div>
                </div>
              </Transition>

              <p class="text-secondary" style="margin-bottom:1rem;">
                Torneo: <strong class="text-gold">{{ unregisteringT?.name }}</strong>
              </p>

              <div class="form-group">
                <label class="form-label" for="unregister-archer-select">Seleccionar Arquero Inscrito</label>
                <div v-if="loadingInscriptions" class="loading-center" style="padding:1rem 0;"><div class="spinner"></div></div>
                <select v-else id="unregister-archer-select" class="form-input" v-model.number="unregisterInscriptionId">
                  <option :value="null" disabled>— Selecciona un arquero —</option>
                  <option v-for="i in tournamentInscriptions" :key="i.inscriptionId" :value="i.inscriptionId">
                    {{ i.archerName }}
                  </option>
                </select>
                <p v-if="!loadingInscriptions && tournamentInscriptions.length === 0" class="text-muted" style="margin-top:0.5rem;font-size:0.8rem;">
                  No hay arqueros inscritos en este torneo.
                </p>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showUnregisterModal = false">Cancelar</button>
              <button class="btn btn-danger" id="btn-confirm-unregister" @click="doUnregister"
                :disabled="saving || !unregisterInscriptionId || tournamentInscriptions.length === 0">
                <span class="material-icons btn-icon">person_remove</span>
                {{ saving ? 'Desinscribiendo...' : 'Desinscribir' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- ── Competition Zone Modal ── -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showZoneModal" @click.self="showZoneModal = false">
          <div class="modal-box" style="max-width:640px;">
            <div class="modal-header">
              <h3>
                <span class="material-icons" style="vertical-align:middle;margin-right:0.4rem;color:var(--lol-gold);">crop_free</span>
                Zona de competencia — {{ zoningT?.name }}
              </h3>
              <button class="modal-close" @click="showZoneModal = false">
                <span class="material-icons">close</span>
              </button>
            </div>
            <div class="modal-body">
              <!-- ── Campo de competencia ── -->
              <p class="text-muted" style="font-size:0.78rem;margin-bottom:0.8rem;">
                Define el polígono del campo de competencia. Haz click en el mapa para agregar vértices.
                El backend valida que los arqueros estén dentro (ST_Contains) al registrar puntajes.
              </p>
              <Transition name="slide-up">
                <div class="alert alert-error" v-if="zoneError" role="alert">
                  <span class="material-icons alert-icon">error</span>
                  <div class="alert-body">
                    <span class="alert-title">Error</span>
                    <span class="alert-msg">{{ zoneError }}</span>
                  </div>
                </div>
              </Transition>
              <Transition name="slide-up">
                <div class="alert alert-success" v-if="zoneSuccess" role="status">
                  <span class="material-icons alert-icon">check_circle</span>
                  <div class="alert-body">
                    <span class="alert-title">Listo</span>
                    <span class="alert-msg">{{ zoneSuccess }}</span>
                  </div>
                </div>
              </Transition>

              <!-- Zonas existentes -->
              <div v-if="existingZones.length > 0" style="margin-bottom:1rem;">
                <label class="form-label">Campo ya definido</label>
                <ul class="inscribed-list">
                  <li v-for="z in existingZones" :key="z.properties.id_zone" class="inscribed-item">
                    <span class="material-icons" style="font-size:1rem;color:var(--lol-gold);flex-shrink:0;">pentagon</span>
                    <span class="inscribed-name">{{ z.properties.name }}</span>
                    <button class="btn btn-danger btn-sm icon-btn" style="margin-left:auto;"
                      :id="`btn-delete-zone-${z.properties.id_zone}`"
                      @click="deleteZone(z.properties.id_zone)" title="Eliminar campo">
                      <span class="material-icons" style="font-size:0.9rem;">delete</span>
                    </button>
                  </li>
                </ul>
              </div>

              <div class="form-group">
                <label class="form-label" for="zone-name">Nombre del campo</label>
                <input id="zone-name" class="form-input" v-model="zoneName" placeholder="Ej. Campo de tiro — Sede Norte" />
              </div>

              <CompetitionZoneMap
                :tournament-id="zoningT?.tournamentId"
                map-id="zone-draw-map"
                load-endpoint="/competition-zones"
                draw-color="#c89b3c"
                @polygon-changed="onPolygonChanged" />
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showZoneModal = false">Cerrar</button>
              <button class="btn btn-gold" id="btn-save-zone" @click="saveZone"
                :disabled="savingZone || !zoneName.trim() || !zoneGeometry">
                <span class="material-icons btn-icon">save</span>
                {{ savingZone ? 'Guardando...' : 'Guardar zona' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Create/Edit Modal -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showModal" @click.self="showModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>{{ editingT ? 'Editar Torneo' : 'Nuevo Torneo' }}</h3>
              <button class="modal-close" @click="showModal = false">
                <span class="material-icons">close</span>
              </button>
            </div>
            <div class="modal-body">
              <Transition name="slide-up">
                <div class="alert alert-error" v-if="modalError">
                  <span class="material-icons">warning</span> {{ modalError }}
                </div>
              </Transition>
              <div class="form-group">
                <label class="form-label" for="t-name">Nombre del Torneo</label>
                <input id="t-name" class="form-input" v-model="form.name" placeholder="Nombre del torneo" />
              </div>
              <div class="form-group">
                <label class="form-label" for="t-category">Categoría</label>
                <select id="t-category" class="form-input" v-model.number="form.categoryId">
                  <option :value="null" disabled>Selecciona una categoría</option>
                  <option v-for="c in categories" :key="c.id_category ?? c.idCategory" :value="c.id_category ?? c.idCategory">{{ c.name }}</option>
                </select>
              </div>
              <div class="grid-2">
                <div class="form-group">
                  <label class="form-label" for="t-start">Fecha de Inicio</label>
                  <input id="t-start" class="form-input" type="date" v-model="form.startDate" />
                </div>
                <div class="form-group">
                  <label class="form-label" for="t-end">Fecha de Fin</label>
                  <input id="t-end" class="form-input" type="date" v-model="form.endDate" />
                </div>
              </div>
              <div class="form-group">
                <label class="form-label" style="display:flex;align-items:center;gap:0.5rem;cursor:pointer;">
                  <input type="checkbox" v-model="form.active" id="t-active" />
                  <span>Activo</span>
                </label>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showModal = false">Cancelar</button>
              <button class="btn btn-gold" id="btn-save-tournament" @click="saveTournament" :disabled="saving">
                {{ saving ? 'Guardando...' : 'Guardar' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Delete Confirm -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showDeleteModal" @click.self="showDeleteModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>Eliminar Torneo</h3>
              <button class="modal-close" @click="showDeleteModal = false">
                <span class="material-icons">close</span>
              </button>
            </div>
            <div class="modal-body">
              <p class="text-secondary">¿Eliminar <strong class="text-gold">{{ deletingT?.name }}</strong>?
                Todos los datos asociados serán eliminados.</p>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showDeleteModal = false">Cancelar</button>
              <button class="btn btn-danger" id="btn-confirm-delete-tournament" @click="doDelete" :disabled="saving">
                {{ saving ? 'Eliminando...' : 'Eliminar' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, reactive } from 'vue'
import { getTournaments, createTournament, updateTournament, deleteTournament } from '@/api/tournaments'
import { getArchers } from '@/api/archers'
import { deleteInscription } from '@/api/inscriptions'
import api from '@/api/axios'
import CompetitionZoneMap from './CompetitionZoneMap.vue'

const tournaments     = ref([])
const categories      = ref([])
const archers         = ref([])
const loading         = ref(true)
const showModal           = ref(false)
const showDeleteModal     = ref(false)
const showInscribeModal   = ref(false)
const showUnregisterModal = ref(false)
const showViewModal       = ref(false)
const showZoneModal       = ref(false)
const editingT            = ref(null)
const deletingT           = ref(null)
const inscribingT         = ref(null)
const unregisteringT      = ref(null)
const viewingT            = ref(null)
const zoningT              = ref(null)
const zoneName             = ref('')
const zoneGeometry         = ref(null)
const existingZones        = ref([])
const zoneError            = ref('')
const zoneSuccess          = ref('')
const savingZone           = ref(false)

const inscribeArcherId        = ref(null)
const unregisterInscriptionId = ref(null)
const allInscriptions         = ref([])   // todas las inscripciones cargadas al montar
const tournamentInscriptions  = ref([])   // inscritos del torneo seleccionado
const viewInscribed           = ref([])   // lista para el modal "Ver inscritos"
const loadingInscriptions     = ref(false)
const inscribeError       = ref('')
const inscribeSuccess     = ref('')
const unregisterError     = ref('')
const unregisterSuccess   = ref('')
const saving              = ref(false)
const modalError          = ref('')

const form = reactive({ name: '', categoryId: null, startDate: '', endDate: '', active: true })

const today = new Date().toISOString().split('T')[0]

function statusLabel(t) {
  if (t.active) return 'Activo'
  if (t.endDate && t.endDate < today) return 'Finalizado'
  return 'Próximo'
}
function statusClass(t) {
  if (t.active) return 'badge-success'
  if (t.endDate && t.endDate < today) return 'badge-muted'
  return 'badge-blue'
}

function formatDate(d) {
  if (!d) return '—'
  return new Date(d + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

async function load() {
  loading.value = true
  try {
    const [tourRes, archerRes, catRes, insRes] = await Promise.all([
      getTournaments(),
      getArchers(),
      api.get('/categories'),
      api.get('/inscriptions'),
    ])
    tournaments.value    = tourRes.data
    archers.value        = archerRes.data
    categories.value     = catRes.data
    allInscriptions.value = Array.isArray(insRes.data) ? insRes.data : []
  } catch (e) {
    console.error('[AdminTournaments] load error:', e.message)
  }
  loading.value = false
}

function openCreate() {
  editingT.value = null
  Object.assign(form, { name: '', categoryId: null, startDate: '', endDate: '', active: true })
  modalError.value = ''
  showModal.value = true
}

function openEdit(t) {
  editingT.value = t
  Object.assign(form, {
    name:       t.name,
    categoryId: t.categoryId,
    startDate:  t.startDate,
    endDate:    t.endDate,
    active:     t.active,
  })
  modalError.value = ''
  showModal.value = true
}

function openInscribe(t) {
  inscribingT.value       = t
  inscribeArcherId.value  = null
  inscribeError.value     = ''
  inscribeSuccess.value   = ''
  categoryMismatch.value  = false
  showInscribeModal.value = true
}

function confirmDelete(t) { deletingT.value = t; showDeleteModal.value = true }

// Construye la lista de inscritos de un torneo cruzando con la lista de arqueros
function getInscribedForTournament(tournamentId) {
  return allInscriptions.value
    .filter(i => i.tournamentId === tournamentId)
    .map(i => ({
      inscriptionId: i.inscriptionId,
      archerId:      i.archerId,
      score:         i.score ?? 0,
      archerName:    archers.value.find(a => a.archerId === i.archerId)?.name ?? `Arquero #${i.archerId}`,
    }))
}

function openViewInscribed(t) {
  viewingT.value      = t
  viewInscribed.value = getInscribedForTournament(t.tournamentId)
  showViewModal.value = true
}

function openUnregister(t) {
  unregisteringT.value          = t
  unregisterInscriptionId.value = null
  unregisterError.value         = ''
  unregisterSuccess.value       = ''
  tournamentInscriptions.value  = getInscribedForTournament(t.tournamentId)
  showUnregisterModal.value     = true
}

async function openZoneModal(t) {
  zoningT.value      = t
  zoneName.value      = ''
  zoneGeometry.value  = null
  zoneError.value     = ''
  zoneSuccess.value   = ''
  existingZones.value = []

  showZoneModal.value = true
  try {
    const res = await api.get(`/competition-zones/tournament/${t.tournamentId}`)
    existingZones.value = res.data.features || []
  } catch (e) {
    console.error('[AdminTournaments] error cargando zonas:', e.message)
  }

}

function onPolygonChanged(geometry) {
  zoneGeometry.value = geometry
}



async function saveZone() {
  zoneError.value = zoneSuccess.value = ''
  savingZone.value = true
  try {
    await api.post('/competition-zones', {
      idTournament: zoningT.value.tournamentId,
      name: zoneName.value.trim(),
      geometry: zoneGeometry.value,
    })
    zoneSuccess.value = 'Zona guardada correctamente.'
    zoneName.value = ''
    zoneGeometry.value = null
    const res = await api.get(`/competition-zones/tournament/${zoningT.value.tournamentId}`)
    existingZones.value = res.data.features || []
  } catch (e) {
    zoneError.value = e.response?.data?.error || 'Error al guardar la zona.'
  } finally { savingZone.value = false }
}

async function deleteZone(idZone) {
  zoneError.value = zoneSuccess.value = ''
  try {
    await api.delete(`/competition-zones/${idZone}`)
    existingZones.value = existingZones.value.filter(z => z.properties.id_zone !== idZone)
    zoneSuccess.value = 'Zona eliminada.'
  } catch (e) {
    zoneError.value = e.response?.data?.error || 'Error al eliminar la zona.'
  }
}

async function saveTournament() {
  modalError.value = ''
  saving.value = true
  try {
    if (editingT.value) await updateTournament(editingT.value.tournamentId, form)
    else await createTournament(form)
    showModal.value = false
    await load()
  } catch (e) {
    modalError.value = e.response?.data?.error || 'Failed to save tournament.'
  } finally { saving.value = false }
}

async function doDelete() {
  saving.value = true
  try {
    await deleteTournament(deletingT.value.tournamentId)
    showDeleteModal.value = false
    await load()
  } catch { /* ignore */ } finally { saving.value = false }
}

// Estado de incompatibilidad de categoría
const categoryMismatch         = ref(false)
const categoryMismatchArcherName = ref('')
const categoryMismatchArcherCat  = ref('')

// Limpiar error y validar categoría al cambiar arquero seleccionado
watch(inscribeArcherId, (newId) => {
  inscribeError.value = ''
  if (!newId || !inscribingT.value?.categoryId) {
    categoryMismatch.value = false
    return
  }
  const archer = archers.value.find(a => a.archerId === newId)
  if (archer && archer.categoryId !== inscribingT.value.categoryId) {
    categoryMismatch.value          = true
    categoryMismatchArcherName.value = archer.name
    categoryMismatchArcherCat.value  = archer.categoryName || `Cat. #${archer.categoryId}`
  } else {
    categoryMismatch.value = false
  }
})
watch(unregisterInscriptionId, () => { unregisterError.value = '' })

function autoDismiss(refVal, ms = 3500) {
  setTimeout(() => { refVal.value = '' }, ms)
}

function parseInscribeError(e) {
  const status = e.response?.status
  const msg    = e.response?.data?.error ?? e.response?.data?.message ?? ''
  if (status === 409 || msg.toLowerCase().includes('duplicate') || msg.toLowerCase().includes('already'))
    return 'Este arquero ya está inscrito en el torneo.'
  if (status === 404)
    return 'Arquero o torneo no encontrado. Recarga la página.'
  if (status === 403)
    return 'No tienes permiso para realizar esta acción.'
  if (status === 400)
    return `Datos inválidos: ${msg || 'verifica la selección.'}`
  return msg || 'No se pudo inscribir al arquero. Intenta de nuevo.'
}

function parseUnregisterError(e) {
  const status = e.response?.status
  const msg    = e.response?.data?.error ?? e.response?.data?.message ?? ''
  if (status === 404)
    return 'Inscripción no encontrada. Es posible que ya fue eliminada.'
  if (status === 403)
    return 'No tienes permiso para realizar esta acción.'
  if (status === 400)
    return `Solicitud inválida: ${msg || 'verifica la selección.'}`
  return msg || 'No se pudo desinscribir al arquero. Intenta de nuevo.'
}

async function doInscribe() {
  inscribeError.value   = ''
  inscribeSuccess.value = ''
  saving.value = true
  try {
    const res = await api.post('/inscriptions', {
      archerId:     inscribeArcherId.value,
      tournamentId: inscribingT.value.tournamentId,
    })
    if (res.data) allInscriptions.value.push(res.data)
    const archerName = archers.value.find(a => a.archerId === inscribeArcherId.value)?.name || 'Arquero'
    inscribeSuccess.value = `${archerName} fue inscrito correctamente en "${inscribingT.value.name}".`
    inscribeArcherId.value = null
    autoDismiss(inscribeSuccess)
  } catch (e) {
    inscribeError.value = parseInscribeError(e)
  } finally { saving.value = false }
}

async function doUnregister() {
  unregisterError.value   = ''
  unregisterSuccess.value = ''
  saving.value = true
  try {
    await deleteInscription(unregisterInscriptionId.value)
    allInscriptions.value = allInscriptions.value.filter(i => i.inscriptionId !== unregisterInscriptionId.value)
    const removed = tournamentInscriptions.value.find(i => i.inscriptionId === unregisterInscriptionId.value)
    tournamentInscriptions.value = tournamentInscriptions.value.filter(i => i.inscriptionId !== unregisterInscriptionId.value)
    unregisterSuccess.value = `${removed?.archerName ?? 'Arquero'} fue desinscrito correctamente de "${unregisteringT.value.name}".`
    unregisterInscriptionId.value = null
    autoDismiss(unregisterSuccess)
  } catch (e) {
    unregisterError.value = parseUnregisterError(e)
  } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/icon?family=Material+Icons');

.admin-page { padding: calc(var(--header-height, 70px) + 2rem) 0 4rem; min-height: 100vh; }
.admin-page-header { margin-bottom: 1.5rem; }

.btn-back {
  display: inline-flex; align-items: center; gap: 0.3rem;
  background: none; border: none; color: var(--text-muted); cursor: pointer;
  font-family: 'Cinzel', serif; font-size: 0.7rem; text-transform: uppercase;
  letter-spacing: 0.1em; padding: 0; margin-bottom: 1rem; transition: color 0.2s;
}
.btn-back:hover { color: var(--lol-gold); }
.btn-back .material-icons { font-size: 1rem; }

.header-row { display: flex; align-items: flex-end; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }

.page-title {
  font-size: 1.5rem; font-family: 'Cinzel', serif;
  display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.3rem;
}
.page-title-icon { font-size: 1.3rem; color: var(--lol-gold); }
.page-subtitle { font-size: 0.82rem; color: var(--text-muted); margin: 0; }
.page-rule { margin: 1rem 0 1.5rem; }

.icon-btn { display: inline-flex; align-items: center; padding: 0.3rem 0.5rem; }
.icon-btn .material-icons { font-size: 1rem; }
.btn-icon { display: inline-flex; align-items: center; gap: 0.3rem; }
.fw-bold { font-weight: 600; }

.alert-success {
  display: flex; align-items: center; gap: 0.5rem;
  background: rgba(10,200,185,0.12); border: 1px solid rgba(10,200,185,0.3);
  color: #0ac8b9; border-radius: 6px; padding: 0.6rem 0.9rem;
  font-size: 0.82rem; margin-bottom: 1rem;
}

/* ── Lista de inscritos ────────────────────────────────── */
.inscribed-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.inscribed-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.55rem 0.8rem;
  border-radius: 6px;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--lol-border);
  transition: border-color 0.2s;
}
.inscribed-item:hover { border-color: rgba(200,155,60,0.3); }
.inscribed-name {
  flex: 1;
  font-family: 'Cinzel', serif;
  font-size: 0.82rem;
  color: var(--text-primary);
}

/* ── Alert mejorada (con título + mensaje) ────────────────── */
.alert {
  align-items: flex-start;
}
.alert-icon {
  font-size: 1.3rem;
  flex-shrink: 0;
  margin-top: 0.05rem;
}
.alert-body {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1;
}
.alert-title {
  font-family: 'Cinzel', serif;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.alert-msg {
  font-size: 0.82rem;
  line-height: 1.4;
  opacity: 0.9;
}

/* ── Alert warning (categoría incompatible) ──────────────── */
.alert-warning {
  background: rgba(255, 180, 0, 0.08);
  border: 1px solid rgba(255, 180, 0, 0.35);
  color: #ffd54f;
}
</style>
