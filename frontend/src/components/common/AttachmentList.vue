<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, FileText, Trash2 } from '@lucide/vue'
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
      <span class="attachment-icon" aria-hidden="true"><FileText :size="18" /></span>
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
          <Download :size="15" aria-hidden="true" />
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
          <Trash2 :size="15" aria-hidden="true" />
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
  padding: 11px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: #fbfcfe;
  transition:
    border-color var(--motion-fast) ease,
    background-color var(--motion-fast) ease;
}
.attachment-item:hover {
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
}
.attachment-icon {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-radius: var(--radius-md);
}
.attachment-main {
  display: grid;
  min-width: 0;
  gap: 3px;
}
.attachment-name {
  overflow: hidden;
  color: var(--color-text-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attachment-meta {
  color: var(--color-text-tertiary);
  font-size: 12px;
}
.attachment-actions {
  display: flex;
  flex: none;
}
.attachment-actions :deep(.el-button) {
  margin: 0;
}
.attachment-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 560px) {
  .attachment-item {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .attachment-main {
    flex: 1;
  }

  .attachment-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
