<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRequestDetail } from '@/api/requests'
import RequestStatusTag from '@/components/common/RequestStatusTag.vue'
import type { RequestDetail, RequestUrgency } from '@/types/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<RequestDetail | null>(null)
const errorMessage = ref('')

const urgencyMap: Record<RequestUrgency, string> = {
  NORMAL: '一般',
  HIGH: '较急',
  URGENT: '紧急',
}

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const moneyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
})

function formatDateTime(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date)
}

function formatBudget(value: number | null): string {
  return value === null ? '未填写' : moneyFormatter.format(value)
}

async function loadDetail() {
  const id = String(route.params.id ?? '')
  detail.value = null
  errorMessage.value = ''

  if (!/^[1-9]\d*$/.test(id)) {
    errorMessage.value = '需求编号格式不正确'
    return
  }

  loading.value = true

  try {
    detail.value = await getRequestDetail(id)
  } catch {
    errorMessage.value = '需求不存在、已被删除，或当前账号没有查看权限'
  } finally {
    loading.value = false
  }
}

function backToList() {
  void router.push({ name: 'request-list' })
}

watch(
  () => route.params.id,
  () => void loadDetail(),
  { immediate: true },
)
</script>

<template>
  <section class="page">
    <el-skeleton v-if="loading" :rows="8" animated />

    <el-result
      v-else-if="errorMessage"
      icon="warning"
      title="无法查看需求"
      :sub-title="errorMessage"
    >
      <template #extra>
        <el-button type="primary" @click="backToList">返回需求列表</el-button>
        <el-button @click="loadDetail">重新加载</el-button>
      </template>
    </el-result>

    <template v-else-if="detail">
      <div class="page__header">
        <div>
          <h2>{{ detail.title }}</h2>
          <span class="request-no">{{ detail.requestNo ?? '尚未生成编号' }}</span>
        </div>
        <div class="header-actions">
          <RequestStatusTag :status="detail.status" />
          <el-button @click="backToList">返回列表</el-button>
        </div>
      </div>

      <el-card>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="需求分类">{{ detail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.creatorName }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            {{ urgencyMap[detail.urgency] }}
          </el-descriptions-item>
          <el-descriptions-item label="期望完成日期">
            {{ detail.expectedDeadline }}
          </el-descriptions-item>
          <el-descriptions-item label="预算金额">
            {{ formatBudget(detail.budgetAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="预算说明">
            {{ detail.budgetDescription ?? '未填写' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(detail.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(detail.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理进度" :span="2">
            <el-progress :percentage="detail.progress" />
          </el-descriptions-item>
          <el-descriptions-item label="联系方式" :span="2">
            {{ detail.contactInfo ?? '当前账号无权查看' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card header="需求背景"
        ><div class="text-content">{{ detail.background }}</div></el-card
      >
      <el-card header="具体需求"
        ><div class="text-content">{{ detail.description }}</div></el-card
      >
      <el-card header="期望成果"
        ><div class="text-content">{{ detail.expectedResult }}</div></el-card
      >
      <el-card header="技术限制">
        <div class="text-content">{{ detail.technicalConstraints ?? '未填写' }}</div>
      </el-card>

      <el-card header="状态历史">
        <el-empty v-if="detail.statusHistory.length === 0" description="暂无状态记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="history in detail.statusHistory"
            :key="history.id"
            :timestamp="formatDateTime(history.createdAt)"
          >
            <div class="history-title">
              <RequestStatusTag :status="history.toStatus" />
              <span>{{ history.operatorName }}</span>
            </div>
            <p>{{ history.reason ?? '无补充说明' }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </section>
</template>

<style scoped>
.request-no {
  color: #6b7280;
}

.header-actions,
.history-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.text-content {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.8;
}
</style>
