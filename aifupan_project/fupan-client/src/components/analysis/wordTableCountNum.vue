<template>
    <div :class="{'flex-jc-sb': countNum, 'flex-ai-c': countNum}">
        <i v-if="countNum>0" class="el-icon-caret-left" @click="prevWords"
            style="font-size: 22px; color: dodgerblue; cursor: pointer;"></i>
        <div>
            <span v-if="isShow && index >= 0">
                {{ (index + 1)+"/"}}
            </span>
            <span v-if="countNum>0">{{ countNum }}</span>
        </div>
        <i  v-if="countNum>0" class="el-icon-caret-right" @click="nextWords"
            style="font-size: 22px; color: dodgerblue; cursor: pointer;"></i>
    </div> 
</template>

<script>

export default {
    components: {},
    props:{
        countNum:{
            type: Number,
            default: 0
        },
        index:{
            type: Number,
            default: -1
        },
        isShow: {
            type:Boolean,
            default: false
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        prevWords() {
            this.$emit('selectMark',  -1)
        },
        nextWords() {
            this.$emit('selectMark',  +1)
        },
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