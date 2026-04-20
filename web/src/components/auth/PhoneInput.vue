<template>
  <div class="phone-input" :class="{ 'has-error': error }">
    <label class="input-label">手机号</label>
    <div class="input-wrapper">
      <span class="input-icon">📱</span>
      <input
        type="tel"
        maxlength="11"
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        placeholder="请输入手机号"
        class="input-field"
      />
    </div>
    <p v-if="error" class="error-text">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  error?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.phone-input {
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
