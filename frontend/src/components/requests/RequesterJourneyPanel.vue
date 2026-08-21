<script setup lang="ts">
import { computed, useId } from 'vue'
import { BadgeCheck, Ban, CircleAlert, Clock3 } from '@lucide/vue'
import type { Component } from 'vue'
import type { RequestStatus } from '@/types/request'

type JourneyTone = 'info' | 'attention' | 'success' | 'negative' | 'muted'

interface JourneyState {
  activeStep: number
  eyebrow: string
  title: string
  serviceUpdate: string
  nextAction: string
  tone: JourneyTone
  icon: Component
  showTrack: boolean
}

const props = defineProps<{ status: RequestStatus }>()
const titleId = useId()

const stages = ['提交与评估', '任务分配', '协作处理', '交付验收', '完成归档']

const journeyStates: Record<RequestStatus, JourneyState> = {
  DRAFT: {
    activeStep: 0,
    eyebrow: '尚未提交',
    title: '需求仍处于草稿阶段',
    serviceUpdate: '服务团队尚未收到这项需求。',
    nextAction: '完善需求信息后提交，需求才会进入评估流程。',
    tone: 'muted',
    icon: Clock3,
    showTrack: true,
  },
  PENDING_REVIEW: {
    activeStep: 0,
    eyebrow: '当前阶段 · 需求评估',
    title: '需求已提交，正在进行可行性评估',
    serviceUpdate: '服务团队正在核对需求范围、资源与实施条件。',
    nextAction: '暂时无需操作；如收到补充通知，请及时完善资料。',
    tone: 'info',
    icon: Clock3,
    showTrack: true,
  },
  NEED_MORE_INFO: {
    activeStep: 0,
    eyebrow: '需要你的处理',
    title: '评估所需信息尚不完整',
    serviceUpdate: '服务团队已完成初步核对，正在等待补充资料。',
    nextAction: '查看评估意见并补充需求内容，随后重新提交。',
    tone: 'attention',
    icon: CircleAlert,
    showTrack: true,
  },
  PENDING_ASSIGNMENT: {
    activeStep: 1,
    eyebrow: '当前阶段 · 任务分配',
    title: '需求已通过评估，正在安排服务团队',
    serviceUpdate: '管理员正在确认负责人和参与成员。',
    nextAction: '暂时无需操作；人员确定后将进入协作处理。',
    tone: 'info',
    icon: Clock3,
    showTrack: true,
  },
  IN_PROGRESS: {
    activeStep: 2,
    eyebrow: '当前阶段 · 协作处理',
    title: '服务团队正在推进需求',
    serviceUpdate: '任务已进入实施阶段，最新进展会持续记录在本页。',
    nextAction: '关注进度记录；需要确认的问题请及时配合反馈。',
    tone: 'info',
    icon: Clock3,
    showTrack: true,
  },
  PENDING_ACCEPTANCE: {
    activeStep: 3,
    eyebrow: '需要你的处理',
    title: '成果已提交，正在等待验收',
    serviceUpdate: '服务团队已提交本轮成果与交付说明。',
    nextAction: '检查交付内容，并在下方“交付与验收”区域确认结果。',
    tone: 'attention',
    icon: CircleAlert,
    showTrack: true,
  },
  COMPLETED: {
    activeStep: 4,
    eyebrow: '流程已完成',
    title: '需求已验收并完成归档',
    serviceUpdate: '成果与处理记录已纳入完整的需求档案。',
    nextAction: '当前无需进一步操作，可随时回看交付与过程记录。',
    tone: 'success',
    icon: BadgeCheck,
    showTrack: true,
  },
  REJECTED: {
    activeStep: -1,
    eyebrow: '流程已终止',
    title: '当前需求暂不承接',
    serviceUpdate: '评估结论已确认，标准处理流程不再继续。',
    nextAction: '查看下方评估意见；如仍有需要，可调整范围后重新发起。',
    tone: 'negative',
    icon: Ban,
    showTrack: false,
  },
  CANCELLED: {
    activeStep: -1,
    eyebrow: '流程已终止',
    title: '需求已取消',
    serviceUpdate: '该需求已停止流转，现有记录仍会保留。',
    nextAction: '当前无需操作；如需继续，可重新发起一项需求。',
    tone: 'muted',
    icon: Ban,
    showTrack: false,
  },
}

const state = computed(() => journeyStates[props.status])

function isStepCompleted(index: number): boolean {
  return props.status === 'COMPLETED' || index < state.value.activeStep
}
</script>

<template>
  <section class="journey-panel" :class="`journey-panel--${state.tone}`" :aria-labelledby="titleId">
    <div class="journey-panel__summary">
      <div class="journey-panel__lead">
        <span class="journey-panel__icon" aria-hidden="true">
          <component :is="state.icon" :size="20" :stroke-width="1.9" />
        </span>
        <div>
          <span class="journey-panel__eyebrow">{{ state.eyebrow }}</span>
          <h2 :id="titleId">{{ state.title }}</h2>
        </div>
      </div>

      <dl class="journey-panel__facts">
        <div>
          <dt>服务团队进展</dt>
          <dd>{{ state.serviceUpdate }}</dd>
        </div>
        <div>
          <dt>你需要做什么</dt>
          <dd>{{ state.nextAction }}</dd>
        </div>
      </dl>
    </div>

    <ol v-if="state.showTrack" class="journey-panel__track" aria-label="需求处理阶段">
      <li
        v-for="(stage, index) in stages"
        :key="stage"
        :class="{
          'is-completed': isStepCompleted(index),
          'is-current': index === state.activeStep,
        }"
        :aria-current="index === state.activeStep ? 'step' : undefined"
      >
        <span class="journey-panel__marker" aria-hidden="true">
          <BadgeCheck v-if="isStepCompleted(index)" :size="17" :stroke-width="2" />
          <span v-else>{{ index + 1 }}</span>
        </span>
        <span class="journey-panel__stage-copy">
          <strong>{{ stage }}</strong>
          <small v-if="index === state.activeStep">当前</small>
          <small v-else-if="isStepCompleted(index)">已完成</small>
          <small v-else>待开始</small>
        </span>
      </li>
    </ol>

    <p v-else class="journey-panel__stopped">
      标准处理流程已停止，具体原因与历史节点可在下方记录中查看。
    </p>
  </section>
