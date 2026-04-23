<template>
  <div class="edit-view">
    <!-- Top Bar: logo + user (same as home) -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <div class="top-bar-left">
          <span class="logo-mark">M</span>
          <span class="logo-text">my-life</span>
        </div>
        <div class="top-bar-right">
          <div class="user-pill" @click="handleLogout">
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

    <!-- Sub Bar: back + title + publish -->
    <header class="sub-bar">
      <div class="sub-bar-inner">
        <div class="sub-bar-left">
          <button class="btn-icon" @click="goBack" aria-label="返回">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
          </button>
          <h1 class="header-title">{{ agent?.name || '编辑智能体' }}</h1>
          <span v-if="agent?.status === 'PUBLISHED'" class="badge-published">已发布</span>
        </div>
        <button class="btn-primary" @click="handlePublish" :disabled="publishing || agent?.status === 'PUBLISHED'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
            <polyline points="16 6 12 2 8 6"/>
            <line x1="12" y1="2" x2="12" y2="15"/>
          </svg>
          {{ agent?.status === 'PUBLISHED' ? '已发布' : '发布' }}
        </button>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="loading-full">
      <div class="spinner"></div>
    </div>

    <!-- Main Content -->
    <div v-else-if="agent" class="edit-body">
      <!-- Left: Form -->
      <div class="form-panel">
        <div class="form-scroll">
          <div class="field">
            <label class="field-label">系统提示词</label>
            <textarea class="field-input field-textarea-lg" v-model="form.systemPrompt" placeholder="定义智能体的角色、行为和回答风格..."></textarea>
          </div>

          <div class="field">
            <label class="field-label">知识库</label>
            <div class="select-wrap">
              <select class="field-select" v-model="form.knowledgeBaseId">
                <option :value="null">暂无知识库</option>
              </select>
              <svg class="select-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
            <p class="field-hint">关联知识库后，智能体可检索相关文档回答问题</p>
          </div>

        </div>

        <div class="form-footer">
          <button class="btn-primary btn-block" @click="handleSave" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>

      <!-- Right: Chat Preview -->
      <ChatPanel
        v-if="agent?.uuid"
        :agent-uuid="agent.uuid"
        :agent-name="form.name"
        :agent-color="form.color"
        :scene="'EDIT'"
        empty-text="保存后可开始对话"
        class="chat-panel"
      />
      <div v-else class="chat-panel">
        <div class="chat-header">
          <div class="chat-agent-icon" :style="{ background: form.color || '#6366f1' }">
            <component :is="renderIcon(form.iconIndex ?? 0)" />
          </div>
          <div>
            <h3 class="chat-agent-name">{{ form.name || '未命名智能体' }}</h3>
            <p class="chat-agent-status">预览模式</p>
          </div>
        </div>

        <div class="chat-messages">
          <div class="chat-empty">
            <div class="chat-empty-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <p class="chat-empty-text">保存后可开始对话</p>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="chat-input-wrap">
            <input type="text" class="chat-input" placeholder="发送消息..." disabled />
            <button class="chat-send-btn" disabled>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useAgentStore } from '@/stores/agent'
import { getAgentApi } from '@/api/agent'
import { showToast } from '@/components/auth/toast-state'
import type { AgentDTO } from '@/types/agent'
import ChatPanel from '@/components/chat/ChatPanel.vue'

// SVG Icon render helpers
const svgProps = { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 1.5 }

