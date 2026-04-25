<template>
  <div class="chat-view">
    <!-- Global Header (matches HomeView) -->
    <header class="global-header">
      <div class="global-header-inner">
        <div class="global-header-left" @click="goBack">
          <span class="logo-mark">M</span>
          <span class="logo-text">my-life</span>
        </div>
        <div class="global-header-right">
          <span v-if="!isLoggedIn" class="guest-badge">游客</span>
          <div v-else class="user-pill" @click="handleLogout">
            <div class="user-avatar">{{ userAvatar }}</div>
            <span class="user-name">{{ userStore.userInfo?.nickName || '用户' }}</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="logout-icon">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </div>
        </div>
      </div>
    </header>

    <!-- Body: Sidebar + Chat Main -->
    <div class="chat-body-wrap">
    <!-- Sidebar -->
    <aside v-if="isLoggedIn" class="sidebar">
      <div class="sidebar-list">
        <button
          v-for="a in availableAgents"
          :key="a.uuid"
          :class="['sidebar-item', { active: a.uuid === agentUuid }]"
          @click="switchAgent(a)"
        >
          <div class="sidebar-item-icon" :style="{ background: a.color || '#6366f1' }">
            <component :is="renderIcon(a.iconIndex ?? 0, 14)" />
          </div>
          <div class="sidebar-item-body">
            <span class="sidebar-item-name">{{ a.name }}</span>
            <span v-if="a.owned" class="sidebar-item-tag">我的</span>
          </div>
          <button v-if="a.added && !a.owned" class="sidebar-item-remove" @click.stop="removeAddedAgent(a)" title="移除">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </button>
      </div>
    </aside>

    <!-- Main -->
    <div class="chat-main">
    <header class="chat-header">
      <div class="header-inner">
        <div class="header-agent">
          <div class="header-agent-icon" :style="{ background: agentColor }">
            <component :is="renderIcon(agentIconIndex, 14)" />
          </div>
          <span class="header-agent-name">{{ agentName }}</span>
        </div>
        <div class="header-actions">
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
            <button class="action-item" @click="handleClearMemory">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2a10 10 0 1 0 10 10"/>
                <path d="M12 2v10l6.93 4"/>
              </svg>
              清空记忆
            </button>
          </div>
        </div>
        <div v-if="menuOpen" class="action-menu-overlay" @click="menuOpen = false" />
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="loading-full">
      <div class="spinner"></div>
    </div>

    <template v-else>
      <!-- Welcome area (no messages) -->
      <div v-if="messages.length === 0" class="chat-body">
        <div class="welcome">
          <div class="welcome-icon" :style="{ background: agentColor }">
            <component :is="renderIcon(agentIconIndex, 32)" />
          </div>
          <h2 class="welcome-name">{{ agentName }}</h2>
          <p v-if="agentDesc" class="welcome-desc">{{ agentDesc }}</p>
          <div class="welcome-suggestions">
            <p class="suggestions-label">试试对话</p>
            <button
              v-for="s in suggestions"
              :key="s"
              class="suggestion-chip"
              @click="sendMessage(s)"
            >{{ s }}</button>
          </div>
        </div>

        <ChatInput
          :disabled="streaming || !agentUuid"
          :placeholder="streaming ? 'AI正在回复...' : '发送消息...'"
          @send="sendMessage"
        />
      </div>

      <!-- Chat messages -->
      <div v-else class="chat-body">
        <div class="messages-area" ref="messagesRef">
          <template v-for="(msg, i) in messages" :key="i">
            <div v-if="showTimeDivider(i)" class="time-divider">
              <span>{{ formatTime(msg.gmtCreated) }}</span>
            </div>
            <ChatMessage v-if="!(msg.role === 'ASSISTANT' && msg.streaming && !msg.content)" :msg="msg" :agent-color="agentColor" :agent-icon-index="agentIconIndex" :user-initial="userAvatar" />
          </template>

          <!-- Typing indicator -->
          <div v-if="streaming && isLastAssistantStreaming" class="typing-indicator">
            <div class="typing-icon" :style="{ background: agentColor }">
              <component :is="renderIcon(agentIconIndex, 14)" />
            </div>
            <div class="typing-dots">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>

        <!-- Error -->
        <div v-if="error" class="chat-error">
          <span>{{ error }}</span>
          <button class="chat-error-close" @click="error = ''">&times;</button>
        </div>

        <ChatInput
          :disabled="streaming || !agentUuid"
          :placeholder="streaming ? 'AI正在回复...' : '发送消息...'"
          @send="sendMessage"
        />
      </div>
    </template>
    </div>
    </div><!-- /chat-body-wrap -->
    <Teleport to="body">
      <Transition name="overlay">
        <div v-if="showClearMemoryDialog" class="overlay" @click="showClearMemoryDialog = false">
          <div class="dialog" @click.stop>
            <div class="dialog-head">
              <h3 class="dialog-title">清空记忆</h3>
            </div>
            <div class="dialog-body">
              <p class="dialog-msg">确定清空AI的记忆吗？清空后AI将不再记住之前的对话内容，但聊天记录仍会保留。</p>
            </div>
            <div class="dialog-foot">
              <button class="btn-ghost" @click="showClearMemoryDialog = false">取消</button>
              <button class="btn-danger" @click="doClearMemory">清空</button>
            </div>
          </div>
        </div>
      </Transition>
      <Transition name="overlay">
        <div v-if="showRemoveDialog" class="overlay" @click="showRemoveDialog = false">
          <div class="dialog" @click.stop>
            <div class="dialog-head">
              <h3 class="dialog-title">移除智能体</h3>
            </div>
            <div class="dialog-body">
              <p class="dialog-msg">确定移除「{{ removeTarget?.name }}」吗？移除后可重新从广场添加。</p>
            </div>
            <div class="dialog-foot">
              <button class="btn-ghost" @click="showRemoveDialog = false">取消</button>
              <button class="btn-danger" @click="doRemove">移除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useChat } from '@/composables/useChat'
