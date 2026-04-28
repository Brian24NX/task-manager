<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { login } from '../auth.js'

const emit = defineEmits(['success'])

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const showPassword = ref(false)
const usernameInput = ref(null)

onMounted(() => {
  nextTick(() => usernameInput.value?.focus())
  const apiBase = import.meta.env.VITE_API_URL || ''
  fetch(`${apiBase}/api/health`, { method: 'GET', cache: 'no-store' }).catch(() => {})
})

async function handleSubmit() {
  if (loading.value) return
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = 'Please enter both username and password'
    return
  }
  loading.value = true
  try {
    const result = await login(username.value.trim(), password.value)
    emit('success', result)
  } catch (err) {
    error.value = err.message || 'Login failed'
    password.value = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">
          <span class="material-symbols-rounded">task_alt</span>
        </div>
        <h1>Task Manager</h1>
        <p class="login-subtitle">Sign in to continue</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit" autocomplete="on">
        <div class="login-field">
          <label for="login-username">
            <span class="material-symbols-rounded">person</span>
            Username
          </label>
          <input
            id="login-username"
            ref="usernameInput"
            v-model="username"
            type="text"
            autocomplete="username"
            autocapitalize="off"
            autocorrect="off"
            spellcheck="false"
            placeholder="Your username"
            :disabled="loading"
          />
        </div>

        <div class="login-field">
          <label for="login-password">
            <span class="material-symbols-rounded">lock</span>
            Password
          </label>
          <div class="login-password-wrap">
            <input
              id="login-password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="Your password"
              :disabled="loading"
            />
            <button
              type="button"
              class="login-eye"
              :title="showPassword ? 'Hide password' : 'Show password'"
              @click="showPassword = !showPassword"
              tabindex="-1"
            >
              <span class="material-symbols-rounded">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </div>
        </div>

        <div v-if="error" class="login-error">
          <span class="material-symbols-rounded">error</span>
          {{ error }}
        </div>

        <button type="submit" class="login-submit" :disabled="loading">
          <span v-if="loading" class="login-spinner"></span>
          <span v-else class="material-symbols-rounded">login</span>
          <span>{{ loading ? 'Signing in…' : 'Sign In' }}</span>
        </button>
      </form>

      <p class="login-footer">Stay organized, stay productive.</p>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  z-index: 1;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 32px 32px;
  border-radius: 24px;
  background: var(--surface-3, rgba(255, 255, 255, 0.95));
  border: 1px solid var(--border-2, rgba(100, 116, 139, 0.16));
  box-shadow:
    0 30px 60px -20px rgba(14, 165, 233, 0.25),
    0 10px 30px -15px rgba(34, 197, 94, 0.2);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  animation: login-enter 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes login-enter {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.login-brand {
  text-align: center;
  margin-bottom: 28px;
}

.login-logo {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--sky-400, #38bdf8), var(--accent-500, #22c55e));
  box-shadow: 0 12px 30px -10px var(--sky-glow, rgba(14, 165, 233, 0.35));
}
.login-logo .material-symbols-rounded {
  color: #fff;
  font-size: 32px;
}

.login-brand h1 {
  margin: 0 0 4px;
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--text-1, #0f172a);
  letter-spacing: -0.02em;
}

.login-subtitle {
  margin: 0;
  color: var(--text-3, #64748b);
  font-size: 0.95rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.login-field label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-2, #334155);
}
.login-field label .material-symbols-rounded {
  font-size: 16px;
  color: var(--sky-500, #0ea5e9);
}

.login-field input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--border-2, rgba(100, 116, 139, 0.22));
  background: var(--surface-2, rgba(255, 255, 255, 0.85));
  color: var(--text-1, #0f172a);
  font-size: 1rem;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
  outline: none;
  box-sizing: border-box;
}
.login-field input:focus {
  border-color: var(--sky-500, #0ea5e9);
  box-shadow: 0 0 0 4px var(--sky-glow, rgba(14, 165, 233, 0.25));
  background: var(--surface-3, #fff);
}
.login-field input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-password-wrap {
  position: relative;
}
.login-password-wrap input {
  padding-right: 44px;
}
.login-eye {
  position: absolute;
  top: 50%;
  right: 6px;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  padding: 6px;
  cursor: pointer;
  border-radius: 8px;
  color: var(--text-3, #64748b);
  display: grid;
  place-items: center;
  transition: background 0.15s, color 0.15s;
}
.login-eye:hover {
  background: var(--surface-1, rgba(148, 163, 184, 0.12));
  color: var(--text-1, #0f172a);
}
.login-eye .material-symbols-rounded {
  font-size: 20px;
}

.login-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  color: #b91c1c;
  font-size: 0.9rem;
  animation: shake 0.35s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}
.login-error .material-symbols-rounded { font-size: 18px; }

@keyframes shake {
  10%, 90% { transform: translateX(-1px); }
  20%, 80% { transform: translateX(2px); }
  30%, 50%, 70% { transform: translateX(-4px); }
  40%, 60% { transform: translateX(4px); }
}

.login-submit {
  margin-top: 8px;
  padding: 13px 16px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--sky-500, #0ea5e9), var(--accent-600, #16a34a));
  box-shadow: 0 10px 24px -10px var(--sky-glow, rgba(14, 165, 233, 0.5));
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: transform 0.1s, box-shadow 0.2s, filter 0.2s;
}
.login-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow: 0 14px 28px -10px var(--sky-glow, rgba(14, 165, 233, 0.6));
}
.login-submit:active:not(:disabled) { transform: translateY(0); }
.login-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: login-spin 0.7s linear infinite;
}
@keyframes login-spin { to { transform: rotate(360deg); } }

.login-footer {
  margin: 24px 0 0;
  text-align: center;
  font-size: 0.8rem;
  color: var(--text-4, #94a3b8);
}

[data-theme='dark'] .login-card {
  background: var(--surface-3, rgba(20, 22, 38, 0.85));
  border-color: var(--border-2, rgba(148, 163, 184, 0.18));
  box-shadow:
    0 30px 60px -20px rgba(0, 0, 0, 0.6),
    0 10px 30px -15px rgba(125, 211, 252, 0.18);
}
[data-theme='dark'] .login-error {
  background: rgba(239, 68, 68, 0.15);
  color: #fecaca;
}
</style>
