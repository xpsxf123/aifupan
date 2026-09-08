<template>
    <div style="height: calc(100vh - 66px);">
        <!-- <div class="pd-8 main-bg">
            <officialBottom style="justify-content: flex-start !important;"></officialBottom>
        </div> -->
        <div class="pd-10 h100">
            <AiAnalysisOnly 
                targetType='webOnline' 
                :httpRequest="getOnlyHttp"
                :fileType="getQuery.type" 
                :id="getQuery.id" 
                :type="getQuery.aiType"
                :shareId="getQuery.shareId"
            >
            </AiAnalysisOnly>
        </div>
    </div>
</template>

<script>
import AiAnalysisOnly from '/src/views/commonComponent/aiAnalysis-item.vue';
export default {
    components: {
        AiAnalysisOnly
    },
    props:{
        
    },
    data() {
        return {
        
        };
    },
    computed: {
        getQuery(){
            // type:  2: 对比,1: 上传文件,0: 录制视频
            return {
                id: this.$route.params.id,
                type: this.$route.params.type,
                shareId: this.$route.params.shareId,
                aiType: this.$route.params.aiType
            }
        },
        isOnly(){
            return this.getQuery.type !== '2'
        },
        isContrast(){
            return this.getQuery.type === '2';
        },
        getContrastHttp(){
            return this.$httpBack.v2000.getContrastAnalysis
        }
        
    },
    watch: {},
    methods: {
        getOnlyHttp(){
            // 类型 0:视频  1:文件。
            return this.$httpBack.v2000.getAnalysis({ uuid: this.getQuery.id, type: this.getQuery.type });
        }
    },
    created() {
        
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