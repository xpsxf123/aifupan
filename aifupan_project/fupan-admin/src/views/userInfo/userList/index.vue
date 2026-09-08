<template>
  <div class="mod-config">
    <transition mode="out-in" name="simple-fade-move">
      <component
        :is="componentName"
        :key="componentName"
        :userId="userId"
        @show-detail="switchCom"
      />
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, computed, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import userDetail from './components/userDetail.vue'
import userListData from './components/userListData.vue'
import emitter from '@/utils/emitter'
const route = useRoute()
const userId = ref('')

const componentName = shallowRef(userListData)

// 映射组件
const componentMap = {
  userListData,
  userDetail,
}

// 切换组件
const switchCom = ({ userId: newUserId, componentName: newComponentName }) => {
  userId.value = newUserId
  componentName.value = componentMap[newComponentName]
  if (newComponentName === userDetail) {
    document.querySelector('.right-main').scrollTop = 0
  }
}
emitter.on('switchCom', switchCom)

onMounted(() => {
  const query = route.query
  if (query && query.userId && query.componentName) {
    switchCom({ componentName: 'userDetail', userId: query.userId })
  }
})
onUnmounted(() => {
  emitter.off('switchCom', switchCom)
})
</script>
<style lang="scss" scoped>
.el-breadcrumb {
  margin-bottom: 15px;
}

.simple-fade-move-enter-active,
.simple-fade-move-leave-active {
  transition: all 0.35s ease;
}

.simple-fade-move-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.simple-fade-move-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
