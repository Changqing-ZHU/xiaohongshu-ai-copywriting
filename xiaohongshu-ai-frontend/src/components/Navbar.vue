<script setup lang="ts">
defineProps<{
  activePath: string
  authenticated: boolean
  username: string
  role: string
}>()

const emit = defineEmits<{
  navigate: [path: string]
  logout: []
}>()
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner">
      <a class="brand" href="/" aria-label="红薯灵感首页" @click.prevent="emit('navigate', '/')">
        <span class="brand-mark">薯</span>
        <span>红薯灵感</span>
      </a>
      <nav aria-label="主要导航">
        <a class="home-link" href="/" :class="{ active: activePath === '/' }" @click.prevent="emit('navigate', '/')">
          首页
        </a>
        <template v-if="authenticated">
          <a
            href="/workbench"
            :class="{ active: activePath === '/workbench' || activePath === '/result' }"
            @click.prevent="emit('navigate', '/workbench')"
          >
            文案生成
          </a>
          <a
            href="/history"
            :class="{ active: activePath === '/history' }"
            @click.prevent="emit('navigate', '/history')"
          >
            历史记录
          </a>
          <a
            v-if="role === 'ADMIN'"
            href="/admin"
            :class="{ active: activePath === '/admin' || activePath.startsWith('/admin/') }"
            @click.prevent="emit('navigate', '/admin')"
          >
            后台管理
          </a>
          <span class="user-chip" :title="username">
            <span class="user-avatar">{{ username.slice(0, 1).toUpperCase() }}</span>
            <span class="user-name">{{ username }}</span>
          </span>
          <button class="logout-button" type="button" @click="emit('logout')">退出</button>
        </template>
        <template v-else>
          <a href="/login" :class="{ active: activePath === '/login' }" @click.prevent="emit('navigate', '/login')">
            登录
          </a>
          <a class="register-link" href="/register" @click.prevent="emit('navigate', '/register')">
            注册
          </a>
        </template>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  border-bottom: 1px solid rgba(238, 228, 226, 0.9);
  background: rgba(255, 249, 247, 0.86);
  backdrop-filter: blur(18px);
}
.navbar-inner {
  width: min(1120px, calc(100% - 40px));
  height: 72px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
  font-size: 19px;
  font-weight: 800;
  text-decoration: none;
  letter-spacing: -0.02em;
}
.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, var(--red), #ff7b66);
  box-shadow: 0 8px 20px rgba(255, 36, 66, 0.22);
}
nav { display: flex; align-items: center; gap: 20px; }
nav a {
  position: relative;
  color: var(--muted);
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}
nav a.active { color: var(--red); }
nav a.active::after {
  content: "";
  position: absolute;
  right: 2px;
  bottom: -10px;
  left: 2px;
  height: 3px;
  border-radius: 999px;
  background: var(--red);
}
.register-link,
.logout-button {
  padding: 8px 13px;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}
.register-link {
  color: #fff;
  background: linear-gradient(135deg, var(--red), #ff596f);
}
.user-chip { max-width: 150px; padding: 5px 9px 5px 5px; display: inline-flex; align-items: center; gap: 7px; border-radius: 999px; color: var(--ink); font-size: 12px; font-weight: 700; background: #fff0f1; }
.user-avatar { display: grid; width: 25px; height: 25px; flex: 0 0 auto; place-items: center; border-radius: 50%; color: #fff; font-size: 10px; background: var(--red); }
.user-name { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.logout-button {
  color: var(--muted);
  background: transparent;
  border: 1px solid var(--line);
}
.logout-button:hover { color: var(--red); border-color: #ffc2ca; }
.register-link::after { display: none; }
.register-link:hover { box-shadow: 0 7px 16px rgba(255, 36, 66, .18); }
button:focus-visible { outline: 3px solid rgba(255, 36, 66, .2); outline-offset: 2px; }
@media (max-width: 850px) {
  nav { gap: 13px; }
  .brand > span:last-child { display: none; }
}
@media (max-width: 700px) {
  .navbar-inner { width: calc(100% - 24px); height: 64px; }
  .user-chip { max-width: 82px; }
}
@media (max-width: 460px) {
  .home-link { display: none; }
  nav { gap: 10px; }
  nav a { font-size: 13px; }
  .user-chip { max-width: 62px; }
  .logout-button { padding-right: 9px; padding-left: 9px; }
}
</style>
