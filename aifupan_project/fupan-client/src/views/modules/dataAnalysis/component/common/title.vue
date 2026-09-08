<template>
    <div class="statusContainer">
        <TitleLeft @leftClick="$emit('leftClick')" :detectionTime="detectionTime">
            <template v-if="$slots.titleLeft" #titleLeft>
                <slot name="titleLeft">
                </slot>
            </template>
        </TitleLeft>
        <title-right :detection="detection" :compereMapList="compereMapList"
        ref="titleRight"
            @start="(bl)=>$emit('start',bl)"
            @stop="(bl)=>$emit('stop',bl)"
            :type="type"
        >
            <template v-if="$slots.search" #search>
                <slot name="search"></slot>
            </template>
            <template v-if="$slots.titleLeftBottom" #titleLeftBottom>
                <slot name="titleLeftBottom"></slot>
            </template>
        </title-right>
    </div>
</template>

<script>
import TitleLeft from './titleLeft.vue';
import TitleRight from './titleRight.vue';
export default {
components: {
    TitleLeft,
    TitleRight,
},
props:{
    type:{
        type:String,
        default: 'all' // all-所有主播title, detection-录制中,
    },
    detection:{
        type: Boolean,
        default: false
    },
    detectionTime:{
        type:[String,Number],
        default: ''
    },
    recordNum:{
        type:Number,
        default: 0
    },
    compereMapList: {
        type: Object,
        default: () => {}
    }
},
data() {
return {

};
},
computed: {},
watch: {},
methods: {
    startClick(){
        this.$nextTick(()=>{
            this.$refs.titleRight.startClick()
        })
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