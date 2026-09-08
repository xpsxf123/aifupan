<template>
    <div class="aiContentRenderer" v-html="displayHtml" @click="handleClick"></div>
</template>

<script>
import Vue from 'vue'
import { AI_RENDER_MODE_MAP, renderAiResponseContent } from './renderers/aiContentRenderParser'
import DataBlockMount from './dataBlocks/DataBlockMount.vue'
import { getDataBlockRegistryMap } from './dataBlocks/registry'
import { setDataBlock } from './dataBlockStore'
import './index.scss'

const normalizeRenderMode = (mode) => {
    if (!mode) return AI_RENDER_MODE_MAP.MD_TAG
    const modeStr = String(mode)
    const match = Object.values(AI_RENDER_MODE_MAP).find((it) => it === modeStr)
    if (match) return match
    if (modeStr === 'pureMd') return AI_RENDER_MODE_MAP.PURE_MD
    if (modeStr === 'mdTag') return AI_RENDER_MODE_MAP.MD_TAG
    if (modeStr === 'json') return AI_RENDER_MODE_MAP.JSON
    return AI_RENDER_MODE_MAP.MD_TAG
}

export default {
    props: {
        content: {
            type: String,
            default: ''
        },
        thinkingContent: {
            type: String,
            default: ''
        },
        renderMode: {
            type: String,
            default: AI_RENDER_MODE_MAP.MD_TAG
        },
        plainTextMode: {
            type: Boolean,
            default: false
        },
        option: {
            type: Object,
            default: () => ({})
        },
        enableLinkOpen: {
            type: Boolean,
            default: true
        },
        dataBlockRegistry: {
            type: Array,
            default: () => []
        },
        dataBlockScope: {
            type: String,
            default: 'global'
        }
    },
    data() {
        return {
            dataBlockVms: []
        }
    },
    computed: {
        resolvedParserMode() {
            if (this.plainTextMode) return AI_RENDER_MODE_MAP.PURE_MD
            return normalizeRenderMode(this.renderMode)
        },
        renderOption() {
            return {
                ...(this.option || {}),
                thinkingContent: this.thinkingContent,
                plainTextMode: this.plainTextMode,
                parserMode: this.resolvedParserMode
            }
        },
        renderResult() {
            return renderAiResponseContent(this.content || '', this.renderOption || {})
        },
        displayHtml() {
            return this.renderResult?.html || ''
        },
        dataBlocks() {
            return this.renderResult?.dataBlocks || []
        },
        dataBlockRegistryMap() {
            return getDataBlockRegistryMap(this.dataBlockRegistry || [])
        }
    },
    watch: {
        displayHtml() {
            this.applyDataBlocks()
        }
    },
    mounted() {
        this.applyDataBlocks()
    },
    beforeDestroy() {
        this.destroyDataBlockVms()
    },
    methods: {
        destroyDataBlockVms() {
            const list = Array.isArray(this.dataBlockVms) ? this.dataBlockVms : []
            list.forEach((vm) => {
                try {
                    vm?.$destroy?.()
                } catch (_) {}
            })
            this.dataBlockVms = []
        },
        applyDataBlocks() {
            this.destroyDataBlockVms()
            const blocks = Array.isArray(this.dataBlocks) ? this.dataBlocks : []
            blocks.forEach((b) => {
                setDataBlock(this.dataBlockScope, b?.cacheKey, b)
            })
            this.$nextTick(() => {
                const roots = this.$el?.querySelectorAll?.('.aifupan-data-block[data-cache-key]') || []
                Array.from(roots).forEach((el) => {
                    const cacheKey = el.getAttribute('data-cache-key')
                    const block = blocks.find((b) => String(b?.cacheKey) === String(cacheKey))
                    if (!block?.visible) return
                    if (!block?.type) return
                    const reg = this.dataBlockRegistryMap.get(String(block.type))
                    if (!reg?.component) return
                    const mountVm = new Vue({
                        parent: this,
                        render: (h) =>
                            h(DataBlockMount, {
                                props: {
                                    is: reg.component,
                                    bindProps: reg?.mapProps ? reg.mapProps(block) : { value: block?.data }
                                }
                            })
                    })
                    mountVm.$mount()
                    el.replaceWith(mountVm.$el)
                    this.dataBlockVms.push(mountVm)
                })
            })
        },
        handleClick(e) {
            if (!this.enableLinkOpen) return
            const root = e?.currentTarget
            if (!root) return
            let node = e?.target
            while (node && node !== root) {
                if (node.tagName === 'A') {
                    const href = node.getAttribute('href')
                    if (href && !href.startsWith('javascript:')) {
                        e.preventDefault()
                        try {
                            window.open(href, '_blank', 'noopener,noreferrer')
                        } catch (_) {}
                    }
                    return
                }
                node = node.parentNode
            }
        }
    }
}
</script>
