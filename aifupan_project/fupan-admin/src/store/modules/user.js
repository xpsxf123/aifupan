import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserInfoStore = defineStore(
  'userInfo',
  () => {
    const loginResultData = ref({})
    const token = ref('')
    const tipList = ref(null)
    const loadRouterFlag = ref(false)
    const breadcrumbList = ref([]) // 面包屑列表
    /**
     * @description 保存登录返回的信息
     * @param {object} val - 登录返回的数据
     */
    const saveLoginResultData = (val) => {
      token.value = val ? val.token || '' : ''
      loginResultData.value = val || {}
    }

    /**
     * @description 设置是否加载了动态路由标识
     * @param {boolean} val - 标识值
     */
    const setLoadRouterFlag = (val) => {
      loadRouterFlag.value = val
    }

    /**
     * @description 保存待办项
     * @param {Array|null} val - 待办事项列表
     */
    const saveTipList = (val) => {
      tipList.value = val || null
    }

    return {
      loginResultData,
      token,
      tipList,
      loadRouterFlag,
      breadcrumbList,
      saveLoginResultData,
      setLoadRouterFlag,
      saveTipList,
    }
  },
  {
    persist: {
      key: 'replayVuex',
      pick: ['loginResultData', 'token', 'systemInfo'],
    },
  }
)
