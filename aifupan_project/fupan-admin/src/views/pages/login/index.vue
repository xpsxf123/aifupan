<template>
  <div class="page-login">
    <div class="content__wrapper">
      <main class="content">
        <div class="login-body">
          <div class="login-form__wrapper">
            <div class="title">欢迎登录</div>
            <div class="subtitle">请输入您的账号和密码</div>
            <el-form
                ref="formRef"
                :model="loginDF"
                :rules="dataRule"
                @keyup.enter="loginHandler"
            >
              <el-form-item prop="username">
                <el-input
                    v-model="loginDF.username"
                    clearable
                    placeholder="请输入账号"
                >
                  <template #prefix>
                    <el-icon>
                      <User/>
                    </el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                    v-model="loginDF.password"
                    placeholder="请输入密码"
                    show-password
                >
                  <template #prefix>
                    <el-icon>
                      <Lock/>
                    </el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-button
                  :loading="isLoading"
                  class="login-btn"
                  type="primary"
                  @click="loginHandler"
              >
                {{ isLoading ? '登入中...' : '登 入' }}
                <template #icon>
                  <SvgIcon name="login"/>
                </template>
              </el-button>
            </el-form>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/store'
import api from '@/utils/request-api'
import { User, Lock } from '@element-plus/icons-vue'
// 获取路由实例
const router = useRouter()

// 获取 store 实例
const userInfoStore = useUserInfoStore()

// 响应式数据
const formRef = ref(null)
const isLoading = ref(false)
const firstMenu = ref({})

// 表单数据
const loginDF = reactive({
  username: '',
  password: '',
  userType: 1
})

// 表单验证规则
const dataRule = reactive({
  username: [
    {
      required: true,
      message: '请输入账号',
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur'
    }
  ]
})

/**
 * 递归获取第一个可访问的菜单
 * @param {Object} menu - 菜单对象
 * @param {Array} menu.children - 子菜单列表
 * @param {string} menu.url - 菜单URL
 */
const getFirstMenu = (menu) => {
  if (!menu.children || menu.children.length < 1) {
    firstMenu.value = menu
  } else {
    getFirstMenu(menu.children[0])
  }
}

/**
 * 处理用户登录
 * @returns {Promise<void>}
 * @throws {Error} 登录失败时抛出错误
 */
const loginHandler = async () => {
  try {
    // 表单验证
    const isValid = await formRef.value?.validate()
    if (!isValid) return

    isLoading.value = true

    // 调用登录 API
    const response = await api.user.login(loginDF)

    // 检查权限
    if (!response.data.menuList || response.data.menuList.length === 0) {
      ElMessage.error('此账户没有权限')
      return
    }

    // 保存登录信息到 store
    userInfoStore.saveLoginResultData(response.data)
    userInfoStore.setLoadRouterFlag(false)

    // 获取第一个菜单并跳转
    getFirstMenu(response.data.menuTreeList[0])
    await router.push({path: firstMenu.value.url})
    // 显示成功消息
    ElMessage.success('登录成功')
  } finally {
    isLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-login {
  width: 100vw;
  height: 100vh;
  background: url('@/assets/images/back.png') no-repeat center center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  position: relative;
}

.page-login::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(800px 400px at 20% 20%, rgba(64, 158, 255, 0.15), transparent 60%),
  radial-gradient(700px 500px at 80% 0%, rgba(255, 120, 80, 0.14), transparent 60%),
  linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.06) 100%);
  pointer-events: none;
}

.content__wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.content {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  max-width: 1200px;
}

.login-body {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.login-form__wrapper {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.78);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 20px 45px rgba(31, 45, 61, 0.14),
  0 1px 0 rgba(255, 255, 255, 0.5) inset;
  backdrop-filter: blur(10px) saturate(120%);
  transition: all 0.25s ease;

  &:hover {
    box-shadow: 0 26px 52px rgba(31, 45, 61, 0.18);
  }

  .title {
    font-size: 30px;
    font-weight: 600;
    color: #1f2d3d;
    margin-bottom: 8px;
    text-align: center;
  }

  .subtitle {
    font-size: 14px;
    color: #6c7a89;
    margin-bottom: 30px;
    text-align: center;
  }

  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  :deep(.el-input__wrapper) {
    height: 48px;
    border-radius: 8px;
    transition: box-shadow 0.2s ease, transform 0.08s ease;

    .el-input__inner {
      height: 48px;
      line-height: 48px;
      font-size: 15px;
    }

    &.is-focus {
      box-shadow: 0 0 0 1.2px #409eff inset;
    }
  }

  :deep(.el-input__prefix) {
    left: 10px;
    color: #7f8fa6;
    font-size: 18px;
    display: flex;
    align-items: center;
  }

  .login-btn {
    width: 100%;
    height: 48px;
    margin-top: 10px;
    color: #fff;
    font-size: 16px;
    font-weight: 500;
    border-radius: 8px;
    border: none;
    position: relative;
    overflow: hidden;
    background-size: 200% 200%;
    background: linear-gradient(135deg, #409eff 0%, #2f6fe2 100%) 0 50%;
    transition: background-position 0.35s ease, box-shadow 0.3s ease, filter 0.3s ease;

    &:hover {
      filter: brightness(1.03);
      background-position: 100% 50%;
      box-shadow: 0 10px 24px rgba(47, 111, 226, 0.35);
    }

    &:active {
      filter: brightness(0.98);
      box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.18);
    }

    &.is-loading {
      transform: none;
    }

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: -60%;
      width: 40%;
      height: 100%;
      background: linear-gradient(
              120deg,
              rgba(255, 255, 255, 0) 0%,
              rgba(255, 255, 255, 0.35) 50%,
              rgba(255, 255, 255, 0) 100%
      );
      transform: skewX(-20deg);
      opacity: 0;
      transition: left 0.6s ease, opacity 0.25s ease;
      pointer-events: none;
    }

    &:hover::after {
      left: 120%;
      opacity: 1;
    }
  }
}
</style>