import { getAgentApi, listAvailableAgentApi, removeAgentApi } from '@/api/agent'
import type { AgentDTO } from '@/types/agent'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { showToast } from '@/components/auth/toast-state'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { logout } = useAuth()

const svgProps = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.5 }
const renderIcon = (index: number, size = 22) => {
  const s = size
  const icons: Record<number, () => ReturnType<typeof h>> = {
    0: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('rect', { x: 3, y: 11, width: 18, height: 10, rx: 2 }),
      h('circle', { cx: 12, cy: 5, r: 2 }),
      h('path', { d: 'M12 7v4' }),
      h('circle', { cx: 8, cy: 16, r: 1, fill: 'currentColor' }),
      h('circle', { cx: 16, cy: 16, r: 1, fill: 'currentColor' })
    ]),
    1: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('path', { d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: 9, cy: 7, r: 4 }),
      h('path', { d: 'M23 21v-2a4 4 0 0 0-3-3.87' }),
      h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' })
    ]),
    2: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('polyline', { points: '16 18 22 12 16 6' }),
      h('polyline', { points: '8 6 2 12 8 18' })
    ]),
    3: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('path', { d: 'M12 20h9' }),
      h('path', { d: 'M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z' })
    ]),
    4: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('line', { x1: 18, y1: 20, x2: 18, y2: 10 }),
      h('line', { x1: 12, y1: 20, x2: 12, y2: 4 }),
      h('line', { x1: 6, y1: 20, x2: 6, y2: 14 })
    ]),
    5: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('circle', { cx: 12, cy: 12, r: 10 }),
      h('line', { x1: 2, y1: 12, x2: 22, y2: 12 }),
      h('path', { d: 'M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z' })
    ]),
    6: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('path', { d: 'M9 3h6' }),
      h('path', { d: 'M12 3v6l-5 9h10l-5-9V3' })
    ]),
    7: () => h('svg', { width: s, height: s, ...svgProps }, [
      h('path', { d: 'M22 10v6M2 10l10-5 10 5-10 5z' }),
      h('path', { d: 'M6 12v5c0 1.66 2.69 3 6 3s6-1.34 6-3v-5' })
    ])
  }
  return (icons[index] ?? icons[0])()
}

