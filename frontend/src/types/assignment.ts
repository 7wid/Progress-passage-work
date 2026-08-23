import type { UserRole } from '@/types/auth'
import type { RequestStatus } from '@/types/request'

export type RequestMemberType = 'OWNER' | 'PARTICIPANT'
export type AssignableMemberRole = Extract<UserRole, 'MEMBER' | 'ADMIN'>

export interface AssignableMemberOption {
  id: string
  account: string
  displayName: string
  role: AssignableMemberRole
}

export interface MemberRecommendation extends AssignableMemberOption {
  skills: string[]
  matchedSkills: string[]
  activeRequestCount: number
  projectedActiveRequestCount: number
  rank: number
}

export interface MemberRecommendationResult {
  requiredSkills: string | null
  members: MemberRecommendation[]
}

export interface RequestMember {
  id: string
  userId: string
  displayName: string
  role: AssignableMemberRole
  memberType: RequestMemberType
  joinedAt: string
}

export interface RequestAssignment {
  requestId: string
  requestStatus: RequestStatus
  requestVersion: number
  owner: RequestMember | null
  participants: RequestMember[]
}

export interface UpdateRequestMembersInput {
  requestVersion: number
  ownerId: string
  participantIds: string[]
  reason: string
}
