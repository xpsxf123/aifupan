<template>
  <div class="forbidden-page">
    <div class="forbidden-page__content">
      <div class="forbidden-page__illustration">
        <div class="forbidden-page__lock forbidden-page__lock--float">
          <el-icon class="forbidden-page__lock-icon">
            <Lock />
          </el-icon>
        </div>
      </div>

      <div class="forbidden-page__title">你不在当前应用的可用范围中</div>
      <div class="forbidden-page__desc">如需访问，请联系管理员配置权限</div>

      <div class="forbidden-page__actions">
        <el-button class="forbidden-page__action" round plain @click="goBack">
          返回上一页
        </el-button>
        <el-button
          v-if="hasToken"
          class="forbidden-page__action"
          round
          plain
          @click="logoutAndGoLogin"
        >
          退出登录
        </el-button>
        <el-button v-else class="forbidden-page__action" round plain @click="goLogin">
          去登录
        </el-button>
      </div>

      <div v-if="fromPath" class="forbidden-page__from">来源：{{ fromPath }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const fromPath = computed(() => {
  const raw = route.query?.from ?? route.query?.redirect
  return typeof raw === 'string' ? raw : ''
})

const hasToken = computed(() => Boolean(userStore.token))

const goLogin = () => {
  const redirect = fromPath.value ? { redirect: fromPath.value } : {}
  router.replace({ path: '/login', query: redirect })
}

const logoutAndGoLogin = async () => {
  await userStore.logout()
  goLogin()
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.forbidden-page {
  position: relative;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f3f5;
}

.forbidden-page__content {
  width: 100%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.forbidden-page__illustration {
  position: relative;
  width: 260px;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;
}

.forbidden-page__lock {
  position: relative;
  width: 112px;
  height: 112px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e6e7ea;
  box-shadow: 0 12px 26px rgba(24, 44, 77, 0.12);
}

.forbidden-page__lock--float {
  animation: forbiddenPageFloat 1.8s ease-in-out infinite;
}

.forbidden-page__lock-icon {
  font-size: 54px;
  color: rgba(31, 45, 61, 0.55);
}

@keyframes forbiddenPageFloat {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

.forbidden-page__title {
  font-size: 18px;
  font-weight: 700;
  color: rgba(31, 45, 61, 0.9);
}

.forbidden-page__desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 18px;
  color: rgba(31, 45, 61, 0.6);
}

.forbidden-page__actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}

.forbidden-page__action {
  min-width: 118px;
  padding: 10px 14px;
  border-color: #d9dadd;
  background: rgba(255, 255, 255, 0.55);
  color: rgba(31, 45, 61, 0.82);
}

.forbidden-page__from {
  margin-top: 16px;
  max-width: 520px;
  font-size: 12px;
  line-height: 18px;
  color: rgba(31, 45, 61, 0.45);
  word-break: break-all;
}
</style>
