<template>
    <div v-if="loading"  v-loading="webLoading" :style="shareId?{height:'100%'}: {height:'calc(100vh - 66px)'}">
        <aiContrast
        ref="aiContrast"
        :contrastId="getContrastId" 
        :sentenceMarkData="sentenceMarkData"
        :aiType="aiType" 
        :shareId="shareId"
        :targetType="targetType" 
        @toggle="onToggle"></aiContrast>
    </div>
</template>

<script>
import aiContrast from './aiAnalysis/ai-contrast.vue';
import myUtils from '@/utils/utils.js';
import analysisMixin from '/src/mixins/analysisMixin';
import publicMixin from "/src/views/commonComponent/analysisLayout/mixin/publicMixin";
export default {
    components: {
        aiContrast
    },
    mixins: [analysisMixin,publicMixin],
    props:{
        targetType: {
            type: String,
            default: ''
        },
        contrastId: {
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
        shareId: {
            type: String,
            default: ''
        }
    },

    data() {
        return {
            aiType: '',
            sentenceMarkData: {
                data1: {},
                data2: {}
            },
            getInifDebounce: myUtils.debounce(100,true),
            webLoading: false,
            loading: false,
        };
    },
    computed: {
        // 对比ID
        getContrastId() {
            return this.$route.query?.contrastId || this.contrastId;
        },
        isWebOnline(){
            return this.targetType === 'webOnline'
        },
        getHttp(){
            if(this.httpRequest){
                return this.httpRequest
            }else if(this.targetType === 'online'){
                return  this.$httpClient.video.lockCloudContrast
            }else{
                return this.$httpClient.contrast.lockanalysiscontrast
            }
        },
    },
    watch: {},
    inject: ['APP'],
    methods: {
        setSentenceMarkData(key, data) {
            let o = {
                ...data
            };
            // 段落数据
            o.sentenceMarkList = data.sentenceMarkList;
            // 视频播放地址
            o.playUrl = data.playUrl;
            // 主播信息
            o.anchorInfo = data.anchorInfo;
            if (data.fileInfo) {
                o.fileInfo = data.fileInfo
            } else if (data.videoInfo) {
                o.videoInfo = data.videoInfo
            }
            this.$set(this.sentenceMarkData, key, o);
        },
        // 获取对比信息
        getContrastInfo(contrastId, type) {
            this.getInifDebounce(()=>{
                this.sentenceMarkData.data1 = {};
                this.sentenceMarkData.data2 = {};
                this.loading = false;
                let getHttp = this.getHttp;
                let params = { contrastId };
                this.webLoading = this.isWebOnline;
                getHttp(params).then(res => {
                    
                    if (res.code == 0 && res.data) {
                        this.sentenceMarkData.data1.sentenceMarkList = [];
                        this.sentenceMarkData.data2.sentenceMarkList = [];
                        this.initPayerIndexMap();
                        this.initAudioaAlyses(res.data.SentenceMark1, (data) => {
                            this.setSentenceMarkData('data1', data);
                            this.setPayerIndexMap(data.sentenceMarkList, 'A1');
                        });
                        this.initAudioaAlyses(res.data.SentenceMark2, (data) => {
                            this.setSentenceMarkData('data2', data);
                            this.setPayerIndexMap(data.sentenceMarkList, 'A2');
                        });
                        this.sentenceMarkData.info = res.data?.VideoContrast;
                        this.loading = true
                        this.$emit('request',{
                            type: 'success',
                            data: res
                        })
                        setTimeout(()=>{
                            this.$nextTick(()=>{
                                this.$refs.aiContrast?.getWordsInfo();
                                let {data1,data2} = this.$route.query;
                                this.setBrush(data1,0);
                                this.setBrush(data2,1);
                            });
                        },0)
                    } else {
                        this.$message.error("对比数据不存在");
                        this.$emit('request',{
                            type: 'lose',
                            data: res
                        });
                    }
                }).finally(()=>{
                    this.webLoading = false;
                }).catch((err) => {
                    console.error(err)
                    this.$emit('request',{
                        type: 'error',
                        data: err
                    })
                });
            })
        },
        // 获取ai问答类型
        getAiType(val){
            const types={
                0: 'assistant',
                1: 'violation'
            }
            return types[val] || val;
        },
        // 切换ai类型
        onToggle(val){
            this.aiType = this.getAiType(val);
        },
        // 初始化
        initGetData(){
            // 获取视频id
            this.aiType = this.getAiType(this.$route.query.type) || this.type || 'assistant';
            // let code = this.$route.query.value || this.$route.query.code;
            // let fileType = this.$route.query.fileType;
            
        },
        setBrush(data,index) {
            if(typeof data === "string" && data?.split(',')?.length > 1){
                this.$nextTick(()=>{
                    this.$refs.aiContrast?.setBrush(data?.split(','),index)
                })
            }
        },
    },
    mounted() {
        this.initGetData();
        // 加载数据
        this.getContrastInfo(this.getContrastId,'mounted')
    },
    activated() {
        this.initGetData();
        this.getContrastInfo(this.getContrastId,' activated')
    }, 
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {
        
        this.APP.refresh();
    }, //生命周期 - 销毁完成
}
</script>
<style lang='scss' scoped>

</style>