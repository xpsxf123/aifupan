<template>
    <div style="height: 100vh;" class="pd-10">
        <AiAnalysisContrast targetType='webOnline' 
        :httpRequest="getHttp" 
        :contrastId="getQuery.id" 
        :type="getQuery.type"  
        @request="requestHandle"></AiAnalysisContrast>
    </div>
</template>

<script>
import AiAnalysisContrast from '/src/views/commonComponent/aiAnalysis-contrast.vue';
export default {
    components: {
        AiAnalysisContrast
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
        getHttp(){
            return this.$httpBack.v2000.getOnlineContrastAnalysis
        },
        getQuery(){
            return {
                id: this.$route.params.id,
                type: this.$route.params.type
            }
        }
    },
    watch: {},
    methods: {
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