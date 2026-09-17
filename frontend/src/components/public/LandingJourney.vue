<script setup lang="ts">
import { ArrowDown, Check, Mouse } from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import LandingRequestCard from './LandingRequestCard.vue'
import {
  getLandingScenario,
  landingSteps,
  type LandingScenarioId,
  type LandingStep,
} from './landingScenarios'

const props = defineProps<{ scenarioId: LandingScenarioId }>()
const section = ref<HTMLElement>()
const activeStep = ref<LandingStep>(0)
const scenario = computed(() => getLandingScenario(props.scenarioId))
let media: MediaQueryList | undefined
let frame: number | undefined
let followingScroll = false

function updateStepFromScroll() {
  frame = undefined
  const element = section.value
  if (!element || !followingScroll) return
  const bounds = element.getBoundingClientRect()
  const readingLine = window.innerHeight * 0.5
  if (bounds.top > readingLine || bounds.bottom < readingLine) return
  const steps = element.querySelectorAll<HTMLElement>('[data-journey-step]')
  let closest = 0
  let distance = Infinity
  steps.forEach((step, index) => {
    const rect = step.getBoundingClientRect()
    const currentDistance = Math.abs(rect.top + rect.height / 2 - readingLine)
    if (currentDistance < distance) {
      closest = index
      distance = currentDistance
    }
  })
  activeStep.value = closest as LandingStep
}

function scheduleStepUpdate() {
  if (frame === undefined) frame = window.requestAnimationFrame(updateStepFromScroll)
}

function syncScrollMode() {
  followingScroll = media?.matches ?? false
  window.removeEventListener('scroll', scheduleStepUpdate)
  window.removeEventListener('resize', scheduleStepUpdate)
  if (frame !== undefined) window.cancelAnimationFrame(frame)
  frame = undefined
  if (followingScroll) {
    window.addEventListener('scroll', scheduleStepUpdate, { passive: true })
    window.addEventListener('resize', scheduleStepUpdate)
    scheduleStepUpdate()
  }
}

onMounted(() => {
  if (!window.matchMedia) return
  media = window.matchMedia('(min-width: 960px) and (prefers-reduced-motion: no-preference)')
  media.addEventListener('change', syncScrollMode)
  syncScrollMode()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', scheduleStepUpdate)
  window.removeEventListener('resize', scheduleStepUpdate)
  media?.removeEventListener('change', syncScrollMode)
  if (frame !== undefined) window.cancelAnimationFrame(frame)
})
</script>

