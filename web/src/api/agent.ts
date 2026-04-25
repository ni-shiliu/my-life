import request from './request'
import type { AgentDTO, AgentSaveDTO, BaseResult, PageResult } from '@/types/agent'

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

export function queryPublishedAgentApi(data: { name?: string; page: number; size: number }) {
  return request.post<BaseResult<PageResult<AgentDTO>>>('/v1/agent/published', data)
}

export function listAgentPageApi(data: { name?: string; page: number; size: number }) {
  return request.post<BaseResult<PageResult<AgentDTO>>>('/v1/agent/queryPage', data)
}

export function addAgentApi(agentUuid: string) {
  return request.post<BaseResult<void>>('/v1/agent/add', null, { params: { agentUuid } })
}

export function removeAgentApi(agentUuid: string) {
  return request.delete<BaseResult<void>>('/v1/agent/remove', { params: { agentUuid } })
}

export function listAvailableAgentApi(data: { name?: string; page: number; size: number }) {
  return request.post<BaseResult<PageResult<AgentDTO>>>('/v1/agent/available', data)
}