const loading = ref(true)
const menuOpen = ref(false)
const showRemoveDialog = ref(false)
const showClearMemoryDialog = ref(false)
const removeTarget = ref<AgentDTO | null>(null)
const agentUuid = ref('')
const availableAgents = ref<AgentDTO[]>([])
const agentName = ref('AI助手')
const agentDesc = ref('')
const agentColor = ref('#6366f1')
const agentIconIndex = ref(0)
const messagesRef = ref<HTMLElement | null>(null)

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userAvatar = computed(() => {
  const name = userStore.userInfo?.nickName || 'U'
  return name.charAt(0).toUpperCase()
})
const agentUuidRef = computed(() => agentUuid.value)
const sceneRef = computed(() => 'PUBLISHED' as const)

const {
  messages,
  streaming,
  error,
  sendMessage: chatSend,
  loadHistory,
  clearHistory,
  clearMemory,
} = useChat(agentUuidRef, sceneRef)

const isLastAssistantStreaming = computed(() => {
  const last = messages.value[messages.value.length - 1]
  return last?.role === 'ASSISTANT' && last.streaming && !last.content
})

const suggestions = [
  '你能给我什么帮助？'
]

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

watch(messages, () => scrollToBottom(), { deep: true })

watch(() => messages.value.length, (len) => {
  if (len > 0) scrollToBottom()
})

onMounted(async () => {
  userStore.fetchUserInfo()
  agentUuid.value = route.params.agentId as string
  if (!agentUuid.value) {
    showToast('智能体不存在', 'error')
    router.push('/')
    return
  }

  await loadAgentInfo(agentUuid.value)
  loading.value = false
  loadHistory()
  loadAvailableAgents()
})

watch(() => route.params.agentId, async (newId) => {
  const uuid = newId as string
  if (!uuid || uuid === agentUuid.value) return
  agentUuid.value = uuid
  await loadAgentInfo(uuid)
  messages.value = []
  loadHistory()
})

async function loadAgentInfo(uuid: string) {
  try {
    const { data } = await getAgentApi(uuid)
    if (data?.data) {
      agentName.value = data.data.name
      agentDesc.value = data.data.description || ''
      agentColor.value = data.data.color || '#6366f1'
      agentIconIndex.value = data.data.iconIndex ?? 0
    }
  } catch {
    // use defaults
  }
}

function goBack() {
  router.push('/')
}

async function handleLogout() {
  await logout()
  router.push('/login')
}

async function handleClear() {
  menuOpen.value = false
  await clearHistory()
}

function handleClearMemory() {
  menuOpen.value = false
  showClearMemoryDialog.value = true
}

async function doClearMemory() {
  showClearMemoryDialog.value = false
  await clearMemory()
  showToast('记忆已清空', 'success')
}

async function loadAvailableAgents() {
  try {
    const { data } = await listAvailableAgentApi({ page: 0, size: 100 })
    availableAgents.value = data?.data?.records ?? []
  } catch {
    availableAgents.value = []
  }
}

async function switchAgent(agent: AgentDTO) {
  if (agent.uuid === agentUuid.value) return
  agentUuid.value = agent.uuid
  agentName.value = agent.name
  agentDesc.value = agent.description || ''
  agentColor.value = agent.color || '#6366f1'
  agentIconIndex.value = agent.iconIndex ?? 0
  messages.value = []
  router.replace(`/chat/${agent.uuid}`)
  loadHistory()
}

async function removeAddedAgent(agent: AgentDTO) {
  removeTarget.value = agent
  showRemoveDialog.value = true
}

