<template>
    <div class="ai-agent-page">
        <div class="ai-agent-panel">
            <div class="ai-agent-header">
                <div class="ai-agent-header__inner">
                    <div class="ai-agent-header__logo-container">
                        <img class="ai-agent-header__logo" src="@/assets/imgs/agent/logo.png" alt="AI智能体工作台">
                        <span class="ai-agent-header__claude-tag">直播界的Claude</span>
                    </div>
                    <div class="ai-agent-header__subtitle">
                        <span>比Claude、Codex更懂</span>
                        <span class="ai-agent-header__highlight">直播</span>
                        <span>、更懂</span>
                        <span class="ai-agent-header__highlight">短视频</span>
                        <span>的AI智能体</span>
                    </div>
                </div>
            </div>

            <div class="ai-agent-grid">
                <div
                    v-for="item in capabilityList"
                    :key="item.order"
                    class="ai-agent-card"
                    :class="`is-${item.theme}`"
                >
                    <div class="ai-agent-card__head">
                        <div class="ai-agent-card__icon">
                            <i :class="item.icon"></i>
                        </div>
                        <div class="ai-agent-card__title-wrap">
                            <div class="ai-agent-card__title">{{ item.title }}</div>
                            <div class="ai-agent-card__desc">{{ item.desc }}</div>
                        </div>
                        <div class="ai-agent-card__order">{{ item.order }}</div>
                    </div>
                    <div class="ai-agent-card__content">{{ item.content }}</div>
                    <div class="ai-agent-card__footer">
                        <div v-for="(line, idx) in item.footer" :key="idx">{{ line }}</div>
                    </div>
                </div>
            </div>

            <div class="ai-agent-actions">
                <afp-button class="ai-agent-actions__button" @click="openWorkbench">
                    点击打开电脑上使用
                </afp-button>
                <mobile-qr-action />
            </div>
        </div>
    </div>
</template>

<script>
/**
 * @file AI 智能体介绍页。
 * @description 展示 AI 智能体能力说明，并作为登录后默认推荐进入的介绍首页。
 */
import { VERSION_TYPE } from '@/enum'
import MobileQrAction from './components/mobileQrAction.vue'
export default {
    name: 'AiAgentPage',
    components: {
        MobileQrAction
    },
    data() {
        return {
            capabilityList: [
                {
                    order: '01',
                    icon: 'el-icon-setting',
                    theme: 'gold',
                    title: '开口即用·0门槛',
                    desc: '你提需求，它干活',
                    content: '想问什么直接说，它拉数据、它分析，不写指令、不选功能。',
                    footer: ['→ "这场为什么卖不动？是话术、排品还是流量？"', '→ 定位到哪一分钟、哪句话']
                },
                {
                    order: '02',
                    icon: 'el-icon-connection',
                    theme: 'blue',
                    title: '越用越懂你·四层记忆',
                    desc: '越用越像你的专属分析师',
                    content: '记住你的账号策略、分析习惯、行业知识、团队方法论，每次分析都按是专属服务。',
                    footer: ['→ 用得越久，越像你专属的资深操盘手']
                },
                {
                    order: '03',
                    icon: 'el-icon-document',
                    theme: 'purple',
                    title: '技能系统·复制金牌操盘手',
                    desc: '一人认知秒变团队能力',
                    content: '把高手方法论一键存成"技能"，团队共享、输入/即调用。',
                    footer: ['→ 让普通运营，也能做出总监级复盘']
                },
                {
                    order: '04',
                    icon: 'el-icon-data-analysis',
                    theme: 'green',
                    title: '直播+短视频全场景',
                    desc: '什么都能分析',
                    content: '单场/多场/多账号/多主播/话术/弹幕/数据/商品/投放/达人短视频，都能分析。',
                    footer: ['→ 一个对话里同时看直播间和短视频——拆解、分析、对比、仿写都搞定']
                }
            ]
        }
    },
    computed: {
        versionType() {
            return this.$store.getters.getVersionType
        }
    },
    methods: {
        /**
         * @description 打开现有 AI 工作台页面，作为介绍页后的功能承接入口。
         * @returns {void}
         */
        openWorkbench() {
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb()
                return
            }
            if (this.versionType !== VERSION_TYPE.AGENT) {
                this.$router.push({
                    path: '/dataAnalysis'
                })
                return
            }
            this.$router.push({
                path: '/aiAssistant'
            })
        }
    }
}
</script>

<style scoped lang="scss">
.ai-agent-page{
    min-height: 100vh;
    margin: -10px;
    padding: 8px 10px 10px;
    background-image: url("~@/assets/imgs/agent/titleBg.png");
    background-repeat: no-repeat;
    background-position: center top;
    background-size: 100% auto;
    box-sizing: border-box;
}

.ai-agent-panel{
    display: flex;
    flex-direction: column;
    min-height: calc(100vh - 20px);
    padding: 0 0 38px;
    border-radius: 12px;
    background: transparent;
}

