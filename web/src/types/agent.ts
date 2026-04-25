export interface AgentDTO {
  uuid: string
  name: string
  description: string
  iconIndex: number
  color: string
  systemPrompt: string
  knowledgeBaseId: number | null
  knowledgeBaseName?: string
  status: 'DRAFT' | 'PUBLISHED'
  owned?: boolean
  added?: boolean
  gmtModified: string
}

export interface AgentSaveDTO {
  uuid?: string
  name: string
  description: string
  iconIndex: number
  color: string
  systemPrompt?: string
  knowledgeBaseId?: number | null
  resetToDraft?: boolean
}

export interface BaseResult<T> {
  code: string
  message: string
  data: T | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
