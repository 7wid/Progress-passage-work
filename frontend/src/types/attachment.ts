export type AttachmentBusinessType = 'REQUEST' | 'DELIVERY'

export interface AttachmentRecord {
  id: string
  requestId: string
  businessType: AttachmentBusinessType
  businessId: string | null
  originalName: string
  contentType: string
  sizeBytes: number
  uploaderId: string
  uploaderName: string
  canDelete: boolean
  createdAt: string
}

export interface AttachmentSnapshot {
  requestId: string
  businessType: AttachmentBusinessType
  canUpload: boolean
  attachments: AttachmentRecord[]
}

export type AttachmentUploadProgress = (percentage: number) => void
