<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getRegistrationStatus, register } from '@/api/auth'
import { getApiErrorMessage } from '@/api/http'

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
  <main class="register-page">
    <el-card v-loading="checking" class="register-card">
      <h1>注册需求方账号</h1>
      <el-result v-if="!checking && !enabled" icon="warning" title="自助注册未开放">
        <template #extra>
          <el-button type="primary" @click="router.replace('/login')">返回登录</el-button>
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
          <el-button @click="router.push('/login')">返回登录</el-button>
          <el-button type="primary" native-type="submit" :loading="loading">注册</el-button>
        </div>
      </el-form>
    </el-card>
  </main>
</template>

<style scoped>
.register-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #eef1f5;
}

.register-card {
  width: min(720px, 100%);
}
.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 620px) {
  .register-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
