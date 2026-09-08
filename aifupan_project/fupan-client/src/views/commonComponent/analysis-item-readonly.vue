<template>
    <div style="display: flex; flex-direction: column;height: 100%;">
        <!-- 新版 -->
        <analysisContrastItem ref='analysis' readonly notWords
                              :sentenceMarkData="sentenceMarkData"
                              :hideContent="isTextExtraction"
                              :targetType="targetType" >
            <template #anchor-right>
<!--                v-if="!isClick3Hiden"-->
                <polish class="mg-l24" v-if="!isTextExtraction">
<!--                    <img src="@/assets/imgs/txhsAI.png"  @click="clickHiden_3" alt="" style="max-height: 24px;"/>-->
                    <afp-button  @click="exportScript" size="medium">导出话术</afp-button>
                    <template v-if="versionType===VERSION_TYPE.PURE">
                        <afp-button @click="()=>selectSliceAnalysis('slice')" size="medium">切片</afp-button>
                        <span @click="howToSlice" class="icon font_family icon-a-Frame530 cursor-pointer" style="color: green;font-size:18px;margin-left: 10px;"></span>
                    </template>
                    <!--     为了慢总的需求， 临时添加 2.6.0要删除-->
                    <template v-if="versionType===VERSION_TYPE.PURE">
                        <afp-button style="margin-left: 12px" @click="()=>selectSliceAnalysis('violation')" size="medium">AI查违规</afp-button>
                    </template>
                </polish>
            </template>
            <template #video-bottom>
                <slot name="video-bottom"></slot>
            </template>
            <template #title-center>
                <div v-if="!isTextExtraction&&versionType === VERSION_TYPE.AGENT" class="mg-t10 pd-8 brs-8 flex-jc-sb main-bg">
                    <div class="font-s14">
                        您目前使用的是{{getPackageLevelName}}，<span class="text-colorErr">智能分析时长不足</span>
                        <span class="text-color2 font-s14">（每个月赠送{{aiAnalysisTime}}个小时智能分析 ，每天{{aiAnalysisDayTime}}小时提取文案时长，<span style="color: red">录制功能永久免费</span>）</span>
                        <br>
                        请联系产品顾问购买会员或增量包，<span class="text-colorTheme" @click="showCustomerServiceQrCode">点我联系</span>
                        您也可以邀请好友，获得奖励的智能分析时长， <span class="text-colorTheme" @click="shareClient">点我邀请</span>
                    </div>
                    <div class="readonly-btns">
                        <afp-button  type="warning" @click="shareClient" plain>点我邀请</afp-button>
                        <afp-button  type="success" @click="showCustomerServiceQrCode" plain>点我联系</afp-button>
                    </div>
                </div>
            </template>
            <template v-if="isTextExtraction" #content-before>
                <div class="font-w400 text-color1 flex-ji-c flex-ai-c h100 main-bg pd-8 mg-b6">
                    <div class="text-center" style="line-height: 34px;">
                        <div>
                            您目前使用的是<span class="text-colorTheme">{{ getPackageLevelName }}</span>
                            <span v-if="!isResources">
                                <span>，</span>
                                <span class="text-colorErr">智能分析时长以及文案提取时长不足</span>
                                <br>
                                <template v-if="versionType === VERSION_TYPE.AGENT">
                                    <span class="text-color2 font-s14">（每个月赠送{{aiAnalysisTime}}个小时智能分析 ，每天{{aiAnalysisDayTime}}小时提取文案时长）</span>
                                    <br>
                                    录制功能永久免费！视频会帮您保存在电脑本地磁盘 <br>
                                    需要增加智能分析时长，请<span class="text-colorTheme">联系产品顾问购买会员</span>
                                </template>
                            </span>
                            <span v-else>
                                <span>，</span>
                                <span class="text-colorErr">由于{{getErrorHint}}导致分析失败。</span>
                                <template v-if="versionType===VERSION_TYPE.AGENT">
                                    <br>
                                    智能分析时长剩余{{ getChargeDataString }}，今日文案提取时长剩余{{ getFreeDataString }}。
                                    <br>
                                    请点击以下按钮，<span class="text-colorTheme" @click="afreshAnalysis">重新分析</span>。
                                </template>
                            </span>
                            <br>
                            <span v-if="versionType === VERSION_TYPE.AGENT">您也可以邀请好友，获得奖励的智能分析时长</span>
                        </div>
                        <div class="mg-t20 readonly-btns">
                            <afp-button v-if="versionType === VERSION_TYPE.AGENT" style="width: 144px;" type="warning" @click="shareClient" plain>点我邀请</afp-button>
                            <afp-button v-if="!isResources" style="width: 144px;" type="success" @click="showCustomerServiceQrCode" plain>点我联系</afp-button>
                            <afp-button v-if="isResources && versionType === VERSION_TYPE.AGENT" style="width: 144px;" type="success" plain @click="afreshAnalysis">重新分析</afp-button>
                        </div>
                    </div>
                </div>
            </template>
            <template #tabs>
                <div class="analysis-tabs-box">
                    <slot name="analysis-tabs">
                        <ControlTabs ref="controlTabs" :readonly="versionType === VERSION_TYPE.AGENT"  :tabs="getTabs" :sentenceMarkData="sentenceMarkData"
                                     :tabsOrder="isTextExtraction?{c:1}:{}"
                                     :notReadonly="{c:isTextExtraction || isKuaishou, g:isKuaishou,h:isKuaishou,f:isKuaishou}">
                            <template #readonly v-if="versionType === VERSION_TYPE.AGENT">
                                <div style="height: 200px;" class="main-bg flex-ji-c">
                                    <span v-if="isPackageFree">
                                        <span v-if="!isCharge">请联系<b class="cs-p text-colorTheme" @click="showCustomerServiceQrCode">产品顾问</b>，升级您的套餐版本。即可查看所有功能!</span>
                                        <span v-else>重新智能分析后，即可查看所有功能</span>
                                    </span>
                                    <span v-else>
                                        <span v-if="!isCharge" class="text-center" style="display: inline-block;">
                                            智能分析时长余量不足，请联系<b class="cs-p text-colorTheme" @click="showCustomerServiceQrCode">产品顾问</b>购买增量包或等待套餐资源更新。
                                            <br><br>
                                            套餐资源下次更新时间：{{getUpdateTime}}
                                        </span>
                                        <span v-else>重新智能分析后，即可查看所有功能</span>
                                    </span>
                                </div>
                            </template>
                        </ControlTabs>
                    </slot>
                </div>
            </template>
        </analysisContrastItem>

        <SliceTutorial ref="slice_tutorial"/>
    </div>
