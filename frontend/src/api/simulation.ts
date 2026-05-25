import request from './request'

export const simulationApi = {
  ingestData(dataList: any[]) {
    return request.post<any, { data: any[] }>('/simulation/ingest', dataList)
  },

  getByIndicator(indicatorId: string) {
    return request.get<any, { data: any[] }>(`/simulation/data/${indicatorId}`)
  },

  getLatest(limit = 50) {
    return request.get<any, { data: any[] }>('/simulation/data/latest', { params: { limit } })
  },
}
