<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  AtSign,
  Building2,
  Check,
  IdCard,
  KeyRound,
  LockKeyhole,
  Mail,
  Phone,
  Save,
  ShieldCheck,
  UserRound,
  UserRoundCog,
} from '@lucide/vue'
import { changePassword, getProfile, updateProfile } from '@/api/profile'
import { getApiErrorMessage } from '@/api/http'
import AppPageHeader from '@/components/common/AppPageHeader.vue'
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
  REQUESTER: '需求申请人',
  MEMBER: '服务团队成员',
  ADMIN: '管理员',
} as const

const profileInitials = computed(() => {
  const name = profile.value?.displayName.trim() || profile.value?.account.trim() || '用户'
  return [...name].slice(0, 2).join('').toUpperCase()
})

const passwordChecks = computed(() => [
  { label: '8～72 个字符', passed: passwordForm.newPassword.length >= 8 },
  { label: '包含字母', passed: /[A-Za-z]/.test(passwordForm.newPassword) },
  { label: '包含数字', passed: /\d/.test(passwordForm.newPassword) },
])

const passwordStrength = computed(() => passwordChecks.value.filter((item) => item.passed).length)

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
    <AppPageHeader
      title="个人设置"
      description="管理个人资料、联系信息与登录凭据。"
      eyebrow="ACCOUNT"
      :icon="UserRoundCog"
      tone="purple"
    />

    <div class="settings-grid">
      <el-card v-loading="loading" class="settings-card settings-card--profile">
        <template #header>
          <div class="section-heading">
            <span class="section-heading__icon section-heading__icon--blue" aria-hidden="true">
              <IdCard :size="18" />
            </span>
            <div>
              <strong>个人资料</strong>
              <small>用于需求协作与站内身份展示</small>
            </div>
          </div>
        </template>

        <div class="profile-summary">
          <span class="profile-avatar" aria-hidden="true">{{ profileInitials }}</span>
          <div class="profile-summary__copy">
            <strong>{{ profile?.displayName || '正在加载资料' }}</strong>
            <span>{{ profile?.department || '暂未填写院系或组织' }}</span>
          </div>
          <el-tag v-if="profile" type="primary" effect="light">
            {{ roleLabels[profile.role] }}
          </el-tag>
        </div>

        <div class="account-facts" aria-label="账号信息">
          <div>
            <span class="account-facts__icon" aria-hidden="true"><AtSign :size="17" /></span>
            <span
              ><small>登录账号</small><strong>{{ profile?.account || '—' }}</strong></span
            >
          </div>
          <div>
            <span class="account-facts__icon" aria-hidden="true"><ShieldCheck :size="17" /></span>
            <span
              ><small>账号角色</small
              ><strong>{{ profile ? roleLabels[profile.role] : '—' }}</strong></span
            >
          </div>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          class="settings-form"
          @submit.prevent="saveProfile"
        >
          <el-form-item label="姓名或称呼" prop="displayName">
            <el-input
              v-model="profileForm.displayName"
              maxlength="80"
              placeholder="请输入姓名或称呼"
            >
              <template #prefix><UserRound :size="16" aria-hidden="true" /></template>
            </el-input>
          </el-form-item>
          <div class="contact-grid">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" maxlength="160" placeholder="name@example.com">
                <template #prefix><Mail :size="16" aria-hidden="true" /></template>
              </el-input>
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" maxlength="32" placeholder="请输入手机号">
                <template #prefix><Phone :size="16" aria-hidden="true" /></template>
              </el-input>
            </el-form-item>
          </div>
          <el-form-item label="院系或组织" prop="department">
            <el-input
              v-model="profileForm.department"
              maxlength="160"
              placeholder="请输入院系或组织"
            >
              <template #prefix><Building2 :size="16" aria-hidden="true" /></template>
            </el-input>
          </el-form-item>
          <div class="actions">
            <el-button type="primary" native-type="submit" :loading="savingProfile">
              <Save :size="16" aria-hidden="true" />
              保存资料
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card class="settings-card settings-card--security">
        <template #header>
          <div class="section-heading">
            <span class="section-heading__icon section-heading__icon--green" aria-hidden="true">
              <ShieldCheck :size="18" />
            </span>
            <div>
              <strong>登录安全</strong>
              <small>更新当前账号的登录密码</small>
            </div>
          </div>
        </template>

        <div class="security-status">
          <span aria-hidden="true"><LockKeyhole :size="19" /></span>
          <div>
            <strong>密码保护已启用</strong>
            <small>修改后其他登录会话将自动失效</small>
          </div>
        </div>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="settings-form"
          @submit.prevent="savePassword"
        >
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入当前密码"
            >
              <template #prefix><KeyRound :size="16" aria-hidden="true" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入新密码"
            >
              <template #prefix><LockKeyhole :size="16" aria-hidden="true" /></template>
            </el-input>
          </el-form-item>
          <div
            class="password-meter"
            :aria-label="`密码要求已满足 ${passwordStrength} 项，共 3 项`"
          >
            <span
              v-for="index in 3"
              :key="index"
              :class="{ 'is-active': passwordStrength >= index }"
            />
          </div>
          <ul class="password-checks">
            <li
              v-for="item in passwordChecks"
              :key="item.label"
              :class="{ 'is-passed': item.passed }"
            >
              <Check :size="13" aria-hidden="true" />{{ item.label }}
            </li>
          </ul>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请再次输入新密码"
            >
              <template #prefix><LockKeyhole :size="16" aria-hidden="true" /></template>
            </el-input>
          </el-form-item>
          <div class="actions">
            <el-button type="primary" native-type="submit" :loading="changingPassword">
              <ShieldCheck :size="16" aria-hidden="true" />
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
  grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.settings-card {
  min-width: 0;
  overflow: hidden;
}

