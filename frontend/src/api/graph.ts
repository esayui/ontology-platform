import request from './request'

export const graphApi = {
  getAllNodes() {
    return request.get<any, { data: any[] }>('/graph/nodes')
  },

  getNeighbors(nodeId: string) {
    return request.get<any, { data: any[] }>(`/graph/neighbors/${nodeId}`)
  },

  getUpstream(nodeId: string) {
    return request.get<any, { data: any[] }>(`/graph/upstream/${nodeId}`)
  },

  getDownstream(nodeId: string) {
    return request.get<any, { data: any[] }>(`/graph/downstream/${nodeId}`)
  },

  findPath(source: string, target: string) {
    return request.get<any, { data: any[] }>('/graph/path', { params: { source, target } })
  },

  getViolationPaths() {
    return request.get<any, { data: any[] }>('/graph/violation-paths')
  },

  analyzeCentrality() {
    return request.get<any, { data: any }>('/graph/analysis/centrality')
  },

  findIsolated() {
    return request.get<any, { data: string[] }>('/graph/analysis/isolated')
  },
}
