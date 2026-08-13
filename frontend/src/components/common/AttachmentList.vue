<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadAttachment } from '@/api/attachments'
import { getApiErrorMessage } from '@/api/http'
import type { AttachmentRecord } from '@/types/attachment'

withDefaults(
  defineProps<{
    attachments: AttachmentRecord[]
    deletingId?: string | null
    deleteDisabled?: boolean
    emptyDescription?: string
  }>(),
  {
    deletingId: null,
    deleteDisabled: false,
    emptyDescription: '暂无附件',
  },
)

const emit = defineEmits<{ remove: [attachment: AttachmentRecord] }>()
const downloadingId = ref<string | null>(null)

function formatSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

async function handleDownload(attachment: AttachmentRecord): Promise<void> {
  if (downloadingId.value !== null) return
  downloadingId.value = attachment.id
  try {
    await downloadAttachment(attachment)
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '附件下载失败，请稍后重试'))
  } finally {
    downloadingId.value = null
  }
}
</script>

<template>
  <el-empty v-if="attachments.length === 0" :description="emptyDescription" :image-size="60" />
  <ul v-else class="attachment-list">
    <li v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
      <div class="attachment-main">
        <span class="attachment-name" :title="attachment.originalName">
          {{ attachment.originalName }}
        </span>
        <span class="attachment-meta">
          {{ formatSize(attachment.sizeBytes) }} · {{ attachment.uploaderName }}
        </span>
      </div>
      <div class="attachment-actions">
        <el-button
          link
          type="primary"
          :loading="downloadingId === attachment.id"
          :disabled="downloadingId !== null && downloadingId !== attachment.id"
          @click="handleDownload(attachment)"
        >
          下载
        </el-button>
        <el-button
          v-if="attachment.canDelete"
          link
          type="danger"
          :loading="deletingId === attachment.id"
          :disabled="deleteDisabled || (deletingId !== null && deletingId !== attachment.id)"
          @click="emit('remove', attachment)"
        >
          删除
        </el-button>
      </div>
    </li>
  </ul>
</template>

<style scoped>
.attachment-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.attachment-main {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.attachment-name {
  overflow: hidden;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attachment-meta {
  color: #6b7280;
  font-size: 12px;
}
.attachment-actions {
  display: flex;
  flex: none;
}
</style>