async function doRemove() {
  const agent = removeTarget.value
  if (!agent) return
  showRemoveDialog.value = false
  await removeAgentApi(agent.uuid)
  availableAgents.value = availableAgents.value.filter(a => a.uuid !== agent.uuid)
  if (agent.uuid === agentUuid.value) {
    const first = availableAgents.value[0]
    if (first) {
      switchAgent(first)
    } else {
      router.push('/')
    }
  }
  removeTarget.value = null
}

function sendMessage(text: string) {
  chatSend(text)
  scrollToBottom()
}

function showTimeDivider(index: number): boolean {
  if (index === 0) return true
  const prev = messages.value[index - 1]
  const curr = messages.value[index]
  if (!prev?.gmtCreated || !curr?.gmtCreated) return false
  return getTimeMinute(prev.gmtCreated) !== getTimeMinute(curr.gmtCreated)
}

function getTimeMinute(dateStr?: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatTime(dateStr?: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = d.getHours()
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}
</script>

<style scoped lang="scss">
$bg: #f7f7f8;
$surface: #ffffff;
$border: #e5e7eb;
$border-hover: #d1d5db;
$text-1: #111827;
$text-2: #6b7280;
$text-3: #9ca3af;
$primary: #6366f1;
$primary-soft: rgba(99, 102, 241, 0.06);
$primary-text: #ffffff;
$danger: #ef4444;
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$transition: 150ms cubic-bezier(0.4, 0, 0.2, 1);
$header-h: 56px;

.chat-view {
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: $bg;
  color: $text-1;
  -webkit-font-smoothing: antialiased;
}

// ── Global Header (matches HomeView) ──
.global-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: $surface;
  border-bottom: 1px solid $border;
  height: 56px;
  flex-shrink: 0;
}

.global-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px 0 24px;
}

.global-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.global-header-right {
  display: flex;
  align-items: center;
}

.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: $primary;
  color: $primary-text;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: $text-1;
  letter-spacing: -0.01em;
}

.guest-badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  background: $primary-soft;
  color: $primary;
  font-weight: 500;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px 6px 6px;
  border-radius: 999px;
  border: 1px solid $border;
  background: $surface;
  cursor: pointer;
  transition: border-color $transition;

  &:hover {
    border-color: $danger;
    .logout-icon { color: $danger; }
  }
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: $primary;
  color: $primary-text;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 12px;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: $text-1;
}

.logout-icon {
  color: $text-3;
  transition: color $transition;
}

// ── Body: Sidebar + Chat Main ──
.chat-body-wrap {
  flex: 1;
  display: flex;
  flex-direction: row;
  min-height: 0;
}

// ── Sidebar ──
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: $surface;
  border-right: 1px solid $border;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: $radius-sm;
  background: transparent;
  color: $text-1;
  cursor: pointer;
  text-align: left;
  transition: background $transition;

  &:hover { background: rgba(0, 0, 0, 0.04); }
  &.active { background: $primary-soft; }
}

.sidebar-item-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.sidebar-item-body {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sidebar-item-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-item-tag {
  font-size: 10px;
  font-weight: 500;
  padding: 1px 5px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  flex-shrink: 0;
}

.sidebar-item-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: $text-3;
  cursor: pointer;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity $transition, color $transition, background $transition;

  .sidebar-item:hover & { opacity: 1; }
  &:hover { color: $danger; background: rgba(239, 68, 68, 0.08); }
}

// ── Chat Main ──
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

// ── Chat Header ──
.chat-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: $surface;
  border-bottom: 1px solid $border;
  flex-shrink: 0;
}

