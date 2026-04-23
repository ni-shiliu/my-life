<template>
  <div class="home-view">
    <!-- Header -->
    <header class="home-header">
      <div class="header-inner">
        <div class="header-left">
          <span class="logo-mark">M</span>
          <span class="logo-text">my-life</span>
        </div>
        <div class="header-right">
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

    <!-- Main Content -->
    <main class="home-main">
      <!-- Tabs -->
      <nav class="tab-nav" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab-btn', { active: activeTab === tab.key }]"
          role="tab"
          :aria-selected="activeTab === tab.key"
          @click="activeTab = tab.key"
        >
          <component :is="renderTabIcon(tab.iconKey)" class="tab-icon" />
          <span>{{ tab.label }}</span>
        </button>
      </nav>

      <!-- Tab Panels -->
      <div class="tab-panels">
        <!-- Agents Tab -->
        <section v-if="activeTab === 'agents'" class="panel" role="tabpanel">
          <div class="panel-top">
            <h2 class="panel-heading">我的智能体</h2>
            <button class="btn-primary" @click="showCreateDialog = true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              创建智能体
            </button>
          </div>

          <!-- Loading -->
          <div v-if="agentStore.loading" class="loading">
            <div class="spinner"></div>
          </div>

          <div v-else-if="agentStore.agents.length === 0" class="empty">
            <div class="empty-visual">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <rect x="3" y="11" width="18" height="10" rx="2"/>
                <circle cx="12" cy="5" r="2"/>
                <path d="M12 7v4"/>
              </svg>
            </div>
            <p class="empty-title">暂无智能体</p>
            <p class="empty-desc">创建你的第一个智能体开始使用</p>
          </div>

          <div v-else class="agent-grid">
            <div v-for="agent in agentStore.agents" :key="agent.uuid" class="agent-card" @click="goToEdit(agent)">
              <div class="card-icon" :style="{ background: agent.color || '#6366f1' }">
                <component :is="renderIcon(agent.iconIndex ?? 0)" />
              </div>
              <div class="card-body">
                <h3 class="card-title">
                  {{ agent.name }}
                  <span v-if="agent.status === 'PUBLISHED'" class="badge">已发布</span>
                </h3>
                <p class="card-desc">{{ agent.description }}</p>
              </div>
              <div class="card-actions" @click.stop>
                <button class="btn-ghost btn-sm" @click="startChat(agent)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                  对话
                </button>
                <button class="btn-ghost btn-sm" @click="openEditDialog(agent)" aria-label="编辑">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                  编辑
                </button>
                <button class="btn-ghost btn-sm btn-delete" @click="confirmDelete(agent)" aria-label="删除">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    <line x1="10" y1="11" x2="10" y2="17"></line>
                    <line x1="14" y1="11" x2="14" y2="17"></line>
                  </svg>
                  删除
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- Knowledge Base Tab -->
        <section v-if="activeTab === 'knowledge'" class="panel" role="tabpanel">
          <div class="panel-top">
            <h2 class="panel-heading">知识库</h2>
            <button class="btn-primary">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              上传文档
            </button>
          </div>
          <div class="empty">
            <div class="empty-visual">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <p class="empty-title">暂无文档</p>
            <p class="empty-desc">上传 PDF 或 Word 文档，让智能体学习你的知识</p>
          </div>
        </section>

        <!-- Agent Square Tab -->
        <section v-if="activeTab === 'square'" class="panel" role="tabpanel">
          <div class="panel-top">
            <h2 class="panel-heading">智能体广场</h2>
            <div class="search-field">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              <input type="text" placeholder="搜索智能体..." v-model="searchQuery" />
            </div>
          </div>
          <div class="empty">
            <div class="empty-visual">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
                <rect x="3" y="3" width="7" height="7" rx="1"/>
                <rect x="14" y="3" width="7" height="7" rx="1"/>
                <rect x="14" y="14" width="7" height="7" rx="1"/>
                <rect x="3" y="14" width="7" height="7" rx="1"/>
              </svg>
            </div>
            <p class="empty-title">即将上线</p>
            <p class="empty-desc">智能体广场功能开发中，敬请期待</p>
          </div>
        </section>
      </div>
    </main>

    <!-- Create Agent Dialog -->
    <Teleport to="body">
      <Transition name="overlay">
        <div v-if="showCreateDialog" class="overlay" @click="showCreateDialog = false">
          <Transition name="dialog">
            <div v-if="showCreateDialog" class="dialog" @click.stop>
              <div class="dialog-head">
                <h3 class="dialog-title">创建智能体</h3>
                <button class="btn-icon" @click="showCreateDialog = false" aria-label="Close">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
              <div class="dialog-body">
                <div class="field">
                  <label class="field-label" for="agent-name">名称</label>
                  <input id="agent-name" type="text" class="field-input" v-model="newAgent.name" placeholder="例如：数学老师" />
                </div>
                <div class="field">
                  <label class="field-label" for="agent-desc">描述</label>
                  <textarea id="agent-desc" class="field-input field-textarea" v-model="newAgent.description" placeholder="描述这个智能体的职责..."></textarea>
                </div>
                <div class="field">
                  <label class="field-label">图标</label>
                  <div class="icon-picker">
                    <button
                      v-for="i in iconCount"
                      :key="i"
                      :class="['icon-option', { selected: newAgent.iconIndex === i - 1 }]"
                      @click="newAgent.iconIndex = i - 1"
                    >
                      <component :is="renderIcon(i - 1)" />
                    </button>
                  </div>
                </div>
                <div class="field">
                  <label class="field-label">颜色</label>
                  <div class="color-picker">
                    <button
                      v-for="c in colors"
                      :key="c"
                      :class="['color-option', { selected: newAgent.color === c }]"
                      :style="{ background: c }"
                      @click="newAgent.color = c"
                    >
                      <svg v-if="newAgent.color === c" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
              <div class="dialog-foot">
                <button class="btn-ghost" @click="showCreateDialog = false">取消</button>
                <button class="btn-primary" @click="createAgent" :disabled="creating">
                  {{ creating ? '创建中...' : '创建' }}
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>

    <!-- Delete Confirm Dialog -->
    <Teleport to="body">
      <Transition name="overlay">
        <div v-if="showDeleteDialog" class="overlay" @click="showDeleteDialog = false">
          <div class="dialog" @click.stop>
            <div class="dialog-head">
              <h3 class="dialog-title">确认删除</h3>
            </div>
            <div class="dialog-body">
              <p class="delete-msg">确定要删除智能体「{{ deleteTarget?.name }}」吗？此操作不可恢复。</p>
            </div>
            <div class="dialog-foot">
              <button class="btn-ghost" @click="showDeleteDialog = false">取消</button>
              <button class="btn-danger" @click="doDelete" :disabled="deleting">
                {{ deleting ? '删除中...' : '删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Edit Agent Dialog -->
    <Teleport to="body">
      <Transition name="overlay">
        <div v-if="showEditDialog" class="overlay" @click="showEditDialog = false">
          <Transition name="dialog">
            <div v-if="showEditDialog" class="dialog" @click.stop>
              <div class="dialog-head">
                <h3 class="dialog-title">编辑智能体</h3>
                <button class="btn-icon" @click="showEditDialog = false" aria-label="Close">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
              <div class="dialog-body">
                <div class="field">
                  <label class="field-label" for="edit-agent-name">名称</label>
                  <input id="edit-agent-name" type="text" class="field-input" v-model="editForm.name" placeholder="例如：数学老师" />
                </div>
                <div class="field">
                  <label class="field-label" for="edit-agent-desc">描述</label>
                  <textarea id="edit-agent-desc" class="field-input field-textarea" v-model="editForm.description" placeholder="描述这个智能体的职责..."></textarea>
                </div>
                <div class="field">
                  <label class="field-label">图标</label>
                  <div class="icon-picker">
                    <button
                      v-for="i in iconCount"
                      :key="i"
                      :class="['icon-option', { selected: editForm.iconIndex === i - 1 }]"
                      @click="editForm.iconIndex = i - 1"
                    >
                      <component :is="renderIcon(i - 1)" />
                    </button>
                  </div>
                </div>
                <div class="field">
                  <label class="field-label">颜色</label>
                  <div class="color-picker">
                    <button
                      v-for="c in colors"
                      :key="c"
                      :class="['color-option', { selected: editForm.color === c }]"
                      :style="{ background: c }"
                      @click="editForm.color = c"
                    >
                      <svg v-if="editForm.color === c" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3">
                        <polyline points="20 6 9 17 4 12"/>
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
              <div class="dialog-foot">
                <button class="btn-ghost" @click="showEditDialog = false">取消</button>
                <button class="btn-primary" @click="saveEditAgent" :disabled="savingEdit">
                  {{ savingEdit ? '保存中...' : '保存' }}
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useAgentStore } from '@/stores/agent'
import type { AgentDTO } from '@/types/agent'

import { h } from 'vue'

// SVG Icon render helpers
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

const iconCount = 8

// Tab nav icons
const renderTabIcon = (key: string) => {
  const tabIcons: Record<string, () => ReturnType<typeof h>> = {
    robot: () => h('svg', { width: 18, height: 18, ...svgProps }, [
      h('rect', { x: 3, y: 11, width: 18, height: 10, rx: 2 }),
      h('circle', { cx: 12, cy: 5, r: 2 }),
      h('path', { d: 'M12 7v4' })
    ]),
    book: () => h('svg', { width: 18, height: 18, ...svgProps }, [
      h('path', { d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' }),
      h('path', { d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z' })
    ]),
    grid: () => h('svg', { width: 18, height: 18, ...svgProps }, [
      h('rect', { x: 3, y: 3, width: 7, height: 7, rx: 1 }),
      h('rect', { x: 14, y: 3, width: 7, height: 7, rx: 1 }),
      h('rect', { x: 14, y: 14, width: 7, height: 7, rx: 1 }),
      h('rect', { x: 3, y: 14, width: 7, height: 7, rx: 1 })
    ])
  }
  return (tabIcons[key] ?? tabIcons.robot)()
}

// State
const router = useRouter()
const userStore = useUserStore()
const { logout } = useAuth()
const agentStore = useAgentStore()

const activeTab = ref('agents')
const showCreateDialog = ref(false)
const showDeleteDialog = ref(false)
const showEditDialog = ref(false)
const searchQuery = ref('')
const creating = ref(false)
const deleting = ref(false)
const savingEdit = ref(false)
const deleteTarget = ref<AgentDTO | null>(null)

const tabs = [
  { key: 'agents', label: '我的智能体', iconKey: 'robot' },
  { key: 'knowledge', label: '知识库', iconKey: 'book' },
  { key: 'square', label: '广场', iconKey: 'grid' }
]

const newAgent = ref({
  name: '',
  description: '',
  iconIndex: 0,
  color: '#6366f1'
})

const editForm = ref({
  uuid: '',
  name: '',
  description: '',
  iconIndex: 0,
  color: '#6366f1'
})

const colors = ['#6366f1', '#059669', '#d946ef', '#2563eb', '#0891b2', '#dc2626', '#ca8a04', '#7c3aed']

// Computed
const userAvatar = computed(() => {
  const name = userStore.userInfo?.nickName || 'U'
  return name.charAt(0).toUpperCase()
})

// Lifecycle
onMounted(() => {
  userStore.fetchUserInfo()
  agentStore.loadAgents()
})

// Methods
const handleLogout = async () => {
  await logout()
  router.push('/login')
}

const createAgent = async () => {
  if (!newAgent.value.name.trim()) return
  creating.value = true
  try {
    await agentStore.saveAgent({
      name: newAgent.value.name,
      description: newAgent.value.description || '',
      iconIndex: newAgent.value.iconIndex,
      color: newAgent.value.color
    })
    newAgent.value = { name: '', description: '', iconIndex: 0, color: '#6366f1' }
    showCreateDialog.value = false
  } finally {
    creating.value = false
  }
}

const startChat = (agent: AgentDTO) => {
  router.push(`/chat/${agent.uuid}`)
}

const goToEdit = (agent: AgentDTO) => {
  router.push(`/agent/${agent.uuid}`)
}

const openEditDialog = (agent: AgentDTO) => {
  editForm.value = {
    uuid: agent.uuid,
    name: agent.name,
    description: agent.description || '',
    iconIndex: agent.iconIndex ?? 0,
    color: agent.color || '#6366f1'
  }
  showEditDialog.value = true
}

const saveEditAgent = async () => {
  if (!editForm.value.name.trim()) return
  savingEdit.value = true
  try {
    await agentStore.saveAgent({
      uuid: editForm.value.uuid,
      name: editForm.value.name,
      description: editForm.value.description,
      iconIndex: editForm.value.iconIndex,
      color: editForm.value.color
    })
    showEditDialog.value = false
  } finally {
    savingEdit.value = false
  }
}

const confirmDelete = (agent: AgentDTO) => {
  deleteTarget.value = agent
  showDeleteDialog.value = true
}

const doDelete = async () => {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await agentStore.deleteAgent(deleteTarget.value.uuid)
    showDeleteDialog.value = false
    deleteTarget.value = null
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped lang="scss">
// ── Design Tokens ──
$bg: #fafafa;
$surface: #ffffff;
$border: #e5e7eb;
$border-hover: #d1d5db;
$text-1: #111827;
$text-2: #6b7280;
$text-3: #9ca3af;
$primary: #6366f1;
$primary-hover: #4f46e5;
$primary-soft: rgba(99, 102, 241, 0.08);
$primary-text: #ffffff;
$danger: #ef4444;
$danger-hover: #dc2626;
$radius-sm: 8px;
$radius-md: 12px;
$radius-lg: 16px;
$shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
$shadow-md: 0 2px 8px rgba(0, 0, 0, 0.06);
$shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.08);
$transition: 150ms cubic-bezier(0.4, 0, 0.2, 1);

.home-view {
  min-height: 100vh;
  background: $bg;
  color: $text-1;
  -webkit-font-smoothing: antialiased;
}

// ── Header ──
.home-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: $surface;
  border-bottom: 1px solid $border;
  height: 56px;
}

.header-inner {
  max-width: 1200px;
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
  gap: 10px;
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

// ── Main ──
.home-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

// ── Tab Nav ──
.tab-nav {
  display: flex;
  gap: 4px;
  margin-bottom: 32px;
  border-bottom: 1px solid $border;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  background: transparent;
  color: $text-2;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: color $transition, border-color $transition;

  &:hover { color: $text-1; }

  &.active {
    color: $primary;
    border-bottom-color: $primary;
  }
}

.tab-icon { flex-shrink: 0; }

// ── Panel ──
.panel {
  animation: panelIn 200ms ease-out;
}

@keyframes panelIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.panel-heading {
  font-size: 20px;
  font-weight: 600;
  color: $text-1;
  letter-spacing: -0.01em;
}

// ── Loading ──
.loading {
  display: flex;
  justify-content: center;
  padding: 80px 20px;
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

// ── Buttons ──
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

  &:active { transform: translateY(0); }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-1;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color $transition, background $transition;

  &.btn-sm { padding: 6px 12px; }

  &:hover {
    border-color: $primary;
    color: $primary;
    background: $primary-soft;
  }
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-1;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color $transition, background $transition, color $transition;

  &.btn-sm { padding: 6px 12px; }

  &:hover {
    border-color: $primary;
    color: $primary;
    background: $primary-soft;
  }

  &.btn-delete:hover {
    border-color: $danger;
    color: $danger;
    background: rgba(239, 68, 68, 0.08);
  }
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
  font-size: 18px;
  line-height: 1;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.btn-danger:hover {
    border-color: $danger;
    color: $danger;
  }
}

.btn-danger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: $radius-sm;
  background: $danger;
  color: $primary-text;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background $transition;

  &:hover { background: $danger-hover; }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

// ── Search ──
.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 40px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  width: 260px;
  transition: border-color $transition;

  &:focus-within { border-color: $primary; }

  input {
    flex: 1;
    border: none;
    background: transparent;
    outline: none;
    font-size: 14px;
    color: $text-1;
    &::placeholder { color: $text-3; }
  }
}

.search-icon { color: $text-3; flex-shrink: 0; }

// ── Empty State ──
.empty {
  text-align: center;
  padding: 80px 20px;
}

.empty-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: $primary-soft;
  color: $primary;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-1;
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: $text-2;
}

// ── Agent Grid ──
.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

// ── Agent Card ──
.agent-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid $border;
  border-radius: $radius-md;
  background: $surface;
  cursor: pointer;
  transition: border-color $transition, box-shadow $transition;

  &:hover {
    border-color: $border-hover;
    box-shadow: $shadow-md;
  }
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: #fff;
}

.card-body {
  flex: 1;
  margin-bottom: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-1;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(5, 150, 105, 0.1);
  color: #059669;
}

.card-desc {
  font-size: 13px;
  color: $text-2;
  line-height: 1.5;
}

.card-actions {
  display: flex;
  gap: 8px;
}

// ── Delete Dialog ──
.delete-msg {
  font-size: 14px;
  color: $text-2;
  line-height: 1.6;
}

// ── Dialog ──
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 24px;
}

.dialog {
  width: 100%;
  max-width: 440px;
  background: $surface;
  border-radius: $radius-lg;
  box-shadow: $shadow-lg;
  overflow: hidden;
}

.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid $border;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-1;
}

