<script setup lang="ts">
import { BadgeCheck, ClipboardList, MessageSquare, Workflow } from '@lucide/vue'
import { PRODUCT_NAME, PRODUCT_NAME_EN } from '@/config/product'

withDefaults(
  defineProps<{
    eyebrow: string
    title: string
    description: string
    wide?: boolean
  }>(),
  { wide: false },
)
</script>

<template>
  <main class="auth-layout">
    <section class="auth-layout__brand" aria-label="产品信息">
      <div class="auth-brand-lockup">
        <span class="auth-brand-mark" aria-hidden="true">
          <Workflow :size="23" :stroke-width="2" />
        </span>
        <span>
          <strong>{{ PRODUCT_NAME }}</strong>
          <small>{{ PRODUCT_NAME_EN }}</small>
        </span>
      </div>

      <div class="auth-brand-message">
        <span>统一需求服务入口</span>
        <h2>从需求提出到成果验收，每一步都有明确记录。</h2>
        <p>面向需求申请人与技术服务团队的协作工作台。</p>
        <ol class="auth-flow" aria-label="需求服务流程">
          <li>
            <span class="auth-flow__node"><ClipboardList :size="19" :stroke-width="1.8" /></span>
            <span><small>01</small><strong>需求发起</strong></span>
          </li>
          <li>
            <span class="auth-flow__node auth-flow__node--build">
              <MessageSquare :size="19" :stroke-width="1.8" />
            </span>
            <span><small>02</small><strong>协同处理</strong></span>
          </li>
          <li>
            <span class="auth-flow__node auth-flow__node--done">
              <BadgeCheck :size="19" :stroke-width="1.8" />
            </span>
            <span><small>03</small><strong>成果验收</strong></span>
          </li>
        </ol>
      </div>

      <div class="auth-brand-footer">
        <span>需求服务入口</span>
        <span>2026</span>
      </div>
    </section>

    <section class="auth-layout__content">
      <div class="auth-form-shell" :class="{ 'auth-form-shell--wide': wide }">
        <header>
          <span>{{ eyebrow }}</span>
          <h1>{{ title }}</h1>
          <p>{{ description }}</p>
        </header>
        <slot />
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-layout {
  display: grid;
  min-height: 100dvh;
  grid-template-columns: minmax(340px, 0.9fr) minmax(520px, 1.35fr);
  background: var(--color-surface);
}

.auth-layout__brand {
  position: relative;
  display: flex;
  min-height: 100dvh;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(32px, 5vw, 64px);
  overflow: hidden;
  color: #f8fbff;
  background: #163d68;
}

.auth-layout__brand > * {
  position: relative;
  z-index: 1;
}

.auth-brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-brand-mark {
  display: inline-grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: #1d4ed8;
  background: #ffffff;
  border-radius: var(--radius-md);
}

.auth-brand-lockup > span:last-child {
  display: grid;
  gap: 1px;
}

.auth-brand-lockup strong {
  font-size: 16px;
  font-weight: 650;
}

.auth-brand-lockup small {
  color: #bfdbfe;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0;
}

.auth-brand-message {
  max-width: 520px;
  animation: auth-copy-enter 420ms var(--ease-standard) both;
}

.auth-brand-message > span {
  display: block;
  margin-bottom: 16px;
  color: #a5d8ff;
  font-size: 12px;
  font-weight: 650;
}

.auth-brand-message h2 {
  margin: 0;
  max-width: 12em;
  font-size: clamp(38px, 3.4vw, 46px);
  font-weight: 650;
  line-height: 1.36;
  text-wrap: balance;
}

.auth-brand-message p {
  max-width: 30em;
  margin: 20px 0 0;
  color: #d9e9f7;
  font-size: 16px;
  line-height: 1.75;
}

.auth-brand-footer {
  display: flex;
  justify-content: space-between;
  color: #93c5fd;
  font-size: 12px;
}

.auth-flow {
  display: grid;
  max-width: 460px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin: 36px 0 0;
  padding: 24px 0 0;
  list-style: none;
  border-top: 1px solid rgb(255 255 255 / 18%);
}

.auth-flow li {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 9px;
}

.auth-flow__node {
  display: inline-grid;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  place-items: center;
  color: #ffffff;
  background: rgb(255 255 255 / 9%);
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: var(--radius-md);
  animation: auth-node-enter 360ms var(--ease-standard) both;
}

.auth-flow__node--build {
  color: #cffafe;
  background: rgb(6 182 212 / 18%);
  animation-delay: 80ms;
}

.auth-flow__node--done {
  color: #ffedd5;
  background: rgb(234 88 12 / 18%);
  animation-delay: 160ms;
}

.auth-flow li > span:last-child {
  display: grid;
  min-width: 0;
}

.auth-flow small {
  color: #8fc5eb;
  font-size: 10px;
  line-height: 1.3;
}

.auth-flow strong {
  color: #f8fbff;
  font-size: 13px;
  font-weight: 620;
  line-height: 1.5;
  white-space: nowrap;
}

@keyframes auth-copy-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
}

@keyframes auth-node-enter {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
}

.auth-layout__content {
  display: grid;
  min-width: 0;
  place-items: center;
  padding: 48px clamp(28px, 7vw, 96px);
  background: var(--color-surface);
}

.auth-form-shell {
  width: min(420px, 100%);
}

.auth-form-shell--wide {
  width: min(700px, 100%);
}

.auth-form-shell header {
  margin-bottom: 32px;
}

.auth-form-shell header > span {
  display: block;
  margin-bottom: 8px;
  color: var(--color-primary-strong);
  font-size: 12px;
  font-weight: 700;
}

.auth-form-shell h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 34px;
  font-weight: 680;
  line-height: 1.2;
}

.auth-form-shell p {
  max-width: 32em;
  margin: 11px 0 0;
  color: var(--color-text-secondary);
  font-size: 15px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-layout__brand {
    min-height: 250px;
    padding: 28px;
  }

  .auth-brand-message h2 {
    max-width: 18em;
    font-size: 28px;
    line-height: 1.45;
  }

  .auth-form-shell h1 {
    font-size: 32px;
  }

  .auth-brand-message p,
  .auth-flow,
  .auth-brand-footer {
    display: none;
  }

  .auth-brand-message > span {
    margin-bottom: 8px;
  }

  .auth-layout__content {
    place-items: start center;
    padding: 40px 24px 56px;
  }
}

@media (max-width: 520px) {
  .auth-layout__brand {
    min-height: 190px;
    padding: 22px 20px;
  }

  .auth-brand-message h2 {
    max-width: 14em;
    font-size: 24px;
  }

  .auth-brand-message > span {
    display: none;
  }

  .auth-layout__content {
    padding: 32px 20px 48px;
  }

  .auth-form-shell h1 {
    font-size: 30px;
  }

  .auth-form-shell p {
    font-size: 16px;
  }
}
</style>