.header-inner {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-agent {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  justify-content: center;
}

.header-agent-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.header-agent-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-actions {
  position: relative;
  flex-shrink: 0;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-3;
  cursor: pointer;
  transition: border-color $transition, color $transition;

  &:hover {
    border-color: $text-3;
    color: $text-1;
  }
}

.action-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  background: $surface;
  border: 1px solid $border;
  border-radius: $radius-sm;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 50;
  min-width: 160px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  border: none;
  background: transparent;
  color: $text-1;
  font-size: 13px;
  cursor: pointer;
  transition: background $transition;

  &:hover { background: rgba(0, 0, 0, 0.04); }
}

.action-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 49;
}

// ── Loading ──
.loading-full {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid $border;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// ── Chat Body ──
.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// ── Welcome ──
.welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.welcome-icon {
  width: 72px;
  height: 72px;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.welcome-name {
  font-size: 22px;
  font-weight: 700;
  color: $text-1;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.welcome-desc {
  font-size: 14px;
  color: $text-2;
  line-height: 1.6;
  max-width: 360px;
  margin-bottom: 40px;
}

.welcome-suggestions {
  width: 100%;
  max-width: 360px;
}

.suggestions-label {
  font-size: 12px;
  font-weight: 500;
  color: $text-3;
  margin-bottom: 12px;
  letter-spacing: 0.04em;
}

.suggestion-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 14px 18px;
  margin-bottom: 8px;
  border: 1px solid $border;
  border-radius: $radius-md;
  background: $surface;
  color: $text-1;
  font-size: 14px;
  cursor: pointer;
  transition: border-color $transition, background $transition, box-shadow $transition;

  &::before {
    content: '→';
    color: $text-3;
    font-size: 13px;
    flex-shrink: 0;
  }

  &:hover {
    border-color: $primary;
    background: $primary-soft;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08);
  }

  &:active { transform: scale(0.99); }
}

// ── Messages ──
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 0;
}

.time-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 24px 4px;

  span {
    font-size: 11px;
    color: $text-3;
    background: $bg;
    padding: 3px 12px;
    border-radius: 999px;
    border: 1px solid $border;
  }
}

// ── Typing Indicator ──
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 24px;
  margin-bottom: 16px;
}

.typing-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}

.typing-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
  background: $surface;
  border: 1px solid $border;
  border-radius: $radius-sm $radius-md $radius-md $radius-md;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: $text-3;
  animation: bounce 1.4s infinite ease-in-out both;

  &:nth-child(1) { animation-delay: 0s; }
  &:nth-child(2) { animation-delay: 0.16s; }
  &:nth-child(3) { animation-delay: 0.32s; }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

// ── Error ──
.chat-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  background: rgba(239, 68, 68, 0.08);
  color: $danger;
  font-size: 13px;
}

.chat-error-close {
  border: none;
  background: none;
  color: $danger;
  cursor: pointer;
  font-size: 18px;
  padding: 0 4px;
}

// ── Dialog ──
.overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.dialog {
  background: $surface;
  border-radius: $radius-lg;
  width: 100%;
  max-width: 360px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.dialog-head {
  padding: 20px 24px 0;
}

.dialog-title {
  font-size: 17px;
  font-weight: 600;
  color: $text-1;
}

.dialog-body {
  padding: 20px 24px;
}

.dialog-msg {
  font-size: 14px;
  color: $text-2;
  line-height: 1.6;
}

.dialog-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 24px 20px;
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-2;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color $transition, color $transition;

  &:hover { border-color: $text-3; color: $text-1; }
}

.btn-danger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: none;
  border-radius: $radius-sm;
  background: $danger;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity $transition;

  &:hover { opacity: 0.9; }
}

.overlay-enter-active,
.overlay-leave-active { transition: opacity 200ms ease; }
.overlay-enter-from,
.overlay-leave-to { opacity: 0; }

// ── Responsive ──
@media (max-width: 768px) {
  .sidebar { display: none; }
  .global-header-inner { padding: 0 12px; }
  .header-inner { padding: 0 12px; }
  .welcome { padding: 32px 16px; }
  .welcome-suggestions { max-width: 100%; }
  .logo-text { display: none; }
  .user-name { display: none; }
}
</style>
