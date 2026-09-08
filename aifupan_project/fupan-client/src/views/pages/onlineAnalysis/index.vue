<!--
@description 云空间详情页：负责加载 webOnline 复盘数据，并承接详情内监控报告页、AI 页等跳转。
-->
<template>
    <div>
        <!-- 文件分析 -->
        <div>
            <div class="analysisItemContainer pd-t6" v-loading="webLoading">
                <analysis-item
                v-if="showAnalysis"
                ref="analysis"
                targetType="webOnline"
                @webElfClick="elfClick"
                @monitorDetailJump="handleMonitorDetailJump"
                :sentenceMarkData="sentenceMarkData">
                <template #video-bottom>
                    <div style="display: flex;margin-top: 8px;">
                        <div style="color: #4D4D4D;font-size: 12px;margin-right: 8px;">对此视频有异议？</div>
                        <div style="color: var(--color-main);font-size: 12px;border-bottom: 1px solid var(--color-main);cursor: pointer;"
                            @click="Complaint">点我申诉</div>
                    </div>
                </template>
                <template #page-right>
                    <officialBottom class="pd-r8"></officialBottom>
                </template>
                </analysis-item>
            </div>
        </div>
    </div>
</template>

<script>
import analysisItem from "@/views/commonComponent/analysis-item.vue"
import analysisMixin from '@/mixins/analysisMixin';
import officialBottom from "../common/officialBottom.vue";
import { getH5BaseUrl } from '/src/config/h5Url/index.js';
export default {
    components: {
        analysisItem,
        officialBottom
    },
    provide() {
        return {
            appVnode: this
        }
    },
    mixins: [analysisMixin],
    data() {
        return {
            // 文字段落信息
            sentenceMarkData: {
                sentenceMarkList: [],
                videoInfo: {},
                anchorInfo: {},
                playUrl: ""
            },
            showAnalysis: false,
            webLoading: false
        };
    },
    
    mounted() {
        // 检测是否为手机端，如果是则进行重定向
        /**/
        if (this.isMobileDevice()) {
            this.redirectToMobilePage();
            return;
        }
        this.getRouteData()
    },
    activated(){
        this.getRouteData()
    },
    methods: {
        /**
         * @description 处理云空间详情页中的监控详情跳转，进入云空间风格的独立监控详情页。
         * @param {{ id?: string, reportType?: string }} payload 跳转参数。
         * @returns {void}
         */
        handleMonitorDetailJump({ id, reportType } = {}) {
            if (!id) return
            this.$router.push({
                path: `/onlineAnalysisMonitor/${this.$route.params.type}/${id}`,
                query: {
                    reportType
                }
            })
        },
        elfClick(type,o){
            this.$router.push({
                path: `/onlineAiAnalysis/${type}/${this.$route.params.type}/${this.$route.params.id}`,
                query: {
                    ...o
                }
            })
        },
        getRouteData(){
            // 获取视频id
            let videoId = this.$route.params.id;
            let fileId = this.$route.params.id;
            // 类型 0：视频 1：文件
            let type = this.$route.params.type;
            if (videoId || fileId) {
                // 根据分类请求数据
                this.getVideoAnalysis(type?fileId:videoId, type);
            }
        },
        Complaint(){
            this.$refs?.analysis?.Complaint()
        },
        /**
         * 检测是否为移动设备
         * @returns {boolean} 是否为移动设备
         */
        isMobileDevice() {
            const userAgent = navigator.userAgent.toLowerCase();
            const mobileKeywords = ['mobile', 'android', 'iphone', 'ipad', 'ipod', 'blackberry', 'windows phone'];
            const isMobileUA = mobileKeywords.some(keyword => userAgent.includes(keyword));
            const isSmallScreen = window.innerWidth <= 768;
            return isMobileUA || isSmallScreen;
        },
        
        /**
         * 重定向到H5移动端页面
         * 直接跳转到对应的H5页面，并携带uid参数
         */
        redirectToMobilePage() {
            try {
                const currentId = this.$route.params.id;
                this.$emit('changeType','web');
                console.log(this.$route.params);
                // 获取H5基础URL
                const h5BaseUrl = getH5BaseUrl();
                if (h5BaseUrl && currentId) {
                    // 构建H5页面URL，将id作为uid参数传递
                    const h5Url = `${h5BaseUrl}/#/pages/anlaysis/index?uid=${currentId}`;
                    if(h5Url.startsWith('/h5')){
                    // 获取当前域名并拼接到 h5Url
                        const currentOrigin = window.location.origin;
                        window.location.href = `${currentOrigin}${h5Url}`;
                    }else{
                        // 直接跳转到H5页面
                        window.location.href = h5Url;
                    }
                } else {
                    console.warn('无法获取H5基础URL或缺少必要参数');
                    // 如果无法跳转，则正常加载页面
                    // this.getRouteData();
                }
            } catch (error) {
                console.error('重定向到H5页面失败:', error);
                // 发生错误时正常加载页面
                // this.getRouteData();
            }
        },
        
        
        
        // 获取视频分析信息
        getVideoAnalysis(uuid,type) {
            this.sentenceMarkData.sentenceMarkList = [];
            this.webLoading = true;
            this.$httpBack.v2000.getOnlineAnalysis({ uuid,type }).then(res => {
                if (res.code == 0 && res.data) {
                    try{
                        this.initAudioaAlyses(res.data);
                        this.showAnalysis = true;
                        if(!this.sentenceMarkData?.anchorInfo){
                            this.$emit('changeType','lose'); 
                        }
                    }catch(e){
                        console.error(e);
                    }
                } else {
                    this.$message.error("复盘数据不存在，请检查链接是否正确");
                }
            }).catch((err)=>{
                if(err.code === 4001){
                    this.$emit('changeType','login')
                }
            }).finally(()=>{
                this.webLoading = false;
            })
        }
    },
};
</script>
<style scoped>

.analysisItemContainer {
    width: 100%;




    
    height: 100vh;
}


</style>