.ai-agent-header{
    flex: 0 0 auto;
    padding: 32px 20px 16px;
    background: transparent;
}

.ai-agent-header__inner{
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.ai-agent-header__logo{
    width: 362px;
    max-width: 100%;
    object-fit: contain;
}

.ai-agent-header__logo-container{
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
}

.ai-agent-header__claude-tag{
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 4px 12px;
    border-radius: 20px;
    background: #f0f4ff;
    color: #5868ff;
    font-size: 14px;
    font-weight: 600;
    line-height: 20px;
}

.ai-agent-header__title{
    margin-bottom: 12px;
}

.ai-agent-header__main-title{
    color: #21242c;
    font-size: 32px;
    line-height: 44px;
    font-weight: 700;
}

.ai-agent-header__subtitle{
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 20px;
    line-height: 28px;
    color: #21242c;
    font-weight: 500;
}

.ai-agent-header__highlight{
    font-weight: 700;
    color: #5868ff;
}

.ai-agent-grid{
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 16px;
    width: min(100%, 880px);
    margin: 20px auto 0;
    padding: 0 20px;
    box-sizing: border-box;
}

.ai-agent-card{
    min-height: 172px;
    padding: 20px 20px 18px;
    border: 1px solid #ebeef5;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 1px 2px rgba(46, 56, 119, 0.03);
    box-sizing: border-box;
}

.ai-agent-card__head{
    display: flex;
    align-items: flex-start;
}

.ai-agent-card__icon{
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    width: 40px;
    height: 40px;
    margin-top: 2px;
    margin-right: 14px;
    border-radius: 12px;
}

.ai-agent-card__icon i{
    font-size: 20px;
    font-weight: 700;
}

.ai-agent-card__title-wrap{
    flex: 1;
    min-width: 0;
}

.ai-agent-card__title{
    font-size: 16px;
    line-height: 24px;
    font-weight: 700;
    color: #3b4151;
}

.ai-agent-card__desc{
    margin-top: 4px;
    font-size: 12px;
    line-height: 18px;
    font-weight: 600;
}

.ai-agent-card__order{
    margin-left: 12px;
    color: #dfe3eb;
    font-size: 40px;
    line-height: 1;
    font-weight: 700;
}

.ai-agent-card__content{
    margin-top: 18px;
    min-height: 48px;
    font-size: 12px;
    line-height: 24px;
    color: #8b909d;
    font-weight: 500;
}

.ai-agent-card__footer{
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-top: 14px;
    font-size: 12px;
    line-height: 18px;
    color: #7b6cff;
    font-weight: 600;
}

.ai-agent-card__footer i{
    font-size: 12px;
    font-weight: 700;
}

.ai-agent-actions{
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    margin-top: auto;
    padding: 54px 20px 0;
}

.ai-agent-actions__button{
    width: 400px;
    font-size: 24px !important;
    height: 62px !important;
    border-radius: 62px !important;
    background: #444DFF !important;
    border: 1px solid #444DFF !important;
    box-shadow: 0 1px 3px rgba(68,77,255,0.3), 0 4px 8px 3px rgba(68,77,255,0.3) !important;
    color: #fff !important;
}

::v-deep(.ai-agent-actions__button span){
    font-size: 24px;
    font-weight: 500;
}

.ai-agent-card.is-gold .ai-agent-card__icon{
    background: #fff3dc;
    color: #d09b43;
}

.ai-agent-card.is-gold .ai-agent-card__desc{
    color: #e3a140;
}

.ai-agent-card.is-blue .ai-agent-card__icon{
    background: #e8f2ff;
    color: #4e8cf2;
}

.ai-agent-card.is-blue .ai-agent-card__desc{
    color: #5d91ff;
}

.ai-agent-card.is-purple .ai-agent-card__icon{
    background: #efe7ff;
    color: #8f62ff;
}

.ai-agent-card.is-purple .ai-agent-card__desc{
    color: #9b74ff;
}

.ai-agent-card.is-green .ai-agent-card__icon{
    background: #e5f8eb;
    color: #53b576;
}

.ai-agent-card.is-green .ai-agent-card__desc{
    color: #52bc77;
}

@media (max-width: 1200px) {
    .ai-agent-grid{
        width: 100%;
    }
}

@media (max-width: 900px) {
    .ai-agent-panel{
        min-height: auto;
        padding-bottom: 26px;
    }

    .ai-agent-header{
        padding: 24px 14px 14px;
    }

    .ai-agent-header__desc{
        flex-wrap: wrap;
        text-align: center;
        font-size: 16px;
        line-height: 24px;
    }

    .ai-agent-grid{
        grid-template-columns: 1fr;
        gap: 14px;
        margin-top: 14px;
        padding: 0 14px;
    }

    .ai-agent-card{
        min-height: auto;
    }

    .ai-agent-actions{
        padding: 32px 14px 0;
        flex-wrap: wrap;
    }

    .ai-agent-actions__button{
        width: 100%;
    }

    ::v-deep(.ai-agent-actions__button span){
        font-size: 18px;
    }
}
</style>
