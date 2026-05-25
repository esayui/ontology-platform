<template>
  <a-layout style="height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible theme="dark" width="220">
      <div class="logo">
        <span v-if="!collapsed">本体规则一体化平台</span>
        <span v-else>平台</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        @click="handleMenuClick"
      >
        <a-menu-item key="/graph">
          <ShareAltOutlined />
          <span>知识图谱</span>
        </a-menu-item>
        <a-menu-item key="/ontology">
          <ApartmentOutlined />
          <span>本体管理</span>
        </a-menu-item>
        <a-menu-item key="/rules">
          <OrderedListOutlined />
          <span>规则管理</span>
        </a-menu-item>
        <a-menu-item key="/rules/test">
          <ExperimentOutlined />
          <span>规则测试台</span>
        </a-menu-item>
        <a-menu-item key="/simulation">
          <DashboardOutlined />
          <span>仿真数据</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <a-space>
          <a-avatar size="small" icon="user" />
          <span>管理员</span>
          <a-button type="link" @click="handleLogout">退出</a-button>
        </a-space>
      </a-layout-header>

      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ShareAltOutlined, ApartmentOutlined, OrderedListOutlined,
  ExperimentOutlined, DashboardOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)
const selectedKeys = ref([route.path])

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.08);
}
.header {
  background: #fff;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.content {
  margin: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  overflow: auto;
}
</style>
