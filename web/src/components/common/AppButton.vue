<template>
  <button
    class="app-button"
    :class="{ loading }"
    :disabled="disabled || loading"
    :type="type"
  >
    <span v-if="loading" class="spinner"></span>
    <span class="btn-text">{{ loading ? loadingText : text }}</span>
  </button>
</template>

<script setup lang="ts">
defineProps<{
  text: string
  loadingText?: string
  loading?: boolean
  disabled?: boolean
  type?: 'submit' | 'button' | 'reset'
}>()
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.app-button {
  width: 100%;
  height: $button-height;
  border: none;
  border-radius: $radius-sm;
  background: $brand-gradient;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: $hover-shadow;
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .app-button {
    height: 44px;
  }
}
</style>
