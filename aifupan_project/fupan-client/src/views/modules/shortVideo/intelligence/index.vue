<!--
@description 短视频智能体落地页：替换原 /expert 导航入口，展示短视频智能体能力并跳转 AI 工作台短视频板块。
注意：旧搜达人页面仍保留在 shortVideo/expert/index.vue，本页面只作为新的导航承载页。
-->
<template>
    <div class="short-video-intelligence-page">
        <section class="short-video-intelligence">
            <div class="short-video-intelligence__breadcrumbs">
                <span class="short-video-intelligence__crumb is-primary">
                    <span class="short-video-intelligence__crumb-dot"></span>
                    <span>AI 智能体工作台</span>
                    /
                    <span>短视频智能体</span>
                </span>
                <!-- <span class="short-video-intelligence__crumb"></span> -->
            </div>

            <div class="short-video-intelligence__hero">
                <span class="short-video-intelligence__badge">⚡ 短视频板块 · 重磅上新</span>
                <div class="short-video-intelligence__eyebrow">SHORT VIDEO INTELLIGENCE</div>
                <h1 class="short-video-intelligence__title">
                    把爆款拆成<span class="is-gradient">可复制的增长公式</span>
                </h1>
                <p class="short-video-intelligence__desc">
                    让 AI 看懂视频、学习高手、发现机会，从结构拆解到文案生成一站式完成，一个人也能日产百条爆款。
                </p>
                <div class="short-video-intelligence__metrics">
                    <div
                        v-for="item in metrics"
                        :key="item.label"
                        class="short-video-intelligence__metric"
                    >
                        <span class="short-video-intelligence__metric-value">{{ item.value }}</span>
                        <span class="short-video-intelligence__metric-label">{{ item.label }}</span>
                    </div>
                </div>
            </div>

            <div class="short-video-intelligence__cards">
                <article
                    v-for="item in capabilityList"
                    :key="item.order"
                    class="short-video-intelligence__card"
                    :class="`is-${item.theme}`"
                >
                    <div class="short-video-intelligence__card-icon">
                        <i :class="item.icon"></i>
                    </div>
                    <div class="short-video-intelligence__card-step">STEP {{ item.order }}</div>
                    <h2 class="short-video-intelligence__card-title">{{ item.title }}</h2>
                    <p class="short-video-intelligence__card-desc">{{ item.desc }}</p>
                    <span class="short-video-intelligence__card-order">{{ item.order }}</span>
                </article>
            </div>

            <div class="short-video-intelligence__footer">
                <p class="short-video-intelligence__footer-tip">5 分钟干一周的活，今天就做出下一条爆款</p>
                <button
                    type="button"
                    class="short-video-intelligence__action"
                    @click="openInfluencerWorkbench"
                >
                    <span>点我立即体验</span>
                    <i class="el-icon-top-right"></i>
                </button>
            </div>
        </section>
    </div>
</template>

<script>
/**
 * @file 短视频智能体落地页。
 * @description 提供短视频板块的能力介绍与统一入口，点击 CTA 后跳转到 AI 工作台 `panel=influencer` 板块。
 */
import { AI_WORKBENCH_PANELS } from '@/utils/aiAgentRoute'

export default {
    name: 'ShortVideoIntelligencePage',
    data() {
        return {
            metrics: [
                {
                    value: '100+',
                    label: '日产爆款文案'
                },
                {
                    value: '5min',
                    label: '一条完整脚本'
                },
                {
                    value: '10x',
                    label: '内容产能提升'
                }
            ],
            capabilityList: [
                {
                    order: '1',
                    theme: 'blue',
                    icon: 'el-icon-search',
                    title: '短视频结构拆解',
                    desc: '逮帧看懂爆款的开场钩子、节奏与转化结构'
                },
                {
                    order: '2',
                    theme: 'violet',
                    icon: 'el-icon-share',
                    title: '博主方法论蒸馏',
                    desc: '提炼头部达人的创作套路，沉淀为你的 SOP'
                },
                {
                    order: '3',
                    theme: 'cyan',
                    icon: 'el-icon-top-right',
                    title: '爆款选题生成',
                    desc: '结合实时趋势，智能推荐可拍可爆的选题'
                },
                {
                    order: '4',
                    theme: 'purple',
                    icon: 'el-icon-document',
                    title: '文案仿写',
                    desc: '一键续写同款高转化脚本，风格能取能用'
                },
                {
                    order: '5',
                    theme: 'orange',
                    icon: 'el-icon-star-on',
                    title: '竞争差异分析',
                    desc: '多维对标竞品，定位属于你的独家突破口'
                }
            ]
        }
    },
    methods: {
        /**
         * @description 查询当前账号已订阅达人总数；若为 0，则弹出引导并阻止进入短视频智能体工作台。
         * @returns {Promise<boolean>} true 表示可继续跳转，false 表示已拦截
         */
        async ensureSubscribedInfluencers() {
            const result = await this.$httpBack.shortVideo.subscriptions({})
            if (result?.code !== 0) {
                this.$message.error(result?.msg || '获取订阅达人列表失败，请稍后再试')
                return false
            }
            const groupList = Array.isArray(result?.data) ? result.data : []
            const totalCount = groupList.reduce((sum, group) => {
                return sum + (Array.isArray(group?.influencers) ? group.influencers.length : 0)
            }, 0)
            if (totalCount > 0) {
                return true
            }
            try {
                await this.$confirm(
                    `
                    <div style="text-align:center;line-height:1.8;">
                        请先订阅达人后再进行短视频创作，否则智能体无法给到您精准的创作方案
                    </div>
                    `,
                    '请先订阅达人',
                    {
                        confirmButtonText: '去订阅',
                        cancelButtonText: '知道了',
                        // type: 'warning',
                        customClass: 'format-front confirm-common-box confirm-btns-center confirm-text-center message-box-500',
                        dangerouslyUseHTMLString: true,
                        closeOnClickModal: false,
                        closeOnPressEscape: false,
                        showClose: false
                    }
                )
                this.$router.push({
                    path: '/subscribeExpert'
                })
            } catch (error) {
            }
            return false
        },
        /**
         * @description 跳转 AI 工作台短视频板块，默认打开 influencer 面板。
         * @returns {Promise<void>}
         */
        async openInfluencerWorkbench() {
            const canOpenWorkbench = await this.ensureSubscribedInfluencers()
            if (!canOpenWorkbench) return
            if (this.$httpClient?.system?.openAIAgentWeb) {
                await this.$httpClient.system.openAIAgentWeb({
                    panel: AI_WORKBENCH_PANELS.INFLUENCER
                })
                return
            }
            this.$router.push({
                path: '/aiAgent',
                query: {
                    panel: AI_WORKBENCH_PANELS.INFLUENCER
                }
            })
        }
    }
}
</script>

<style scoped lang="scss" src="./index.scss"></style>
