<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { resetPassword } from '@/api/auth'
import { getApiErrorCode, getApiErrorMessage } from '@/api/http'
import { getRegistrationPasswordError } from '@/utils/authValidation'
import { useAuthStore } from '@/stores/auth'
import '@/styles/authRecovery.css'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
// Fragments are never sent to the web server. Keep the credential only in component memory.
let token = new URLSearchParams(route.hash.slice(1)).get('token') ?? ''
const invalid = ref(!/^[A-Za-z0-9_-]{43}$/.test(token))
const formRef = ref<FormInstance>()
const feedbackRef = ref<HTMLElement>()
const loading = ref(false)
const error = ref('')
const form = reactive({ newPassword: '', confirmPassword: '' })
const rules: FormRules = {
  newPassword: [
    {
      validator: (_rule, value: string, callback) => {
        const message = getRegistrationPasswordError(value)
        callback(message ? new Error(message) : undefined)
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    {
      validator: (_rule, value: string, callback) => {
        callback(
          value && value === form.newPassword ? undefined : new Error('两次输入的密码不一致'),
        )
      },
      trigger: 'blur',
    },
  ],
}

onMounted(() => {
  if (route.hash) void router.replace({ path: route.path, query: route.query, hash: '' })
})

async function submit() {
  if (!formRef.value || loading.value || invalid.value) return
  error.value = ''
  if (!(await formRef.value.validate().catch(() => false))) {
    error.value = '请检查下方密码字段。'
  } else {
    loading.value = true
    try {
      await resetPassword(token, form.newPassword)
      token = ''
      form.newPassword = ''
      form.confirmPassword = ''
      authStore.$resetSession()
      await router.replace({ path: '/login', query: { passwordReset: '1' } })
      return
    } catch (cause) {
      if (getApiErrorCode(cause) === 'INVALID_RESET_TOKEN') invalid.value = true
      error.value = getApiErrorMessage(cause, '密码重置未完成，请稍后重试。')
    } finally {
      loading.value = false
    }
  }
  await nextTick()
  feedbackRef.value?.focus()
}
</script>

<template>
  <AuthLayout
    eyebrow="账户安全"
    title="设置新密码"
    description="验证通过后，所有设备上的旧登录状态都会失效。"
  >
    <section class="recovery">
      <div v-if="invalid" ref="feedbackRef" class="recovery__notice" role="alert" tabindex="-1">
        <p>重置链接无效或已过期，请重新申请邮件。</p>
        <p>如果刚刚刷新过页面，请重新打开邮件中的链接。</p>
        <RouterLink to="/forgot-password">重新申请重置邮件</RouterLink>
      </div>
      <template v-else>
        <div
          v-if="error"
          ref="feedbackRef"
          class="recovery__notice recovery__notice--error"
          role="alert"
          tabindex="-1"
        >
          {{ error }} <a href="#reset-password">检查新密码</a> ·
          <a href="#confirm-password">检查确认密码</a>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          scroll-to-error
          @submit.prevent="submit"
        >
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              id="reset-password"
              v-model="form.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              maxlength="72"
              :disabled="loading"
              aria-describedby="reset-password-help"
            />
          </el-form-item>
          <p id="reset-password-help" class="recovery__help">
            8～72 个字符，包含字母和数字；建议使用密码管理器生成独立密码。
          </p>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              id="confirm-password"
              v-model="form.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              maxlength="72"
              :disabled="loading"
            />
          </el-form-item>
          <el-button class="recovery__submit" type="primary" native-type="submit" :loading="loading"
            >确认重置密码</el-button
          >
        </el-form>
      </template>
      <RouterLink class="recovery__back" to="/login">返回登录</RouterLink>
    </section>
  </AuthLayout>
</template>
