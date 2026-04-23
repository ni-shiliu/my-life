<template>
  <div class="chat-panel">
    <!-- Header -->
    <div class="chat-header">
      <div class="chat-header-left">
        <div class="chat-agent-icon" :style="{ background: agentColor || '#6366f1' }">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.5">
            <rect x="3" y="11" width="18" height="10" rx="2"/>
            <circle cx="12" cy="5" r="2"/>
            <path d="M12 7v4"/>
            <circle cx="8" cy="16" r="1" fill="#fff"/>
            <circle cx="16" cy="16" r="1" fill="#fff"/>
          </svg>
        </div>
        <div>
          <h3 class="chat-agent-name">{{ agentName || '未命名智能体' }}</h3>
          <p class="chat-agent-status">{{ agentName || '智能体' }}</p>
        </div>
      </div>
      <div class="chat-header-actions">
        <button class="btn-action" @click="menuOpen = !menuOpen" aria-label="更多操作">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <circle cx="12" cy="5" r="2"/>
            <circle cx="12" cy="12" r="2"/>
            <circle cx="12" cy="19" r="2"/>
          </svg>
        </button>
        <div v-if="menuOpen" class="action-menu">
          <button class="action-item" @click="handleClear">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
            清空聊天记录
          </button>
        </div>
      </div>
    </div>
    <div v-if="menuOpen" class="action-menu-overlay" @click="menuOpen = false" />

    <!-- Messages -->
    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="chat-empty">
        <div class="chat-empty-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <p class="chat-empty-text">{{ emptyText }}</p>
      </div>

      <template v-else>
        <ChatMessage
          v-for="(msg, i) in messages"
          :key="i"
          :msg="msg"
          :agent-color="agentColor"
        />
      </template>
    </div>

    <!-- Error -->
    <div v-if="error" class="chat-error">
      <span>{{ error }}</span>
      <button class="chat-error-close" @click="error = ''">&times;</button>
    </div>

    <!-- Input -->
    <ChatInput
      :disabled="streaming || !agentUuid"
      :placeholder="streaming ? 'AI正在回复...' : '发送消息...'"
      @send="sendMessage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, toRef } from 'vue'
import { useChat } from '@/composables/useChat'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'

const props = defineProps<{
  agentUuid?: string
  agentName?: string
  agentColor?: string
  emptyText?: string
  scene?: 'EDIT' | 'PUBLISHED'
}>()

const sceneRef = computed(() => props.scene)

const {
  messages,
  streaming,
  error,
  sendMessage: chatSend,
  loadHistory,
  clearHistory,
} = useChat(toRef(props, 'agentUuid'), sceneRef)

const messagesRef = ref<HTMLElement | null>(null)
const menuOpen = ref(false)

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(messages, () => {
  scrollToBottom()
}, { deep: true })

watch(() => props.agentUuid, (newUuid) => {
  if (newUuid) {
    loadHistory()
  }
})

onMounted(() => {
  if (props.agentUuid) {
    loadHistory()
  }
})

function sendMessage(text: string) {
  chatSend(text)
  scrollToBottom()
}

async function handleClear() {
  menuOpen.value = false
  await clearHistory()
}
</script>

<style scoped lang="scss">
$bg: #fafafa;
$surface: #ffffff;
$border: #e5e7eb;
$text-1: #111827;
$text-2: #6b7280;
$text-3: #9ca3af;
$primary: #6366f1;
$primary-soft: rgba(99, 102, 241, 0.08);
$radius-sm: 8px;
$radius-md: 12px;

.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: $bg;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid $border;
  background: $surface;
  position: relative;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.chat-header-actions {
  position: relative;
  flex-shrink: 0;
}

.btn-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: $radius-sm;
  background: transparent;
  color: $text-3;
  cursor: pointer;
  transition: background 150ms, color 150ms;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
    color: $text-2;
  }
}

.action-menu {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 4px;
  background: $surface;
  border: 1px solid $border;
  border-radius: $radius-sm;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  min-width: 160px;
  z-index: 100;
  padding: 4px 0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: $text-1;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 150ms;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
  }
}

.action-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 99;
}

.chat-agent-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-agent-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-1;
}

.chat-agent-status {
  font-size: 12px;
  color: $text-3;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;
}

.chat-empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: $primary-soft;
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.chat-empty-text {
  font-size: 14px;
  color: $text-2;
}

.chat-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
  font-size: 13px;
}

.chat-error-close {
  border: none;
  background: none;
  color: #dc2626;
  cursor: pointer;
  font-size: 18px;
  padding: 0 4px;
}
</style>