</template>

<style scoped>
.journey-panel {
  --journey-color: var(--color-primary);
  --journey-soft: var(--color-primary-soft);
  --journey-border: var(--color-primary-border);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-top: 3px solid var(--journey-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.journey-panel--attention {
  --journey-color: var(--color-warning);
  --journey-soft: #fff7ed;
  --journey-border: #fed7aa;
}

.journey-panel--success {
  --journey-color: var(--color-success);
  --journey-soft: #ecfdf5;
  --journey-border: #a7f3d0;
}

.journey-panel--negative {
  --journey-color: var(--color-danger);
  --journey-soft: #fef2f2;
  --journey-border: #fecaca;
}

.journey-panel--muted {
  --journey-color: #64748b;
  --journey-soft: #f1f5f9;
  --journey-border: #cbd5e1;
}

.journey-panel__summary {
  display: grid;
  grid-template-columns: minmax(260px, 0.85fr) minmax(420px, 1.35fr);
  align-items: stretch;
  padding: 22px 24px;
}

.journey-panel__lead {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 12px;
  padding-right: 24px;
}

.journey-panel__icon {
  display: inline-grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  color: var(--journey-color);
  background: var(--journey-soft);
  border: 1px solid var(--journey-border);
  border-radius: var(--radius-md);
}

.journey-panel__eyebrow {
  display: block;
  margin: 0 0 3px;
  color: var(--journey-color);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
}

.journey-panel h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 19px;
  font-weight: 680;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.journey-panel__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  border-left: 1px solid var(--color-border-subtle);
}

.journey-panel__facts > div {
  min-width: 0;
  padding: 1px 22px;
}

.journey-panel__facts > div + div {
  border-left: 1px solid var(--color-border-subtle);
}

.journey-panel__facts dt {
  margin-bottom: 5px;
  color: var(--color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.journey-panel__facts dd {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.journey-panel__track {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin: 0;
  padding: 20px 24px 22px;
  list-style: none;
  background: #f8fafc;
  border-top: 1px solid var(--color-border-subtle);
}

.journey-panel__track li {
  position: relative;
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 9px;
}

.journey-panel__track li:not(:last-child)::after {
  position: absolute;
  z-index: 0;
  top: 17px;
  right: 10px;
  left: 46px;
  height: 1px;
  background: var(--color-border);
  content: '';
}

.journey-panel__track li.is-completed:not(:last-child)::after {
  background: var(--journey-border);
}

.journey-panel__marker {
  position: relative;
  z-index: 1;
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}

.is-completed .journey-panel__marker {
  color: var(--color-success);
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.is-current .journey-panel__marker {
  color: #ffffff;
  background: var(--journey-color);
  border-color: var(--journey-color);
  box-shadow: 0 0 0 4px var(--journey-soft);
}

.journey-panel__stage-copy {
  position: relative;
  z-index: 1;
  display: grid;
  min-width: 0;
  padding-right: 8px;
  background: #f8fafc;
}

.journey-panel__stage-copy strong {
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 620;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.journey-panel__stage-copy small {
  color: var(--color-text-tertiary);
  font-size: 11px;
  line-height: 1.4;
}

.is-current .journey-panel__stage-copy strong {
  color: var(--color-text-primary);
}

.is-current .journey-panel__stage-copy small {
  color: var(--journey-color);
  font-weight: 700;
}

.journey-panel__stopped {
  margin: 0;
  padding: 16px 24px;
  color: var(--color-text-secondary);
  background: #f8fafc;
  border-top: 1px solid var(--color-border-subtle);
  font-size: 13px;
}

@media (max-width: 980px) {
  .journey-panel__summary {
    grid-template-columns: 1fr;
  }

  .journey-panel__lead {
    padding: 0 0 18px;
  }

  .journey-panel__facts {
    padding-top: 18px;
    border-top: 1px solid var(--color-border-subtle);
    border-left: 0;
  }

  .journey-panel__facts > div:first-child {
    padding-left: 0;
  }
}

@media (max-width: 700px) {
  .journey-panel__summary {
    padding: 20px;
  }

  .journey-panel__facts {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .journey-panel__facts > div,
  .journey-panel__facts > div:first-child {
    padding: 0;
  }

  .journey-panel__facts > div + div {
    padding-top: 14px;
    border-top: 1px solid var(--color-border-subtle);
    border-left: 0;
  }

  .journey-panel__track {
    grid-template-columns: 1fr;
    gap: 0;
    padding: 18px 20px;
  }

  .journey-panel__track li {
    min-height: 54px;
  }

  .journey-panel__track li:not(:last-child)::after {
    top: 38px;
    bottom: -4px;
    left: 17px;
    width: 1px;
    height: auto;
  }

  .journey-panel__stage-copy {
    padding: 4px 0;
  }

  .journey-panel__stage-copy strong {
    white-space: normal;
  }
}
</style>
