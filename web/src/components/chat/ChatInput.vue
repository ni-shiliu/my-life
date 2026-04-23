<template>
  <div class="chat-input-area">
    <div class="chat-input-wrap">
      <input
        type="text"
        class="chat-input"
        :placeholder="placeholder"
        :disabled="disabled"
        v-model="text"
        @keydown.enter="handleSend"
      />
      <button class="chat-send-btn" :disabled="disabled || !text.trim()" @click="handleSend">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  disabled?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  send: [text: string]
}>()

const text = ref('')

function handleSend() {
  const trimmed = text.value.trim()
  if (!trimmed) return
  emit('send', trimmed)
  text.value = ''
}
</script>

<style scoped lang="scss">
$bg: #fafafa;
$surface: #ffffff;
$border: #e5e7eb;
$text-1: #111827;
$text-3: #9ca3af;
$primary: #6366f1;
$primary-text: #ffffff;
$radius-sm: 8px;
$transition: 150ms cubic-bezier(0.4, 0, 0.2, 1);

.chat-input-area {
  padding: 12px 24px 16px;
  border-top: 1px solid $border;
  background: $surface;
}

.chat-input-wrap {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  font-size: 14px;
  background: $bg;
  color: $text-1;
  outline: none;
  transition: border-color $transition;

  &::placeholder { color: $text-3; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
  &:focus { border-color: $primary; }
}

.chat-send-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: $radius-sm;
  background: $primary;
  color: $primary-text;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background $transition, transform $transition;
  flex-shrink: 0;

  &:hover:not(:disabled) {
    background: #4f46e5;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}
</style>
