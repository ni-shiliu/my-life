import request from './request'
import type { KnowledgeBaseDTO, KnowledgeBaseSaveDTO, BaseResult, PageResult } from '@/types/knowledgeBase'

export function saveKnowledgeBaseApi(data: KnowledgeBaseSaveDTO) {
  return request.post<BaseResult<KnowledgeBaseDTO>>('/v1/knowledge-base/save', data)
}

export function deleteKnowledgeBaseApi(uuid: string) {
  return request.delete<BaseResult<void>>('/v1/knowledge-base/delete', { params: { uuid } })
}

export function listKnowledgeBaseApi() {
  return request.post<BaseResult<KnowledgeBaseDTO[]>>('/v1/knowledge-base/list')
}

export function listKnowledgeBasePageApi(data: { name?: string; page: number; size: number }) {
  return request.post<BaseResult<PageResult<KnowledgeBaseDTO>>>('/v1/knowledge-base/queryPage', data)
}
