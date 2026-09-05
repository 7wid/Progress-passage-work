<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowRight, LockKeyhole, LogIn, UserRound } from '@lucide/vue'
import { getRegistrationStatus } from '@/api/auth'
import { getLoginErrorMessage } from '@/api/http'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { resolveSafeRedirect } from '@/utils/authValidation'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const errorSummaryRef = ref<HTMLElement>()
const loading = ref(false)
const registrationEnabled = ref(false)
const submitError = ref('')
const validationError = ref('')
const registrationSuccess = route.query.registered === '1'
const form = reactive({ account: '', password: '' })

const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { max: 64, message: '账号不能超过 64 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { max: 128, message: '密码格式不正确', trigger: 'blur' },
  ],
}

function clearSubmitError() {
  submitError.value = ''
  validationError.value = ''
}

async function submit() {
  if (!formRef.value || loading.value) return
  clearSubmitError()
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    validationError.value = '请检查下方标记的账号和密码字段。'
    await nextTick()
    errorSummaryRef.value?.focus()
    return
  }

  loading.value = true
  try {
    await authStore.signIn(form)
    const redirect = resolveSafeRedirect(route.query.redirect)
    await router.replace(redirect)
  } catch (error) {
    submitError.value = getLoginErrorMessage(error)
    await nextTick()
    errorSummaryRef.value?.focus()
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
  <AuthLayout
    eyebrow="账户访问"
    title="登录服务工作台"
    description="使用平台账号继续查看和处理需求。"
  >
    <div v-if="registrationSuccess" class="auth-feedback auth-feedback--success" role="status">
      账号注册成功，请使用刚创建的账号登录。
    </div>
    <div
      v-if="validationError || submitError"
      ref="errorSummaryRef"
      class="auth-feedback auth-feedback--error"
      role="alert"
      tabindex="-1"
    >
      {{ validationError || submitError }}
      <span v-if="submitError">连续多次登录失败时，账号将被暂时锁定，请稍后再试。</span>
    </div>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      class="login-form"
      label-position="top"
      status-icon
      scroll-to-error
      @submit.prevent="submit"
    >
      <el-form-item label="账号" prop="account">
        <el-input
          v-model="form.account"
          autocomplete="username"
          maxlength="64"
          placeholder="请输入账号"
          @input="clearSubmitError"
        >
          <template #prefix><UserRound :size="17" aria-hidden="true" /></template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          autocomplete="current-password"
          show-password
          maxlength="128"
          placeholder="请输入密码"
          @input="clearSubmitError"
        >
          <template #prefix><LockKeyhole :size="17" aria-hidden="true" /></template>
        </el-input>
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="loading" class="login-card__submit">
        <LogIn :size="17" aria-hidden="true" />
        登录
      </el-button>
      <div v-if="registrationEnabled" class="register-entry">
        <span>还没有账号？</span>
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

.auth-feedback {
  margin-bottom: 20px;
  padding: 12px 14px;
  border: 1px solid;
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.55;
}

.auth-feedback:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.auth-feedback span {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.auth-feedback--success {
  color: var(--color-success-strong);
  background: var(--color-success-soft);
  border-color: #a7f3d0;
}

.auth-feedback--error {
  color: var(--color-danger);
  background: #fef2f2;
  border-color: #fecaca;
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
