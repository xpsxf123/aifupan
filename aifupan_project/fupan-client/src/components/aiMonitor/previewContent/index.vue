<!--
/**
 * @description AI 监控列表预览组件：对话术质检/还原度/互动巡检摘要做容错解析，并在解析异常时回退为“查看详情”入口。
 */
-->

<template>
    <div
        :class="previewClassList"
        @click="handlePreviewClick">
        <template v-if="previewModel.kind === 'metrics'">
            <div
                v-for="(item, index) in previewModel.items"
                :key="`${item.label}-${item.value}-${index}`"
                class="aiMonitorPreviewMetric">
                <span class="aiMonitorPreviewMetricLabel">{{ item.label }}：</span>
                <span class="aiMonitorPreviewMetricValue">{{ item.value }}</span>
            </div>
        </template>

        <el-button
            v-else-if="previewModel.kind === 'error'"
            type="text"
            class="aiMonitorPreviewDetailButton"
            @click.stop="handleView">
            查看详情
        </el-button>

        <div v-else-if="previewModel.kind === 'text'" class="aiMonitorPreviewText">
            {{ previewModel.text }}
        </div>

        <div v-else class="aiMonitorPreviewEmpty">-</div>
    </div>
</template>

<script>
import { buildAiMonitorPreviewModel } from '@/utils/aiMonitorPreviewParser'
import './index.scss'

export default {
    name: 'AiMonitorPreviewContent',
    props: {
        content: {
            type: [String, Array, Object],
            default: ''
        },
        previewType: {
            type: String,
            default: 'qc'
        },
        parseMode: {
            type: String,
            default: 'text'
        },
        withStatsStyle: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        /**
         * @description 生成当前预览的展示模型，内部已封装容错解析与异常兜底。
         * @returns {{kind:string, items?:Array, text?:string, message?:string}}
         */
        previewModel() {
            return buildAiMonitorPreviewModel(this.content, {
                type: this.previewType,
                parseMode: this.parseMode
            })
        },
        /**
         * @description 统一控制根节点样式，兼容录制列表里的 aiMonitorStats 视觉。
         * @returns {Array}
         */
        previewClassList() {
            return [
                'aiMonitorPreview',
                'aiMonitorPreviewContent',
                {
                    aiMonitorStats: this.withStatsStyle,
                    'aiMonitorPreviewContent--error': this.previewModel.kind === 'error'
                }
            ]
        }
    },
    methods: {
        /**
         * @description 点击预览时统一打开报告详情；异常态则由“查看详情”按钮触发同一逻辑。
         * @returns {void}
         */
        handlePreviewClick() {
            if (this.previewModel.kind === 'error') return
            this.handleView()
        },
        /**
         * @description 抛出查看详情事件，复用父层原有打开报告链路。
         * @returns {void}
         */
        handleView() {
            this.$emit('view')
        }
    }
}
</script>
