<template>
    <div>
        <el-row :gutter="flodModel?20:8">
            <el-col :span="getSpan" :class="{'mg-t20':flodModel,'mg-t16':!flodModel}" v-for="(item,index) in getItems">
                <div class="item-box" v-if="scene==='all'">
                    <problemItem :item="item" :flod="flod" :type="type" :index="index+1" @click="onClick" :key="index"></problemItem>
                </div>
                <div class="item-box" v-if="scene==='custom'">
                    <customProblemItem :item="item" :flod="flod" :type="type" :index="index+1" @click="onClick" :key="index"></customProblemItem>
                </div>
            </el-col>
            <el-col :span="24">
                <div v-if="getBindOptionLabel" class="flex-jc-e font-s12 text-colorErr pd-t4">
                    {{ getBindOptionLabel }}
                </div>
            </el-col>
        </el-row>
    </div>
</template>

<script>
import problemItem from './item.vue'
import customProblemItem from './customItem.vue'
import aiTypeMixin from '@/mixins/aiTypeMixin'
export default {
    mixins: [aiTypeMixin],
    components: {
        problemItem,
        customProblemItem
    },
    props:{
        items: {
            type: Array,
            default: ()=>{return []}
        },
        flod: {
            type: Boolean,
            default: false
        },
        bindConfig: {
            type: Object,
            default: ()=>{return {}}
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        scene: {
            type: String,
            default: ''
        },
        type:{
            type: String,
            default: ''
        },
    },
    data() {
        return {
            
        };
    },
    computed: {
        getSpan(){
            // AI 问答全屏时改为 4 列，避免最小卡片样式下仍然挤成 3 列一行。
            if (this.flod) {
                return 6
            }
            return this.isCompare ? 8 : 8
        },
        getItems(){
            return this.items
        },
        getBindOptionLabel(){
            return this.bindConfig?.bindConfigLabel;
        },
        flodModel(){
            return this.flod||this.isViolation
        }
    },
    watch: {},
    methods: {
        onClick(data){
            this.$emit('click',data)
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
