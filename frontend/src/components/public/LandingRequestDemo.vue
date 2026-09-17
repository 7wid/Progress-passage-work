<script setup lang="ts">
import { ArrowRight, RotateCcw } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import LandingRequestCard from './LandingRequestCard.vue'
import {
  getLandingScenario,
  landingScenarios,
  landingSteps,
  type LandingScenarioId,
  type LandingStep,
} from './landingScenarios'

const props = defineProps<{ scenarioId: LandingScenarioId }>()
const emit = defineEmits<{ 'update:scenarioId': [id: LandingScenarioId] }>()
const step = ref<LandingStep>(0)
const accepted = ref(false)
const adjusting = ref(false)
const nextButton = ref<HTMLButtonElement>()
const scenario = computed(() => getLandingScenario(props.scenarioId))
const announcement = computed(
  () =>
    `${scenario.value.label}示例：${accepted.value ? '验收完成' : adjusting.value ? '已返回处理中，调整意见已记录' : landingSteps[step.value].status}`,
)

function reset() {
  step.value = 0
  accepted.value = false
  adjusting.value = false
}
function advance() {
  if (step.value < 4) {
    step.value = (step.value + 1) as LandingStep
    adjusting.value = false
  } else accepted.value = true
}
function requestAdjustment() {
  step.value = 3
  adjusting.value = true
  nextButton.value?.focus({ preventScroll: true })
}
function restart() {
  reset()
  nextButton.value?.focus({ preventScroll: true })
}
watch(() => props.scenarioId, reset)
</script>

<template>
  <div class="request-demo" aria-label="可操作的需求协作示例">
    <div class="request-demo__scenarios" aria-label="选择演示场景">
      <button
        v-for="item in landingScenarios"
        :key="item.id"
        type="button"
        :aria-pressed="scenarioId === item.id"
        @click="emit('update:scenarioId', item.id)"
      >
        {{ item.label }}
      </button>
    </div>
    <div class="request-demo__canvas">
      <LandingRequestCard
        :scenario="scenario"
        :step="step"
        :accepted="accepted"
        :adjusting="adjusting"
      />
    </div>
    <ol class="request-demo__steps" aria-label="示例协作阶段">
      <li
        v-for="(item, index) in landingSteps"
        :key="item.title"
        :aria-current="step === index ? 'step' : undefined"
        :class="{ 'is-complete': index < step || accepted }"
      >
        <span>{{ index + 1 }}</span
        >{{ item.short }}
      </li>
    </ol>
    <div class="request-demo__controls">
      <button
        v-if="step > 0 || accepted"
        class="request-demo__reset"
        type="button"
        @click="restart"
      >
        <RotateCcw :size="14" aria-hidden="true" /> 重来
      </button>
      <span v-else class="request-demo__prompt">点一下，看看下一步</span>
      <div class="request-demo__actions">
        <button
          v-if="step === 4 && !accepted"
          class="request-demo__adjust"
          type="button"
          @click="requestAdjustment"
        >
          需要调整
        </button>
        <button
          ref="nextButton"
          class="request-demo__next"
          type="button"
          @click="accepted ? restart() : advance()"
        >
          {{
            accepted
              ? '再体验一次'
              : step === 4
                ? '确认成果'
                : `下一步：${landingSteps[step + 1]!.title}`
          }}<ArrowRight :size="15" aria-hidden="true" />
        </button>
      </div>
    </div>
    <p class="request-demo__disclaimer">示例操作仅用于体验，不会提交真实需求。</p>
    <span class="sr-only" role="status">{{ announcement }}</span>
  </div>
</template>

<style scoped>
.request-demo {
  position: relative;
  min-width: 0;
}
.request-demo__scenarios {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-bottom: 15px;
}
.request-demo__scenarios button {
  border: 1px solid transparent;
  background: transparent;
  color: #45627e;
  padding: 10px 13px;
  border-radius: 7px;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  min-height: 44px;
  transition:
    background 180ms ease,
    color 180ms ease;
}
.request-demo__scenarios button:hover {
  background: #e3edfa;
}
.request-demo__scenarios button[aria-pressed='true'] {
  background: #133350;
  color: #fff;
}
.request-demo__canvas {
  position: relative;
  isolation: isolate;
}
.request-demo__canvas::before {
  content: '';
  position: absolute;
  inset: 19px -16px -15px 17px;
  background: #c9def6;
  border: 1px solid #b6cfea;
  border-radius: 18px;
  z-index: -1;
  transform: rotate(2deg);
}
.request-demo__steps {
  list-style: none;
  margin: 32px 0 15px;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 5px;
}
.request-demo__steps li {
  display: flex;
  gap: 7px;
  align-items: center;
  font-size: 12px;
  color: #52677c;
}
.request-demo__steps li > span {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  background: #e5edf6;
  border-radius: 50%;
  font-size: 10px;
}
.request-demo__steps [aria-current='step'] {
  color: #2056b6;
  font-weight: 650;
}
.request-demo__steps [aria-current='step'] > span {
  background: #245ed0;
  color: #fff;
}
.request-demo__steps .is-complete > span {
  background: #d8ede2;
  color: #16634a;
}
.request-demo__controls {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
.request-demo__controls button {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  font: inherit;
  font-size: 12px;
  border-radius: 7px;
  cursor: pointer;
  padding: 10px 13px;
  transition:
    transform 160ms ease,
    background 160ms ease;
}
.request-demo__controls button:active {
  transform: scale(0.97);
}
.request-demo__prompt {
  font-size: 11px;
  color: #52677c;
}
.request-demo__reset {
  background: transparent;
  border: 0;
  color: #52677c;
}
.request-demo__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-left: auto;
}
.request-demo__next {
  background: #fff;
  color: #16417b;
  border: 1px solid #aac3e1;
  box-shadow: 0 2px 3px rgb(20 49 81 / 3%);
}
.request-demo__next:hover {
  background: #e4edfc;
}
.request-demo__adjust {
  background: transparent;
  border: 1px solid #b8cadd;
  color: #435d75;
}
.request-demo__disclaimer {
  font-size: 10px;
  line-height: 1.6;
  color: #52677c;
  margin: 13px 0 0;
}
@media (max-width: 540px) {
  .request-demo__scenarios {
    gap: 2px;
  }
  .request-demo__scenarios button {
    flex: 1;
    padding-inline: 6px;
    font-size: 11px;
  }
  .request-demo__steps li {
    gap: 4px;
    font-size: 11px;
  }
  .request-demo__canvas::before {
    inset: 12px -7px -10px 12px;
    transform: rotate(1deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .request-demo button {
    transition: none;
  }
}
</style>
