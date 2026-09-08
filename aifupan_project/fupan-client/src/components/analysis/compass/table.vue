<template>
    <div class="main-bg" style="height:100%">
        <!-- 纵向排列 -->
        <div v-if="isVertical" class="compass-table-vertical flex-jc-sb text-center h100">
            <div v-for="(item1,index1) in tableDatas" class="flex-column flex-1 h100" :class="index1 === 0?'flex-05 ':''" style="align-items: stretch;display: flex;" :key="index1">
                <div class="compass-table-level-1" :class="`level-${index1+1}`">
                    <barChat :id="index1 === 0 ? `barchat-1-dom`: ''" :label="item1?.label" :compareData="compareData" :chatData="item1.datas">
                        <span v-if="!isContrast" class="">{{ item1?.data1 }}%</span>
                        <span v-else>
                            <span class="contrast-c1">{{ item1.data1 }}%</span>
                            <span class="pd-l4 pd-r4">|</span>
                            <span class="contrast-c2">{{ item1.data2 }}%</span>
                        </span>
                    </barChat>
                </div>
                <div class="compass-table flex-jc-sb text-center h100 flex-1"  :class="index1 !=0 ?'b-r1-c2':''">
                    <div v-for="(item2,index2) in item1.datas" class="flex-column flex-1" :class="`b-l1-c2`" :key="index2">
                        <div class="compass-table-level-item" :class="`level-${index1+1}-${index2+1}`">
                            <barChat  :label="item2?.label">
                                <span v-if="!isContrast" class="">{{ item2?.data1 }}%</span>
                                <span v-else>
                                    <span class="contrast-c1">{{ item2.data1 }}%</span>
                                    <span class="pd-l4 pd-r4">|</span>
                                    <span class="contrast-c2">{{ item2.data2 }}%</span>
                                </span>
                            </barChat>
                        </div>
                        <div class="text-center flex-column h100" style="flex: 1 1 0%;" >
                            <div v-for="(item3,index3) in item2.datas" :key="index3" class="compass-table-level-item level-3">
                                <barChat :label="item3?.label">
                                    <span v-if="!isContrast" class="">{{ item3?.data1  }}%</span>
                                    <span v-else>
                                        <span class="contrast-c1">{{ item3.data1 }}%</span>
                                        <span class="pd-l4 pd-r4">|</span>
                                        <span class="contrast-c2">{{ item3.data2 }}%</span>
                                    </span>
                                </barChat>
                            </div>
                        </div>
                    </div>
                </div>
            </div> 
        </div>
        <!-- 横向排列 -->
        <div v-else-if="isCross" class="compass-table-cross text-center">
            <div v-for="(item1,index1) in tableDatas" :key="index1" class="flex-row">
                <div class="compass-level-title1" :class="`level-${index1+1}`">
                    <barChat :label="item1?.label" :compareData="compareData" :chatData="item1.datas">
                        <div>
                            <span class="contrast-c1">{{ item1.data1 }}%</span>
                            <span class="pd-l4 pd-r4">|</span>
                            <span class="contrast-c2">{{ item1.data2 }}%</span>
                        </div>
                    </barChat>
                </div>
                <div v-for="(item2,index2) in item1?.datas" :key="index2" class="flex-1 b-b1-c2 b-r1-c2">
                    <div  class="compass-level-title" :class="`level-${index1+1}-${index2+1}`">
                        <barChat  :label="item2?.label">
                            <span>
                                <span class="contrast-c1">{{ item2.data1 }}%</span>
                                <span class="pd-l4 pd-r4">|</span>
                                <span class="contrast-c2">{{ item2.data2 }}%</span>
                            </span>
                        </barChat>
                    </div>
                    <div class="flex-row">
                        <div v-for="(item3,index3) in item2?.datas" :key="index3" class="flex-1">
                            <div class="compass-level-title  b-b1-c2">
                                <barChat  :label="item3?.label"></barChat>
                            </div>
                            <div class="flex-jc-c flex-ai-c flex-row text-center compass-level-item3 font-s12 text-colorMain">
                                <div>
                                    <span class="contrast-c1">{{ item3.data1 }}%</span>
                                    <span class="pd-l4 pd-r4">|</span>
                                    <span class="contrast-c2">{{ item3.data2 }}%</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import barChat from './barChat.vue';
export default {
    components: {
        barChat
    },
    props:{
        type: {
            type:String,
            default: ''
        },
        tableDatas: {
            type: Array,
            default: () => {
                return []
            }
        },
        compareData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        isContrast: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
        };
    },
    computed: {
        // 是否纵向
        isVertical(){
            return this.type === 'vertical'
        },
        // 是否横向
        isCross(){
            return this.type === 'cross'
        },
        
    },
    watch: {},
    methods: {
        
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
.level-1{background: #EDF6E8;}
.level-1-1{background: #D4E9D6;}
.level-1-2{background: #AFD473;}
.level-2{background: #F0EDF6;}
.level-2-1{background: #DAD5E9;}
.level-2-2{background: #978CB6;}
.level-2-3{background: #CCE3E7;}
.level-2-4{background: #8DB2CD;}
.compass-table-vertical{
    .compass-table-level-1{
        height: 44px;
    }
    .compass-table-level-item{
        height: 28px;
    }
    .level-3{
        padding: 4px 0;
    }
}

.compass-table-cross{
    .compass-level-title1{
        width: 127px;
    }
    .compass-level-item3{
        min-width: 50px;
    }
    .compass-level-title,.compass-level-item3{
        height: 34px;
    }

}

</style>