import request from './request'
import type { KnowledgeBaseDTO, KnowledgeBaseSaveDTO, BaseResult } from '@/types/knowledgeBase'

export function saveKnowledgeBaseApi(data: KnowledgeBaseSaveDTO) {
  return request.post<BaseResult<KnowledgeBaseDTO>>('/v1/knowledge-base/save', data)
}

export function deleteKnowledgeBaseApi(uuid: string) {
  return request.delete<BaseResult<void>>('/v1/knowledge-base/delete', { params: { uuid } })
}

export function listKnowledgeBaseApi() {
  return request.post<BaseResult<KnowledgeBaseDTO[]>>('/v1/knowledge-base/list')
}
