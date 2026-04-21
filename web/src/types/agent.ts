export interface AgentDTO {
  id: number
  name: string
  description: string
  iconIndex: number
  color: string
  systemPrompt: string
  knowledgeBaseId: number | null
  status: 'DRAFT' | 'PUBLISHED'
  gmtModified: string
}

export interface AgentSaveDTO {
  id?: number
  name: string
  description: string
  iconIndex: number
  color: string
  systemPrompt?: string
  knowledgeBaseId?: number | null
}

export interface BaseResult<T> {
  code: string
  message: string
  data: T | null
}
