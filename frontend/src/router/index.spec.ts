import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import router from './index'
import { useAuthStore } from '@/stores/auth'

describe('管理后台路由权限', () => {
  it('普通成员会被管理路由拦截，管理员可以进入命名路由', async () => {
    setActivePinia(createPinia())
    const authStore = useAuthStore()
    authStore.initialized = true
    authStore.user = {
      id: '2',
      account: 'member01',
      displayName: '成员甲',
      role: 'MEMBER',
    }

    await router.push('/admin/members')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')

    authStore.user = {
      id: '1',
      account: 'admin',
      displayName: '管理员',
      role: 'ADMIN',
    }
    await router.push({ name: 'admin-categories' })

    expect(router.currentRoute.value.name).toBe('admin-categories')
    expect(router.currentRoute.value.meta.roles).toEqual(['ADMIN'])

    await router.push({ name: 'admin-audit-logs' })
    expect(router.currentRoute.value.name).toBe('admin-audit-logs')
    expect(router.currentRoute.value.meta.roles).toEqual(['ADMIN'])
  })
})