.dialog-body {
  padding: 24px;
}

.dialog-foot {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid $border;
}

// ── Form Fields ──
.field {
  margin-bottom: 20px;
  &:last-child { margin-bottom: 0; }
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
  border: 1px solid $border;
  border-radius: $radius-sm;
  font-size: 14px;
  color: $text-1;
  outline: none;
  background: $surface;
  transition: border-color $transition;

  &:focus { border-color: $primary; }
  &::placeholder { color: $text-3; }
}

.field-textarea {
  min-height: 88px;
  resize: vertical;
  font-family: inherit;
}

// ── Icon Picker ──
.icon-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.icon-option {
  width: 44px;
  height: 44px;
  border: 2px solid $border;
  border-radius: $radius-sm;
  background: $surface;
  color: $text-2;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color $transition, background $transition, color $transition;

  &:hover {
    border-color: $primary;
    color: $primary;
  }

  &.selected {
    border-color: $primary;
    background: $primary-soft;
    color: $primary;
  }
}

// ── Color Picker ──
.color-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-option {
  width: 36px;
  height: 36px;
  border: 2px solid transparent;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform $transition, border-color $transition;

  &:hover { transform: scale(1.1); }

  &.selected {
    border-color: $text-1;
    transform: scale(1.1);
  }
}

// ── Dialog Transitions ──
.overlay-enter-active,
.overlay-leave-active { transition: opacity 200ms ease; }
.overlay-enter-from,
.overlay-leave-to { opacity: 0; }

.dialog-enter-active { transition: opacity 200ms ease, transform 200ms ease; }
.dialog-leave-active { transition: opacity 150ms ease, transform 150ms ease; }
.dialog-enter-from { opacity: 0; transform: scale(0.95) translateY(8px); }
.dialog-leave-to { opacity: 0; transform: scale(0.97); }

// ── Responsive ──
@media (max-width: 768px) {
  .home-main { padding: 20px 16px; }
  .agent-grid { grid-template-columns: 1fr; }
  .search-field { width: 180px; }
  .panel-top { flex-wrap: wrap; gap: 12px; }
  .dialog { max-width: 100%; }
  .tab-btn span { display: none; }
}
</style>
