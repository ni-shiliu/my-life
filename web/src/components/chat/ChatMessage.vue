<template>
  <div :class="['chat-msg', `chat-msg--${msg.role.toLowerCase()}`]">
    <div v-if="msg.role === 'ASSISTANT'" class="chat-msg-avatar">
      <div class="agent-icon" :style="{ background: agentColor }">
        <component :is="renderIcon(agentIconIndex ?? 0, 14)" />
      </div>
    </div>
    <div v-else-if="msg.role === 'USER'" class="chat-msg-avatar">
      <div class="user-icon">{{ userInitial }}</div>
    </div>
    <div class="chat-msg-content">
      <div class="chat-msg-bubble">
        <div class="chat-msg-text" v-html="renderContent(msg.content)"></div>
        <span v-if="msg.streaming && msg.content" class="chat-msg-cursor"></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { h } from 'vue'
import type { ChatMessage } from '@/types/chat'

defineProps<{
  msg: ChatMessage
  agentColor?: string
  agentIconIndex?: number
  userInitial?: string
}>()

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
  color: #fff;
}

.user-icon {
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

.chat-msg-content {
  min-width: 0;
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
  display: inline-block;
  width: 2px;
  height: 1em;
  background: $primary;
  margin-left: 1px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
