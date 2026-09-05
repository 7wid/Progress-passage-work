<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, KeyRound, LogIn, RefreshCw, UserPlus } from '@lucide/vue'
import { getRegistrationStatus, register } from '@/api/auth'
import { getApiErrorMessage, getApiFieldErrors } from '@/api/http'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { ACCOUNT_PATTERN, getRegistrationPasswordError } from '@/utils/authValidation'

const router = useRouter()
const formRef = ref<FormInstance>()
const errorSummaryRef = ref<HTMLElement>()
const loading = ref(false)
const checking = ref(true)
const enabled = ref(false)
const statusError = ref(false)
const submitError = ref('')
const validationError = ref('')
const serverFieldErrors = ref<Record<string, string>>({})
const emailSuffix = ref<string | null>(null)
const form = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  email: '',
  phone: '',
  department: '',
})

const rules: FormRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 64, message: '账号长度应为 3～64 个字符', trigger: 'blur' },
    {
      pattern: ACCOUNT_PATTERN,
      message: '账号须以字母或数字开头和结尾，可包含点、下划线和连字符',
      trigger: 'blur',
    },
  ],
  displayName: [{ required: true, message: '请输入姓名或称呼', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (emailSuffix.value && !value.trim().toLowerCase().endsWith(emailSuffix.value)) {
          callback(new Error(`请使用 ${emailSuffix.value} 后缀的邮箱`))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const error = getRegistrationPasswordError(value)
        if (error) {
          callback(new Error(error))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  phone: [
    {
      pattern: /^$|^\+?[0-9()\- ]{6,32}$/,
      message: '请输入有效手机号',
      trigger: 'blur',
    },
  ],
}

const emailHelp = computed(() =>
  emailSuffix.value ? `仅支持 ${emailSuffix.value} 后缀的邮箱。` : '用于账号识别和必要的服务联系。',
)

function clearFieldError(field: string) {
  if (serverFieldErrors.value[field]) {
    const nextErrors = { ...serverFieldErrors.value }
    delete nextErrors[field]
    serverFieldErrors.value = nextErrors
  }
  submitError.value = ''
  validationError.value = ''
}

async function submit() {
  if (!formRef.value || loading.value || !enabled.value) return
  submitError.value = ''
  validationError.value = ''
  serverFieldErrors.value = {}
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    validationError.value = '请检查下方标记的注册信息。'
    await nextTick()
    errorSummaryRef.value?.focus()
    return
  }
  loading.value = true
  try {
    await register(form)
    await router.replace({ path: '/login', query: { registered: '1' } })
  } catch (error) {
    serverFieldErrors.value = getApiFieldErrors(error)
    submitError.value = getApiErrorMessage(error, '账号注册失败，请稍后重试')
    await nextTick()
    errorSummaryRef.value?.focus()
  } finally {
    loading.value = false
  }
}

async function loadRegistrationStatus() {
  checking.value = true
  statusError.value = false
  try {
    const status = await getRegistrationStatus()
    enabled.value = status.enabled
    emailSuffix.value = status.emailSuffix
  } catch {
    enabled.value = false
    statusError.value = true
  } finally {
    checking.value = false
  }
}

onMounted(loadRegistrationStatus)
</script>

<template>
  <AuthLayout
    eyebrow="账号注册"
    title="创建个人账号"
    description="完善基本信息后，即可发起需求并持续跟踪进展。"
    wide
  >
    <div class="register-content" :aria-busy="checking">
      <div v-if="checking" class="registration-loading" role="status">
        <el-skeleton :rows="6" animated />
        <span class="sr-only">正在读取注册状态</span>
      </div>
      <el-result
        v-else-if="statusError"
        icon="error"
        title="暂时无法读取注册状态"
        sub-title="请检查网络连接后重试；系统不会在状态不明时提交注册信息。"
      >
        <template #extra>
          <div class="access-actions">
            <el-button @click="router.replace('/login')">
              <ArrowLeft :size="16" aria-hidden="true" />
              返回登录
            </el-button>
            <el-button type="primary" @click="loadRegistrationStatus">
              <RefreshCw :size="16" aria-hidden="true" />
              重新检查
            </el-button>
          </div>
        </template>
      </el-result>
      <el-result
        v-else-if="!enabled"
        icon="warning"
        title="当前采用受控开通方式"
        sub-title="为避免无效账号和信息泄露，当前环境暂未开放自行注册。"
      >
        <template #extra>
          <div class="access-guide">
            <span class="access-guide__icon" aria-hidden="true">
              <KeyRound :size="22" :stroke-width="1.8" />
            </span>
            <div>
              <strong>如何开始使用</strong>
              <p>
                请通过计算机技术组公布的服务渠道联系管理员，说明姓名、院系或组织及常用邮箱，并确认需求方账号的开通安排。
              </p>
              <p>获得账号后直接登录即可发起需求；不要通过聊天发送个人密码。</p>
            </div>
          </div>
          <div class="access-actions">
            <el-button @click="router.replace('/')">
              <ArrowLeft :size="16" aria-hidden="true" />
              返回首页
            </el-button>
            <el-button type="primary" @click="router.replace('/login')">
              <LogIn :size="16" aria-hidden="true" />
              已有账号，去登录
            </el-button>
          </div>
        </template>
      </el-result>
      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        status-icon
        scroll-to-error
        @submit.prevent="submit"
      >
        <div
          v-if="validationError || submitError"
          ref="errorSummaryRef"
          class="register-error"
          role="alert"
          tabindex="-1"
        >
          {{ validationError || submitError }}
        </div>
        <div class="register-grid">
          <el-form-item label="账号" prop="account" :error="serverFieldErrors.account">
            <el-input
              v-model="form.account"
              maxlength="64"
              autocomplete="username"
              placeholder="3～64 个字符"
              @input="clearFieldError('account')"
            />
          </el-form-item>
          <el-form-item
            label="姓名或称呼"
            prop="displayName"
            :error="serverFieldErrors.displayName"
          >
            <el-input
              v-model="form.displayName"
              maxlength="80"
              autocomplete="name"
              @input="clearFieldError('displayName')"
            />
          </el-form-item>
          <el-form-item label="邮箱" prop="email" :error="serverFieldErrors.email">
            <el-input
              v-model="form.email"
              maxlength="160"
              autocomplete="email"
              inputmode="email"
              @input="clearFieldError('email')"
            />
            <div class="field-help">{{ emailHelp }}</div>
          </el-form-item>
          <el-form-item label="手机号（可选）" prop="phone" :error="serverFieldErrors.phone">
            <el-input
              v-model="form.phone"
              maxlength="32"
              autocomplete="tel"
              inputmode="tel"
              @input="clearFieldError('phone')"
            />
          </el-form-item>
          <el-form-item
            label="院系或组织（可选）"
            prop="department"
            :error="serverFieldErrors.department"
          >
            <el-input
              v-model="form.department"
              maxlength="160"
              autocomplete="organization"
              @input="clearFieldError('department')"
            />
          </el-form-item>
        </div>
        <div class="register-grid">
          <el-form-item label="密码" prop="password" :error="serverFieldErrors.password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="new-password"
              maxlength="72"
              @input="clearFieldError('password')"
            />
            <div class="field-help">8～72 个字符，须同时包含字母和数字。</div>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              maxlength="72"
              @input="clearFieldError('confirmPassword')"
            />
          </el-form-item>
        </div>
        <div class="actions">
          <el-button @click="router.push('/login')">
            <ArrowLeft :size="16" aria-hidden="true" />
            返回登录
          </el-button>
          <el-button type="primary" native-type="submit" :loading="loading">
            <UserPlus :size="16" aria-hidden="true" />
            创建账号
          </el-button>
        </div>
      </el-form>
    </div>
  </AuthLayout>
</template>

<style scoped>
.register-content {
  min-height: 180px;
}

.registration-loading {
  min-height: 280px;
}

.register-error {
  margin-bottom: 20px;
  padding: 12px 14px;
  color: var(--color-danger);
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-md);
  font-size: 14px;
  line-height: 1.55;
}

.register-error:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.field-help {
  width: 100%;
  margin-top: 6px;
  color: var(--color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.access-guide {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  max-width: 560px;
  margin: 0 auto 18px;
  padding: 16px;
  color: var(--color-text-secondary);
  text-align: left;
  background: var(--color-primary-soft);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
}

.access-guide__icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  color: var(--color-primary-strong);
  background: var(--color-surface);
  border-radius: 50%;
}

.access-guide strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.access-guide p {
  margin: 5px 0 0;
  font-size: 13px;
  line-height: 1.65;
}

.access-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.access-actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-subtle);
}

.actions :deep(.el-button) {
  margin: 0;
}

.actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 620px) {
  .register-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .access-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .access-actions :deep(.el-button) {
    min-height: 44px;
    margin: 0;
  }
}
</style>
