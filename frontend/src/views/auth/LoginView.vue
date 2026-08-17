<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, LockKeyhole, LogIn, UserRound } from '@lucide/vue'
import { getRegistrationStatus } from '@/api/auth'
import { getLoginErrorMessage } from '@/api/http'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const registrationEnabled = ref(false)
const form = reactive({ account: '', password: '' })

async function submit() {
  loading.value = true
  try {
    await authStore.signIn(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(getLoginErrorMessage(error))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    registrationEnabled.value = (await getRegistrationStatus()).enabled
  } catch {
    registrationEnabled.value = false
  }
})
</script>

<template>
  <AuthLayout eyebrow="账号登录" title="欢迎回来" description="登录后继续处理和跟踪技术需求。">
    <el-form class="login-form" label-position="top" @submit.prevent="submit">
      <el-form-item label="账号">
        <el-input v-model="form.account" autocomplete="username" placeholder="请输入账号">
          <template #prefix><UserRound :size="17" aria-hidden="true" /></template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="form.password"
          type="password"
          autocomplete="current-password"
          show-password
          placeholder="请输入密码"
        >
          <template #prefix><LockKeyhole :size="17" aria-hidden="true" /></template>
        </el-input>
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="loading" class="login-card__submit">
        <LogIn :size="17" aria-hidden="true" />
        登录
      </el-button>
      <div v-if="registrationEnabled" class="register-entry">
        <span>还没有需求方账号？</span>
        <button type="button" @click="router.push('/register')">
          注册账号
          <ArrowRight :size="15" aria-hidden="true" />
        </button>
      </div>
    </el-form>
  </AuthLayout>
</template>

<style scoped>
.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
}

.login-card__submit {
  width: 100%;
  min-height: 46px;
  margin-left: 0;
}

.login-card__submit :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.register-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 22px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.register-entry button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 4px;
  padding: 5px 7px;
  color: var(--color-primary-strong);
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  font-weight: 650;
}

.register-entry button:hover {
  background: var(--color-primary-soft);
}
</style>
