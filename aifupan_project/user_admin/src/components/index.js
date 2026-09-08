/**
 * @description: 全局组件注册
 */
import SvgIcon from '@/components/SvgIcon/index.vue'
import CloseSvg from '@/components/CloseSvgIcon/index.vue'
const allGlobalComponents = {
  SvgIcon,
  CloseSvg
}

export default {
  install(app) {
    Object.keys(allGlobalComponents).forEach((key) => {
      // 使用 app.component 来注册组件
      app.component(key, allGlobalComponents[key])
    })
  }
}
