<template>
    <div style="height: 100vh;" class="pd-10">
        <AiAnalysisOnly targetType='webOnline' 
        :httpRequest="getHttp" 
        :fileType="getQuery.fileType" 
        :id="getQuery.id" 
        :type="getQuery.type"  
        @request="requestHandle"></AiAnalysisOnly>
    </div>
</template>

<script>
import AiAnalysisOnly from '/src/views/commonComponent/aiAnalysis-item.vue';
export default {
    components: {
        AiAnalysisOnly
    },
    provide() {
        return {
            appVnode: this
        }
    },
    props:{
        
    },
    data() {
        return {
        
        };
    },
    computed: {
        getQuery(){
            return {
                id: this.$route.params.id,
                type: this.$route.params.type,
                fileType: this.$route.params.fileType
            }
        }
    },
    watch: {},
    methods: {
        getHttp(){
            // 类型 0:视频  1:文件。
            return this.$httpBack.v2000.getOnlineAnalysis({ uuid: this.getQuery.id, type: this.getQuery.fileType });
        },
        requestHandle({type,data}){
            if(type === 'error'){
                if(data.code === 4001){
                    this.$emit('changeType','login')
                }
            }else if(type === 'lose'){
                this.$emit('changeType','lose');
            }
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