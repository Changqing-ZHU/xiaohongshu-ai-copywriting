<script setup lang="ts">
import { ref } from 'vue'
import { login } from '../services/authApi'
import { saveAuthSession } from '../stores/authStore'

const emit = defineEmits<{
  authenticated: []
  navigate: [path: string]
}>()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const submitting = ref(false)

const submit = async () => {
  if (submitting.value) return
  errorMessage.value = ''
  if (!username.value.trim() || !password.value) {
    errorMessage.value = '请输入用户名和密码。'
    return
  }

  submitting.value = true
  try {
    const session = await login(username.value.trim(), password.value)
    saveAuthSession(session)
    emit('authenticated')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page page-container">
    <section class="auth-intro">
      <p class="eyebrow">WELCOME BACK</p>
      <h1>欢迎回到<br /><em>红薯灵感</em></h1>
      <p>登录后继续使用 AI 图片理解、文案生成与历史记录。</p>
      <div class="intro-card">
        <span>✦</span>
        <div><strong>让灵感继续发生</strong><small>你的创作工作台已经准备好了</small></div>
      </div>
    </section>

    <form class="auth-card" @submit.prevent="submit">
      <div class="card-heading">
        <span class="step-label">LOGIN</span>
        <h2>登录账号</h2>
        <p>输入账号信息进入文案生成工作台</p>
      </div>

      <label>
        <span>用户名</span>
        <input v-model="username" type="text" autocomplete="username" placeholder="请输入用户名" maxlength="50" />
      </label>
      <label>
        <span>密码</span>
        <input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" />
      </label>

      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button class="primary-button submit-button" type="submit" :disabled="submitting">
        {{ submitting ? '正在登录…' : '登录并开始创作' }}
      </button>
      <p class="form-switch">
        还没有账号？
        <a href="/register" @click.prevent="emit('navigate', '/register')">立即注册</a>
      </p>
    </form>
  </div>
</template>

<style scoped>
.auth-page { min-height: calc(100vh - 72px); padding-top: 84px; padding-bottom: 84px; display: grid; grid-template-columns: 1fr 440px; align-items: center; gap: 100px; }
.eyebrow { margin: 0 0 18px; color: var(--red); font-size: 12px; font-weight: 850; letter-spacing: .2em; }
h1 { margin: 0; color: var(--ink); font-size: clamp(48px, 6vw, 72px); line-height: 1.08; letter-spacing: -.06em; }
h1 em { color: var(--red); font-style: normal; }
.auth-intro > p:not(.eyebrow) { max-width: 500px; margin: 24px 0 0; color: var(--muted); font-size: 16px; line-height: 1.8; }
.intro-card { width: fit-content; margin-top: 38px; padding: 15px 18px; display: flex; align-items: center; gap: 13px; border: 1px solid #f1dfdc; border-radius: 18px; background: rgba(255,255,255,.72); }
.intro-card > span { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 12px; color: var(--red); background: var(--soft); }
.intro-card strong, .intro-card small { display: block; }
.intro-card strong { font-size: 13px; }
.intro-card small { margin-top: 4px; color: var(--muted); font-size: 11px; }
.auth-card { padding: 38px; border: 1px solid var(--line); border-radius: 28px; background: var(--card); box-shadow: var(--shadow); }
.card-heading { margin-bottom: 30px; }
.step-label { display: inline-flex; padding: 6px 10px; border-radius: 999px; color: var(--red); font-size: 10px; font-weight: 850; letter-spacing: .12em; background: var(--soft); }
h2 { margin: 18px 0 8px; color: var(--ink); font-size: 28px; }
.card-heading p { margin: 0; color: var(--muted); font-size: 13px; }
label { margin-top: 18px; display: grid; gap: 9px; }
label > span { color: var(--ink); font-size: 13px; font-weight: 700; }
input { width: 100%; height: 50px; padding: 0 16px; border: 1px solid var(--line); border-radius: 14px; color: var(--ink); outline: 0; background: #fff; transition: border-color .2s, box-shadow .2s; }
input:focus { border-color: rgba(255,36,66,.55); box-shadow: 0 0 0 4px rgba(255,36,66,.08); }
.form-error { margin: 16px 0 0; padding: 11px 13px; border-radius: 12px; color: #b4233a; font-size: 12px; background: #fff0f2; }
.submit-button { width: 100%; margin-top: 24px; }
.form-switch { margin: 20px 0 0; color: var(--muted); font-size: 13px; text-align: center; }
.form-switch a { color: var(--red); font-weight: 700; text-decoration: none; }
@media (max-width: 850px) { .auth-page { grid-template-columns: 1fr; gap: 46px; } .auth-intro { text-align: center; } .auth-intro > p, .intro-card { margin-right: auto; margin-left: auto; } .auth-card { width: min(440px, 100%); margin: 0 auto; } }
@media (max-width: 520px) { .auth-page { padding-top: 52px; } .auth-card { padding: 28px 22px; } h1 { font-size: 48px; } }
</style>
