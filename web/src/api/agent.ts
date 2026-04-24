import request from './request'
import type { AgentDTO, AgentSaveDTO, BaseResult } from '@/types/agent'

export function saveAgentApi(data: AgentSaveDTO) {
  return request.post<BaseResult<AgentDTO>>('/v1/agent/save', data)
}

export function deleteAgentApi(uuid: string) {
  return request.delete<BaseResult<void>>('/v1/agent/delete', { params: { uuid } })
}

export function getAgentApi(uuid: string) {
  return request.get<BaseResult<AgentDTO>>(`/v1/agent/get/${uuid}`)
}

export function listAgentApi() {
  return request.post<BaseResult<AgentDTO[]>>('/v1/agent/list')
}

export function publishAgentApi(uuid: string) {
  return request.post<BaseResult<void>>('/v1/agent/publish', null, { params: { uuid } })
}

export function listPublishedAgentApi() {
  return request.get<BaseResult<AgentDTO[]>>('/v1/agent/published')
}
