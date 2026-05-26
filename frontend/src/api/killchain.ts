import request from './request'
import type { KillChainTask } from '@/types/killchain'

export const killChainApi = {
  listTasks(page = 1, size = 20, keyword?: string) {
    return request.get<any, { data: any }>('/killchain/tasks', { params: { page, size, keyword } })
  },
  getTask(id: string) {
    return request.get<any, { data: KillChainTask }>(`/killchain/tasks/${id}`)
  },
  createTask(data: Partial<KillChainTask>) {
    return request.post<any, { data: KillChainTask }>('/killchain/tasks', data)
  },
  updateTask(data: Partial<KillChainTask>) {
    return request.put<any, { data: KillChainTask }>('/killchain/tasks', data)
  },
  deleteTask(id: string) {
    return request.delete(`/killchain/tasks/${id}`)
  },
  saveModel(id: string, data: Record<string, any>) {
    return request.post(`/killchain/tasks/${id}/save-model`, data)
  },
}
