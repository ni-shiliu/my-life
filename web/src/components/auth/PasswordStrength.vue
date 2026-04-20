<template>
  <div class="password-strength" v-if="password.length > 0">
    <div
      v-for="rule in rules"
      :key="rule.label"
      class="rule-item"
      :class="rule.met ? 'met' : 'unmet'"
    >
      <span class="rule-icon">{{ rule.met ? '✓' : '✗' }}</span>
      <span class="rule-label">{{ rule.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { toRef } from 'vue'
import { usePasswordRule } from '@/composables/usePasswordRule'

const props = defineProps<{
  password: string
}>()

const { rules } = usePasswordRule(toRef(props, 'password'))
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;

.password-strength {
  margin: -8px 0 20px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: $radius-sm;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding: 2px 0;

  &.met {
    color: $color-success;
  }

  &.unmet {
    color: $color-error;
  }
}

.rule-icon {
  width: 14px;
  text-align: center;
}
</style>
