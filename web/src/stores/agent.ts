import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AgentDTO, AgentSaveDTO } from '@/types/agent'
import { saveAgentApi, deleteAgentApi, listAgentApi, publishAgentApi } from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
  const agents = ref<AgentDTO[]>([])
  const loading = ref(false)

  async function loadAgents() {
    loading.value = true
    try {
      const { data } = await listAgentApi()
      agents.value = data?.data ?? []
    } finally {
      loading.value = false
    }
  }

  async function saveAgent(dto: AgentSaveDTO): Promise<AgentDTO | null> {
    const { data } = await saveAgentApi(dto)
    const saved = data?.data ?? null
    if (saved) {
      const idx = agents.value.findIndex(a => a.uuid === saved.uuid)
      if (idx >= 0) {
        agents.value[idx] = saved
      } else {
        agents.value.unshift(saved)
      }
    }
    return saved
  }

  async function deleteAgent(uuid: string) {
    await deleteAgentApi(uuid)
    agents.value = agents.value.filter(a => a.uuid !== uuid)
  }

  async function publishAgent(uuid: string) {
    await publishAgentApi(uuid)
    const agent = agents.value.find(a => a.uuid === uuid)
    if (agent) {
      agent.status = 'PUBLISHED'
    }
  }

  return { agents, loading, loadAgents, saveAgent, deleteAgent, publishAgent }
})
