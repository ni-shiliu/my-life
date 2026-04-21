import request from './request'
import type { AgentDTO, AgentSaveDTO, BaseResult } from '@/types/agent'

export function saveAgentApi(data: AgentSaveDTO) {
  return request.post<BaseResult<AgentDTO>>('/v1/agent/save', data)
}

export function deleteAgentApi(id: number) {
  return request.delete<BaseResult<void>>('/v1/agent/delete', { params: { id } })
}

export function getAgentApi(id: number) {
  return request.get<BaseResult<AgentDTO>>(`/v1/agent/get/${id}`)
}

export function listAgentApi() {
  return request.post<BaseResult<AgentDTO[]>>('/v1/agent/list')
}

export function publishAgentApi(id: number) {
  return request.post<BaseResult<void>>('/v1/agent/publish', null, { params: { id } })
}
