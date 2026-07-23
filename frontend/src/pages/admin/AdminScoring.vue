<template>
  <div class="page-wrapper admin-page">
    <div class="container" style="max-width:860px;">

      <div class="admin-page-header">
        <button class="btn-back" id="btn-back-scoring" @click="$router.push('/dashboard')">
          <span class="material-icons">arrow_back</span> Volver
        </button>
        <h1 class="page-title">
          <span class="material-icons page-title-icon">sports_score</span>
          Registro de Puntajes
        </h1>
        <p class="page-subtitle">Registra o edita los puntajes de rondas para arqueros en torneos.</p>
        <hr class="page-rule" />
      </div>

      <!-- TABS -->
      <div class="mode-tabs">
        <button class="mode-tab" :class="{ active: mode === 'register' }" id="tab-register" @click="mode = 'register'">
          <span class="material-icons">add_circle</span> Registrar Ronda
        </button>
        <button class="mode-tab" :class="{ active: mode === 'edit' }" id="tab-edit" @click="mode = 'edit'">
          <span class="material-icons">edit</span> Editar Puntajes
        </button>
      </div>

      <!-- ══ TAB 1: REGISTER ══ -->
      <div class="lol-card" v-show="mode === 'register'">
        <Transition name="slide-up">
          <div class="alert alert-error" v-if="error"><span class="material-icons">warning</span> {{ error }}</div>
        </Transition>
        <Transition name="slide-up">
          <div class="alert alert-success" v-if="success"><span class="material-icons">check_circle</span> {{ success }}</div>
        </Transition>
        <div v-if="loadingData" class="loading-center" style="padding:2rem 0;"><div class="spinner"></div></div>
        <template v-else>
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label" for="scoring-tournament">Torneo</label>
              <select id="scoring-tournament" class="form-input" v-model.number="form.tournamentId">
                <option :value="null" disabled>Selecciona un torneo</option>
                <option v-for="t in activeTournaments" :key="t.tournamentId" :value="t.tournamentId">{{ t.name }}</option>
              </select>
              <span v-if="activeTournaments.length === 0" class="hint-text">No hay torneos activos.</span>
            </div>
            <div class="form-group">
              <label class="form-label" for="scoring-archer">Arquero</label>
              <select id="scoring-archer" class="form-input" v-model.number="form.archerId"
                :disabled="!form.tournamentId || loadingTab1Archers">
                <option :value="null" disabled>
                  {{ !form.tournamentId ? 'Selecciona primero un torneo' : loadingTab1Archers ? 'Cargando...' : tab1InscribedArchers.length === 0 ? 'Sin arqueros inscritos' : 'Selecciona un arquero' }}
                </option>
                <option v-for="a in tab1InscribedArchers" :key="a.archerId" :value="a.archerId">{{ a.name }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" for="scoring-round">Número de Ronda</label>
            <input id="scoring-round" class="form-input" type="number" v-model.number="form.roundNumber" placeholder="Ronda (ej. 1)" min="1" style="max-width:160px;" />
          </div>

          <!-- Mapa de campo, cono y blanco -->
          <div class="form-group" v-if="form.tournamentId">
            <label class="form-label">Posición en el campo</label>
            <p class="hint-text" style="color:var(--text-muted);margin-bottom:0.5rem;">
              Coloca el <strong>cono</strong> (posición del arquero) y el <strong>blanco</strong> dentro del campo del torneo.
              Puedes girar el cono presionando los botones de rotar.
            </p>

            <!-- Distance display -->
            <div v-if="distanceMeter !== null" class="distance-live-panel">
              <span class="material-icons" style="color:var(--lol-gold);">straighten</span>
              <span>Distancia cono → blanco:</span>
              <strong class="dist-live-val">{{ distanceMeter.toFixed(2) }} m</strong>
              <span class="dist-live-note">Verifica con la normativa de tu categoría</span>
            </div>

            <InscriptionMap
              ref="inscriptionMapRef"
              :tournament-id="form.tournamentId"
              :other-cones="sessionOtherCones"
              :category-name="selectedCategoryName"
              :initial-cone="tab1InitialCone"
              :locked="positionLocked"
              map-id="scoring-field-map"
              @cone-selected="onConeSelected"
              @target-selected="onTargetSelected"
              @distance-changed="onDistanceChanged" />
            <div v-if="positionLocked" class="position-locked-notice">
              <span class="material-icons" style="font-size:0.95rem;">lock</span>
              Posición fija para este arquero en el torneo. Cambia desde "Editar Puntajes" si necesitas moverla.
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Puntajes de Flechas (0–10 por flecha)</label>
            <div class="arrow-inputs">
              <input v-for="(_, i) in arrowCount" :key="i" :id="`arrow-score-${i+1}`"
                class="form-input arrow-score-input" type="text" inputmode="numeric" maxlength="2"
                :placeholder="`#${i+1}`" :value="displayArrow(form.arrows[i])"
                @keydown="onArrowKeydown(i, $event)" @input="onArrowInput(i, $event.target.value)" />
            </div>
            <div class="arrow-controls mt-1">
              <button class="btn btn-ghost btn-sm" id="btn-add-arrow" @click="addArrow"><span class="material-icons" style="font-size:1rem;">add</span> Flecha</button>
              <button class="btn btn-ghost btn-sm" id="btn-remove-arrow" @click="removeArrow" :disabled="arrowCount <= 1"><span class="material-icons" style="font-size:1rem;">remove</span> Flecha</button>
            </div>
          </div>
          <div class="score-preview lol-card mt-2" v-if="hasScores">
            <div class="preview-row">
              <span class="preview-label">Total de Ronda</span>
              <span class="preview-value">{{ roundTotal }}</span>
            </div>
            <div class="arrow-bubbles">
              <span v-for="(s, i) in form.arrows" :key="i" class="arrow-bubble" :class="bubbleClass(s)">{{ displayArrow(s) || '?' }}</span>
            </div>
          </div>
          <div class="modal-footer" style="padding:1.5rem 0 0;">
            <div v-if="noFieldDefined" class="no-field-alert">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
              Este torneo no tiene campo de competencia definido. Defínelo en <strong>Gestión de Torneos</strong> antes de registrar puntajes.
            </div>
            <div style="display:flex;gap:0.75rem;justify-content:flex-end;">
              <button class="btn btn-ghost" @click="resetForm" id="btn-reset-scoring"><span class="material-icons btn-icon">restart_alt</span> Limpiar</button>
              <button class="btn btn-gold" @click="submitScore" :disabled="submitting || noFieldDefined" id="btn-submit-score">
                <span class="material-icons btn-icon">send</span>{{ submitting ? 'Registrando...' : 'Registrar Ronda' }}
              </button>
            </div>
          </div>
        </template>

        <!-- Post-register result panel -->
        <Transition name="slide-up">
          <div v-if="postRegister.show" class="post-reg-panel">
            <div class="post-reg-header">
              <span class="material-icons">emoji_events</span>
              Resultado del registro
            </div>
            <div v-if="postRegister.ranking !== null" class="post-reg-row">
              <span class="post-reg-label">Posición en torneo</span>
              <span class="pos-badge" :class="rankClass(postRegister.ranking)">#{{ postRegister.ranking }} de {{ postRegister.totalInscribed }}</span>
            </div>
            <div v-if="postRegister.totalScore !== null" class="post-reg-row">
              <span class="post-reg-label">Total acumulado</span>
              <span class="post-reg-val">{{ postRegister.totalScore }} pts</span>
            </div>
            <template v-if="postRegister.distanceCheck">
              <div class="post-reg-divider">Verificación de distancia (Procedimiento Almacenado PostGIS)</div>
              <div class="post-reg-row">
                <span class="post-reg-label">Distancia real cono→blanco</span>
                <span class="post-reg-val">{{ Number(postRegister.distanceCheck.distanciaRealM).toFixed(2) }} m</span>
              </div>
              <div class="post-reg-row">
                <span class="post-reg-label">Normativa de categoría</span>
                <span class="post-reg-val">{{ Number(postRegister.distanceCheck.distanciaNormativaM).toFixed(2) }} m</span>
              </div>
              <div class="post-reg-row">
                <span class="post-reg-label">ST_Contains (geocercado)</span>
                <span class="verdict-ok"><span class="material-icons">check_circle</span> Dentro del campo</span>
              </div>
              <div class="post-reg-row">
                <span class="post-reg-label">Cumple normativa</span>
                <span :class="postRegister.distanceCheck.cumpleNormativa ? 'verdict-ok' : 'verdict-fail'">
                  <span class="material-icons">{{ postRegister.distanceCheck.cumpleNormativa ? 'check_circle' : 'cancel' }}</span>
                  {{ postRegister.distanceCheck.cumpleNormativa ? 'Sí cumple' : 'No cumple' }}
                </span>
              </div>
            </template>
            <div v-if="postRegister.posError" class="alert alert-error" style="margin-top:0.6rem;font-size:0.8rem;">
              <span class="material-icons">error</span> {{ postRegister.posError }}
            </div>
            <button class="btn btn-ghost btn-sm" style="margin-top:0.8rem;" @click="postRegister.show = false">Cerrar</button>
          </div>
        </Transition>
      </div>

      <!-- ══ TAB 2: EDIT ══ -->
      <div class="lol-card" v-show="mode === 'edit'">
        <Transition name="slide-up">
          <div class="alert alert-error" v-if="editError"><span class="material-icons">warning</span> {{ editError }}</div>
        </Transition>
        <Transition name="slide-up">
          <div class="alert alert-success" v-if="editSuccess"><span class="material-icons">check_circle</span> {{ editSuccess }}</div>
        </Transition>

        <div class="edit-search-bar">
          <div class="form-group" style="flex:1;">
            <label class="form-label" for="edit-tournament">Torneo</label>
            <select id="edit-tournament" class="form-input" v-model.number="editQuery.tournamentId">
              <option :value="null" disabled>Selecciona un torneo</option>
              <option v-for="t in allTournaments" :key="t.tournamentId" :value="t.tournamentId">
                {{ t.name }}{{ t.active ? ' ✓' : '' }}
              </option>
            </select>
          </div>
          <div class="form-group" style="flex:1;">
            <label class="form-label" for="edit-archer">Arquero</label>
            <select id="edit-archer" class="form-input" v-model.number="editQuery.archerId"
              :disabled="!editQuery.tournamentId || loadingEditArchers">
              <option :value="null" disabled>
                {{ !editQuery.tournamentId ? 'Selecciona primero un torneo' : loadingEditArchers ? 'Cargando...' : editInscribedArchers.length === 0 ? 'Sin arqueros inscritos' : 'Selecciona un arquero' }}
              </option>
              <option v-for="a in editInscribedArchers" :key="a.archerId" :value="a.archerId">{{ a.name }}</option>
            </select>
          </div>
          <div class="form-group" style="flex:0 0 auto; align-self:flex-end;">
            <button class="btn btn-gold" id="btn-search-rounds"
              :disabled="!editQuery.tournamentId || !editQuery.archerId || searchingRounds"
              @click="loadRounds">
              <span class="material-icons btn-icon">search</span>{{ searchingRounds ? 'Buscando...' : 'Buscar Rondas' }}
            </button>
          </div>
        </div>

        <div v-if="searchingRounds" class="loading-center" style="padding:2rem;"><div class="spinner"></div></div>

        <!-- Position map (shows whenever rounds are searched for) -->
        <div v-if="hasSearched && !searchingRounds" class="edit-position-section">
          <div class="edit-position-header">
            <span class="material-icons" style="color:var(--lol-gold);font-size:1rem;">person_pin_circle</span>
            <span>Posición del arquero en el torneo</span>
            <span class="edit-pos-hint">La posición debe ser la misma en todas las rondas del torneo.</span>
          </div>
          <Transition name="slide-up">
            <div class="alert alert-error" v-if="updatePosError"><span class="material-icons">error</span> {{ updatePosError }}</div>
          </Transition>
          <Transition name="slide-up">
            <div class="alert alert-success" v-if="updatePosSuccess"><span class="material-icons">check_circle</span> {{ updatePosSuccess }}</div>
          </Transition>
          <InscriptionMap
            ref="editMapRef"
            :tournament-id="editQuery.tournamentId"
            :category-name="editCategoryName"
            :initial-cone="editInitialCone"
            map-id="edit-field-map"
            @cone-selected="onEditConeSelected" />
          <div style="text-align:right;margin-top:0.6rem;">
            <button class="btn btn-gold btn-sm" id="btn-update-position" @click="updateAllRoundsPosition"
              :disabled="updatingPosition || !editCone.lat">
              <span class="material-icons btn-icon" style="font-size:0.9rem;">save</span>
              {{ updatingPosition ? 'Guardando...' : 'Guardar posición para todas las rondas' }}
            </button>
          </div>
        </div>

        <div v-if="editRounds.length === 0 && hasSearched && !searchingRounds" class="empty-edit">
          <span class="material-icons" style="font-size:2rem;opacity:0.3;">search_off</span>
          <p>No se encontraron rondas para este arquero en este torneo.</p>
        </div>

        <div v-if="editRounds.length > 0" class="rounds-list">
          <div v-for="round in editRounds" :key="round.roundId" class="round-block lol-card" :id="`round-block-${round.roundId}`">
            <div class="round-header" @click="toggleRound(round.roundId)">
              <div class="round-header-left">
                <span class="material-icons round-icon">arrow_right</span>
                <span class="round-label">Ronda {{ round.roundNumber }}</span>
                <span class="round-total-badge">Total: {{ roundSum(round) }} pts</span>
                <span v-if="hasChangesInRound(round)" class="round-unsaved-badge">● Sin guardar</span>
              </div>
              <span class="material-icons expand-icon" :class="{ rotated: openRounds.has(round.roundId) }">expand_more</span>
            </div>

            <Transition name="expand">
              <div v-if="openRounds.has(round.roundId)" class="arrow-editor">
                <div class="arrow-editor-grid">
                  <div v-for="arrow in round.arrows" :key="arrow.arrowId" class="arrow-edit-cell" :id="`arrow-cell-${arrow.arrowId}`">
                    <label class="arrow-edit-label">F{{ arrow.arrowNumber }}</label>
                    <input class="form-input arrow-score-input" :class="{ 'score-changed': changedArrows.has(arrow.arrowId) }"
                      type="number" min="0" max="10" v-model.number="arrow.score" @input="markChanged(arrow)" />
                    <span class="arrow-bubble-mini" :class="bubbleClass(arrow.score)">{{ arrow.score }}</span>
                  </div>
                </div>
                <div class="round-footer">
                  <span class="round-new-total">Nuevo total: <strong>{{ roundSum(round) }}</strong> pts</span>
                  <button class="btn btn-gold btn-sm" :id="`btn-save-round-${round.roundId}`"
                    :disabled="!hasChangesInRound(round) || savingRound === round.roundId"
                    @click="saveRound(round)">
                    <span class="material-icons btn-icon" style="font-size:1rem;">save</span>
                    {{ savingRound === round.roundId ? 'Guardando...' : 'Guardar Cambios' }}
                  </button>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import api from '@/api/axios'
import { getArchers } from '@/api/archers'
import { getTournaments } from '@/api/tournaments'
import InscriptionMap from './InscriptionMap.vue'

const mode        = ref('register')
const loadingData = ref(true)
const archers           = ref([])
const activeTournaments = ref([])
const allTournaments    = ref([])
const categories        = ref([])

// Session-level map: `${tournamentId}-${archerId}` → { lat, lng, bearing }
const archerPositions = reactive({})

// Returns the stored position key
function posKey(tId, aId) { return `${tId}-${aId}` }

// Category name for the selected tournament
const selectedCategoryName = computed(() => {
  if (!form.tournamentId) return ''
  const t = allTournaments.value.find(t => t.tournamentId === form.tournamentId)
  if (!t?.categoryId) return ''
  const cat = categories.value.find(c => c.categoryId === t.categoryId || c.id_category === t.categoryId)
  return cat?.name || cat?.categoryName || ''
})

// Pre-fill Tab 1 map if the archer already has a registered position in this tournament
const tab1InitialCone = computed(() => {
  if (!form.tournamentId || !form.archerId) return null
  return archerPositions[posKey(form.tournamentId, form.archerId)] ?? null
})

// True when archer already has rounds with position → map is read-only
const positionLocked = ref(false)

// ── TAB 1 ─────────────────────────────────────────────────────────────
const error      = ref('')
const success    = ref('')
const submitting = ref(false)
const arrowCount = ref(6)
const form = reactive({
  tournamentId: null, archerId: null, roundNumber: 1, arrows: Array(6).fill(null),
  targetId: null, lon: null, lat: null, targetLon: null, targetLat: null,
})
const tournamentTargets = ref([])

const inscriptionMapRef  = ref(null)
const distanceMeter      = ref(null)
const sessionOtherCones  = ref([])   // cones from other archers in current session

const postRegister = reactive({
  show: false, ranking: null, totalScore: null, totalInscribed: null,
  distanceCheck: null, posError: '',
})

function onConeSelected(point) {
  if (point) {
    form.lon = point.coordinates[0]
    form.lat = point.coordinates[1]
  } else {
    form.lon = null; form.lat = null
  }
}
function onTargetSelected(point) {
  // We store target coords for submission but we don't have a target entity yet
  // The target is a free-form point the admin places on the map
  // We encode it as form.targetLon/targetLat
  if (point) {
    form.targetLon = point.coordinates[0]
    form.targetLat = point.coordinates[1]
  } else {
    form.targetLon = null; form.targetLat = null
  }
}
function onDistanceChanged(d) {
  distanceMeter.value = d
}

async function loadTournamentTargets(tournamentId) {
  tournamentTargets.value = []
  form.targetId = null
  if (!tournamentId) return
  try {
    const res = await api.get(`/targets/tournament/${tournamentId}`)
    tournamentTargets.value = res.data.features || []
  } catch (e) { console.error('[AdminScoring] targets error:', e.message) }
}

const tab1InscribedArchers = ref([])
const loadingTab1Archers   = ref(false)
const noFieldDefined       = ref(false)

watch(() => form.tournamentId, async (id) => {
  loadTournamentTargets(id)
  sessionOtherCones.value = []
  distanceMeter.value = null
  positionLocked.value = false
  form.archerId = null
  tab1InscribedArchers.value = []
  noFieldDefined.value = false
  if (!id) return
  // Load inscribed archers
  loadingTab1Archers.value = true
  try {
    const res = await api.get('/inscriptions')
    const all = Array.isArray(res.data) ? res.data : []
    tab1InscribedArchers.value = all
      .filter(i => i.tournamentId === id)
      .map(i => archers.value.find(a => a.archerId === i.archerId))
      .filter(Boolean)
  } catch (e) { console.error('[AdminScoring] tab1 archers error:', e.message) }
  finally { loadingTab1Archers.value = false }
  // Check if tournament has a competition zone defined (filter client-side, API returns all zones)
  try {
    const zRes = await api.get('/competition-zones')
    const features = Array.isArray(zRes.data?.features) ? zRes.data.features
                   : Array.isArray(zRes.data) ? zRes.data : []
    const tourneyZones = features.filter(f => {
      const tid = f.properties?.id_tournament ?? f.properties?.tournamentId
                ?? f.id_tournament ?? f.tournamentId
      return Number(tid) === Number(id)
    })
    noFieldDefined.value = tourneyZones.length === 0
  } catch { noFieldDefined.value = false }
})

// When archer changes, try to load their existing position from backend
watch(() => form.archerId, async (aId) => {
  positionLocked.value = false
  if (!aId || !form.tournamentId) return
  const key = posKey(form.tournamentId, aId)
  // If already in session store, use it and lock
  if (archerPositions[key]) {
    positionLocked.value = true
    return
  }
  // Otherwise query backend for existing rounds with position
  try {
    const res = await api.get('/rounds', { params: { tournamentId: form.tournamentId, archerId: aId } })
    const rounds = Array.isArray(res.data) ? res.data : []
    // Look for a round that has archer_location / position data
    // Backend may return lon/lat in the round DTO
    const withPos = rounds.find(r => r.lon != null || r.archerLon != null || r.archerLocation != null)
    if (withPos) {
      const lon = withPos.lon ?? withPos.archerLon ?? withPos.archerLocation?.coordinates?.[0]
      const lat = withPos.lat ?? withPos.archerLat ?? withPos.archerLocation?.coordinates?.[1]
      if (lon != null && lat != null) {
        archerPositions[key] = { lat, lng: lon, bearing: 0 }
        positionLocked.value = true
      }
    }
  } catch { /* non-critical */ }
})

const hasScores  = computed(() => form.arrows.some(a => a !== null))
const roundTotal = computed(() => form.arrows.reduce((sum, v) => sum + (typeof v === 'number' ? v : 0), 0))

function displayArrow(v) { return (v === null || v === undefined) ? '' : String(v) }

function onArrowInput(i, raw) {
  const val = raw.trim()
  if (val === '') { form.arrows[i] = null; return }
  if (val.toUpperCase() === 'M') { form.arrows[i] = 'M'; return }
  const num = parseInt(val, 10)
  if (!isNaN(num)) form.arrows[i] = Math.min(10, Math.max(0, num))
}
function onArrowKeydown(i, e) {
  const allowed = ['Backspace','Delete','ArrowLeft','ArrowRight','Tab','Enter']
  if (allowed.includes(e.key) || e.key === 'm' || e.key === 'M') return
  if (!/^\d$/.test(e.key)) { e.preventDefault(); return }
  const next = String(displayArrow(form.arrows[i])) + e.key
  if (parseInt(next, 10) > 10) e.preventDefault()
}
function addArrow()    { arrowCount.value++; form.arrows.push(null) }
function removeArrow() { if (arrowCount.value > 1) { arrowCount.value--; form.arrows.pop() } }
function bubbleClass(s) {
  if (s === null || s === undefined) return 'bubble-empty'
  if (s === 'M')  return 'bubble-miss'
  if (s === 10)   return 'bubble-perfect'
  if (s >= 7)     return 'bubble-high'
  if (s >= 4)     return 'bubble-mid'
  return 'bubble-low'
}
function resetForm() {
  Object.assign(form, {
    tournamentId: null, archerId: null, roundNumber: 1, arrows: Array(arrowCount.value).fill(null),
    targetId: null, lon: null, lat: null, targetLon: null, targetLat: null,
  })
  error.value = success.value = ''
  distanceMeter.value = null
  inscriptionMapRef.value?.clearAll()
}

async function submitScore() {
  error.value = success.value = ''
  if (!form.tournamentId) { error.value = 'Selecciona un torneo.'; return }
  if (!form.archerId)     { error.value = 'Selecciona un arquero.'; return }
  if (!form.lon || !form.lat) {
    error.value = 'Debes colocar la posición del arquero en el mapa antes de registrar.'
    return
  }

  // ── Pre-check: verify the round number doesn't already exist ──
  try {
    const checkRes = await api.get('/rounds', { params: { tournamentId: form.tournamentId, archerId: form.archerId } })
    const existingRounds = Array.isArray(checkRes.data) ? checkRes.data : []
    const duplicate = existingRounds.find(r => r.roundNumber === form.roundNumber)
    if (duplicate) {
      error.value = `La ronda ${form.roundNumber} ya tiene flechas registradas para este arquero en este torneo. Ve a la pestaña "Editar Puntajes" para modificarlas.`
      return
    }
  } catch { /* if check fails, let the backend handle it */ }

  const scores     = form.arrows.map(v => (v === 'M' || v === null) ? 0 : Number(v))
  const snapTId    = form.tournamentId
  const snapAId    = form.archerId
  const snapRNum   = form.roundNumber
  const snapLon    = form.lon
  const snapLat    = form.lat
  const snapTLon   = form.targetLon
  const snapTLat   = form.targetLat
  submitting.value = true
  postRegister.show = false
  try {
    await api.post('/rounds', {
      tournamentId: snapTId, archerId: snapAId, roundNumber: snapRNum, scores,
      lon: snapLon, lat: snapLat,
    })
    success.value = '¡Ronda registrada exitosamente!'
    // Save archer position for reuse in this session and lock it
    if (snapLon && snapLat) {
      archerPositions[posKey(snapTId, snapAId)] = {
        lat: snapLat,
        lng: snapLon,
        bearing: inscriptionMapRef.value?.getBearing?.() ?? 0,
      }
      positionLocked.value = true
    }
    // Track this archer's cone in session so other archers see it
    if (snapLon && snapLat) {
      const archer = archers.value.find(a => a.archerId === snapAId)
      sessionOtherCones.value = [
        ...sessionOtherCones.value.filter(c => c.archerId !== snapAId),
        { archerId: snapAId, lat: snapLat, lng: snapLon, archerName: archer?.name || `Arquero #${snapAId}` }
      ]
    }
    resetForm()
    // ── Post-register panel ──
    postRegister.show = true
    postRegister.ranking = null; postRegister.totalScore = null
    postRegister.totalInscribed = null; postRegister.distanceCheck = null
    postRegister.posError = ''
    // 1) Ranking
    try {
      const [roundsRes, inscRes] = await Promise.all([
        api.get('/rounds', { params: { tournamentId: snapTId, archerId: snapAId } }),
        api.get('/inscriptions'),
      ])
      const archerTotal = roundsRes.data.reduce((sum, r) =>
        sum + r.arrows.reduce((s, a) => s + (typeof a.score === 'number' ? a.score : 0), 0), 0)
      postRegister.totalScore = archerTotal
      const allIns = Array.isArray(inscRes.data) ? inscRes.data : []
      const tourIns = allIns.filter(i => i.tournamentId === snapTId)
      postRegister.totalInscribed = tourIns.length
      const ranked = tourIns.map(i => ({ archerId: i.archerId, score: i.score ?? 0 })).sort((a,b) => b.score - a.score)
      const pos = ranked.findIndex(r => r.archerId === snapAId)
      postRegister.ranking = pos >= 0 ? pos + 1 : tourIns.length
    } catch { /* non-critical */ }
    // 2) DistanceCheck via position endpoint
    if (snapLon && snapLat) {
      try {
        const roundsRes2 = await api.get('/rounds', { params: { tournamentId: snapTId, archerId: snapAId } })
        const newRound = roundsRes2.data.find(r => r.roundNumber === snapRNum)
        if (newRound) {
          const posRes = await api.put(`/rounds/${newRound.roundId}/position`, {
            lon: snapLon, lat: snapLat, targetId: null
          })
          if (posRes.data?.distanceCheck && Object.keys(posRes.data.distanceCheck).length > 0) {
            postRegister.distanceCheck = posRes.data.distanceCheck
          }
        }
      } catch (e) {
        const msg = e.response?.data?.error ?? ''
        if (msg) postRegister.posError = parsePosError(msg)
      }
    }
  } catch (e) {
    const httpStatus = e.response?.status
    const rawMsg = e.response?.data?.error || e.response?.data?.message || ''
    error.value = parseSubmitError(rawMsg, httpStatus, form.roundNumber)
  } finally { submitting.value = false }
}

function parseSubmitError(msg, status, roundNumber) {
  const m = (msg || '').toLowerCase()
  // Round already registered / duplicate key
  if (
    status === 409 ||
    m.includes('duplicate') ||
    m.includes('already exists') ||
    m.includes('ya existe') ||
    m.includes('unique') ||
    m.includes('flechas') && m.includes('registradas') ||
    m.includes('round') && (m.includes('exist') || m.includes('registr'))
  ) {
    return `⚠️ La ronda ${roundNumber ?? ''} ya tiene flechas registradas para este arquero en este torneo. Usa la pestaña "Editar Puntajes" para modificarlas.`
  }
  // Archer not inscribed
  if (m.includes('inscri') || m.includes('not found') || status === 404)
    return '❌ El arquero no está inscrito en este torneo o no existe.'
  // Tournament not active
  if (m.includes('activo') || m.includes('inactive'))
    return '🔒 El torneo no está activo. Solo se puede registrar en torneos activos.'
  // Category mismatch
  if (m.includes('categor'))
    return '❌ El arquero no pertenece a la categoría de este torneo.'
  return msg || 'Error al registrar la ronda.'
}

function parsePosError(msg) {
  if (!msg) return ''
  const m = msg.toLowerCase()
  if (m.includes('zona') || m.includes('fuera') || m.includes('contain') || m.includes('dentro'))
    return '🚫 El cono está fuera del campo de competencia (ST_Contains). El puntaje fue registrado pero la posición GPS es inválida.'
  if (m.includes('5 metro') || m.includes('seguridad') || m.includes('distanc'))
    return '⚠️ El cono está a menos de 5 m de otro arquero activo (ST_Distance). Riesgo de seguridad.'
  return msg
}

function rankClass(pos) {
  if (pos === 1) return 'rank-gold'
  if (pos === 2) return 'rank-silver'
  if (pos === 3) return 'rank-bronze'
  return 'rank-normal'
}

// ── TAB 2 ─────────────────────────────────────────────────────────────
const editError   = ref('')
const editSuccess = ref('')
const editQuery   = reactive({ tournamentId: null, archerId: null })
const editRounds  = ref([])
const openRounds  = ref(new Set())
const changedArrows   = ref(new Set())
const originalScores  = ref({})
const searchingRounds = ref(false)
const savingRound     = ref(null)
const hasSearched     = ref(false)

const editMapRef        = ref(null)
const editCone          = reactive({ lat: null, lng: null })
const updatingPosition  = ref(false)
const updatePosSuccess  = ref('')
const updatePosError    = ref('')
const editInscribedArchers = ref([])   // arqueros inscritos al torneo seleccionado en Tab 2
const loadingEditArchers   = ref(false)

// Al cambiar el torneo en Tab 2, cargar los arqueros inscritos y resetear selección
watch(() => editQuery.tournamentId, async (tId) => {
  editQuery.archerId = null
  editInscribedArchers.value = []
  hasSearched.value = false
  editRounds.value = []
  updatePosSuccess.value = ''
  updatePosError.value = ''
  if (!tId) return
  loadingEditArchers.value = true
  try {
    const res = await api.get('/inscriptions')
    const all = Array.isArray(res.data) ? res.data : []
    const tourneyIns = all.filter(i => i.tournamentId === tId)
    // Map inscribed archerId list to full archer objects
    editInscribedArchers.value = tourneyIns
      .map(i => archers.value.find(a => a.archerId === i.archerId))
      .filter(Boolean)
  } catch (e) {
    console.error('[AdminScoring] edit archers error:', e.message)
  } finally { loadingEditArchers.value = false }
})

// Category name for the edit tab's selected tournament
const editCategoryName = computed(() => {
  if (!editQuery.tournamentId) return ''
  const t = allTournaments.value.find(t => t.tournamentId === editQuery.tournamentId)
  if (!t?.categoryId) return ''
  const cat = categories.value.find(c => c.categoryId === t.categoryId || c.id_category === t.categoryId)
  return cat?.name || cat?.categoryName || ''
})

// Initial cone for edit map (from session store)
const editInitialCone = computed(() => {
  if (!editQuery.tournamentId || !editQuery.archerId) return null
  return archerPositions[posKey(editQuery.tournamentId, editQuery.archerId)] ?? null
})

function onEditConeSelected(point) {
  if (point) { editCone.lat = point.coordinates[1]; editCone.lng = point.coordinates[0] }
  else { editCone.lat = null; editCone.lng = null }
}

async function updateAllRoundsPosition() {
  if (!editCone.lat || !editCone.lng) {
    updatePosError.value = 'Coloca el cono del arquero en el mapa primero.'
    return
  }
  if (editRounds.value.length === 0) {
    updatePosError.value = 'No hay rondas cargadas para actualizar.'
    return
  }
  updatePosError.value = ''
  updatePosSuccess.value = ''
  updatingPosition.value = true
  try {
    await Promise.all(
      editRounds.value.map(r =>
        api.put(`/rounds/${r.roundId}/position`, { lon: editCone.lng, lat: editCone.lat, targetId: null })
      )
    )
    // Save to session store
    archerPositions[posKey(editQuery.tournamentId, editQuery.archerId)] = {
      lat: editCone.lat,
      lng: editCone.lng,
      bearing: editMapRef.value?.getBearing?.() ?? 0,
    }
    updatePosSuccess.value = `✅ Posición actualizada en ${editRounds.value.length} ronda(s) del torneo.`
  } catch (e) {
    updatePosError.value = e.response?.data?.error || 'Error al actualizar la posición.'
  } finally { updatingPosition.value = false }
}

async function loadRounds() {
  editError.value = editSuccess.value = ''
  updatePosSuccess.value = ''
  updatePosError.value = ''
  searchingRounds.value = true
  hasSearched.value = true
  openRounds.value = new Set()
  changedArrows.value = new Set()
  try {
    const res = await api.get('/rounds', { params: { tournamentId: editQuery.tournamentId, archerId: editQuery.archerId } })
    editRounds.value = res.data
    const orig = {}
    res.data.forEach(r => r.arrows.forEach(a => { orig[a.arrowId] = a.score }))
    originalScores.value = orig
  } catch (e) {
    editError.value = e.response?.data?.error || 'Error al cargar las rondas.'
    editRounds.value = []
  } finally { searchingRounds.value = false }
}

function toggleRound(roundId) {
  const s = new Set(openRounds.value)
  s.has(roundId) ? s.delete(roundId) : s.add(roundId)
  openRounds.value = s
}
function markChanged(arrow) {
  const s = new Set(changedArrows.value)
  arrow.score !== originalScores.value[arrow.arrowId] ? s.add(arrow.arrowId) : s.delete(arrow.arrowId)
  changedArrows.value = s
}
function hasChangesInRound(round) { return round.arrows.some(a => changedArrows.value.has(a.arrowId)) }
function roundSum(round) { return round.arrows.reduce((sum, a) => sum + (typeof a.score === 'number' ? a.score : 0), 0) }

async function saveRound(round) {
  editError.value = editSuccess.value = ''
  savingRound.value = round.roundId
  const changed = round.arrows.filter(a => changedArrows.value.has(a.arrowId))
  try {
    await Promise.all(changed.map(arrow => api.put(`/rounds/${round.roundId}/arrows/${arrow.arrowId}`, { score: arrow.score })))
    const orig = { ...originalScores.value }
    changed.forEach(a => { orig[a.arrowId] = a.score })
    originalScores.value = orig
    const s = new Set(changedArrows.value)
    changed.forEach(a => s.delete(a.arrowId))
    changedArrows.value = s
    editSuccess.value = `Ronda ${round.roundNumber} actualizada. El log de auditoría fue registrado automáticamente.`
    setTimeout(() => { editSuccess.value = '' }, 4000)
  } catch (e) {
    editError.value = e.response?.data?.error || 'Error al guardar los cambios.'
  } finally { savingRound.value = null }
}

onMounted(async () => {
  try {
    const res = await getTournaments()
    allTournaments.value = Array.isArray(res.data) ? res.data : []
    activeTournaments.value = allTournaments.value.filter(t => t.active === true || t.active === 'true')
  } catch (e) { console.error('[AdminScoring] tournaments error:', e.message) }
  try {
    const res = await getArchers()
    archers.value = res.data
  } catch (e) { console.error('[AdminScoring] archers error:', e.message) }
  try {
    const res = await api.get('/categories')
    categories.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { console.error('[AdminScoring] categories error:', e.message) }
  loadingData.value = false
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/icon?family=Material+Icons');
.admin-page { padding: calc(var(--header-height, 70px) + 2rem) 0 4rem; min-height: 100vh; }
.admin-page-header { margin-bottom: 1.5rem; }
.btn-back { display:inline-flex;align-items:center;gap:0.3rem;background:none;border:none;color:var(--text-muted);cursor:pointer;font-family:'Cinzel',serif;font-size:0.7rem;text-transform:uppercase;letter-spacing:0.1em;padding:0;margin-bottom:1rem;transition:color 0.2s; }
.btn-back:hover { color: var(--lol-gold); }
.btn-back .material-icons { font-size: 1rem; }
.page-title { font-size:1.5rem;font-family:'Cinzel',serif;display:flex;align-items:center;gap:0.5rem;margin-bottom:0.3rem; }
.page-title-icon { font-size:1.3rem;color:var(--lol-gold); }
.page-subtitle { font-size:0.82rem;color:var(--text-muted);margin:0; }
.page-rule { margin:1rem 0 1.5rem; }
.hint-text { font-size:0.75rem;color:var(--lol-danger,#e84057);margin-top:0.3rem;display:block; }
.btn-icon { display:inline-flex;align-items:center;gap:0.3rem; }
/* Tabs */
.mode-tabs { display:flex;gap:0.5rem;margin-bottom:1.2rem; }
.mode-tab { display:flex;align-items:center;gap:0.4rem;padding:0.55rem 1.2rem;font-family:'Cinzel',serif;font-size:0.72rem;font-weight:700;text-transform:uppercase;letter-spacing:0.1em;background:transparent;border:1px solid var(--lol-border,rgba(200,155,60,0.2));color:var(--text-muted);cursor:pointer;transition:all 0.2s; }
.mode-tab .material-icons { font-size:1rem; }
.mode-tab:hover { border-color:var(--lol-gold-dark);color:var(--lol-gold-light); }
.mode-tab.active { background:rgba(200,155,60,0.1);border-color:var(--lol-gold);color:var(--lol-gold-bright); }
/* Arrow inputs */
.arrow-inputs { display:grid;grid-template-columns:repeat(auto-fill,minmax(72px,1fr));gap:0.5rem; }
.arrow-score-input { text-align:center; }
.arrow-controls { display:flex;gap:0.5rem; }
.score-preview { padding:1rem;border-color:var(--lol-gold-dark); }
.preview-row { display:flex;justify-content:space-between;align-items:center;margin-bottom:0.8rem; }
.preview-label { font-family:'Cinzel',serif;font-size:0.72rem;text-transform:uppercase;letter-spacing:0.1em;color:var(--text-muted); }
.preview-value { font-family:'Cinzel',serif;font-size:1.6rem;font-weight:700;color:var(--lol-gold-bright); }
.arrow-bubbles { display:flex;gap:0.4rem;flex-wrap:wrap; }
.arrow-bubble { width:36px;height:36px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-family:'Cinzel',serif;font-size:0.85rem;font-weight:700;border:1px solid; }
/* Edit tab */
.edit-search-bar { display:flex;gap:1rem;align-items:flex-start;flex-wrap:wrap;margin-bottom:1.5rem; }
.empty-edit { text-align:center;padding:3rem 1rem;color:var(--text-muted);display:flex;flex-direction:column;align-items:center;gap:0.5rem; }
.empty-edit p { font-size:0.88rem; }
.rounds-list { display:flex;flex-direction:column;gap:0.75rem; }
.round-block { border:1px solid var(--lol-border,rgba(200,155,60,0.15));border-radius:4px;overflow:hidden;padding:0; }
.round-header { display:flex;align-items:center;justify-content:space-between;padding:0.9rem 1.1rem;cursor:pointer;transition:background 0.2s; }
.round-header:hover { background:rgba(200,155,60,0.05); }
.round-header-left { display:flex;align-items:center;gap:0.6rem; }
.round-icon { font-size:1.1rem;color:var(--lol-gold-dark); }
.round-label { font-family:'Cinzel',serif;font-size:0.85rem;font-weight:700;color:var(--text-primary); }
.round-total-badge { font-size:0.72rem;font-family:'Cinzel',serif;color:var(--lol-gold);background:rgba(200,155,60,0.1);border:1px solid rgba(200,155,60,0.25);padding:0.15rem 0.5rem;border-radius:2px; }
.round-unsaved-badge { font-size:0.7rem;color:var(--lol-warning,#ff9800);font-family:'Cinzel',serif;animation:pulse 1.5s infinite; }
@keyframes pulse { 0%,100% { opacity:1 } 50% { opacity:0.4 } }
.expand-icon { color:var(--text-muted);transition:transform 0.25s;font-size:1.2rem; }
.expand-icon.rotated { transform:rotate(180deg); }
.arrow-editor { padding:1rem 1.1rem 1.1rem;border-top:1px solid var(--lol-border,rgba(200,155,60,0.15));background:rgba(0,0,0,0.15); }
.arrow-editor-grid { display:flex;flex-wrap:wrap;gap:0.75rem;margin-bottom:1rem; }
.arrow-edit-cell { display:flex;flex-direction:column;align-items:center;gap:0.3rem;width:64px; }
.arrow-edit-label { font-family:'Cinzel',serif;font-size:0.62rem;text-transform:uppercase;letter-spacing:0.1em;color:var(--text-muted); }
.arrow-edit-cell .form-input { width:64px;text-align:center;padding:0.4rem 0.3rem;transition:border-color 0.2s,box-shadow 0.2s; }
.score-changed { border-color:var(--lol-gold)!important;box-shadow:0 0 6px rgba(200,155,60,0.35)!important; }
.arrow-bubble-mini { width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-family:'Cinzel',serif;font-size:0.75rem;font-weight:700;border:1px solid; }
.round-footer { display:flex;align-items:center;justify-content:space-between;padding-top:0.75rem;border-top:1px solid rgba(200,155,60,0.1); }
.round-new-total { font-size:0.82rem;color:var(--text-muted);font-family:'Cinzel',serif; }
.round-new-total strong { color:var(--lol-gold-bright); }
/* Bubbles */
.bubble-empty   { border-color:var(--lol-border);color:var(--text-muted); }
.bubble-perfect { border-color:var(--lol-gold);color:var(--lol-gold-bright);background:rgba(200,155,60,0.1); }
.bubble-high    { border-color:var(--lol-success,#4caf50);color:var(--lol-success,#4caf50); }
.bubble-mid     { border-color:var(--lol-warning,#ff9800);color:var(--lol-warning,#ff9800); }
.bubble-low     { border-color:var(--lol-danger,#e84057);color:var(--lol-danger,#e84057); }
.bubble-miss    { border-color:#888;color:#888;font-style:italic; }
/* Transitions */
.expand-enter-active,.expand-leave-active { transition:all 0.25s ease;overflow:hidden; }
.expand-enter-from,.expand-leave-to { opacity:0;max-height:0; }
.expand-enter-to,.expand-leave-from { opacity:1;max-height:800px; }
.slide-up-enter-active,.slide-up-leave-active { transition:all 0.2s ease; }
.slide-up-enter-from,.slide-up-leave-to { opacity:0;transform:translateY(-8px); }

/* Distance live panel */
.distance-live-panel {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 0.9rem; border-radius: 6px; margin-bottom: 0.6rem;
  background: rgba(200,155,60,0.08); border: 1px solid rgba(200,155,60,0.3);
  font-size: 0.82rem; flex-wrap: wrap;
}
.dist-live-val {
  font-family: 'Cinzel', serif; font-size: 1.1rem;
  color: var(--lol-gold-bright); font-weight: 700;
}
.dist-live-note { font-size: 0.72rem; color: var(--text-muted); }

/* Post-register panel */
.post-reg-panel {
  margin-top: 1rem; padding: 1.1rem 1.2rem;
  background: rgba(200,155,60,0.06); border: 1px solid rgba(200,155,60,0.25); border-radius: 8px;
}
.post-reg-header {
  display: flex; align-items: center; gap: 0.4rem;
  font-family: 'Cinzel', serif; font-size: 0.8rem; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.08em; color: var(--lol-gold); margin-bottom: 0.9rem;
}
.post-reg-header .material-icons { font-size: 1rem; }
.post-reg-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.3rem 0; border-bottom: 1px solid rgba(200,155,60,0.08);
}
.post-reg-label { font-size: 0.75rem; color: var(--text-muted); font-family: 'Cinzel', serif; text-transform: uppercase; letter-spacing: 0.05em; }
.post-reg-val { font-family: 'Cinzel', serif; font-size: 0.88rem; font-weight: 700; color: var(--lol-gold-light); }
.post-reg-divider {
  font-size: 0.68rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.08em;
  padding: 0.5rem 0 0.3rem; margin-top: 0.4rem; font-family: 'Cinzel', serif;
  border-top: 1px solid rgba(200,155,60,0.15);
}
.verdict-ok   { display: inline-flex; align-items: center; gap: 0.25rem; color: #0ac8b9; font-size: 0.78rem; font-weight: 700; }
.verdict-fail { display: inline-flex; align-items: center; gap: 0.25rem; color: #e84057; font-size: 0.78rem; font-weight: 700; }
.verdict-ok .material-icons, .verdict-fail .material-icons { font-size: 0.9rem; }
.pos-badge {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 0.2rem 0.65rem; border-radius: 4px; font-family: 'Cinzel', serif;
  font-size: 0.88rem; font-weight: 700; border: 1px solid;
}
.rank-gold   { background: rgba(200,155,60,0.2);  border-color: var(--lol-gold);  color: var(--lol-gold-bright); }
.rank-silver { background: rgba(192,192,192,0.15); border-color: #c0c0c0;          color: #c0c0c0; }
.rank-bronze { background: rgba(205,127,50,0.15);  border-color: #cd7f32;          color: #cd7f32; }
.rank-normal { background: rgba(255,255,255,0.04); border-color: rgba(255,255,255,0.12); color: var(--text-muted); }

/* Edit position section (Tab 2) */
.edit-position-section {
  margin: 1rem 0; padding: 1rem;
  background: rgba(200,155,60,0.04); border: 1px solid rgba(200,155,60,0.18); border-radius: 8px;
}
.edit-position-header {
  display: flex; align-items: center; gap: 0.5rem;
  font-family: 'Cinzel', serif; font-size: 0.78rem; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.07em; color: var(--lol-gold);
  margin-bottom: 0.75rem;
}
.edit-pos-hint {
  font-size: 0.7rem; color: var(--text-muted); text-transform: none; letter-spacing: 0;
  font-weight: 400; font-family: inherit; margin-left: auto;
}
.position-locked-notice {
  display: flex; align-items: center; gap: 0.4rem;
  margin-top: 0.4rem; padding: 0.4rem 0.75rem; border-radius: 6px;
  background: rgba(200,155,60,0.08); border: 1px solid rgba(200,155,60,0.3);
  color: var(--lol-gold-light); font-size: 0.76rem;
}
.no-field-alert {
  display: flex; align-items: flex-start; gap: 0.5rem;
  padding: 0.6rem 0.9rem; border-radius: 6px; margin-bottom: 0.75rem;
  background: rgba(232,64,87,0.08); border: 1px solid rgba(232,64,87,0.3);
  color: #ff8a9a; font-size: 0.78rem; line-height: 1.4;
}
.no-field-alert svg { flex-shrink: 0; margin-top: 2px; fill: #e84057; }
.no-field-alert strong { color: #ffb3bc; }
</style>
