import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSystemInfoStore = defineStore('systemInfo', () => {
    const collapse = ref(false) // false展开 true收起
    const isMobile = ref(false) // 是否是移动端
    const detectDevice = () => {
        const ua = navigator.userAgent.toLowerCase()

        let mobile = /mobile|android|iphone|ipod|phone/i.test(ua)

        // 额外检测触摸，兼容 iPadOS 13+
        if (!mobile && navigator.maxTouchPoints > 1) {
            mobile = true
        }
        isMobile.value = mobile
    }

    // 初始化执行一次
    detectDevice()
    return {
        collapse,
        isMobile
    }
})
