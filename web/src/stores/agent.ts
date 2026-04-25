import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AgentDTO, AgentSaveDTO } from '@/types/agent'
import { saveAgentApi, deleteAgentApi, listAgentApi, listAgentPageApi, publishAgentApi } from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
  const agents = ref<AgentDTO[]>([])
  const loading = ref(false)
  const currentPage = ref(0)
  const totalPages = ref(0)
  const pageSize = 6

  async function loadAgents() {
    loading.value = true
    try {
      const { data } = await listAgentApi()
      agents.value = data?.data ?? []
    } finally {
      loading.value = false
    }
  }

  async function loadAgentsPage(page?: number) {
    loading.value = true
    try {
      if (page !== undefined) currentPage.value = page
      const { data } = await listAgentPageApi({
        page: currentPage.value,
        size: pageSize
      })
      const p = data?.data
      agents.value = p?.records ?? []
      totalPages.value = p?.pages ?? 0
    } finally {
      loading.value = false
    }
  }

  async function saveAgent(dto: AgentSaveDTO): Promise<AgentDTO | null> {
    const { data } = await saveAgentApi(dto)
    const saved = data?.data ?? null
    if (saved) {
      await loadAgentsPage()
    }
    return saved
  }

  async function deleteAgent(uuid: string) {
    await deleteAgentApi(uuid)
    await loadAgentsPage()
  }

  async function publishAgent(uuid: string) {
    await publishAgentApi(uuid)
    await loadAgentsPage()
  }

  return { agents, loading, currentPage, totalPages, pageSize, loadAgents, loadAgentsPage, saveAgent, deleteAgent, publishAgent }
})
