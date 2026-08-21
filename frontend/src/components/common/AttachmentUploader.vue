<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@lucide/vue'
import {
  ATTACHMENT_ACCEPT,
  deletePendingAttachment,
  MAX_ATTACHMENT_COUNT,
  uploadRequestAttachment,
  validateAttachmentFile,
} from '@/api/attachments'
import { getApiErrorMessage } from '@/api/http'
import AttachmentList from './AttachmentList.vue'
import type { AttachmentBusinessType, AttachmentRecord } from '@/types/attachment'

interface UploadTask {
  key: string
  file: File
  progress: number
  state: 'WAITING' | 'UPLOADING' | 'FAILED'
  error: string
}

const props = withDefaults(
  defineProps<{
    requestId: string
    businessType: AttachmentBusinessType
    modelValue: AttachmentRecord[]
    disabled?: boolean
  }>(),
  { disabled: false },
)

const emit = defineEmits<{
  'update:modelValue': [attachments: AttachmentRecord[]]
  'uploading-change': [uploading: boolean]
}>()

const inputRef = ref<HTMLInputElement>()
const attachments = ref<AttachmentRecord[]>([...props.modelValue])
const tasks = ref<UploadTask[]>([])
const deletingId = ref<string | null>(null)
const processing = ref(false)

const occupiedCount = computed(() => attachments.value.length + tasks.value.length)
const selectionDisabled = computed(
  () =>
    props.disabled ||
    processing.value ||
    deletingId.value !== null ||
    occupiedCount.value >= MAX_ATTACHMENT_COUNT,
)

watch(
  () => props.modelValue,
  (value) => {
    attachments.value = [...value]
  },
)

function publishAttachments(next: AttachmentRecord[]): void {
  attachments.value = next
  emit('update:modelValue', [...next])
}

function taskKey(file: File): string {
  return `${file.name.toLowerCase()}|${file.size}|${file.lastModified}`
}

function alreadySelected(file: File): boolean {
  const key = taskKey(file)
  return (
    tasks.value.some((task) => task.key === key) ||
    attachments.value.some(
      (attachment) =>
        attachment.originalName.toLowerCase() === file.name.toLowerCase() &&
        attachment.sizeBytes === file.size,
    )
  )
}

async function uploadTask(task: UploadTask): Promise<void> {
  task.state = 'UPLOADING'
  task.error = ''
  task.progress = 0
  try {
    const attachment = await uploadRequestAttachment(
      props.requestId,
      props.businessType,
      task.file,
      (percentage) => {
        task.progress = percentage
      },
    )
    publishAttachments([...attachments.value, attachment])
    tasks.value = tasks.value.filter((candidate) => candidate !== task)
  } catch (error) {
    task.state = 'FAILED'
    task.error = getApiErrorMessage(
      error,
      error instanceof Error ? error.message : `${task.file.name} 上传失败`,
    )
  }
}

async function processWaitingTasks(): Promise<void> {
  if (processing.value) return
  processing.value = true
  emit('uploading-change', true)
  try {
    for (const task of [...tasks.value]) {
      if (task.state === 'WAITING') await uploadTask(task)
    }
  } finally {
    processing.value = false
    emit('uploading-change', false)
  }
}

function handleFileSelection(event: Event): void {
  const target = event.target as HTMLInputElement
  const selected = Array.from(target.files ?? [])
  target.value = ''
  if (selected.length === 0) return

  let remaining = MAX_ATTACHMENT_COUNT - occupiedCount.value
  for (const file of selected) {
    if (remaining <= 0) {
      ElMessage.warning('每组附件最多 5 个')
      break
    }
    if (alreadySelected(file)) {
      ElMessage.warning(`${file.name} 已经在附件列表中`)
      continue
    }
    const error = validateAttachmentFile(file)
    if (error) {
      ElMessage.error(`${file.name}：${error}`)
      continue
    }
    tasks.value.push({
      key: taskKey(file),
      file,
      progress: 0,
      state: 'WAITING',
      error: '',
    })
    remaining -= 1
  }
  void processWaitingTasks()
}

async function retryTask(task: UploadTask): Promise<void> {
  if (processing.value || task.state !== 'FAILED') return
  task.state = 'WAITING'
  await processWaitingTasks()
}

function removeFailedTask(task: UploadTask): void {
  if (processing.value) return
  tasks.value = tasks.value.filter((candidate) => candidate !== task)
}

async function removeAttachment(attachment: AttachmentRecord): Promise<void> {
  if (props.disabled || deletingId.value || processing.value || !attachment.canDelete) return
  try {
    await ElMessageBox.confirm(`确定删除附件“${attachment.originalName}”吗？`, '删除附件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  deletingId.value = attachment.id
  try {
    await deletePendingAttachment(props.requestId, attachment.id)
    publishAttachments(attachments.value.filter((candidate) => candidate.id !== attachment.id))
    ElMessage.success('附件已删除')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '删除附件失败，请稍后重试'))
  } finally {
    deletingId.value = null
  }
}
</script>

<template>
  <div class="attachment-uploader">
    <AttachmentList
      :attachments="attachments"
      :deleting-id="deletingId"
      :delete-disabled="disabled || processing"
      empty-description="尚未选择附件"
      @remove="removeAttachment"
    />

    <ul v-if="tasks.length > 0" class="task-list">
      <li v-for="task in tasks" :key="task.key" class="task-item">
        <div class="task-heading">
          <span>{{ task.file.name }}</span>
          <span>{{ task.state === 'FAILED' ? '上传失败' : `${task.progress}%` }}</span>
        </div>
        <el-progress
          v-if="task.state !== 'FAILED'"
          :percentage="task.progress"
          :status="task.progress === 100 ? 'success' : undefined"
        />
        <div v-else class="task-error">
          <span>{{ task.error }}</span>
          <el-button link type="primary" @click="retryTask(task)">重试</el-button>
          <el-button link type="danger" @click="removeFailedTask(task)">移除</el-button>
        </div>
      </li>
    </ul>

    <div class="upload-actions">
      <input
        ref="inputRef"
        class="visually-hidden"
        type="file"
        multiple
        :accept="ATTACHMENT_ACCEPT"
        :disabled="selectionDisabled"
        @change="handleFileSelection"
      />
      <el-button :disabled="selectionDisabled" @click="inputRef?.click()">
        <Upload :size="16" aria-hidden="true" />
        选择并上传附件
      </el-button>
      <span>单个不超过 20 MB，每组最多 5 个；仅支持常用文档、图片和 ZIP。</span>
    </div>
  </div>
</template>

<style scoped>
.attachment-uploader {
  display: grid;
  gap: 12px;
}
.task-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.task-item {
  padding: 10px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: #fbfcfe;
}
.task-heading,
.task-error,
.upload-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.task-error {
  color: var(--color-danger);
}
.upload-actions {
  justify-content: flex-start;
  flex-wrap: wrap;
  padding: 12px;
  border: 1px dashed #aebbd0;
  border-radius: var(--radius-md);
  background: #fbfcfe;
}
.upload-actions span {
  color: var(--color-text-tertiary);
  font-size: 12px;
}
.upload-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  clip-path: inset(50%);
}
</style>