</template>

<script>
import analysisContrastItem from './analysisLayout/analysis-readonly.vue'
import polish from '@/components/polish/index.vue'
import analysisMixin from '/src/mixins/analysisMixin.js'
import commonMixin from './analysisLayout/mixin/commonMixin'
import analysisTourMixin from './analysisLayout/tourConfig/analysisTourMixin'
import ControlTabs from '/src/components/analysis/controlTabs.vue'
import myUtils from '/src/utils/utils'
import clickHiden from '@/mixins/clickHiden';
import share from '@/mixins/share.js';
import {VERSION_TYPE} from "@/enum";
import SliceTutorial from "@/views/modules/replay/component/sliceTutorial.vue";
import upgradeToAgentTip from '@/mixins/upgradeToAgentTip'
/*
1、免费版本或者激活版本:  
智能分析时长不足
a、请联系产品顾问，升级您的套餐版本。即可查看所有功能!   
--------
b（智能分析时长充足）   
b.重新智能分析后，即可查看所有功能
2、其他版本：（智能分析时长不足）
a.智能分析时长余量不足，请联系产品顾问购买增量包或等待套餐资源更新。套餐资源下次更新时间：XXXXX       （智能分析时长充足）   
b.重新智能分析后，即可查看所有功能
*/
export default {
    components: {
        SliceTutorial,
        analysisContrastItem,
        polish,
        ControlTabs
    },
    mixins: [analysisMixin, commonMixin, analysisTourMixin, clickHiden('analysis-item-readonly'), share, upgradeToAgentTip],
    props: {
        targetType: {
            type: String,
            default: ''
        }
    },
    provide () {
        return {
            analysisMain: this
        }
    },
    inject: ['appVnode', 'APP'],
    computed: {
        getTabs () {
            return [
                {
                    label: 'AI数据识图', name: 'd', hide: () => {
                        return this.versionType === VERSION_TYPE.PURE
                    }
                },
            ]
        },
        versionType(){
            return this.$store.getters.getVersionType
        },
        numberToSting(){
            return myUtils.numberToSting(this.freeVersion?.aiTokenNum || 0);
        },
        aiAnalysisTime(){
            return myUtils.retainDecimals((this.freeVersion?.aiAnalysisTime||0)/60);
        },
        aiAnalysisDayTime(){
            return myUtils.retainDecimals((this.freeVersion?.textExtractionNum||0)/60);
        },
        getPackageLevelName(){
            return this.$store.getters.getPackageLevelName;
        },
        isPackageFree(){
            return this.$store.getters.isFree || this.$store.getters.isActivated;
        },
        // 文本提取失败
        isTextExtraction(){
            return this.$route.query.analysisStatus != 2;
        },
        getVideoInfo(){
            const { videoInfo } = this.sentenceMarkData;
            return videoInfo;
        },
        getChargeData(){
            // aiAnalysisTime
            return this.$store.getters.getUserproperty?.aiAnalysisTime;
        },
        getFreeData(){
            return this.$store.getters.getUserproperty?.textExtractionNum;
        },
        getChargeDataString(){
            return myUtils.minutesToHours(this.getChargeData,{unit: '小时'})
        },
        getFreeDataString(){
            return myUtils.minutesToHours(this.getFreeData,{unit: '小时'})
        },
        isCharge(){
            return this.getChargeData>= parseInt(this.getVideoInfo.durationTime)/60;
        },
        isFree(){
            return this.getFreeData >= parseInt(this.getVideoInfo.durationTime)/60;
        },
        isResources(){
            return this.isCharge || this.isFree || this.versionType === VERSION_TYPE.PURE;
        },
        getErrorHint(){
            const {ErrorReason} = this.getVideoInfo;
            return ErrorReason;
        },
        // 如果文本提取失败，有资源，则不显示AI数据识图。
        //如果文本提取失败，且没有资源，则显示AI数据识图。
        // isShowTabsCurveData(){
        //     return this.isTextExtraction && !this.isResources;
        // },
        getUpdateTime(){
            if(this.$store.state?.userInfo?.resourceUpdateTime){
                return this.$store.state?.userInfo?.resourceUpdateTime?.split(' ')?.[0];
            }else{
                return '-'
            }
        }
    },
    data () {
        return {
            VERSION_TYPE,
            freeVersion: {},
            isRepair: false
        }
    },
    watch: {},
    mounted () {
        this.initTabs();
        this.currentFreeVersion();
    },
    activated () {
    },
    beforeDestroy () {
    },
    destroyed () {},
    methods: {
        exportScript() {
            this.$store.commit("setActionKey", new Date().getTime());
        },
        howToSlice(){
            this.$refs.slice_tutorial?.open()
        },
        selectSliceAnalysis(str){
            const desc = str === 'slice'
                ? '切片功能可以对直播间的高光片段进行切片。'
                : 'AI查违规功能可以排查直播间违禁词和违禁语句。'
            this.showUpgradeToAgentConfirm(desc)
        },
        afreshAnalysis () {
            const { videoInfo} = this.sentenceMarkData;
            const { VideoId, TradeId} = videoInfo;
            let data = {
                id: TradeId,
                videoId:VideoId
            }
            this.createAnalysisConfirm({
                id: TradeId,
                data,
                callback:()=>{
                    this.$router.go(-1);
                }
            })
        },
        // 确认生成直播分析
        createAnalysisConfirm ({ id, data, callback }) {
            let token = this.$store.state.token;
            // 重新分析
            this.$httpClient.video.reanalysis({ videoId: data.videoId, token, tradeId: id }).then((res) => {
                if (res.code == 0 && res.data) {
                    callback?.()
                    this.$message.success('已重新加入分析排队中等待分析')
                } else {
                    this.$message.warning('请等待上一个分析完再继续')
                }
            })


        },
        currentFreeVersion(){
            this.$httpBack.v2400.currentFreeVersion().then(res=>{
                if(res.code === 0){
                    this.freeVersion = res.data
                }
            })
        },
        initTabs(){
            this.$nextTick(()=>{
                // AI数据识图
                if(this.isTextExtraction){
                    this.$refs.controlTabs?.selectActiveName?.('c')
                }else{
                    this.$refs?.controlTabs?.setFold?.(true)
                }
            })
        },
        repairVideo(val){
            this.isRepair = val
            this.$emit('repairVideo', val)
        },
        isRepairStop(callback){
            this.$nextTick(()=>{
                this.$refs?.analysis?.isRepairStop(callback)
            })
        },
        // 显示客服二维码
        showCustomerServiceQrCode () {
            this.appVnode.showQrCode()
        },
        shareClient(){
            this.$router.push({
                path: '/invite'
            })
        }
    }
}
</script>
<style scoped lang="less">
/* 联系客服 */
.contact-kefu {
    display: flex;
    align-items: center;
    cursor: pointer;
}

.txyAi {
    // margin-top: -3px;
    display: flex;
    // flex-direction: column;
    flex-direction: row;
    // justify-content: flex-end;
    align-items: center;
    vertical-align: bottom;
}
.readonly-btns{
    >.el-button:first-child{
        color: #CD4310;
        border-color: #CD4310;
        background: rgba(255,107,53,0.05);;
    }
    >.el-button:last-child{
        color: #00853D;
        border-color: #00853D;
        background: rgba(40,189,108,0.05);
    }
}

</style>
