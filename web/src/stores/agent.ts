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
      const idx = agents.value.findIndex(a => a.id === saved.id)
      if (idx >= 0) {
        agents.value[idx] = saved
      } else {
        agents.value.unshift(saved)
      }
    }
    return saved
  }

  async function deleteAgent(id: number) {
    await deleteAgentApi(id)
    agents.value = agents.value.filter(a => a.id !== id)
  }

  async function publishAgent(id: number) {
    await publishAgentApi(id)
    const agent = agents.value.find(a => a.id === id)
    if (agent) {
      agent.status = 'PUBLISHED'
    }
  }

  return { agents, loading, loadAgents, saveAgent, deleteAgent, publishAgent }
})
