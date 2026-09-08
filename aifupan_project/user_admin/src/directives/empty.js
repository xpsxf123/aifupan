import { render, h, unref, watch } from 'vue'
import EmptyState from '@/components/EmptyState/index.vue'

/**
 * @description 全局空白页指令
 * @example
 * <div v-empty="{ visible: true, title: '暂无数据', buttons: [...] }"></div>
 */
export const empty = {
    mounted(el, binding) {
        const div = document.createElement('div')
        div.className = 'v-empty-container'
        div.style.position = 'absolute'
        div.style.top = '0'
        div.style.left = '0'
        div.style.width = '100%'
        div.style.height = '100%'
        div.style.zIndex = '100'

        el.emptyDiv = div
        setupWatch(el, binding)
    },

    updated(el, binding) {
        if (el.__emptySource !== binding.value) {
            setupWatch(el, binding)
            return
        }
        update(el, binding)
    },

    unmounted(el) {
        if (el.__emptyStop) {
            el.__emptyStop()
            el.__emptyStop = null
        }
        el.__emptySource = null
        // ✅ 销毁时还原 minHeight
        el.style.minHeight = el.__emptyOriginalMinHeight ?? ''
        el.__emptyOriginalMinHeight = null

        if (el.emptyDiv) {
            render(null, el.emptyDiv)
            if (el.contains(el.emptyDiv)) {
                el.removeChild(el.emptyDiv)
            }
            el.emptyDiv = null
        }
    }
}

function setupWatch(el, binding) {
    if (el.__emptyStop) {
        el.__emptyStop()
        el.__emptyStop = null
    }
    el.__emptySource = binding.value
    el.__emptyStop = watch(
        () => unref(binding.value),
        () => update(el, binding),
        { deep: true, immediate: true }
    )
}

function update(el, binding) {
    const raw = unref(binding.value)
    const value = raw && typeof raw === 'object' ? raw : {}

    if (value.visible) {
        const originalPosition = getComputedStyle(el).position
        if (originalPosition === 'static') {
            el.style.position = 'relative'
        }

        // ✅ 记录原始 minHeight（只记录一次），并设置最小高度
        if (el.__emptyOriginalMinHeight === undefined) {
            el.__emptyOriginalMinHeight = el.style.minHeight
        }
        el.style.minHeight = value.minHeight || '300px'

        if (!el.contains(el.emptyDiv)) {
            el.appendChild(el.emptyDiv)
        }

        const vnode = h(EmptyState, { ...value })
        if (binding.instance && binding.instance.$) {
            vnode.appContext = binding.instance.$.appContext
        }
        render(vnode, el.emptyDiv)
    } else {
        // ✅ 还原 minHeight
        if (el.__emptyOriginalMinHeight !== undefined) {
            el.style.minHeight = el.__emptyOriginalMinHeight
            el.__emptyOriginalMinHeight = undefined
        }

        if (el.contains(el.emptyDiv)) {
            render(null, el.emptyDiv)
            el.removeChild(el.emptyDiv)
        }
    }
}

export default empty