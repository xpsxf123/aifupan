<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useSystemInfoStore } from '@/store'
import BreadCrumb from '@/layout/headerNav/breadCrumb/index.vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/store/index.js'
import { storeToRefs } from 'pinia'
import { clearLoginInfo } from '@/utils/index.js'
import api from '@/utils/request-api.js'
import emitter from '@/utils/emitter.js'
// 获取路由实例
const router = useRouter()
const {collapse, isMobile} = storeToRefs(useSystemInfoStore())
const {loginResultData} = storeToRefs(useUserInfoStore())
const isFullscreen = ref(false)
const dialogVisible = ref(false)
const dataFormRef = ref(null)
const dataForm = reactive({
  password: '',
  newPassword: '',
  checkPassword: ''
})
const dataRule = reactive({
  password: [{required: true, message: '原密码不能为空', trigger: 'blur'}],
  newPassword: [{required: true, message: '新密码不能为空', trigger: 'blur'}],
  checkPassword: [
    {required: true, message: '确认密码不能为空', trigger: 'blur'}
  ]
})
const iconStyle = computed(() => {
  return {
    width: '20px',
    height: '20px'
  }
})
// 折叠侧边栏
const handleFold = () => {
  collapse.value = !collapse.value
}
const showUpdatePwd = () => {
  // 重置表单数据
  Object.assign(dataForm, {
    password: '',
    newPassword: '',
    checkPassword: ''
  })
  dialogVisible.value = true
}

//更新密码
const updatePassword = async () => {
  if (!dataFormRef.value) return
  const valid = await dataFormRef.value.validate()
  if (!valid) return
  if (dataForm.newPassword !== dataForm.checkPassword) {
    ElMessage.error('两次输入密码不一致')
    return
  }
  const res = await api.user.updatePassword(dataForm)
  ElMessage.success(res.msg)
  clearLoginInfo()
  router.push({name: 'login'})
  dialogVisible.value = false
}

// 退出登录
const signOut = async () => {
  await ElMessageBox.confirm('确认退出', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const logoutRes = await api.user.logout({})
  if (logoutRes.code === 0) {
    clearLoginInfo()
    await router.push({name: 'login'})
  }
}

// 处理弹窗关闭的回调
const handleClose = () => {
  dataFormRef.value.resetFields()
}

// 处理全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

// 切换全屏状态
const toggleFullScreen = async () => {
  try {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
    } else {
      if (document.exitFullscreen) {
        await document.exitFullscreen()
      }
    }
  } catch (error) {
    if (isMobile) {
      ElMessage.error('移动端不支持全屏操作')
      return
    }
    ElMessage.error('全屏操作失败')
  }
}

// 刷新
const handleRefresh = () => {
  emitter.emit('changeRefreshFlag')
}

// 生命周期钩子
onMounted(() => {
  // 监听全屏状态变化
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  // 移除事件监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})
</script>

<template>
  <div class="header-nav">
    <div class="left-content">
      <div class="icon" @click="handleFold">
        <SvgIcon :iconStyle="iconStyle" name="fold"/>
      </div>
      <div class="bread-crumb-container">
        <BreadCrumb/>
      </div>
    </div>
    <div class="right-content">
      <el-tooltip
          :enterable="false"
          :hide-after="0"
          class="box-item"
          content="局部内容刷新（可用于重置搜索条件）"
          effect="dark"
          placement="bottom"
          trigger="hover"
      >
        <div class="refresh" @click="handleRefresh">
          <SvgIcon name="refresh"/>
        </div>
      </el-tooltip>
      <div class="fullscreen-icon" @click="toggleFullScreen">
        <SvgIcon :iconStyle="iconStyle" name="fullscreen"/>
      </div>
      <div class="setting">
        <SvgIcon
            :icon-style="{ width: '12px', height: '12px' }"
            class="online"
            name="dot"
        />
        <el-dropdown :teleported="false">
          <div class="avatar">
            <SvgIcon
                :icon-style="{ width: '30px', height: '30px' }"
                name="avatar"
            ></SvgIcon>
            <span>{{ loginResultData?.nickName ?? '' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="showUpdatePwd"
              >修改密码
              </el-dropdown-item
              >
            </el-dropdown-menu>
            <el-dropdown-menu>
              <el-dropdown-item @click="signOut">退出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <!--修改密码弹出-->
    <el-dialog
        v-model="dialogVisible"
        :close-on-click-modal="false"
        :width="480"
        append-to-body
        title="修改密码"
        @close="handleClose"
    >
      <el-form
          ref="dataFormRef"
          :model="dataForm"
          :rules="dataRule"
          label-width="100px"
      >
        <el-form-item label="原密码" prop="password">
          <el-input
              v-model="dataForm.password"
              placeholder="请输入原密码"
              show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
              v-model="dataForm.newPassword"
              placeholder="请输入新密码"
              show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="确认新密码" prop="checkPassword">
          <el-input
              v-model="dataForm.checkPassword"
              placeholder="请再次输入新密码"
              show-password
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="updatePassword">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  user-select: none;

  .left-content {
    height: 100%;
    padding: 0 10px;
    display: flex;
    gap: 20px;
    cursor: pointer;

    .icon {
      display: flex;
      flex-shrink: 0;
      align-items: center;
      padding: 0 10px;
      height: 100%;

      &:hover {
        background-color: #e4e1e1;
      }
    }

    .bread-crumb-container {
      display: flex;
      align-items: center;
      @media (max-width: 770px) {
        & {
          display: none;
        }
      }
    }
  }

  .right-content {
    height: 100%;
    display: flex;
    gap: 10px;
    align-items: center;

    .refresh {
      display: flex;
      flex-shrink: 0;
      align-items: center;
      padding: 0 10px;
      height: 100%;
      cursor: pointer;

      &:hover {
        background-color: #e4e1e1;
      }
    }

    .fullscreen-icon {
      display: flex;
      flex-shrink: 0;
      align-items: center;
      vertical-align: bottom;
      cursor: pointer;
      height: 100%;
      padding: 0 10px;

      &:hover {
        background-color: #e4e1e1;
      }
    }

    .setting {
      position: relative;
      height: 100%;
      flex-shrink: 0;

      .online {
        position: absolute;
        left: 30px;
        top: 37px;
        z-index: 999;
      }

      .el-dropdown {
        height: 100%;

        margin-right: 10px;

        .avatar {
          padding: 0 10px;
          display: flex;

          align-items: center;
          gap: 8px;
          cursor: pointer;
          transition: background-color 0.3s ease;
          outline: none; // 移除默认的黑色边框
          &:hover {
            background-color: #e4e1e1;
          }

          span {
            font-size: 14px;
            color: #303133;
            user-select: none;
          }
        }
      }
    }
  }
}
</style>