.settings-card :deep(.el-card__header) {
  background: #fbfcfe;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 11px;
}

.section-heading__icon,
.account-facts__icon {
  display: inline-grid;
  place-items: center;
  border-radius: var(--radius-md);
}

.section-heading__icon {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
}

.section-heading__icon--blue {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.section-heading__icon--green {
  color: var(--color-success);
  background: #ecfdf5;
}

.section-heading > div {
  display: grid;
  gap: 1px;
}

.section-heading strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.section-heading small,
.profile-summary__copy span,
.account-facts small,
.security-status small {
  color: var(--color-text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 15px;
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
}

.profile-avatar {
  display: inline-grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  color: var(--color-on-primary);
  background: var(--color-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-brand);
  font-size: 15px;
  font-weight: 700;
}

.profile-summary__copy {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 2px;
}

.profile-summary__copy strong,
.profile-summary__copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-summary__copy strong {
  color: var(--color-text-primary);
  font-size: 16px;
}

.account-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  margin: 16px 0 20px;
  overflow: hidden;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-border-subtle);
}

.account-facts > div {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #fbfcfe;
}

.account-facts__icon {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  color: var(--color-text-secondary);
  background: var(--color-surface-secondary);
}

.account-facts > div > span:last-child {
  display: grid;
  min-width: 0;
  gap: 1px;
}

.account-facts strong {
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.security-status {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 20px;
  padding: 13px;
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-md);
}

.security-status > span {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  background: #d1fae5;
  border-radius: var(--radius-md);
}

.security-status > div {
  display: grid;
  gap: 1px;
}

.security-status strong {
  font-size: 13px;
}

.password-meter {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 5px;
  margin: -8px 0 8px;
}

.password-meter span {
  height: 4px;
  border-radius: 2px;
  background: var(--color-border);
  transition: background-color var(--motion-base) ease;
}

.password-meter span.is-active {
  background: var(--color-success);
}

.password-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin: 0 0 18px;
  padding: 0;
  color: var(--color-text-tertiary);
  list-style: none;
  font-size: 11px;
}

.password-checks li {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.password-checks li.is-passed {
  color: var(--color-success);
}

.actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-subtle);
}

.actions :deep(.el-button span) {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

@media (max-width: 900px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .contact-grid,
  .account-facts {
    grid-template-columns: 1fr;
  }

  .profile-summary {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .profile-summary__copy {
    min-width: calc(100% - 66px);
  }
}
</style>
