<template>
  <div class="page-wrapper">
    <div class="container" style="max-width:700px;">
      <div class="flex-between mb-4">
        <h1 class="section-title" style="margin-bottom:0;">Categorías</h1>
        <button class="btn btn-gold" id="btn-add-category" @click="openCreate">+ Nueva Categoría</button>
      </div>

      <div v-if="loading" class="loading-center"><div class="spinner"></div></div>

      <div v-else class="lol-table-wrapper">
        <table class="lol-table" id="categories-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categories" :key="c.id_category" :id="`category-row-${c.id_category}`">
              <td class="text-muted">#{{ c.id_category }}</td>
              <td><span class="badge badge-blue">{{ c.name }}</span></td>
              <td>
                <div class="flex gap-1">
                  <button class="btn btn-ghost btn-sm" :id="`btn-edit-category-${c.id_category}`" @click="openEdit(c)">Editar</button>
                  <button class="btn btn-danger btn-sm" :id="`btn-delete-category-${c.id_category}`" @click="confirmDelete(c)">Eliminar</button>
                </div>
              </td>
            </tr>
            <tr v-if="categories.length === 0">
              <td colspan="3" class="text-center text-muted" style="padding:2rem;">Aún no hay categorías.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Create/Edit Modal -->
      <Transition name="fade">
        <div class="modal-overlay" v-if="showModal" @click.self="showModal = false">
          <div class="modal-box">
            <div class="modal-header">
              <h3>{{ editingCat ? 'Editar Categoría' : 'Nueva Categoría' }}</h3>
              <button class="modal-close" @click="showModal = false">✕</button>
            </div>
            <div class="modal-body">
              <Transition name="slide-up">
                <div class="alert alert-error" v-if="modalError">⚠ {{ modalError }}</div>
              </Transition>
              <div class="form-group">
                <label class="form-label" for="cat-name">Nombre de la Categoría</label>
                <input id="cat-name" class="form-input" v-model="catName" placeholder="ej. Recurvo Abierto" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showModal = false">Cancelar</button>
              <button class="btn btn-gold" id="btn-save-category" @click="saveCategory" :disabled="saving">
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
              <h3>Eliminar Categoría</h3>
              <button class="modal-close" @click="showDeleteModal = false">✕</button>
            </div>
            <div class="modal-body">
              <Transition name="slide-up">
                <div class="alert alert-error" v-if="deleteError">⚠ {{ deleteError }}</div>
              </Transition>
              <p class="text-secondary">¿Eliminar la categoría <strong class="text-gold">{{ deletingCat?.name }}</strong>?
              Los arqueros de esta categoría quedarán sin categoría.</p>
            </div>
            <div class="modal-footer">
              <button class="btn btn-ghost" @click="showDeleteModal = false">Cancelar</button>
              <button class="btn btn-danger" id="btn-confirm-delete-category" @click="doDelete" :disabled="saving">
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
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const categories     = ref([])
const loading        = ref(true)
const showModal      = ref(false)
const showDeleteModal = ref(false)
const editingCat     = ref(null)
const deletingCat    = ref(null)
const catName        = ref('')
const saving         = ref(false)
const modalError     = ref('')
const deleteError    = ref('')

async function load() {
  try { const res = await api.get('/categories'); categories.value = res.data }
  catch { /* ignore */ } finally { loading.value = false }
}

function openCreate() { editingCat.value = null; catName.value = ''; modalError.value = ''; showModal.value = true }
function openEdit(c)  { editingCat.value = c; catName.value = c.name; modalError.value = ''; showModal.value = true }
function confirmDelete(c) { deletingCat.value = c; deleteError.value = ''; showDeleteModal.value = true }

async function saveCategory() {
  modalError.value = ''
  saving.value = true
  try {
    if (editingCat.value) await api.put(`/categories/${editingCat.value.id_category}`, { name: catName.value })
    else await api.post('/categories', { name: catName.value })
    showModal.value = false
    await load()
  } catch (e) {
    modalError.value = e.response?.data?.error || 'No se pudo guardar la categoría.'
  } finally { saving.value = false }
}

async function doDelete() {
  deleteError.value = ''
  saving.value = true
  try {
    await api.delete(`/categories/${deletingCat.value.id_category}`)
    showDeleteModal.value = false
    await load()
  } catch (e) {
    deleteError.value = e.response?.data?.error || 'No se pudo eliminar la categoría.'
  } finally { saving.value = false }
}

onMounted(load)
</script>
