<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { getPasswordRecoveryStatus, requestPasswordReset } from '@/api/auth'
import { getApiErrorMessage } from '@/api/http'
import '@/styles/authRecovery.css'

const formRef = ref<FormInstance>()
const feedbackRef = ref<HTMLElement>()
const checking = ref(true)
const enabled = ref(false)
const statusError = ref('')
const error = ref('')
const loading = ref(false)
const sent = ref(false)
const cooldown = ref(0)
let timer: ReturnType<typeof setInterval> | undefined
const form = reactive({ email: '' })
const rules: FormRules = {
  email: [
    { required: true, message: '请输入账号绑定的邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: 'blur' },
    { max: 160, message: '邮箱不能超过 160 个字符', trigger: 'blur' },
  ],
}

async function loadStatus() {
  checking.value = true
  statusError.value = ''
  try {
    enabled.value = (await getPasswordRecoveryStatus()).enabled
  } catch (cause) {
    statusError.value = getApiErrorMessage(cause, '暂时无法读取密码找回状态，请重试。')
  } finally {
    checking.value = false
  }
}

async function submit() {
  if (!formRef.value || loading.value || cooldown.value > 0) return
  error.value = ''
  if (!(await formRef.value.validate().catch(() => false))) {
    error.value = '请检查下方邮箱字段。'
  } else {
    loading.value = true
    try {
      await requestPasswordReset(form.email)
      sent.value = true
      cooldown.value = 60
      clearInterval(timer)
      timer = setInterval(() => {
        cooldown.value = Math.max(0, cooldown.value - 1)
        if (cooldown.value === 0) clearInterval(timer)
      }, 1000)
    } catch (cause) {
      error.value = getApiErrorMessage(cause, '申请未完成，请稍后重试。')
    } finally {
      loading.value = false
    }
  }
  await nextTick()
  feedbackRef.value?.focus()
}

onMounted(loadStatus)
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <AuthLayout
    eyebrow="账户安全"
    title="找回密码"
    description="通过账号绑定的邮箱验证身份并设置新密码。"
  >
    <section class="recovery" :aria-busy="checking">
      <el-skeleton v-if="checking" :rows="3" animated aria-label="正在检查密码找回服务" />
      <div v-else-if="statusError" class="recovery__notice" role="alert">
        <p>{{ statusError }}</p>
        <el-button @click="loadStatus">重新加载</el-button>
      </div>
      <div v-else-if="!enabled" class="recovery__notice" role="status">
        <p>自助找回密码暂未开放，请联系管理员核实身份并恢复账号访问。</p>
        <p>请勿通过聊天发送个人密码。</p>
      </div>
      <template v-else>
        <div
          v-if="error || sent"
          ref="feedbackRef"
          class="recovery__notice"
          :class="{ 'recovery__notice--error': error }"
          :role="error ? 'alert' : 'status'"
          tabindex="-1"
        >
          <template v-if="error">{{ error }} <a href="#recovery-email">检查邮箱</a></template>
          <template v-else
            >如果该邮箱已绑定可用账号，您将收到重置邮件。请查看收件箱或垃圾邮件，并使用最新邮件中的链接。</template
          >
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          scroll-to-error
          @submit.prevent="submit"
        >
          <el-form-item label="绑定邮箱" prop="email">
            <el-input
              id="recovery-email"
              v-model.trim="form.email"
              type="email"
              autocomplete="email"
              maxlength="160"
              placeholder="请输入账号绑定的邮箱"
              :disabled="loading"
              aria-describedby="recovery-email-help"
            />
          </el-form-item>
          <p id="recovery-email-help" class="recovery__help">
            无法访问原邮箱？请联系管理员核实身份后更新绑定邮箱。
          </p>
          <el-button
            class="recovery__submit"
            type="primary"
            native-type="submit"
            :loading="loading"
            :disabled="cooldown > 0"
          >
            {{
              cooldown > 0
                ? `${cooldown} 秒后可重新发送`
                : sent
                  ? '重新发送重置邮件'
                  : '发送重置邮件'
            }}
          </el-button>
        </el-form>
      </template>
      <RouterLink class="recovery__back" to="/login">返回登录</RouterLink>
    </section>
  </AuthLayout>
</template>
