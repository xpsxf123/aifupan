<template>
  <div class="header-content">
    <div class="header-left">
      <!-- 全局搜索组件替换原有标题 -->
      <GlobalSearch />
    </div>
    <div class="header-right">
      <!--       消息通知
      <div class="header-icon-btn info-icon" @click="showNotification = true">
        <el-badge :is-dot="hasUnread" class="item">
          <el-icon><Bell /></el-icon>
        </el-badge>
      </div>
      &lt;!&ndash; 日历图标 (占位) &ndash;&gt;
      <div class="header-icon-btn">
        <el-icon><Calendar /></el-icon>
      </div>
      <div class="vertical-line"> </div>-->
      <el-dropdown trigger="hover" @command="handleCommand">
        <div class="user-info flex-ai-center cursor-pointer">
          <div class="avtar-container">
            <el-avatar :size="32" :src="userInfo.userAvatar || defaultImg" />
          </div>
          <span class="user-name ml-2">{{ userInfo.name || '用户' }}</span>
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 消息抽屉 -->
    <NotificationDrawer v-model="showNotification" @read="hasUnread = false" />
  </div>
</template>

<script setup>
  /**
   * @file layout/components/Header.vue
   * @description 顶部导航栏组件，包含系统标题和用户信息
   */
  import { computed } from 'vue'
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useUserStore } from '@/store/user'
  import { ArrowDown, Bell, Calendar } from '@element-plus/icons-vue'
  import GlobalSearch from '@/components/GlobalSearch/index.vue'
  import NotificationDrawer from '@/components/NotificationDrawer/index.vue'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const router = useRouter()
  const userStore = useUserStore()

 /* // 默认头像
  const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'*/

  const showNotification = ref(false)
  const hasUnread = ref(true) // 模拟有未读消息

  /**
   * @description 用户信息
   */
  const userInfo = computed(() => userStore.userInfo)

  /**
   * @description 处理下拉菜单命令
   * @param {String} command 命令
   */
  const handleCommand = async (command) => {
    if (command === 'logout') {
      await handleLogout()
    }
  }

  /**
   * @description 退出登录
   */
  const handleLogout = async () => {
    try {
      await userStore.logout()
      router.push('/login')
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }
</script>

<style lang="scss" scoped>
  .header-content {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .header-left {
    display: flex;
    align-items: center;
  }
  .header-right {
    display: flex;
    align-items: center;
    gap: 24px; // 增加间距
  }
  .vertical-line {
    width: 2px;
    height: 24px;
    background-color: #abaeb3;
  }

  .header-icon-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 20px;
    color: #606266;

    &:hover {
      color: var(--el-color-primary);
    }
  }

  .info-icon {
    margin-right: -5px;
  }

  .item {
    display: flex;
    align-items: center;
  }
  .user-info {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #606266;
    outline: none;
    .avtar-container {
      display: flex;
      align-items: center;
      margin-right: 10px;
    }
  }
  .user-name {
    margin-left: 8px;
    margin-right: 4px;
  }
  .ml-2 {
    margin-left: 8px;
  }
  .cursor-pointer {
    cursor: pointer;
  }
  .flex-ai-center {
    display: flex;
    align-items: center;
  }
</style>
