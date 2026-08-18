<script setup lang="ts">
import { ref } from 'vue'
import { register } from '../services/authApi'

const emit = defineEmits<{
  registered: []
  navigate: [path: string]
}>()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMessage = ref('')
const submitting = ref(false)

const submit = async () => {
  if (submitting.value) return
  errorMessage.value = ''
  if (!username.value.trim() || !password.value || !confirmPassword.value) {
    errorMessage.value = '请完整填写注册信息。'
    return
  }
  if (password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致。'
    return
  }
  if (password.value.length < 6) {
    errorMessage.value = '密码至少需要 6 个字符。'
    return
  }

  submitting.value = true
  try {
    await register(username.value.trim(), password.value)
    emit('registered')
  } catch (error) {
    // Preserve the backend message, including duplicate username errors.
    errorMessage.value = error instanceof Error ? error.message : '注册失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="register-page page-container">
    <form class="register-card" @submit.prevent="submit">
      <div class="card-heading">
        <span class="step-label">JOIN US</span>
        <h1>创建你的灵感账号</h1>
        <p>注册后即可进入 AI 文案工作台</p>
      </div>

      <label>
        <span>用户名</span>
        <input v-model="username" type="text" autocomplete="username" placeholder="3-50 个字符" maxlength="50" />
      </label>
      <label>
        <span>密码</span>
        <input v-model="password" type="password" autocomplete="new-password" placeholder="至少 6 个字符" />
      </label>
      <label>
        <span>确认密码</span>
        <input v-model="confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入密码" />
      </label>

      <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
      <button class="primary-button submit-button" type="submit" :disabled="submitting">
        {{ submitting ? '正在注册…' : '创建账号' }}
      </button>
      <p class="form-switch">
        已经有账号？
        <a href="/login" @click.prevent="emit('navigate', '/login')">返回登录</a>
      </p>
    </form>

    <section class="register-intro">
      <p class="eyebrow">CREATE WITH AI</p>
      <h2>把每一次看见<br />变成<em>值得分享</em>的内容</h2>
      <div class="benefits">
        <div><span>01</span><p><strong>视觉理解</strong><small>AI 看懂图片中的真实内容</small></p></div>
        <div><span>02</span><p><strong>多种风格</strong><small>为不同表达场景生成文案</small></p></div>
        <div><span>03</span><p><strong>历史记录</strong><small>随时回看过去的创作灵感</small></p></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.register-page { min-height: calc(100vh - 72px); padding-top: 72px; padding-bottom: 80px; display: grid; grid-template-columns: 440px 1fr; align-items: center; gap: 100px; }
.register-card { padding: 38px; border: 1px solid var(--line); border-radius: 28px; background: var(--card); box-shadow: var(--shadow); }
.step-label { display: inline-flex; padding: 6px 10px; border-radius: 999px; color: var(--red); font-size: 10px; font-weight: 850; letter-spacing: .12em; background: var(--soft); }
h1 { margin: 18px 0 8px; color: var(--ink); font-size: 30px; letter-spacing: -.04em; }
.card-heading > p { margin: 0 0 28px; color: var(--muted); font-size: 13px; }
label { margin-top: 17px; display: grid; gap: 9px; }
label > span { color: var(--ink); font-size: 13px; font-weight: 700; }
input { width: 100%; height: 50px; padding: 0 16px; border: 1px solid var(--line); border-radius: 14px; color: var(--ink); outline: 0; background: #fff; transition: border-color .2s, box-shadow .2s; }
input:focus { border-color: rgba(255,36,66,.55); box-shadow: 0 0 0 4px rgba(255,36,66,.08); }
.form-error { margin: 16px 0 0; padding: 11px 13px; border-radius: 12px; color: #b4233a; font-size: 12px; background: #fff0f2; }
.submit-button { width: 100%; margin-top: 24px; }
.form-switch { margin: 20px 0 0; color: var(--muted); font-size: 13px; text-align: center; }
.form-switch a { color: var(--red); font-weight: 700; text-decoration: none; }
.eyebrow { margin: 0 0 18px; color: var(--red); font-size: 12px; font-weight: 850; letter-spacing: .2em; }
.register-intro h2 { margin: 0; color: var(--ink); font-size: clamp(45px, 5.5vw, 68px); line-height: 1.12; letter-spacing: -.055em; }
.register-intro h2 em { color: var(--red); font-style: normal; }
.benefits { margin-top: 42px; display: grid; gap: 13px; }
.benefits > div { max-width: 500px; padding: 17px 20px; display: flex; align-items: center; gap: 18px; border: 1px solid #f1dfdc; border-radius: 18px; background: rgba(255,255,255,.7); }
.benefits span { color: var(--red); font-size: 11px; font-weight: 850; }
.benefits p, .benefits strong, .benefits small { display: block; margin: 0; }
.benefits strong { font-size: 14px; }
.benefits small { margin-top: 4px; color: var(--muted); font-size: 11px; }
@media (max-width: 850px) { .register-page { grid-template-columns: 1fr; gap: 52px; } .register-card { width: min(440px, 100%); margin: 0 auto; } .register-intro { grid-row: 1; text-align: center; } .benefits > div { margin: 0 auto; text-align: left; } }
@media (max-width: 520px) { .register-page { padding-top: 48px; } .register-card { padding: 28px 22px; } .register-intro h2 { font-size: 43px; } }
</style>
