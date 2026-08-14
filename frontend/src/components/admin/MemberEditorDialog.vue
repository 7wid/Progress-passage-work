<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getInitialPasswordValidationMessage,
  INITIAL_PASSWORD_VALIDATION_MESSAGE,
} from './adminFormValidation'
import type { AdminMember, AdminMemberEditorValue, AdminMemberRole, SkillTag } from '@/types/admin'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    member: AdminMember | null
    skillTags: SkillTag[]
    submitting?: boolean
    serverErrors?: Record<string, string>
  }>(),
  { submitting: false, serverErrors: () => ({}) },
)

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  submit: [value: AdminMemberEditorValue]
}>()

const formRef = ref<FormInstance>()
const editing = computed(() => props.member !== null)
const form = reactive<AdminMemberEditorValue>({
  account: '',
  initialPassword: '',
  displayName: '',
  email: '',
  phone: '',
  department: '',
  role: 'MEMBER',
  skillIds: [],
  reason: '',
})

const rules = computed<FormRules<AdminMemberEditorValue>>(() => ({
  account: editing.value
    ? []
    : [
        { required: true, message: '请输入登录账号', trigger: 'blur' },
        {
          pattern: /^[A-Za-z0-9._-]{3,64}$/,
          message: '账号应为 3～64 位字母、数字、点、下划线或短横线',
          trigger: 'blur',
        },
      ],
  initialPassword: editing.value
    ? []
    : [
        { required: true, message: '请输入初始密码', trigger: 'blur' },
        {
          validator: (_rule, value, callback) => {
            if (!value) return callback()
            const message = getInitialPasswordValidationMessage(value)
            return message ? callback(new Error(message)) : callback()
          },
          message: INITIAL_PASSWORD_VALIDATION_MESSAGE,
          trigger: 'blur',
        },
      ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { min: 2, max: 80, message: '显示名称应为 2～80 个字符', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [
    {
      pattern: /^[0-9+()\-\s]{6,32}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
  department: [{ max: 160, message: '院系或部门不能超过 160 个字符', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  skillIds: [{ type: 'array', max: 20, message: '技能标签不能超过 20 个', trigger: 'change' }],
  reason: [
    { required: true, message: '请输入操作原因', trigger: 'blur' },
    { min: 5, max: 500, message: '操作原因应为 5～500 个字符', trigger: 'blur' },
  ],
}))

function resetForm(): void {
  const member = props.member
  form.account = member?.account ?? ''
  form.initialPassword = ''
  form.displayName = member?.displayName ?? ''
  form.email = member?.email ?? ''
  form.phone = member?.phone ?? ''
  form.department = member?.department ?? ''
  form.role = (member?.role ?? 'MEMBER') as AdminMemberRole
  form.skillIds = member?.skills.map((skill) => skill.id) ?? []
  form.reason = ''
  void nextTick(() => formRef.value?.clearValidate())
}

watch([() => props.modelValue, () => props.member], ([visible]) => {
  if (visible) resetForm()
})

function close(): void {
  if (props.submitting) return
  form.initialPassword = ''
  emit('update:modelValue', false)
}

async function submit(): Promise<void> {
  if (props.submitting) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', {
    account: form.account,
    initialPassword: form.initialPassword,
    displayName: form.displayName,
    email: form.email,
    phone: form.phone,
    department: form.department,
    role: form.role,
    skillIds: [...form.skillIds],
    reason: form.reason,
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="editing ? '编辑成员' : '新建成员'"
    width="680px"
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @close="close"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="登录账号" prop="account" :error="serverErrors.account">
          <el-input
            v-model="form.account"
            :disabled="editing"
            maxlength="64"
            autocomplete="off"
            placeholder="例如 member01"
          />
        </el-form-item>

        <el-form-item
          v-if="!editing"
          label="初始密码"
          prop="initialPassword"
          :error="serverErrors.initialPassword"
        >
          <el-input
            v-model="form.initialPassword"
            type="password"
            show-password
            maxlength="72"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="显示名称" prop="displayName" :error="serverErrors.displayName">
          <el-input v-model="form.displayName" maxlength="80" />
        </el-form-item>

        <el-form-item label="角色" prop="role" :error="serverErrors.role">
          <el-select v-model="form.role">
            <el-option label="技术组成员" value="MEMBER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>

        <el-form-item label="邮箱" prop="email" :error="serverErrors.email">
          <el-input v-model="form.email" maxlength="160" autocomplete="email" />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone" :error="serverErrors.phone">
          <el-input v-model="form.phone" maxlength="32" autocomplete="tel" />
        </el-form-item>

        <el-form-item label="院系或部门" prop="department" :error="serverErrors.department">
          <el-input v-model="form.department" maxlength="160" />
        </el-form-item>

        <el-form-item label="技能标签" prop="skillIds" :error="serverErrors.skillIds">
          <el-select v-model="form.skillIds" multiple collapse-tags collapse-tags-tooltip>
            <el-option
              v-for="skill in skillTags"
              :key="skill.id"
              :label="skill.name"
              :value="skill.id"
            />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="操作原因" prop="reason" :error="serverErrors.reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请说明创建账号或调整信息的原因"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ editing ? '保存修改' : '创建成员' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
