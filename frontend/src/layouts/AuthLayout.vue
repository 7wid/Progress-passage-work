<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowLeft, BadgeCheck, ClipboardList, MessageSquare } from '@lucide/vue'
import ProductLogo from '@/components/common/ProductLogo.vue'
import { PRODUCT_NAME, PRODUCT_NAME_EN } from '@/config/product'
const selectedStep = ref(0)
const flowSteps = [
  {
    title: '需求发起',
    icon: ClipboardList,
    heading: '先把问题说清楚',
    description: '填写背景、期望成果和时间要求；还没整理好，可以先保存草稿。',
  },
  {
    title: '协同处理',
    icon: MessageSquare,
    heading: '关注下一步需要什么',
    description: '补充团队需要的资料，查看负责人和进度记录，让讨论跟随同一条需求。',
  },
  {
    title: '成果验收',
    icon: BadgeCheck,
    heading: '用实际成果确认完成',
    description: '查看交付内容，符合预期就确认验收；需要调整时，提交具体反馈。',
  },
]
const selectedFlow = computed(() => flowSteps[selectedStep.value]!)

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
          <ProductLogo :size="23" />
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
          <li v-for="(step, index) in flowSteps" :key="step.title">
            <button
              type="button"
              :aria-pressed="selectedStep === index"
              @click="selectedStep = index"
            >
              <component :is="step.icon" :size="19" aria-hidden="true" />{{ step.title }}
            </button>
          </li>
        </ol>
        <div class="auth-flow-preview" aria-live="polite">
          <Transition name="auth-step" mode="out-in"
            ><div :key="selectedStep">
              <strong>{{ selectedFlow.heading }}</strong>
              <p>{{ selectedFlow.description }}</p>
            </div></Transition
          >
        </div>
      </div>

      <div class="auth-brand-footer">
        <span>需求服务入口</span>
        <span>2026</span>
      </div>
    </section>

    <section class="auth-layout__content">
      <div class="auth-form-shell" :class="{ 'auth-form-shell--wide': wide }">
        <RouterLink class="auth-home-link" to="/"
          ><ArrowLeft :size="16" aria-hidden="true" />返回宣传页</RouterLink
        >
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
  background: var(--color-ink);
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

.auth-flow button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 46px;
  width: 100%;
  padding: 8px;
  border: 1px solid #4e6b83;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-on-ink-muted);
  font-size: 13px;
  transition:
    color 160ms,
    background-color 160ms;
}
.auth-flow button[aria-pressed='true'] {
  color: var(--color-note-text);
  background: var(--color-note);
  border-color: var(--color-note);
}
.auth-flow-preview {
  min-height: 150px;
  margin-top: 20px;
  padding: 22px 0 0;
}
.auth-flow-preview strong {
  color: var(--color-on-ink);
  font-size: 20px;
}
.auth-flow-preview p {
  margin-top: 10px;
  font-size: 14px;
}
.auth-home-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  margin-bottom: 28px;
  color: var(--color-text-secondary);
  font-size: 13px;
}
.auth-home-link:hover {
  color: var(--color-primary);
}
.auth-step-enter-active,
.auth-step-leave-active {
  transition:
    opacity 160ms,
    transform 160ms;
}
.auth-step-enter-from {
  opacity: 0;
  transform: translateX(8px);
}
.auth-step-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
@media (max-width: 900px) {
  .auth-flow-preview {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .auth-step-enter-active,
  .auth-step-leave-active,
  .auth-flow button {
    transition: none;
  }
}

.auth-flow li {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 9px;
}

@keyframes auth-copy-enter {
  from {
    opacity: 0;
    transform: translateY(10px);
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
