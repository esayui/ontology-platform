<template>
  <div class="login-container">
    <div class="login-card">
      <h1>装备能力指标本体库与规则引擎一体化平台</h1>
      <a-form :model="form" layout="vertical" @finish="handleLogin">
        <a-form-item label="用户名" name="username" :rules="[{ required: true }]">
          <a-input v-model:value="form.username" size="large" />
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true }]">
          <a-input-password v-model:value="form.password" size="large" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { message } from 'ant-design-vue'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

async function handleLogin() {
  loading.value = true
  try {
    const res = await axios.post('/api/auth/login', {
      username: form.username,
      password: form.password,
    })
    if (res.data.code === 200) {
      localStorage.setItem('token', res.data.data.token)
      message.success('登录成功')
      router.push('/graph')
    } else {
      message.error(res.data.message || '登录失败')
    }
  } catch (e: any) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
.login-card h1 {
  text-align: center;
  color: #1a1a2e;
  margin-bottom: 32px;
  font-size: 20px;
}
</style>
