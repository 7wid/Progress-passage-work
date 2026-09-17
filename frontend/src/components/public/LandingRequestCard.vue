<script setup lang="ts">
import {
  Check,
  CheckCircle2,
  ClipboardCheck,
  FileText,
  MessageCircleMore,
  Paperclip,
} from '@lucide/vue'
import { computed } from 'vue'
import { landingSteps, type LandingScenario, type LandingStep } from './landingScenarios'

const props = withDefaults(
  defineProps<{
    scenario: LandingScenario
    step: LandingStep
    accepted?: boolean
    adjusting?: boolean
  }>(),
  { accepted: false, adjusting: false },
)

const currentStep = computed(() => landingSteps[props.step])
</script>

<template>
  <article class="request-scene" :class="{ 'is-accepted': accepted }" aria-label="需求详情示例">
    <div class="request-scene__bar">
      <span><ClipboardCheck :size="16" aria-hidden="true" /> 我的需求</span><span>示例体验</span>
    </div>
    <div class="request-scene__heading">
      <span class="request-scene__category">{{ scenario.service }}</span>
      <h3>{{ scenario.title }}</h3>
      <span class="request-scene__status"
        ><span aria-hidden="true"></span>{{ accepted ? '已验收' : currentStep.status }}</span
      >
    </div>
    <div class="request-scene__content">
      <Transition name="scene-change">
        <div :key="`${scenario.id}-${step}-${accepted}-${adjusting}`" class="request-scene__panel">
          <template v-if="step === 0">
            <div class="request-scene__section-label">
              <MessageCircleMore :size="16" aria-hidden="true" /> 你想解决什么问题
            </div>
            <p class="request-scene__description">{{ scenario.description }}</p>
            <div class="request-scene__expectation">
              <span>期待的结果</span><strong>{{ scenario.outcome }}</strong>
            </div>
          </template>
          <template v-else-if="step === 1">
            <div class="request-scene__section-label">
              <Paperclip :size="16" aria-hidden="true" /> 资料跟随需求保存
            </div>
            <div class="request-scene__file">
              <FileText :size="26" aria-hidden="true" /><span
                ><strong>{{ scenario.attachment }}</strong
                ><small>参考附件 · 示例文件</small></span
              ><CheckCircle2 :size="18" aria-hidden="true" />
            </div>
            <p class="request-scene__hint">参考资料、补充说明和期望时间放在一起，减少来回确认。</p>
          </template>
          <template v-else-if="step === 2">
            <div class="request-scene__section-label">
              <ClipboardCheck :size="16" aria-hidden="true" /> 专业可行性评估
            </div>
            <div class="request-scene__assessment">
              <strong>先明确范围，再安排处理</strong>
              <p>核对目标、资料与时间要求，并给出承接结论。</p>
            </div>
            <p class="request-scene__hint">
              这里展示评估过程；真实需求是否承接，以团队评估结果为准。
            </p>
          </template>
          <template v-else-if="step === 3">
            <div class="request-scene__section-label">
              <MessageCircleMore :size="16" aria-hidden="true" /> 最新进展
            </div>
            <ol class="request-scene__timeline">
              <li class="is-complete">
                <CheckCircle2 :size="17" aria-hidden="true" /><span
                  >已明确需求范围<small>目标与资料集中留存</small></span
                >
              </li>
              <li class="is-current">
                <span class="request-scene__dot" aria-hidden="true"></span
                ><span
                  >{{ adjusting ? '根据你的反馈继续调整' : scenario.processing
                  }}<small>{{
                    adjusting
                      ? '调整意见已记录，重新处理后再邀请验收'
                      : '处理结论和最新说明持续更新'
                  }}</small></span
                >
              </li>
              <li>
                <span class="request-scene__dot" aria-hidden="true"></span
                ><span>下一步：邀请你确认成果</span>
              </li>
            </ol>
          </template>
          <template v-else>
            <div class="request-scene__section-label">
              <CheckCircle2 :size="16" aria-hidden="true" />
              {{ accepted ? '成果已确认' : '交付内容，由你确认' }}
            </div>
            <div class="request-scene__delivery">
              <span class="request-scene__delivery-mark"
                ><ClipboardCheck :size="26" aria-hidden="true" /></span
              ><strong>{{ scenario.outcome }}</strong>
            </div>
            <ul class="request-scene__deliverables">
              <li v-for="item in scenario.deliverables" :key="item">
                <Check :size="14" aria-hidden="true" />{{ item }}
              </li>
            </ul>
          </template>
        </div>
      </Transition>
    </div>
    <div class="request-scene__footer">
      <span>{{ accepted ? '成果、说明与反馈集中留存' : '每一步都有清楚的下一步' }}</span
      ><span>{{ accepted ? '协作完成' : `${step + 1} / ${landingSteps.length}` }}</span>
    </div>
  </article>
</template>

