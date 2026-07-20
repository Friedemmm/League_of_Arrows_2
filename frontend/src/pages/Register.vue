<template>
  <!-- No background image — the global aurora from App.vue is the background -->
  <div class="login-page">

    <!-- Register card — same panel aesthetic as Login -->
    <div class="login-card">

      <!-- ── Section header ── -->
      <div class="section-header login-header">
        <div class="card-crest" aria-hidden="true">
          <svg width="38" height="38" viewBox="0 0 32 32" fill="none">
            <polygon
              points="16,2 20,12 30,12 22,18 25,29 16,23 7,29 10,18 2,12 12,12"
              fill="none" stroke="currentColor" stroke-width="1.5"
            />
            <circle cx="16" cy="16" r="3.5" fill="currentColor" opacity="0.9"/>
          </svg>
        </div>

        <h1 class="section-title login-title">Crear cuenta</h1>

        <div class="section-meta-row login-meta">
          <p class="section-subtitle">Regístrate y compite</p>
        </div>
        <hr class="header-rule" />
      </div>

      <!-- ── Error alert ── -->
      <Transition name="slide-up">
        <div class="login-alert" v-if="error" id="register-error" role="alert">
          ⚠ {{ error }}
        </div>
      </Transition>

      <!-- ── Register form ── -->
      <form @submit.prevent="handleRegister" id="register-form" novalidate>

        <div class="field-group">
          <label class="field-label" for="register-name">Nombre</label>
          <input
            id="register-name"
            class="field-input"
            type="text"
            v-model="name"
            placeholder="Tu nombre"
            required
            autocomplete="name"
          />
        </div>

        <div class="field-group">
          <label class="field-label" for="register-email">Email</label>
          <input
            id="register-email"
            class="field-input"
            type="email"
            v-model="email"
            placeholder="archer@example.com"
            required
            autocomplete="email"
          />
        </div>

        <div class="field-group">
          <label class="field-label" for="register-password">Contraseña</label>
          <input
            id="register-password"
            class="field-input"
            type="password"
            v-model="password"
            placeholder="••••••••"
            required
            autocomplete="new-password"
          />
        </div>

        <div class="field-group">
          <label class="field-label" for="register-category">Categoría (opcional)</label>
          <select
            id="register-category"
            class="field-input"
            v-model="categoryId"
          >
            <option :value="null">Sin categoría</option>
            <option v-for="c in categories" :key="c.id_category" :value="c.id_category">
              {{ c.name }}
            </option>
          </select>
        </div>

        <button
          type="submit"
          id="btn-register"
          class="btn-primary"
          :disabled="loading"
        >
          <span v-if="loading" class="btn-spinner">⟳</span>
          <span>{{ loading ? 'Creando cuenta...' : 'Registrarme' }}</span>
        </button>

      </form>

      <!-- ── Links ── -->
      <div class="back-row">
        <RouterLink to="/login" class="back-link" id="link-to-login">
          ¿Ya tienes cuenta? Inicia sesión
        </RouterLink>
      </div>
      <div class="back-row">
        <RouterLink to="/" class="back-link" id="link-back-home">
          ← Volver al inicio
        </RouterLink>
      </div>

    </div><!-- /.login-card -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import api from '../api/axios.js'

const router = useRouter()
const auth   = useAuthStore()

const name       = ref('')
const email      = ref('')
const password   = ref('')
const categoryId = ref(null)
const categories = ref([])
const loading    = ref(false)
const error      = ref('')

// ── Load categories for the selector (public endpoint) ────────────────
onMounted(async () => {
  try {
    const res = await api.get('/categories')
    categories.value = res.data
  } catch (e) {
    // Non-blocking: registrar sin categoría sigue siendo válido
    categories.value = []
  }
})