<template>
  <div ref="section" class="landing-journey">
    <div class="landing-journey__steps">
      <ol>
        <li
          v-for="(step, index) in landingSteps"
          :key="step.title"
          data-journey-step
          :class="{ 'is-active': activeStep === index, 'is-complete': activeStep > index }"
        >
          <button
            type="button"
            :aria-pressed="activeStep === index"
            @click="activeStep = index as LandingStep"
          >
            <span class="landing-journey__number"
              ><Check v-if="activeStep > index" :size="19" aria-hidden="true" /><span v-else>{{
                String(index + 1).padStart(2, '0')
              }}</span></span
            ><span
              ><strong>{{ step.title }}</strong
              ><span class="landing-journey__description">{{ step.description }}</span></span
            >
          </button>
        </li>
      </ol>
    </div>
    <div class="landing-journey__visual">
      <div class="landing-journey__sticky">
        <div class="landing-journey__caption">
          <span>同一条需求，完整的协作记录</span><span>流程示例</span>
        </div>
        <LandingRequestCard :scenario="scenario" :step="activeStep" />
        <p class="landing-journey__hint">
          <Mouse :size="15" aria-hidden="true" /><span class="landing-journey__scroll-hint"
            >继续滚动，或点选左侧步骤</span
          ><span class="landing-journey__tap-hint">点选步骤，查看对应的协作内容</span
          ><ArrowDown :size="15" aria-hidden="true" />
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.landing-journey {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 92px;
  align-items: start;
}
.landing-journey__steps ol {
  list-style: none;
  padding: 0;
  margin: 0;
}
.landing-journey__steps li {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 210px;
}
.landing-journey__steps li:not(:last-child)::after {
  content: '';
  position: absolute;
  top: calc(50% + 25px);
  bottom: calc(-50% + 25px);
  left: 23px;
  width: 1px;
  background: #37536b;
}
.landing-journey__steps button {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  width: 100%;
  gap: 23px;
  text-align: left;
  padding: 16px 0;
  border: 0;
  background: transparent;
  color: #a8bdd0;
  font: inherit;
  cursor: pointer;
  border-radius: 8px;
}
.landing-journey__number {
  display: grid;
  place-items: center;
  width: 47px;
  height: 47px;
  flex-shrink: 0;
  color: #aec5d9;
  border: 1px solid #4b6781;
  border-radius: 50%;
  font-size: 13px;
  background: #12304a;
  transition:
    color 220ms ease,
    background 220ms ease,
    border-color 220ms ease;
}
.landing-journey__steps strong {
  display: block;
  margin: 0 0 11px;
  font-size: 24px;
  line-height: 1.5;
  font-weight: 600;
  color: #b8ccdd;
  transition: color 220ms ease;
}
.landing-journey__description {
  display: block;
  font-size: 14px;
  line-height: 1.9;
  max-width: 25em;
}
.landing-journey__steps .is-active .landing-journey__number {
  background: #d4e5ff;
  border-color: #d4e5ff;
  color: #123c74;
}
.landing-journey__steps .is-active strong {
  color: #fff;
}
.landing-journey__steps .is-complete .landing-journey__number {
  border-color: #78bea4;
  color: #9bd9bf;
}
.landing-journey__steps button:hover strong {
  color: #fff;
}
.landing-journey__visual {
  align-self: stretch;
  min-width: 0;
}
.landing-journey__sticky {
  position: sticky;
  top: 125px;
  padding: 37px 0 25px;
}
.landing-journey__caption {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 12px;
  color: #e7f1f9;
  margin-bottom: 16px;
  line-height: 1.7;
}
.landing-journey__caption > span:last-child {
  color: #afc5d8;
  font-size: 11px;
  white-space: nowrap;
}
.landing-journey__hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  font-size: 11px;
  color: #b0c4d5;
  margin: 22px 0 0;
}
.landing-journey__tap-hint {
  display: none;
}
@media (min-width: 960px) and (max-height: 740px) {
  .landing-journey__sticky {
    top: 88px;
    padding-top: 12px;
  }
}
@media (max-width: 1100px) {
  .landing-journey {
    gap: 48px;
  }
}
@media (max-width: 959px), (prefers-reduced-motion: reduce) {
  .landing-journey {
    grid-template-columns: 1fr;
    gap: 22px;
  }
  .landing-journey__steps ol {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 8px;
  }
  .landing-journey__steps li {
    min-height: 0;
    display: block;
  }
  .landing-journey__steps li::after {
    display: none;
  }
  .landing-journey__steps button {
    display: grid;
    justify-items: center;
    gap: 10px;
    text-align: center;
    padding: 12px 3px;
  }
  .landing-journey__steps strong {
    font-size: 13px;
    margin: 0;
  }
  .landing-journey__description {
    display: none;
  }
  .landing-journey__number {
    width: 38px;
    height: 38px;
  }
  .landing-journey__sticky {
    position: static;
    padding: 0;
    max-width: 600px;
    margin-inline: auto;
  }
  .landing-journey__scroll-hint {
    display: none;
  }
  .landing-journey__tap-hint {
    display: inline;
  }
}
@media (max-width: 380px) {
  .landing-journey__steps ol {
    gap: 2px;
  }
  .landing-journey__steps strong {
    font-size: 11px;
  }
  .landing-journey__number {
    width: 32px;
    height: 32px;
  }
}
</style>
