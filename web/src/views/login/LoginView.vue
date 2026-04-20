<template>
  <AuthLayout>
    <div class="login-view">
      <h2 class="page-title">欢迎回来</h2>
      <p class="page-subtitle">登录您的 my-life 账号</p>

      <form @submit.prevent="handleSubmit">
        <PhoneInput v-model="phone" :error="phoneError" />
        <PasswordInput
          v-model="password"
          label="密码"
          placeholder="请输入密码"
          :error="passwordError"
        />
        <AppButton text="登录" loading-text="登录中..." :loading="loading" />
      </form>

      <p class="switch-link">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </p>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import PhoneInput from '@/components/auth/PhoneInput.vue'
import PasswordInput from '@/components/auth/PasswordInput.vue'
import AppButton from '@/components/common/AppButton.vue'
import { useAuth } from '@/composables/useAuth'
import { validatePhone } from '@/utils/phone'

const phone = ref('')
const password = ref('')
const phoneError = ref('')
const passwordError = ref('')

const { login, loading } = useAuth()

function handleSubmit() {
  phoneError.value = ''
  passwordError.value = ''

  if (!phone.value) {
    phoneError.value = '请输入手机号'
    return
  }
  if (!validatePhone(phone.value)) {
    phoneError.value = '请输入正确的手机号'
    return
  }
  if (!password.value) {
    passwordError.value = '请输入密码'
    return
  }

  login(phone.value, password.value)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.login-view {
  width: 100%;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 32px;
}

.switch-link {
  text-align: center;
  font-size: 14px;
  color: $text-secondary;
  margin-top: 24px;
}
</style>
