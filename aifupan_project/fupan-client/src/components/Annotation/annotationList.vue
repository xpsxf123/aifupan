<template>
    <div>
        <annotation class="main-bg pd-6 mg-t10" v-for="(item,index) in data" :key="index" 
        :item="item" v-bind="item" :sentenceMarkData="sentenceMarkData" @save="saveHandler" :isPlayUrl="!!isPlayUrl"  @del="delHandler" @toTextMark="toTextMark"></annotation>
    </div>
</template>

<script>
import annotation from './index.vue'
export default {
    components: {
        annotation
    },
    props:{
        sentenceMarkData:{
            type: Object,
            default:()=>{
                return {}
            }
        },
        data: {
            type: Array,
            default:()=>{
                return []
            }
        },
        isPlayUrl:{
            type: [String,Boolean],
            default: ''
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        saveHandler(item){
            this.$emit('saveAnnotation', item);
        },
        delHandler(id){
            this.$emit('delAnnotation', id);
        },
        toTextMark(data){
            this.$emit('toTextMark',data);
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