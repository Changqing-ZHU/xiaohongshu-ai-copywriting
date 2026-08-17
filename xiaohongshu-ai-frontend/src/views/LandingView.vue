<script setup lang="ts">
const emit = defineEmits<{ start: [] }>()

const features = [
  { index: '01', title: 'AI 图片理解', description: '识别画面主题、氛围与视觉细节，让文案真正贴合图片内容。', symbol: '◉' },
  { index: '02', title: '智能生成小红书文案', description: '一次生成吸睛标题、自然正文与相关话题标签，快速完成创作。', symbol: '✦' },
  { index: '03', title: '支持图片 URL 输入', description: '除了本地上传，也可以粘贴公网图片链接，灵感获取更灵活。', symbol: '↗' },
  { index: '04', title: '历史生成记录管理', description: '集中查看过去生成的内容，随时回顾标题、正文与标签。', symbol: '◎' },
]

const faqs = [
  { question: '支持哪些图片格式？', answer: '支持 JPEG、PNG、WebP。' },
  { question: '是否支持图片 URL？', answer: '支持公网图片 URL 输入。' },
  { question: '生成失败怎么办？', answer: '系统会返回错误提示，可重新生成。' },
]
</script>

<template>
  <div class="landing-page">
    <section class="hero page-container">
      <div class="hero-copy">
        <p class="eyebrow">AI CONTENT STUDIO</p>
        <h1>红薯灵感<span>AI 小红书文案生成助手</span></h1>
        <p class="hero-description">
          利用 AI 视觉理解能力，根据图片自动生成小红书风格文案。
          从看懂画面到写好内容，让每一份灵感更快被表达。
        </p>
        <div class="hero-actions">
          <button class="primary-button hero-button" type="button" @click="emit('start')">
            立即体验 <span aria-hidden="true">→</span>
          </button>
          <span class="hero-note">上传图片 · 获取标题、正文与标签</span>
        </div>
      </div>

      <div class="hero-showcase" aria-label="AI 文案生成流程示意">
        <div class="showcase-glow"></div>
        <div class="visual-card image-card">
          <div class="image-scene">
            <span class="sun"></span>
            <span class="hill hill-back"></span>
            <span class="hill hill-front"></span>
          </div>
          <div class="image-meta"><span>今日灵感.jpg</span><span class="status-dot">已识别</span></div>
        </div>
        <div class="ai-node"><span>AI</span><small>视觉理解</small></div>
        <div class="visual-card copy-card">
          <span class="card-label">生成结果</span>
          <strong>把春日装进今天的心情里</strong>
          <p>阳光、微风，还有刚刚好的松弛感。记录下这一刻的小确幸……</p>
          <div class="tags"><span>#今日份美好</span><span>#生活灵感</span></div>
        </div>
      </div>
    </section>

    <section class="features-section page-container">
      <div class="section-heading">
        <div>
          <p class="eyebrow">WHY REDNOTE INSPIRE</p>
          <h2>从图片到好文案，只需一个灵感瞬间</h2>
        </div>
        <p>围绕真实创作流程打造，让内容生成更自然、更高效。</p>
      </div>
      <div class="feature-grid">
        <article v-for="feature in features" :key="feature.index" class="feature-card">
          <div class="feature-topline">
            <span class="feature-symbol">{{ feature.symbol }}</span>
            <span class="feature-index">{{ feature.index }}</span>
          </div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </article>
      </div>
    </section>

    <section class="faq-section page-container">
      <div class="faq-intro">
        <p class="eyebrow">FAQ</p>
        <h2>开始创作前，你可能想知道</h2>
        <p>简单上传，安心生成。常见问题都在这里。</p>
      </div>
      <div class="faq-list">
        <article v-for="(faq, index) in faqs" :key="faq.question" class="faq-card">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
          <div><h3>{{ faq.question }}</h3><p>{{ faq.answer }}</p></div>
        </article>
      </div>
    </section>

    <section class="bottom-cta page-container">
      <div>
        <p class="eyebrow">YOUR NEXT INSPIRATION</p>
        <h2>下一篇令人心动的小红书文案，从这里开始</h2>
      </div>
      <button class="primary-button" type="button" @click="emit('start')">开始生成</button>
    </section>
  </div>
