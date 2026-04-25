export interface KnowledgeBaseDTO {
  id: number
  uuid: string
  name: string
  source: string
  externalId: string
  gmtModified: string
}

export interface KnowledgeBaseSaveDTO {
  uuid?: string
  name: string
  externalId: string
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
