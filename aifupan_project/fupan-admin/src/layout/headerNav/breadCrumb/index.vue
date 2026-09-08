<script setup>
import { useRoute } from 'vue-router'
import { useUserInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import emitter from '@/utils/emitter'
const router = useRouter()
const menuList = ref([])
const route = useRoute()

const userInfoStore = useUserInfoStore()
const { loginResultData, breadcrumbList } = storeToRefs(userInfoStore)
menuList.value = loginResultData.value.menuList

const findRoute = (menuList, path) => {
  // 建立一个 id -> item 的映射，加速查找
  const map = {}
  menuList.forEach((item) => {
    map[item.id] = item
  })

  // 找到目标节点
  const target = menuList.find((item) => item.url === path)
  if (!target) return []

  // 递归向上查找父节点
  const result = []
  let current = target
  while (current) {
    result.unshift(current) // 头插，保证顺序是从父到子
    current = map[current.parentId]
  }

  return result
}

const handleJump = (url) => {
  if (url === '/userInfo/userList') {
    router.push({ path: '/userInfo/userList' })
    emitter.emit('switchCom', { componentName: 'userListData' })
  }
}

// 监听路由变化，更新面包屑列表
watch(
  () => route.fullPath,
  (newVal) => {
    breadcrumbList.value = findRoute(menuList.value, route.path)
  },
  {
    immediate: true,
  }
)
</script>

<template>
  <el-breadcrumb separator="/">
    <TransitionGroup name="list" tag="span">
      <el-breadcrumb-item
        v-for="route in breadcrumbList"
        :key="route.id"
        class="breadcrumb-item"
        @click="handleJump(route.url)"
      >
        <a>{{ route.name }}</a>
      </el-breadcrumb-item>
    </TransitionGroup>
  </el-breadcrumb>
</template>

<style lang="scss" scoped>
.list-move, /* 对移动中的元素应用的过渡 */
.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 确保将离开的元素从布局流中删除
  以便能够正确地计算移动的动画。 */
.list-leave-active {
  position: absolute;
}
</style>