<style scoped>
.request-scene {
  --scene-ink: #15314e;
  --scene-muted: #53677e;
  --scene-line: #e0e8f0;
  --scene-blue: #245ed0;
  --scene-soft: #f2f6fc;
  color: var(--scene-ink);
  background: #fff;
  border: 1px solid var(--scene-line);
  border-radius: 16px;
  box-shadow: 0 22px 54px rgb(15 42 73 / 12%);
  overflow: clip;
  text-align: left;
}
.request-scene__bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 15px 24px;
  background: #133350;
  color: #fff;
  font-size: 12px;
}
.request-scene__bar > span:first-child {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.request-scene__bar > span:last-child {
  color: #c7d9e9;
  font-size: 11px;
}
.request-scene__heading {
  position: relative;
  padding: 24px 24px 18px;
  border-bottom: 1px solid var(--scene-line);
}
.request-scene__category {
  display: block;
  padding-right: 85px;
  color: var(--scene-muted);
  font-size: 11px;
  margin-bottom: 9px;
}
.request-scene__heading h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.5;
  letter-spacing: -0.025em;
}
.request-scene__status {
  position: absolute;
  right: 24px;
  top: 23px;
  display: flex;
  gap: 6px;
  align-items: center;
  color: var(--scene-blue);
  font-size: 11px;
}
.request-scene__status > span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.is-accepted .request-scene__status {
  color: #137451;
}
.request-scene__content {
  display: grid;
  padding: 21px 24px 20px;
  min-height: 238px;
}
.request-scene__panel {
  grid-area: 1/1;
  min-width: 0;
}
.request-scene__section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--scene-muted);
  font-size: 12px;
  margin-bottom: 16px;
}
.request-scene__description {
  margin: 0;
  font-size: 15px;
  line-height: 1.9;
}
.request-scene__expectation {
  margin-top: 20px;
  padding-left: 13px;
  border-left: 3px solid #88b3ed;
  display: grid;
  gap: 6px;
}
.request-scene__expectation > span {
  font-size: 11px;
  color: var(--scene-muted);
}
.request-scene__expectation strong {
  font-size: 13px;
  font-weight: 550;
  line-height: 1.7;
}
.request-scene__file {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 15px 0;
  border-block: 1px solid var(--scene-line);
  color: var(--scene-blue);
}
.request-scene__file > span {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 5px;
}
.request-scene__file strong {
  font-size: 13px;
  color: var(--scene-ink);
  overflow-wrap: anywhere;
  font-weight: 550;
}
.request-scene__file small {
  font-size: 11px;
  color: var(--scene-muted);
}
.request-scene__file > svg {
  flex-shrink: 0;
}
.request-scene__hint {
  color: var(--scene-muted);
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.8;
}
.request-scene__assessment {
  padding: 16px;
  background: var(--scene-soft);
  border-radius: 7px;
}
.request-scene__assessment strong {
  font-size: 15px;
  font-weight: 600;
}
.request-scene__assessment p {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--scene-muted);
}
.request-scene__timeline {
  margin: 0;
  padding: 0;
  list-style: none;
}
.request-scene__timeline li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  position: relative;
  font-size: 13px;
  line-height: 1.6;
  color: var(--scene-muted);
  padding-bottom: 17px;
}
.request-scene__timeline li:last-child {
  padding-bottom: 0;
}
.request-scene__timeline li:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 8px;
  top: 22px;
  height: calc(100% - 26px);
  width: 1px;
  background: var(--scene-line);
}
.request-scene__timeline svg {
  margin-top: 2px;
  flex-shrink: 0;
}
.request-scene__timeline small {
  display: block;
  font-size: 11px;
  color: var(--scene-muted);
  margin-top: 3px;
}
.request-scene__timeline .is-complete {
  color: #167053;
}
.request-scene__timeline .is-current {
  color: var(--scene-blue);
  font-weight: 550;
}
.request-scene__dot {
  width: 7px;
  height: 7px;
  margin: 7px 5px 0;
  flex-shrink: 0;
  background: #b8c7d6;
  border-radius: 50%;
}
.is-current .request-scene__dot {
  background: var(--scene-blue);
  box-shadow: 0 0 0 4px #e9f0ff;
}
.request-scene__delivery {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 14px;
}
.request-scene__delivery-mark {
  display: grid;
  place-items: center;
  width: 49px;
  height: 49px;
  flex-shrink: 0;
  background: #e9f5ee;
  border-radius: 10px;
  color: #137451;
}
.request-scene__delivery strong {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.6;
}
.request-scene__deliverables {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}
.request-scene__deliverables li {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--scene-muted);
}
.request-scene__deliverables svg {
  color: #137451;
}
.request-scene__footer {
  margin: 0 24px;
  padding: 13px 0 16px;
  border-top: 1px solid var(--scene-line);
  display: flex;
  gap: 16px;
  justify-content: space-between;
  color: var(--scene-muted);
  font-size: 11px;
  line-height: 1.6;
}
.request-scene__footer > span:last-child {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.scene-change-enter-active {
  transition:
    opacity 220ms ease,
    transform 300ms cubic-bezier(0.2, 0.7, 0.2, 1);
}
.scene-change-leave-active {
  transition: opacity 120ms ease;
  pointer-events: none;
}
.scene-change-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.scene-change-leave-to {
  opacity: 0;
}
@media (max-width: 540px) {
  .request-scene__bar {
    padding: 13px 18px;
  }
  .request-scene__heading {
    padding: 20px 18px 16px;
  }
  .request-scene__heading h3 {
    font-size: 18px;
  }
  .request-scene__status {
    right: 18px;
    top: 20px;
  }
  .request-scene__content {
    padding: 18px;
    min-height: 256px;
  }
  .request-scene__footer {
    margin-inline: 18px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .scene-change-enter-active,
  .scene-change-leave-active {
    transition: none;
  }
  .scene-change-enter-from {
    transform: none;
  }
}
</style>
