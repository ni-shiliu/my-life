<template>
  <div class="home-view">
    <div class="home-content">
      <h1 class="welcome-title">欢迎回来，{{ userStore.userInfo?.nickName || '用户' }}</h1>
      <p class="welcome-subtitle">my-life AI 智能生活助手</p>
      <button class="logout-btn" @click="logout">退出登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'

const userStore = useUserStore()
const { logout } = useAuth()
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.home-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.home-content {
  text-align: center;
}

.welcome-title {
  font-size: 28px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 12px;
}

.welcome-subtitle {
  font-size: 16px;
  color: $text-secondary;
  margin-bottom: 32px;
}

.logout-btn {
  padding: 10px 32px;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  background: #fff;
  color: $text-secondary;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: $color-error;
    color: $color-error;
  }
}
</style>