const renderIcon = (index: number, size = 20) => {
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

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { logout } = useAuth()
const agentStore = useAgentStore()

const userAvatar = computed(() => {
  const name = userStore.userInfo?.nickName || 'U'
  return name.charAt(0).toUpperCase()
})

const handleLogout = async () => {
  await logout()
  router.push('/login')
}

const loading = ref(true)
const saving = ref(false)
const publishing = ref(false)
const agent = ref<AgentDTO | null>(null)

const form = ref({
  name: '',
  description: '',
  systemPrompt: '',
  iconIndex: 0,
  color: '#6366f1',
  knowledgeBaseId: null as number | null
})

onMounted(async () => {
  const uuid = route.params.id as string
  try {
    const { data } = await getAgentApi(uuid)
    if (data?.data) {
      agent.value = data.data
      form.value = {
        name: data.data.name,
        description: data.data.description || '',
        systemPrompt: data.data.systemPrompt || '',
        iconIndex: data.data.iconIndex ?? 0,
        color: data.data.color || '#6366f1',
        knowledgeBaseId: data.data.knowledgeBaseId
      }
    }
  } finally {
    loading.value = false
  }
})

const goBack = () => {
  router.push('/')
}

const handleSave = async () => {
  if (!form.value.name.trim()) return
  saving.value = true
  try {
    const saved = await agentStore.saveAgent({
      uuid: agent.value?.uuid,
      name: form.value.name,
      description: form.value.description,
      systemPrompt: form.value.systemPrompt,
      iconIndex: form.value.iconIndex,
      color: form.value.color,
      knowledgeBaseId: form.value.knowledgeBaseId
    })
    if (saved) {
      agent.value = saved
      showToast('保存成功', 'success')
    }
  } finally {
    saving.value = false
  }
}

const handlePublish = async () => {
  if (!agent.value || agent.value.status === 'PUBLISHED') return
  publishing.value = true
  try {
    await agentStore.publishAgent(agent.value.uuid)
    agent.value = { ...agent.value, status: 'PUBLISHED' }
  } finally {
    publishing.value = false
  }
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
$primary-hover: #4f46e5;
$primary-soft: rgba(99, 102, 241, 0.08);
$primary-text: #ffffff;
$danger: #ef4444;
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
$shadow-md: 0 2px 8px rgba(0, 0, 0, 0.06);
$transition: 150ms cubic-bezier(0.4, 0, 0.2, 1);

.edit-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg;
  color: $text-1;
  -webkit-font-smoothing: antialiased;
}

// ── Top Bar (white, same as home) ──
.top-bar {
  background: $surface;
  height: 56px;
  flex-shrink: 0;
}

.top-bar-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.top-bar-right {
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

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
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

// ── Sub Bar (light gray, color diff instead of border) ──
.sub-bar {
  background: $bg;
  height: 44px;
  flex-shrink: 0;
}

.sub-bar-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.sub-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-1;
}

.badge-published {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(5, 150, 105, 0.1);
  color: #059669;
}

// ── Loading ──
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

// ── Main Body ──
.edit-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

// ── Form Panel ──
.form-panel {
  width: 55%;
  display: flex;
  flex-direction: column;
  background: $surface;
}

.form-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

.form-footer {
  padding: 16px 32px;
  background: $bg;
}

// ── Chat Panel ──
.chat-panel {
  width: 45%;
  display: flex;
  flex-direction: column;
  background: $bg;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: $surface;
}

.chat-agent-icon {
  width: 36px;
  height: 36px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
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

.chat-input-area {
  padding: 12px 24px 16px;
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

  &::placeholder { color: $text-3; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
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
  transition: background $transition;

  &:disabled { opacity: 0.4; cursor: not-allowed; }
}

// ── Buttons ──
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

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: $radius-sm;
  background: $primary;
  color: $primary-text;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background $transition, transform $transition;

  &:hover {
    background: $primary-hover;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

.btn-block {
  width: 100%;
  justify-content: center;
  padding: 12px;
}

// ── Form Fields ──
.field {
  margin-bottom: 24px;
}

.field-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: $text-1;
  margin-bottom: 6px;
}

.field-input {
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: $radius-sm;
  font-size: 14px;
  color: $text-1;
  outline: none;
  background: $bg;
  transition: box-shadow $transition;

  &:focus { box-shadow: 0 0 0 2px rgba($primary, 0.2); }
  &::placeholder { color: $text-3; }

  &.field-readonly {
    background: $bg;
    color: $text-2;
    cursor: default;
    &:focus { box-shadow: none; }
  }
}

.field-textarea {
  min-height: 80px;
  resize: vertical;
  font-family: inherit;
}

.field-textarea-lg {
  min-height: 160px;
  resize: vertical;
  font-family: inherit;
}

.field-select {
  width: 100%;
  padding: 10px 32px 10px 12px;
  border: none;
  border-radius: $radius-sm;
  font-size: 14px;
  color: $text-1;
  outline: none;
  background: $bg;
  appearance: none;
  cursor: pointer;
  transition: box-shadow $transition;

  &:focus { box-shadow: 0 0 0 2px rgba($primary, 0.2); }
}

.select-wrap {
  position: relative;
}

.select-arrow {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: $text-3;
}

.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: $text-3;
}

// ── Responsive ──
@media (max-width: 768px) {
  .edit-body {
    flex-direction: column;
  }

  .form-panel {
    width: 100%;
    max-height: 60vh;
  }

  .chat-panel {
    width: 100%;
    min-height: 40vh;
  }

  .form-scroll {
    padding: 20px 16px;
  }
}
</style>
