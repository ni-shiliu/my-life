export type ChatRole = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL_RESULT'
export type ChatScene = 'EDIT' | 'PUBLISHED'

export interface ChatMessage {
  id?: number
  role: ChatRole
  content: string
  toolName?: string
  scene?: ChatScene
  gmtCreated?: string
  streaming?: boolean
}

export interface ToolCallInfo {
  name: string
  args: string
  result: string
}

// SSE 消息信封
export interface WsMessage<T = Record<string, unknown>> {
  type: 'SEND' | 'STREAM_CHUNK' | 'STREAM_END' | 'TOOL_CALL' | 'ERROR'
  payload: T
}

export interface SendPayload {
  agentUuid: string
  message: string
}

export interface StreamChunkPayload {
  chunk: string
}

export interface StreamEndPayload {
  fullText: string
  toolCalls: ToolCallInfo[]
}

export interface ErrorPayload {
  code: string
  message: string
  retryable: boolean
}

export interface GuestTokenResponse {
  token: string
  sessionId: string
}
