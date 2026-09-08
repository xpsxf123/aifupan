<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.go(-1)
}

const goLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="error-page">
    <div class="error-container">
      <div class="error-content">
        <div class="error-number">404</div>
        <div class="error-title">页面未找到</div>
        <div class="error-description">抱歉，您访问的页面不存在或已被删除</div>
        <div class="error-actions">
          <el-button @click="goBack"> 返回上页 </el-button>
          <el-button type="info" @click="goLogin"> 重新登录 </el-button>
        </div>
      </div>
      <div class="error-illustration">
        <svg viewBox="0 0 404 300" class="error-svg">
          <defs>
            <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color: #409eff; stop-opacity: 1" />
              <stop
                offset="100%"
                style="stop-color: #67c23a; stop-opacity: 1"
              />
            </linearGradient>
          </defs>
          <text
            x="50%"
            y="50%"
            text-anchor="middle"
            dominant-baseline="middle"
            fill="url(#gradient)"
            font-size="120"
            font-weight="bold"
            opacity="0.1"
          >
            404
          </text>
          <circle cx="150" cy="100" r="20" fill="#409EFF" opacity="0.3">
            <animate
              attributeName="r"
              values="20;25;20"
              dur="2s"
              repeatCount="indefinite"
            />
          </circle>
          <circle cx="250" cy="150" r="15" fill="#67C23A" opacity="0.4">
            <animate
              attributeName="r"
              values="15;20;15"
              dur="1.5s"
              repeatCount="indefinite"
            />
          </circle>
          <circle cx="320" cy="80" r="18" fill="#E6A23C" opacity="0.3">
            <animate
              attributeName="r"
              values="18;23;18"
              dur="1.8s"
              repeatCount="indefinite"
            />
          </circle>
        </svg>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  max-width: 1200px;
  width: 100%;
  gap: 60px;
}

.error-content {
  flex: 1;
  max-width: 500px;
  text-align: center;
}

.error-number {
  font-size: 120px;
  font-weight: bold;
  color: #409eff;
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.error-description {
  font-size: 16px;
  color: #606266;
  margin-bottom: 40px;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.error-actions .el-button {
  min-width: 120px;
  height: 40px;
}

.error-illustration {
  flex: 1;
  max-width: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.error-svg {
  width: 100%;
  height: 300px;
  max-width: 400px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .error-container {
    flex-direction: column;
    gap: 40px;
    text-align: center;
  }

  .error-number {
    font-size: 80px;
  }

  .error-title {
    font-size: 24px;
  }

  .error-actions {
    flex-direction: column;
    align-items: center;
  }

  .error-actions .el-button {
    width: 200px;
  }

  .error-svg {
    height: 200px;
  }
}

@media (max-width: 480px) {
  .error-page {
    padding: 10px;
  }

  .error-number {
    font-size: 60px;
  }

  .error-title {
    font-size: 20px;
  }

  .error-description {
    font-size: 14px;
  }
}
</style>
