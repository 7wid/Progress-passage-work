<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { changePassword, getProfile, updateProfile } from '@/api/profile'
import { getApiErrorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { UpdateProfileInput, UserProfile } from '@/types/profile'

const authStore = useAuthStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const loading = ref(false)
const savingProfile = ref(false)
const changingPassword = ref(false)
const profile = ref<UserProfile | null>(null)

const profileForm = reactive<UpdateProfileInput>({
  displayName: '',
  email: '',
  phone: '',
  department: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: FormRules = {
  displayName: [
    { required: true, message: '请输入姓名或称呼', trigger: 'blur' },
    { max: 80, message: '姓名或称呼不能超过 80 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' }],
  phone: [{ max: 32, message: '手机号不能超过 32 个字符', trigger: 'blur' }],
  department: [{ max: 160, message: '院系或组织不能超过 160 个字符', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 72, message: '新密码长度应为 8～72 个字符', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) {
          callback(new Error('新密码必须同时包含字母和数字'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const roleLabels = {
  REQUESTER: '需求方',
  MEMBER: '技术组成员',
  ADMIN: '管理员',
} as const

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getProfile()
    Object.assign(profileForm, {
      displayName: profile.value.displayName,
      email: profile.value.email ?? '',
      phone: profile.value.phone ?? '',
      department: profile.value.department ?? '',
    })
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '个人资料加载失败'))
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!profileFormRef.value || savingProfile.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    profile.value = await updateProfile(profileForm)
    authStore.updateDisplayName(profile.value.displayName)
    ElMessage.success('个人资料已更新')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '个人资料更新失败'))
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!passwordFormRef.value || changingPassword.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPassword.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
    ElMessage.success('密码已修改，其他登录会话已失效')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '密码修改失败'))
  } finally {
    changingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <section class="page">
    <div class="page__header"><h2>个人设置</h2></div>

    <div class="settings-grid">
      <el-card header="个人资料" v-loading="loading">
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          @submit.prevent="saveProfile"
        >
          <div class="readonly-grid">
            <el-form-item label="账号">
              <el-input :model-value="profile?.account ?? ''" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-input :model-value="profile ? roleLabels[profile.role] : ''" disabled />
            </el-form-item>
          </div>
          <el-form-item label="姓名或称呼" prop="displayName">
            <el-input v-model="profileForm.displayName" maxlength="80" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" maxlength="160" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" maxlength="32" />
          </el-form-item>
          <el-form-item label="院系或组织" prop="department">
            <el-input v-model="profileForm.department" maxlength="160" />
          </el-form-item>
          <div class="actions">
            <el-button type="primary" native-type="submit" :loading="savingProfile">
              保存资料
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card header="修改密码">
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          @submit.prevent="savePassword"
        >
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
          <div class="actions">
            <el-button type="primary" native-type="submit" :loading="changingPassword">
              修改密码
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
  gap: 16px;
  align-items: start;
}

.readonly-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .readonly-grid {
    grid-template-columns: 1fr;
  }
}
</style>
