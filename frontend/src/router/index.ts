import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/views/layout/MainLayout.vue'),
    redirect: '/graph',
    children: [
      {
        path: 'ontology',
        name: 'Ontology',
        component: () => import('@/views/ontology/OntologyManager.vue'),
        meta: { title: '本体管理' },
      },
      {
        path: 'graph',
        name: 'KnowledgeGraph',
        component: () => import('@/views/graph/KnowledgeGraph.vue'),
        meta: { title: '知识图谱' },
      },
      {
        path: 'rules',
        name: 'RuleList',
        component: () => import('@/views/rule/RuleList.vue'),
        meta: { title: '规则管理' },
      },
      {
        path: 'rules/editor/:id?',
        name: 'RuleEditor',
        component: () => import('@/views/rule/RuleEditor.vue'),
        meta: { title: '规则编辑器' },
      },
      {
        path: 'rules/test',
        name: 'RuleTestBench',
        component: () => import('@/views/rule/RuleTestBench.vue'),
        meta: { title: '规则测试台' },
      },
      {
        path: 'simulation',
        name: 'Simulation',
        component: () => import('@/views/simulation/SimulationPanel.vue'),
        meta: { title: '仿真数据' },
      },
      {
        path: 'killchain',
        name: 'KillChainList',
        component: () => import('@/views/killchain/KillChainTaskList.vue'),
        meta: { title: '杀伤链建模' },
      },
      {
        path: 'killchain/modeler/:id',
        name: 'KillChainModeler',
        component: () => import('@/views/killchain/KillChainModeler.vue'),
        meta: { title: '建模画布' },
      },
      {
        path: 'killchain/analysis/:id',
        name: 'KillChainAnalysis',
        component: () => import('@/views/killchain/KillChainAnalysis.vue'),
        meta: { title: '匹配性分析' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
