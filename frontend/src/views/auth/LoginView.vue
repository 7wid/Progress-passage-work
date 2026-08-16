<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRegistrationStatus } from '@/api/auth'
import { getLoginErrorMessage } from '@/api/http'
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
  <main class="login-page">
    <el-card class="login-card">
      <h1>技术需求管理系统</h1>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.account" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          class="login-card__submit"
        >
          登录
        </el-button>
        <el-button
          v-if="registrationEnabled"
          class="login-card__register"
          @click="router.push('/register')"
        >
          注册需求方账号
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #eef1f5;
}

.login-card {
  width: min(420px, 100%);
}

.login-card__submit,
.login-card__register {
  width: 100%;
  margin-left: 0;
}

.login-card__register {
  margin-top: 12px;
}
</style>
