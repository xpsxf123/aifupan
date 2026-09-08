import SvgIcon from '@/components/svgIcon/index.vue'

const globalComponents = {
  SvgIcon,
}

export default {
  install(Vue) {
    Object.keys(globalComponents).forEach((key) => {
      Vue.component(key, globalComponents[key])
    })
  },
}
