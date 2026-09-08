<template>
    <div class="h100" v-loading="webLoading">
        <aiContrastOnly
            v-if="showAnalysis"
            ref="aiContrastOnly"
            :sentenceMarkData="sentenceMarkData"
            :targetType="targetType"
            :type="aiType"
            :shareId="shareId"
            :selectedText.sync="selectedText"
            @rightTickContextMenu="rightTickContextMenu"
            @toggle="onToggle"
        >
        <template #anchor-right>
                <polish class="mg-l24">
                    <img src="@/assets/imgs/aiTitle.png" alt="" style="max-height: 24px;" />
                </polish>
            </template>
        </aiContrastOnly>
        <!-- 额外功能添加位置-附加功能 -->
        <!-- 自定义菜单 -->
        <contextmenu :selected-text="selectedText" :targetType="targetType"  notLexicon ref="contextmenu" @task="onTask" ></contextmenu>
    </div>
</template>

<script>
import aiContrastOnly from './aiAnalysis/ai-contrast-only.vue';
import analysisMixin from '@/mixins/analysisMixin';
import contextmenu from './contextmenu.vue';
import polish from '@/components/polish/index.vue'
export default {
    components: {
        aiContrastOnly,
        contextmenu,
        polish
    },
    mixins: [analysisMixin],
    props:{
        targetType: {
            type: String,
            default: ''
        },
        id: {
            type: String,
            default: ''
        },
        httpRequest: {
            type: Function,
            default: null
        },
        type: {
            type: String,
            default: ''
        },
        fileType: {
            type: String,
            default: ''
        },
        shareId: {
            type: String,
            default: ''
        }
    },
    inject: ['APP'],
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
            webLoading: false,
            selectedText: '',
            aiType: ''
        };
    },
    computed: {
        getAnalysisId(){
            return this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.fileInfo?.fileId
        },
        isVideoId(){
            return this.sentenceMarkData?.videoInfo?.VideoId
        },
        getHttp(){
            if(this.httpRequest){
                return this.httpRequest
            }else if(this.targetType === 'online'){
                return this.$httpClient.video.lockCloudAnalysis;
            }else if(this.targetType === 'analysis'){
                return this.$httpClient.video.lockanalysis;
            }else if(this.targetType === 'uploadFile'){
                return this.$httpClient.uploadFile.lockanalysis;
            }else if(this.targetType === 'webOnline'){
                return this.$httpBack.v2000.getOnlineContrastAnalysis
            }
        },
        isWebOnline(){
            return this.targetType === 'webOnline'
        }
    },
    watch: {},
    methods: {
        getAiType(val){
            const types={
                0: 'assistant',
                1: 'violation'
            }
            return types[val] || val;
        },
        onToggle(val){
            this.aiType = this.getAiType(val);
        },
        //右键打开自定义菜单
        rightTickContextMenu({event,data}) {
            this.$nextTick(() => {
                this.$refs.contextmenu.rightContextMenu(event,{
                    id: this.getAnalysisId,
                    sourceType: this.isVideoId ? 0 : 1,
                    notModel: ['slice'],
                    aiType: this.aiType,
                    data
                })
            })
        },
        async initGetData(){
            // 获取视频id
            let id = this.$route.query.id || this.id || this.$store.state.videoAnalysisId;
            // ai分析类型，assistant 运营助手，violation 违规分析
            this.aiType = this.getAiType(this.$route.query.type) || this.type || 'assistant';
            // 选中段落Code
            let code = this.$route.query.value || this.$route.query.code;
            // 分析内容类型，1：文件，0：视频
            let fileType =( this.$route.query.fileType || this.fileType) == '1' ? 'file' : 'video';
            if (id) {
                let list = await this.getBarrageDataList(id);
                this.getVideoAnalysis(id,fileType,list).then(()=>{
                    let { data1 } = this.$route.query;
                    // 数据曲线切片
                    if(data1){
                        this.setBrush(data1)
                    }
                });
            }
            // 如果有code则选中段落
            if(code){
                setTimeout(()=>{
                    this.$nextTick(()=>{
                        this.$refs.aiContrastOnly?.selectParagraph(code);
                    })
                },500)
            }
        },
        // 获取视频分析信息
        getVideoAnalysis(id,type,list) {
            this.sentenceMarkData.sentenceMarkList = [];
            this.showAnalysis = false;
            let http = this.getHttp;
            let params = type ==='file'?{ fileId: id }:{videoId: id};
            this.webLoading = this.isWebOnline;
            /*
                this.$httpBack.v2000.getOnlineAnalysis({ uuid,type }).then(res => {
                if (res.code == 0 && res.data) {
                    try{
                        this.initAudioaAlyses(res.data);
                        if(this.sentenceMarkData?.anchorInfo){
                            this.showAnalysis = true;
                        }else{
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
            */
            return http(params).then((res) => {
                if (res.code == 0 && res.data) {
                    try{
                        res.data.barrageDataList = list || res.data.barrageDataList;
                        this.initAudioaAlyses(res.data);
                        this.showAnalysis = true;
                        if(!this.sentenceMarkData?.anchorInfo){
                            this.$emit('request',{
                                type: 'lose',
                                data: res
                            }); 
                        }
                    }catch(e){
                        console.error(e);
                    }
                } else {
                    this.$message.error("复盘数据不存在，请检查链接是否正确");
                }
            }).finally(()=>{
                this.webLoading = false;
            }).catch((err)=>{
                this.$emit('request',{
                    type: 'error',
                    data: err
                  })
            });
        },
        onTask(data){
            // 判断是否切换类型
            let isToggle = this.aiType !== data.type;
            // 切换类型则刷新类型
            if(isToggle){
                this.aiType = data.type;
            }
            this.$nextTick(()=>{
                this.$refs.aiContrastOnly.addAiTitleText({
                    ...data,
                    isToggle
                });
            });
        },
        setBrush(data) {
            if(typeof data === "string" && data?.split(',')?.length > 1){
                this.$nextTick(()=>{
                    this.$refs.aiContrastOnly?.setBrush(data?.split(','))
                })
            }
        },
        getBarrageDataList(id){
            if(this.aiType!=='scrolling'){
                return false
            }
            return this.$httpBack.third.getBarrageDataList({videoId:id, isBlessBag: 0}).then(res=>{
                if(res.code === 0){
                    return res.data
                }else{
                    return false
                }
            }).catch(err=>{
                return false
            })
        }
    },
    mounted() {
        this.initGetData();
    },
    activated() {
        
        this.initGetData();
    }, 
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {
        this.showAnalysis = false;
    }, //生命周期 - 销毁之前
    destroyed() {
        this.APP.refresh();
    }, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>