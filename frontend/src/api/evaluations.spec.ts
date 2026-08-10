import { beforeEach, describe, expect, it, vi } from 'vitest'
import { confirmEvaluationRejection, createEvaluation, getEvaluations } from './evaluations'
import { http } from './http'
import type { CreatedEvaluationResult, EvaluationRecord } from '@/types/evaluation'

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

describe('evaluations api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('查询并解包评估历史', async () => {
    const evaluations = [
      {
        id: '1',
        requestId: '10',
        version: 1,
      },
    ] as EvaluationRecord[]

    getMock.mockResolvedValueOnce({
      data: {
        data: evaluations,
        requestId: 'request-id',
      },
    } as never)

    await expect(getEvaluations('10')).resolves.toEqual(evaluations)

    expect(getMock).toHaveBeenCalledWith('/requests/10/evaluations')
  })

  it('创建评估前先获取 CSRF 并发送数字工作量', async () => {
    const result = {
      evaluation: {
        id: '1',
      },
      requestStatus: 'PENDING_ASSIGNMENT',
      requestVersion: 1,
      adminConfirmationRequired: false,
    } as CreatedEvaluationResult

    getMock.mockResolvedValueOnce({
      data: {
        data: 'csrf-token',
        requestId: 'csrf-request',
      },
    } as never)

    postMock.mockResolvedValueOnce({
      data: {
        data: result,
        requestId: 'request-id',
      },
    } as never)

    await createEvaluation('10', {
      requestVersion: 0,
      conclusion: 'FEASIBLE',
      publicComment: '  该需求可以承接  ',
      solutionSummary: '  使用 Spring Boot 实现  ',
      estimatedWorkload: 16.5,
      estimatedFinishAt: '2030-09-01T08:00:00Z',
      requiredSkills: '  Java  ',
      risks: '',
      internalNote: '',
    })

    expect(getMock).toHaveBeenCalledWith('/auth/csrf')

    expect(postMock).toHaveBeenCalledWith('/requests/10/evaluations', {
      requestVersion: 0,
      conclusion: 'FEASIBLE',
      publicComment: '该需求可以承接',
      solutionSummary: '使用 Spring Boot 实现',
      estimatedWorkload: 16.5,
      estimatedFinishAt: '2030-09-01T08:00:00Z',
      requiredSkills: 'Java',
      risks: null,
      internalNote: null,
    })

    expect(getMock.mock.invocationCallOrder[0]).toBeLessThan(postMock.mock.invocationCallOrder[0])
  })

  it('管理员确认前先获取 CSRF', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        data: 'csrf-token',
        requestId: 'csrf-request',
      },
    } as never)

    postMock.mockResolvedValueOnce({
      data: {
        data: {
          requestId: '10',
          requestStatus: 'REJECTED',
          requestVersion: 2,
        },
        requestId: 'request-id',
      },
    } as never)

    await confirmEvaluationRejection('10', '2', 1)

    expect(postMock).toHaveBeenCalledWith('/requests/10/evaluations/2/confirm-rejection', {
      requestVersion: 1,
    })
  })
})
