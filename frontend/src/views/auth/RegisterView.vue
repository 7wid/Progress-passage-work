<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, KeyRound, LogIn, UserPlus } from '@lucide/vue'
import { getRegistrationStatus, register } from '@/api/auth'
import { getApiErrorMessage } from '@/api/http'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const checking = ref(true)
const enabled = ref(false)
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
    {
      pattern: /^[A-Za-z0-9._-]+$/,
      message: '账号只能包含字母、数字、点、下划线和连字符',
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
    { min: 8, max: 72, message: '密码长度应为 8～72 个字符', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) {
          callback(new Error('密码必须同时包含字母和数字'))
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
}

async function submit() {
  if (!formRef.value || loading.value || !enabled.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register(form)
    ElMessage.success('账号注册成功，请登录')
    await router.replace('/login')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '账号注册失败'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const status = await getRegistrationStatus()
    enabled.value = status.enabled
    emailSuffix.value = status.emailSuffix
  } catch {
    enabled.value = false
  } finally {
    checking.value = false
  }
})
</script>

<template>
  <AuthLayout
    eyebrow="账号注册"
    title="创建个人账号"
    description="完善基本信息后，即可发起需求并持续跟踪进展。"
    wide
  >
    <div v-loading="checking" class="register-content">
      <el-result
        v-if="!checking && !enabled"
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
        @submit.prevent="submit"
      >
        <div class="register-grid">
          <el-form-item label="账号" prop="account">
            <el-input v-model="form.account" maxlength="64" autocomplete="username" />
          </el-form-item>
          <el-form-item label="姓名或称呼" prop="displayName">
            <el-input v-model="form.displayName" maxlength="80" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="160" autocomplete="email" />
          </el-form-item>
          <el-form-item label="手机号（可选）">
            <el-input v-model="form.phone" maxlength="32" autocomplete="tel" />
          </el-form-item>
          <el-form-item label="院系或组织（可选）">
            <el-input v-model="form.department" maxlength="160" />
          </el-form-item>
        </div>
        <div class="register-grid">
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
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
