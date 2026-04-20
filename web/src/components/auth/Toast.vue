<template>
  <div class="toast-container">
    <transition-group name="toast">
      <div
        v-for="item in toasts"
        :key="item.id"
        class="toast-item"
        :class="item.type"
      >
        <span class="toast-icon">{{ item.type === 'success' ? '✓' : '⚠' }}</span>
        <span class="toast-message">{{ item.message }}</span>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { toasts } from './toast-state'
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.toast-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: $radius-sm;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-size: 14px;
  white-space: nowrap;

  &.success {
    background: $color-success-bg;
    color: $color-success;
  }

  &.error {
    background: $color-error-bg;
    color: $color-error;
  }
}

.toast-enter-active {
  animation: slideDown 0.3s ease-in-out;
}

.toast-leave-active {
  animation: slideDown 0.3s ease-in-out reverse;
}

@keyframes slideDown {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>
