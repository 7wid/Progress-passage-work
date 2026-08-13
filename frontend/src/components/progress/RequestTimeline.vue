<script setup lang="ts">
import { computed } from 'vue'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import type { ProgressLog } from '@/types/progress'
import type { RequestStatusHistory } from '@/types/request'
import type { AcceptanceRecord, DeliveryRecord } from '@/types/delivery'

const props = defineProps<{
  statusHistory: RequestStatusHistory[]
  progressLogs: ProgressLog[]
  deliveries: DeliveryRecord[]
  acceptances: AcceptanceRecord[]
}>()

type TimelineEvent =
  | {
      key: string
      kind: 'STATUS'
      createdAt: string
      history: RequestStatusHistory
    }
  | {
      key: string
      kind: 'PROGRESS'
      createdAt: string
      log: ProgressLog
    }
  | {
      key: string
      kind: 'DELIVERY'
      createdAt: string
      delivery: DeliveryRecord
    }
  | {
      key: string
      kind: 'ACCEPTANCE'
      createdAt: string
      acceptance: AcceptanceRecord
    }

const events = computed<TimelineEvent[]>(() => {
  const statusEvents: TimelineEvent[] = props.statusHistory.map((history) => ({
    key: `status-${history.id}`,
    kind: 'STATUS',
    createdAt: history.createdAt,
    history,
  }))
  const progressEvents: TimelineEvent[] = props.progressLogs.map((log) => ({
    key: `progress-${log.id}`,
    kind: 'PROGRESS',
    createdAt: log.createdAt,
    log,
  }))
  const deliveryEvents: TimelineEvent[] = props.deliveries.map((delivery) => ({
    key: `delivery-${delivery.id}`,
    kind: 'DELIVERY',
    createdAt: delivery.createdAt,
    delivery,
  }))
  const acceptanceEvents: TimelineEvent[] = props.acceptances.map((acceptance) => ({
    key: `acceptance-${acceptance.id}`,
    kind: 'ACCEPTANCE',
    createdAt: acceptance.createdAt,
    acceptance,
  }))

  return [...statusEvents, ...progressEvents, ...deliveryEvents, ...acceptanceEvents].sort(
    (left, right) => {
      const timeDifference =
        new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
      return timeDifference === 0 ? right.key.localeCompare(left.key) : timeDifference
    },
  )
})

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function safeHttpUrl(value: string | null): string | null {
  if (!value) return null
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:' ? value : null
  } catch {
    return null
  }
}
</script>

<template>
  <el-card header="处理时间线">
    <el-empty v-if="events.length === 0" description="暂无处理记录" />

    <el-timeline v-else>
      <el-timeline-item
        v-for="event in events"
        :key="event.key"
        :timestamp="formatDateTime(event.createdAt)"
      >
        <article v-if="event.kind === 'STATUS'" class="timeline-event">
          <div class="event-header">
            <RequestStatusTag :status="event.history.toStatus" />
            <strong>{{ event.history.operatorName }}</strong>
          </div>
          <p>{{ event.history.reason ?? '无补充说明' }}</p>
        </article>

        <article v-else-if="event.kind === 'PROGRESS'" class="timeline-event">
          <div class="event-header">
            <el-tag type="primary">进度 {{ event.log.progress }}%</el-tag>
            <strong>{{ event.log.authorName }}</strong>
            <el-tag :type="event.log.visibleToRequester ? 'success' : 'warning'" size="small">
              {{ event.log.visibleToRequester ? '需求方可见' : '仅技术组可见' }}
            </el-tag>
          </div>
          <el-progress :percentage="event.log.progress" />
          <p>{{ event.log.content }}</p>
          <p v-if="event.log.nextPlan"><strong>下一步：</strong>{{ event.log.nextPlan }}</p>
          <p v-if="event.log.nextUpdateAt">
            <strong>预计下次更新：</strong>{{ formatDateTime(event.log.nextUpdateAt) }}
          </p>
        </article>

        <article v-else-if="event.kind === 'DELIVERY'" class="timeline-event">
          <div class="event-header">
            <el-tag type="success">提交交付</el-tag>
            <strong>{{ event.delivery.submitterName }}</strong>
          </div>
          <p>{{ event.delivery.description }}</p>
          <a
            v-if="safeHttpUrl(event.delivery.deliveryUrl)"
            :href="safeHttpUrl(event.delivery.deliveryUrl) ?? undefined"
            target="_blank"
            rel="noopener noreferrer"
            >打开交付地址</a
          >
        </article>

        <article v-else class="timeline-event">
          <div class="event-header">
            <el-tag :type="event.acceptance.result === 'ACCEPTED' ? 'success' : 'warning'">
              {{ event.acceptance.result === 'ACCEPTED' ? '验收通过' : '退回修改' }}
            </el-tag>
            <strong>{{ event.acceptance.operatorName }}</strong>
          </div>
          <p>{{ event.acceptance.comment ?? '无补充评价' }}</p>
        </article>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<style scoped>
.timeline-event {
  display: grid;
  gap: 10px;
}

.event-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.timeline-event p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
