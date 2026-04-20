<template>
  <div class="password-input" :class="{ 'has-error': error }">
    <label class="input-label">{{ label }}</label>
    <div class="input-wrapper">
      <span class="input-icon">🔒</span>
      <input
        :type="visible ? 'text' : 'password'"
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        :placeholder="placeholder"
        class="input-field"
      />
      <button type="button" class="toggle-btn" @click="visible = !visible">
        {{ visible ? '🙈' : '👁' }}
      </button>
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  modelValue: string
  label?: string
  placeholder?: string
  error?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()

const visible = ref(false)
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.password-input {
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

    .input-icon {
      color: $brand-primary;
    }
  }
}

.has-error .input-wrapper {
  border-color: $border-error;

  &:focus-within {
    box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.25);
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

.toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  color: $text-placeholder;
  flex-shrink: 0;

  &:hover {
    color: $text-secondary;
  }
}

.error-text {
  font-size: 12px;
  color: $color-error;
  margin-top: 6px;
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
