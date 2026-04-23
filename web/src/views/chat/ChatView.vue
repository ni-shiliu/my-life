<template>
  <div class="chat-view">
    <!-- Header -->
    <header class="chat-header">
      <div class="header-inner">
        <div class="header-left">
          <button class="btn-icon" @click="goBack" aria-label="返回">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
          </button>
          <h1 class="header-title">{{ agentName }}</h1>
        </div>
        <div class="header-right">
          <span v-if="!isLoggedIn" class="guest-badge">游客模式</span>
          <button v-else class="btn-ghost btn-sm" @click="goEdit">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            编辑
          </button>
        </div>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="loading-full">
      <div class="spinner"></div>
    </div>

    <!-- Chat -->
    <div v-else class="chat-body">
      <ChatPanel
        :agent-uuid="agentUuid"
        :agent-name="agentName"
        :agent-color="agentColor"
        :scene="'PUBLISHED'"
        empty-text="开始与AI老师对话吧"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAgentApi } from '@/api/agent'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import { showToast } from '@/components/auth/toast-state'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const agentUuid = ref('')
const agentName = ref('')
const agentColor = ref('#6366f1')

const isLoggedIn = computed(() => userStore.isLoggedIn)

onMounted(async () => {
  agentUuid.value = route.params.agentId as string
  if (!agentUuid.value) {
    showToast('智能体不存在', 'error')
    router.push('/')
    return
  }

  try {
    const { data } = await getAgentApi(agentUuid.value)
    if (data?.data) {
      agentName.value = data.data.name
      agentColor.value = data.data.color || '#6366f1'
    }
  } catch {
    // Guest might not have access to getAgent — use defaults
    agentName.value = 'AI助手'
  } finally {
    loading.value = false
  }
})

function goBack() {
  router.push('/')
}

function goEdit() {
  router.push(`/agent/${agentUuid.value}`)
}
</script>

<style scoped lang="scss">
$bg: #fafafa;
$surface: #ffffff;
$border: #e5e7eb;
$text-1: #111827;
$text-2: #6b7280;
$primary: #6366f1;
$primary-soft: rgba(99, 102, 241, 0.08);
$primary-text: #ffffff;
$radius-sm: 8px;
$transition: 150ms cubic-bezier(0.4, 0, 0.2, 1);

.chat-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg;
  color: $text-1;
}

.chat-header {
  background: $surface;
  border-bottom: 1px solid $border;
  height: 56px;
  flex-shrink: 0;
}

.header-inner {
  max-width: 900px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.guest-badge {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  background: $primary-soft;
  color: $primary;
  font-weight: 500;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-2;
  cursor: pointer;
  transition: border-color $transition, color $transition;

  &:hover {
    border-color: $primary;
    color: $primary;
  }
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-1;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color $transition, color $transition, background $transition;

  &:hover {
    border-color: $primary;
    color: $primary;
    background: $primary-soft;
  }
}

.loading-full {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid $border;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.chat-body {
  flex: 1;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  overflow: hidden;
}
</style>
