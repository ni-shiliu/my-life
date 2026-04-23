<template>
  <div :class="['chat-msg', `chat-msg--${msg.role.toLowerCase()}`]">
    <div v-if="msg.role === 'ASSISTANT'" class="chat-msg-avatar">
      <div class="agent-icon" :style="{ background: agentColor }">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2">
          <rect x="3" y="11" width="18" height="10" rx="2"/>
          <circle cx="12" cy="5" r="2"/>
          <path d="M12 7v4"/>
        </svg>
      </div>
    </div>
    <div class="chat-msg-bubble">
      <div class="chat-msg-text" v-html="renderContent(msg.content)"></div>
      <span v-if="msg.streaming" class="chat-msg-cursor">|</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/types/chat'

const props = defineProps<{
  msg: ChatMessage
  agentColor?: string
}>()

function renderContent(content: string): string {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br/>')
}
</script>

<style scoped lang="scss">
$surface: #ffffff;
$border: #e5e7eb;
$text-1: #111827;
$text-2: #6b7280;
$primary: #6366f1;
$primary-soft: rgba(99, 102, 241, 0.08);
$primary-text: #ffffff;
$radius-sm: 8px;
$radius-md: 12px;

.chat-msg {
  display: flex;
  gap: 8px;
  padding: 0 24px;
  margin-bottom: 16px;

  &--user {
    flex-direction: row-reverse;

    .chat-msg-bubble {
      background: $primary;
      color: $primary-text;
      border-radius: $radius-md $radius-sm $radius-md $radius-md;
      max-width: 75%;
    }
  }

  &--assistant {
    .chat-msg-bubble {
      background: $surface;
      border: 1px solid $border;
      color: $text-1;
      border-radius: $radius-sm $radius-md $radius-md $radius-md;
      max-width: 80%;
    }
  }

  &--system, &--tool_result {
    justify-content: center;

    .chat-msg-bubble {
      background: $primary-soft;
      color: $text-2;
      font-size: 12px;
      border-radius: 999px;
      padding: 4px 12px;
      max-width: 90%;
    }
  }
}

.chat-msg-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.agent-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-msg-bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-msg-text {
  white-space: pre-wrap;
}

.chat-msg-cursor {
  animation: blink 0.8s infinite;
  font-weight: bold;
  color: $primary;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