</template>

<style scoped>
.landing-page { overflow: hidden; padding-bottom: 72px; }
.hero { min-height: 650px; padding-top: 92px; padding-bottom: 86px; display: grid; grid-template-columns: 1.05fr .95fr; align-items: center; gap: 74px; }
.eyebrow { margin: 0 0 18px; color: var(--red); font-size: 12px; font-weight: 800; letter-spacing: .2em; }
.hero h1 { max-width: 650px; margin: 0; color: var(--ink); font-size: clamp(48px, 6vw, 78px); line-height: 1.04; letter-spacing: -.065em; }
.hero h1 span { display: block; margin-top: 14px; color: var(--red); font-size: .65em; letter-spacing: -.045em; }
.hero-description { max-width: 580px; margin: 28px 0 0; color: var(--muted); font-size: 18px; line-height: 1.85; }
.hero-actions { margin-top: 38px; display: flex; align-items: center; gap: 20px; }
.hero-button { min-width: 154px; display: inline-flex; align-items: center; justify-content: center; gap: 14px; }
.hero-note { color: #9a8e91; font-size: 13px; }
.hero-showcase { position: relative; min-height: 460px; }
.showcase-glow { position: absolute; inset: 50px 35px 40px; border-radius: 48%; background: rgba(255, 133, 127, .2); filter: blur(34px); }
.visual-card { position: absolute; z-index: 1; border: 1px solid rgba(238, 228, 226, .9); border-radius: 26px; background: rgba(255, 255, 255, .94); box-shadow: var(--shadow); }
.image-card { top: 0; left: 0; width: 62%; padding: 14px; transform: rotate(-4deg); }
.image-scene { position: relative; height: 245px; overflow: hidden; border-radius: 18px; background: linear-gradient(165deg, #ffd8d0 0%, #fff0d7 46%, #ef9b85 47%, #c95c59 100%); }
.sun { position: absolute; top: 35px; right: 40px; width: 54px; height: 54px; border-radius: 50%; background: #fff7dd; box-shadow: 0 0 34px rgba(255, 247, 221, .9); }
.hill { position: absolute; width: 125%; height: 160px; left: -12%; bottom: -75px; border-radius: 50% 50% 0 0; transform: rotate(5deg); }
.hill-back { background: #db7d70; bottom: -50px; }
.hill-front { background: #923f47; left: 12%; bottom: -95px; }
.image-meta { padding: 14px 4px 2px; display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; }
.status-dot { color: var(--red); font-weight: 700; }
.ai-node { position: absolute; z-index: 3; top: 195px; left: 48%; width: 86px; height: 86px; display: grid; place-content: center; text-align: center; border: 6px solid #fff9f7; border-radius: 50%; color: #fff; background: linear-gradient(135deg, var(--red), #ff7567); box-shadow: 0 18px 36px rgba(255, 36, 66, .3); }
.ai-node span { font-size: 21px; font-weight: 900; }
.ai-node small { margin-top: 2px; font-size: 9px; }
.copy-card { right: 0; bottom: 0; width: 60%; padding: 28px; transform: rotate(3deg); }
.card-label { color: var(--red); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.copy-card strong { display: block; margin-top: 12px; color: var(--ink); font-size: 20px; line-height: 1.45; }
.copy-card p { margin: 12px 0 18px; color: var(--muted); font-size: 12px; line-height: 1.75; }
.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.tags span { padding: 6px 9px; border-radius: 999px; color: #a54e59; font-size: 10px; background: var(--soft); }
.features-section, .faq-section { padding-top: 92px; padding-bottom: 92px; }
.features-section { position: relative; }
.features-section::before { content: ''; position: absolute; z-index: -1; inset: 20px -8vw; border-radius: 48px; background: rgba(255, 255, 255, .5); }
.section-heading { margin-bottom: 36px; display: flex; align-items: end; justify-content: space-between; gap: 40px; }
.section-heading h2, .faq-intro h2, .bottom-cta h2 { margin: 0; color: var(--ink); font-size: clamp(30px, 4vw, 44px); line-height: 1.25; letter-spacing: -.045em; }
.section-heading > p { max-width: 310px; margin: 0 0 6px; color: var(--muted); font-size: 14px; line-height: 1.8; }
.feature-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.feature-card { min-height: 265px; padding: 26px; border: 1px solid var(--line); border-radius: 24px; background: var(--card); transition: transform .25s ease, box-shadow .25s ease; }
.feature-card:hover { transform: translateY(-6px); box-shadow: var(--shadow); }
.feature-topline { display: flex; align-items: center; justify-content: space-between; }
.feature-symbol { width: 46px; height: 46px; display: grid; place-items: center; border-radius: 15px; color: var(--red); font-size: 21px; background: var(--soft); }
.feature-index { color: #c8bfc1; font-size: 12px; font-weight: 800; }
.feature-card h3 { margin: 38px 0 12px; font-size: 19px; }
.feature-card p { margin: 0; color: var(--muted); font-size: 14px; line-height: 1.75; }
.faq-section { display: grid; grid-template-columns: .85fr 1.15fr; gap: 90px; }
.faq-intro > p:last-child { margin: 22px 0 0; color: var(--muted); line-height: 1.75; }
.faq-list { display: grid; gap: 12px; }
.faq-card { padding: 24px; display: grid; grid-template-columns: 38px 1fr; gap: 16px; border: 1px solid var(--line); border-radius: 20px; background: rgba(255, 255, 255, .82); }
.faq-card > span { color: var(--red); font-size: 12px; font-weight: 800; }
.faq-card h3 { margin: 0; color: var(--ink); font-size: 16px; }
.faq-card p { margin: 9px 0 0; color: var(--muted); font-size: 14px; }
.bottom-cta { padding: 46px 52px; display: flex; align-items: center; justify-content: space-between; gap: 40px; border-radius: 30px; color: #fff; background: linear-gradient(125deg, #2d2024, #4b292e); box-shadow: 0 24px 60px rgba(62, 31, 37, .2); }
.bottom-cta .eyebrow { color: #ff9aa8; }
.bottom-cta h2 { max-width: 700px; color: #fff; font-size: clamp(28px, 3.5vw, 40px); }
.bottom-cta .primary-button { flex: 0 0 auto; }
@media (max-width: 920px) {
  .hero { grid-template-columns: 1fr; gap: 56px; padding-top: 70px; }
  .hero-copy { text-align: center; }
  .hero h1, .hero-description { margin-right: auto; margin-left: auto; }
  .hero-actions { justify-content: center; }
  .hero-showcase { width: min(570px, 100%); margin: 0 auto; }
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .faq-section { grid-template-columns: 1fr; gap: 38px; }
}
@media (max-width: 620px) {
  .landing-page { padding-bottom: 28px; }
  .hero { min-height: auto; padding-top: 56px; padding-bottom: 48px; }
  .hero h1 { font-size: clamp(43px, 15vw, 60px); }
  .hero h1 span { font-size: .5em; line-height: 1.3; }
  .hero-description { font-size: 15px; }
  .hero-actions { flex-direction: column; }
  .hero-showcase { min-height: 390px; }
  .image-card { width: 70%; }
  .image-scene { height: 190px; }
  .ai-node { top: 165px; left: 43%; width: 76px; height: 76px; }
  .copy-card { width: 68%; padding: 22px; }
  .section-heading { align-items: start; flex-direction: column; gap: 16px; }
  .feature-grid { grid-template-columns: 1fr; }
  .feature-card { min-height: 220px; }
  .features-section, .faq-section { padding-top: 64px; padding-bottom: 64px; }
  .bottom-cta { padding: 34px 26px; align-items: stretch; flex-direction: column; }
}
</style>
