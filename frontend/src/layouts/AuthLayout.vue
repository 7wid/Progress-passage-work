<script setup lang="ts">
import { BadgeCheck, Braces, ClipboardList, Code2 } from '@lucide/vue'

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
          <Braces :size="23" :stroke-width="2" />
        </span>
        <span>
          <strong>技术需求管理</strong>
          <small>TECH REQUESTS</small>
        </span>
      </div>

      <div class="auth-brand-message">
        <span>REQUEST · BUILD · DELIVER</span>
        <h2>让每一次需求流转<br />清晰、有序、可追踪</h2>
        <p>计算机技术组协作平台</p>
        <div class="auth-flow" aria-hidden="true">
          <span class="auth-flow__node auth-flow__node--request">
            <ClipboardList :size="21" :stroke-width="1.8" />
          </span>
          <span class="auth-flow__line" />
          <span class="auth-flow__node auth-flow__node--build">
            <Code2 :size="21" :stroke-width="1.8" />
          </span>
          <span class="auth-flow__line" />
          <span class="auth-flow__node auth-flow__node--done">
            <BadgeCheck :size="21" :stroke-width="1.8" />
          </span>
        </div>
      </div>

      <div class="auth-brand-footer">
        <span>内部工作空间</span>
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
  background: #1e40af;
}

.auth-layout__brand::before,
.auth-layout__brand::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.auth-layout__brand::before {
  top: 24%;
  right: -190px;
  width: 360px;
  height: 360px;
  border: 1px solid rgb(255 255 255 / 11%);
  transform: rotate(18deg);
}

.auth-layout__brand::after {
  right: 15%;
  bottom: -120px;
  width: 120px;
  height: 360px;
  background: rgb(59 130 246 / 20%);
  transform: rotate(32deg);
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
  max-width: 560px;
  animation: auth-copy-enter 420ms var(--ease-standard) both;
}

.auth-brand-message > span {
  display: block;
  margin-bottom: 18px;
  color: #93c5fd;
  font-size: 11px;
  font-weight: 650;
}

.auth-brand-message h2 {
  margin: 0;
  font-size: 58px;
  font-weight: 650;
  line-height: 1.18;
}

.auth-brand-message p {
  margin: 18px 0 0;
  color: #dbeafe;
  font-size: 15px;
}

.auth-brand-footer {
  display: flex;
  justify-content: space-between;
  color: #93c5fd;
  font-size: 12px;
}

.auth-flow {
  display: flex;
  max-width: 330px;
  align-items: center;
  margin-top: 34px;
}

.auth-flow__node {
  display: inline-grid;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  place-items: center;
  color: #ffffff;
  background: rgb(255 255 255 / 10%);
  border: 1px solid rgb(255 255 255 / 20%);
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

.auth-flow__line {
  height: 1px;
  flex: 1;
  background: rgb(191 219 254 / 45%);
  transform-origin: left;
  animation: auth-line-enter 420ms var(--ease-standard) both;
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

@keyframes auth-line-enter {
  from {
    opacity: 0;
    transform: scaleX(0);
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
  margin-bottom: 30px;
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
  font-size: 38px;
  font-weight: 680;
  line-height: 1.2;
}

.auth-form-shell p {
  margin: 10px 0 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

@media (max-width: 900px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .auth-layout__brand {
    min-height: 220px;
    padding: 28px;
  }

  .auth-brand-message h2 {
    font-size: 30px;
  }

  .auth-form-shell h1 {
    font-size: 32px;
  }

  .auth-brand-message h2 br,
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
    min-height: 160px;
    padding: 22px 20px;
  }

  .auth-brand-message h2 {
    font-size: 23px;
  }

  .auth-brand-message > span {
    display: none;
  }

  .auth-layout__content {
    padding: 32px 20px 48px;
  }
}
</style>