// ── Register handler ─────────────────────────────────────────────────
async function handleRegister() {
  error.value = ''

  if (!name.value.trim() || !email.value.trim() || !password.value) {
    error.value = 'Completa nombre, email y contraseña.'
    return
  }

  loading.value = true
  try {
    await auth.register({
      name: name.value.trim(),
      email: email.value.trim(),
      password: password.value,
      categoryId: categoryId.value,
    })
    // Registro exitoso → sesión iniciada, ir al dashboard
    router.push('/dashboard')
  } catch (e) {
    error.value = e.response?.data?.error || 'No se pudo crear la cuenta. Intenta de nuevo.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ── Page shell ──────────────────────────────────────────────────────
   No custom background — App.vue aurora layer shows through.
   Top padding clears the fixed AppHeader.
   ────────────────────────────────────────────────────────────────── */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: calc(var(--header-height) + 2rem) 1rem 4rem;
  box-sizing: border-box;
}

/* ── Register card — identical panel style to Login ── */
.login-card {
  width: 100%;
  max-width: 440px;
  padding: 2.2rem 2rem 1.8rem;
  background: var(--bg-panel);
  border: 1px solid var(--border-subtle);
  box-shadow:
    0 0 60px rgba(0, 0, 0, 0.6),
    0 0 20px rgba(200, 155, 60, 0.06),
    inset 0 0 40px rgba(200, 155, 60, 0.02);
  position: relative;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, var(--gold-rich) 45%, var(--gold-rich) 55%, transparent 100%);
}

/* ── Header block ──────────────────────────────────────────────────── */
.login-header {
  align-items: center;
  margin-bottom: 1.5rem;
}

.card-crest {
  color: var(--gold-rich);
  filter: drop-shadow(0 0 8px rgba(200, 155, 60, 0.4));
  margin-bottom: 0.4rem;
}

.login-title {
  font-size: 1.5rem !important;
  letter-spacing: 0.08em !important;
  margin-bottom: 0;
}

.login-meta {
  justify-content: center !important;
  width: 100%;
}

/* ── Error alert ─────────────────────────────────────────────────── */
.login-alert {
  background: rgba(231, 76, 60, 0.1);
  border: 1px solid rgba(231, 76, 60, 0.4);
  color: #e74c3c;
  font-family: 'Inter', sans-serif;
  font-size: 0.78rem;
  padding: 0.6rem 0.85rem;
  margin-bottom: 1rem;
  letter-spacing: 0.03em;
}

.slide-up-enter-active, .slide-up-leave-active { transition: all 0.22s ease; }
.slide-up-enter-from, .slide-up-leave-to       { opacity: 0; transform: translateY(-5px); }

/* ── Form fields ────────────────────────────────────────────────── */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 1rem;
}

.field-label {
  font-family: 'Cinzel', serif;
  font-size: 0.6rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-muted);
}

.field-input {
  width: 100%;
  padding: 0.65rem 0.85rem;
  background: rgba(4, 10, 20, 0.9);
  border: 1px solid var(--border-subtle);
  color: var(--text-title);
  font-family: 'Inter', sans-serif;
  font-size: 0.88rem;
  outline: none;
  transition: border-color var(--t), box-shadow var(--t);
  box-sizing: border-box;
}

.field-input::placeholder {
  color: var(--text-muted);
  opacity: 0.55;
}

.field-input:focus {
  border-color: var(--gold-rich);
  box-shadow: 0 0 0 2px rgba(200, 155, 60, 0.15), var(--glow-gold);
}

/* ── Primary button ─────────────────────────────────────────────── */
.btn-primary {
  width: 100%;
  padding: 0.75rem 1rem;
  margin-top: 0.4rem;
  background: linear-gradient(135deg, var(--lol-gold-dark) 0%, var(--gold-rich) 100%);
  border: 1px solid var(--gold-rich);
  color: var(--bg-base);
  font-family: 'Cinzel', serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  transition: filter var(--t), box-shadow var(--t);
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.14);
  box-shadow: var(--glow-gold);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-spinner {
  display: inline-block;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Links ───────────────────────────────────────────────────────── */
.back-row {
  display: flex;
  justify-content: center;
  margin-top: 1.1rem;
}

.back-link {
  font-family: 'Inter', sans-serif;
  font-size: 0.7rem;
  color: var(--text-muted);
  text-decoration: none;
  letter-spacing: 0.06em;
  transition: color var(--t);
}
.back-link:hover { color: var(--gold-mid); }

/* ── Responsive ─────────────────────────────────────────────────── */
@media (max-width: 480px) {
  .login-card { padding: 1.6rem 1.2rem 1.4rem; }
}
</style>