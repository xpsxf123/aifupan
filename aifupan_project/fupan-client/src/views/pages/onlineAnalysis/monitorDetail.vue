<!--
@description 云空间监控详情页：页面入口按云空间在线详情页方式加载数据，页面内容与样式完全复用 monitorDetail 原布局。
-->
<template>
    <div class="monitorDetailPage" v-loading="webLoading">
        <div class="monitorDetailPageInner">
            <div class="monitorDetailTopBar">
                <div class="monitorDetailBackBtn cursor-pointer" @click="handlePageBack">
                    <i class="el-icon-arrow-left"></i>
                    <span>返回详情</span>
                </div>
                <div class="monitorDetailTitle">{{ pageTitle }}</div>
            </div>
            <div class="monitorDetailContent">
                <MonitorDetailLayout
                    v-if="showPage"
                    :sentenceMarkData="sentenceMarkData"
                    :targetType="getTargetType"
                    :defaultReportType="defaultReportType"
                />
            </div>
        </div>
    </div>
</template>

<script>
/**
 * @description 云空间监控详情页入口：复用 monitorDetail 原页面结构，仅替换为云空间在线详情页的数据获取链路。
 */
import analysisMixin from '@/mixins/analysisMixin'
import MonitorDetailLayout from '@/views/modules/replay/monitorDetail/components/MonitorDetailLayout.vue'
import { getH5BaseUrl } from '/src/config/h5Url/index.js';

export default {
    components: {
        MonitorDetailLayout
    },
    mixins: [analysisMixin],
    data() {
        return {
            sentenceMarkData: {
                sentenceMarkList: [],
                videoInfo: {},
                anchorInfo: {},
                playUrl: ''
            },
            showPage: false,
            webLoading: false
        }
    },
    computed: {
        /**
         * @description 云空间监控详情页固定按 webOnline 场景渲染。
         * @returns {string}
         */
        getTargetType() {
            return 'webOnline'
        },
        /**
         * @description 默认激活的监控报告类型。
         * @returns {string}
         */
        defaultReportType() {
            const reportType = String(this.$route?.query?.reportType || 'scriptQuality')
            if (['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(reportType)) {
                return reportType
            }
            return 'scriptQuality'
        },
        /**
         * @description 顶部返回条标题，随当前监控类型切换。
         * @returns {string}
         */
        pageTitle() {
            const titleMap = {
                scriptQuality: '话术质检',
                interactionInspection: '互动巡检',
                scriptRestoration: '话术还原度'
            }
            return titleMap[this.defaultReportType] || '监控详情'
        }
    },
    watch: {
        '$route.params.id': {
            handler() {
                this.getRouteData()
            }
        },
        '$route.params.type': {
            handler() {
                this.getRouteData()
            }
        }
    },
    mounted() {
        if (this.isMobileDevice()) {
            this.redirectToMobilePage()
            return
        }
        this.registerPageBackHandler()
        this.getRouteData()
    },
    activated() {
        this.registerPageBackHandler()
        this.getRouteData()
    },
    beforeDestroy() {
        this.clearPageBackHandler()
        this.showPage = false
    },
    methods: {
        /**
         * @description 为当前监控详情页注册返回上一页能力。
         * @returns {void}
         */
        registerPageBackHandler() {
            this.$store.state.routerPathBack = this.handlePageBack
        },
        /**
         * @description 清理当前页面的返回处理。
         * @returns {void}
         */
        clearPageBackHandler() {
            if (this.$store.state.routerPathBack === this.handlePageBack) {
                this.$store.state.routerPathBack = null
            }
        },
        /**
         * @description 返回上一页。
         * @returns {void}
         */
        handlePageBack() {
            this.$router.back()
        },
        /**
         * @description 读取路由参数并加载云空间监控详情数据。
         * @returns {void}
         */
        getRouteData() {
            const id = this.$route?.params?.id
            const type = this.$route?.params?.type
            if (!id && id !== 0) return
            this.getVideoAnalysis(id, type)
        },
        /**
         * @description 检测是否为移动设备。
         * @returns {boolean}
         */
        isMobileDevice() {
            const userAgent = navigator.userAgent.toLowerCase()
            const mobileKeywords = ['mobile', 'android', 'iphone', 'ipad', 'ipod', 'blackberry', 'windows phone']
            const isMobileUA = mobileKeywords.some(keyword => userAgent.includes(keyword))
            const isSmallScreen = window.innerWidth <= 768
            return isMobileUA || isSmallScreen
        },
        /**
         * @description 重定向到 H5 移动端页面。
         * @returns {void}
         */
        redirectToMobilePage() {
            try {
                const currentId = this.$route.params.id
                this.$emit('changeType', 'web')
                const h5BaseUrl = getH5BaseUrl()
                if (h5BaseUrl && currentId) {
                    const h5Url = `${h5BaseUrl}/#/pages/anlaysis/index?uid=${currentId}`
                    if (h5Url.startsWith('/h5')) {
                        const currentOrigin = window.location.origin
                        window.location.href = `${currentOrigin}${h5Url}`
                    } else {
                        window.location.href = h5Url
                    }
                } else {
                    console.warn('无法获取H5基础URL或缺少必要参数')
                }
            } catch (error) {
                console.error('重定向到H5页面失败:', error)
            }
        },
        /**
         * @description 获取云空间分析数据并初始化监控详情页。
         * @param {string|number} uuid 视频或文件唯一标识
         * @param {string|number} type 类型 0: 视频 1: 文件
         * @returns {void}
         */
        getVideoAnalysis(uuid, type) {
            this.sentenceMarkData.sentenceMarkList = []
            this.showPage = false
            this.webLoading = true
            this.$httpBack.v2000.getOnlineAnalysis({ uuid, type }).then(res => {
                if (res.code == 0 && res.data) {
                    try {
                        this.initAudioaAlyses(res.data)
                        this.showPage = true
                        if (!this.sentenceMarkData?.anchorInfo) {
                            this.$emit('changeType', 'lose')
                        }
                    } catch (e) {
                        console.error(e)
                    }
                } else {
                    this.$message.error('复盘数据不存在，请检查链接是否正确')
                }
            }).catch((err) => {
                if (err.code === 4001) {
                    this.$emit('changeType', 'login')
                }
            }).finally(() => {
                this.webLoading = false
            })
        }
    },
    beforeRouteLeave(to, from, next) {
        this.clearPageBackHandler()
        next()
    }
}
</script>

<style scoped>
.monitorDetailPage {
    width: 100%;
    height:  calc(100vh - 10px);
}

.monitorDetailPageInner {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
}

.monitorDetailTopBar {
    height: 52px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    background: #FFFFFF;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    box-sizing: border-box;
    flex-shrink: 0;
}

.monitorDetailBackBtn {
    display: inline-flex;
    align-items: center;
    color: #303133;
    font-size: 14px;
    font-weight: 500;
}

.monitorDetailBackBtn i {
    margin-right: 6px;
    font-size: 16px;
}

.monitorDetailTitle {
    flex: 1;
    text-align: center;
    margin-right: 82px;
    color: #303133;
    font-size: 15px;
    font-weight: 600;
}

.monitorDetailContent {
    flex: 1;
    min-height: 0;
    overflow: hidden;
}
</style>
