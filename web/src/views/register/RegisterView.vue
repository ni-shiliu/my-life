<template>
  <AuthLayout>
    <div class="register-view">
      <h2 class="page-title">创建账号</h2>
      <p class="page-subtitle">开始您的 my-life 之旅</p>

      <form @submit.prevent="handleSubmit">
        <PhoneInput v-model="phone" :error="phoneError" />
        <PasswordInput
          v-model="password"
          label="密码"
          placeholder="请输入密码"
        />
        <PasswordStrength :password="password" />
        <PasswordInput
          v-model="confirmPassword"
          label="确认密码"
          placeholder="请再次输入密码"
          :error="confirmPasswordError"
        />
        <div class="nickname-field">
          <label class="input-label">昵称（选填）</label>
          <div class="input-wrapper">
            <span class="input-icon">😊</span>
            <input
              v-model="nickName"
              type="text"
              placeholder="请输入昵称"
              maxlength="20"
              class="input-field"
            />
          </div>
        </div>
        <AppButton text="注册" loading-text="注册中..." :loading="loading" />
      </form>

      <p class="switch-link">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </p>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import PhoneInput from '@/components/auth/PhoneInput.vue'
import PasswordInput from '@/components/auth/PasswordInput.vue'
import PasswordStrength from '@/components/auth/PasswordStrength.vue'
import AppButton from '@/components/common/AppButton.vue'
import { useAuth } from '@/composables/useAuth'
import { usePasswordRule } from '@/composables/usePasswordRule'
import { validatePhone } from '@/utils/phone'

const phone = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickName = ref('')
const phoneError = ref('')
const confirmPasswordError = ref('')

const { register, loading } = useAuth()
const { allMet } = usePasswordRule(password)

watch(confirmPassword, (val) => {
  if (val && val !== password.value) {
    confirmPasswordError.value = '两次密码输入不一致'
  } else {
    confirmPasswordError.value = ''
  }
})

function handleSubmit() {
  phoneError.value = ''
  confirmPasswordError.value = ''

  if (!phone.value) {
    phoneError.value = '请输入手机号'
    return
  }
  if (!validatePhone(phone.value)) {
    phoneError.value = '请输入正确的手机号'
    return
  }
  if (!allMet.value) {
    return
  }
  if (confirmPassword.value !== password.value) {
    confirmPasswordError.value = '两次密码输入不一致'
    return
  }

  register(phone.value, password.value, nickName.value || undefined)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.register-view {
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

.nickname-field {
  margin-bottom: 20px;
}

.input-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 8px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  height: $input-height;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  padding: 0 12px;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:focus-within {
    border-color: $border-focus;
    box-shadow: $focus-shadow;
  }
}

.input-icon {
  font-size: 16px;
  margin-right: 8px;
  color: $text-placeholder;
  flex-shrink: 0;
}

.input-field {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: $text-primary;
  background: transparent;

  &::placeholder {
    color: $text-placeholder;
  }
}

.switch-link {
  text-align: center;
  font-size: 14px;
  color: $text-secondary;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .input-wrapper {
    height: 44px;
  }

  .input-field {
    font-size: 16px;
  }
}
</style>
