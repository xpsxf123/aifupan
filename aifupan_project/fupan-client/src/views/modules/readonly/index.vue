<template>
   <div class="h100">
        <analysis-item-readonly v-if="showAnalysis" :sentenceMarkData="sentenceMarkData"></analysis-item-readonly>
   </div>
</template>

<script>
import analysisItemReadonly from '@/views/commonComponent/analysis-item-readonly.vue';
import analysisMixin from '@/mixins/analysisMixin';
export default {
    mixins: [analysisMixin],
    components: {
        analysisItemReadonly
    },
    props:{
        
    },
    data() {
        return {
            targetType: '',
            sentenceMarkData:{},
            showAnalysis: false,
        };
    },
    computed: {},
    watch: {},
    methods: {
        initData(){
            this.targetType = this.$route.params.type;
            this.getData(this.$route.query.id);
        },
        getData(id){
            this.sentenceMarkData.sentenceMarkList = [];
            let http = null;
            if(this.targetType === 'video'){
                http = this.$httpClient.video.lockanalysis({ videoId: id })
            }else if(this.targetType === 'file'){
                http = this.$httpClient.uploadFile.lockanalysis({ fileId: id })
            }else if(this.targetType === 'online'){
                http = this.$httpClient.video.lockCloudAnalysis({ videoId: id })
            }
            http?.then((res)=>{
                if (res.code == 0) {
                    // 视频/音频播放地址
                    this.initAudioaAlyses(res.data);
                    this.showAnalysis = true;
                }
            })
        }
    },
    created() {
        this.initData();
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>