import type { AxiosResponse } from 'axios'
import { http } from './http'
import type { ApiResponse } from '@/types/api'
import type {
  AttachmentBusinessType,
  AttachmentRecord,
  AttachmentSnapshot,
  AttachmentUploadProgress,
} from '@/types/attachment'

export const MAX_ATTACHMENT_COUNT = 5
export const MAX_ATTACHMENT_SIZE_BYTES = 20 * 1024 * 1024
export const ATTACHMENT_ACCEPT = [
  '.pdf',
  '.doc',
  '.docx',
  '.xls',
  '.xlsx',
  '.ppt',
  '.pptx',
  '.txt',
  '.md',
  '.csv',
  '.png',
  '.jpg',
  '.jpeg',
  '.webp',
  '.zip',
].join(',')

const ALLOWED_EXTENSIONS = new Set(ATTACHMENT_ACCEPT.split(','))
const ALLOWED_CONTENT_TYPES = new Set([
  'application/octet-stream',
  'application/pdf',
  'application/msword',
  'application/vnd.ms-excel',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'application/zip',
  'application/x-zip-compressed',
  'image/jpeg',
  'image/png',
  'image/webp',
  'text/csv',
  'text/markdown',
  'text/plain',
])

function requirePositiveId(value: string, label: string): string {
  if (!/^[1-9]\d*$/.test(value)) {
    throw new Error(`${label}格式不正确`)
  }
  return value
}

function requestAttachmentPath(requestId: string): string {
  return `/requests/${encodeURIComponent(requirePositiveId(requestId, '需求编号'))}/attachments`
}

function fileExtension(fileName: string): string {
  const index = fileName.lastIndexOf('.')
  return index < 0 ? '' : fileName.slice(index).toLowerCase()
}

export function validateAttachmentFile(file: File): string | null {
  if (file.size <= 0) return '不能上传空文件'
  if (file.size > MAX_ATTACHMENT_SIZE_BYTES) return '单个附件不能超过 20 MB'

  const extension = fileExtension(file.name)
  if (!ALLOWED_EXTENSIONS.has(extension)) {
    return '不支持该文件类型，请选择文档、图片或 ZIP 压缩包'
  }

  if (file.type && !ALLOWED_CONTENT_TYPES.has(file.type.toLowerCase())) {
    return '文件声明的内容类型不在允许范围内'
  }

  return null
}

export async function getRequestAttachments(
  requestId: string,
  businessType: AttachmentBusinessType,
  pendingOnly = false,
): Promise<AttachmentSnapshot> {
  const response = await http.get<ApiResponse<AttachmentSnapshot>>(
    requestAttachmentPath(requestId),
    { params: { businessType, pendingOnly } },
  )
  return response.data.data
}

export async function uploadRequestAttachment(
  requestId: string,
  businessType: AttachmentBusinessType,
  file: File,
  onProgress?: AttachmentUploadProgress,
): Promise<AttachmentRecord> {
  const validationError = validateAttachmentFile(file)
  if (validationError) throw new Error(validationError)

  const formData = new FormData()
  formData.append('file', file)
  formData.append('businessType', businessType)

  await http.get<ApiResponse<string>>('/auth/csrf')
  const response = await http.post<ApiResponse<AttachmentRecord>>(
    requestAttachmentPath(requestId),
    formData,
    {
      timeout: 120_000,
      onUploadProgress(event) {
        if (!onProgress || !event.total) return
        onProgress(Math.min(100, Math.round((event.loaded * 100) / event.total)))
      },
    },
  )
  return response.data.data
}

export async function deletePendingAttachment(
  requestId: string,
  attachmentId: string,
): Promise<void> {
  const path = `${requestAttachmentPath(requestId)}/${encodeURIComponent(
    requirePositiveId(attachmentId, '附件编号'),
  )}`
  await http.get<ApiResponse<string>>('/auth/csrf')
  await http.delete(path)
}

function fileNameFromContentDisposition(value: string | undefined): string | null {
  if (!value) return null
  const encoded = value.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try {
      return decodeURIComponent(encoded)
    } catch {
      return null
    }
  }
  return value.match(/filename="?([^";]+)"?/i)?.[1] ?? null
}

function safeDownloadName(value: string): string {
  const withoutReserved = value.replace(/[\\/:*?"<>|]/g, '_')
  const cleaned = Array.from(withoutReserved)
    .map((character) => {
      const code = character.charCodeAt(0)
      return code <= 31 || code === 127 ? '_' : character
    })
    .join('')
    .trim()
  return cleaned || 'attachment'
}

export async function downloadAttachment(attachment: AttachmentRecord): Promise<void> {
  const attachmentId = requirePositiveId(attachment.id, '附件编号')
  const response: AxiosResponse<Blob> = await http.get(
    `/files/${encodeURIComponent(attachmentId)}/download`,
    { responseType: 'blob', timeout: 120_000 },
  )
  const headerName = fileNameFromContentDisposition(response.headers['content-disposition'])
  const objectUrl = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = safeDownloadName(headerName ?? attachment.originalName)
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  try {
    anchor.click()
  } finally {
    anchor.remove()
    URL.revokeObjectURL(objectUrl)
  }
}
