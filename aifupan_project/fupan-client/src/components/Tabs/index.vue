<template>
   <div :class="{'is-scroll': isScroll}">
        <el-tabs v-model="getActive" @tab-click="handleClick" :class="{'value-class':!!tabs[0].showTextKey}">
            <el-tab-pane v-for="(item,index) in tabs" v-if="tabsHide[index]" :key="item.name" :label="item.label" :name="item.name">
                <template #label>
                    <span>
                        <span class="tab-text">{{ item.label }}</span>
                        <span v-if="item.showTextKey"
                              style="position:absolute;right: 40px;color: #000">{{ compereInfo[item.showTextKey] }}</span>
                    </span>
                </template>
                <div :style="{padding: contentPadding || 0}">
                    <slot name="common"></slot>
                    <slot v-if="isReload(item)" :name="item.name" v-bind="item"></slot>
                </div>
            </el-tab-pane>
        </el-tabs>
   </div>
</template>

<script>

import store from "@/store";

export default {
components: {},
data() {
    return {
        activeName: ''
    };
},
props: {
    tabs: {
        type:Array,
        default: ()=>{
            return []
        }
    },
    reload: {
        type: Boolean,
        default: false
    },
    contentPadding: {
        type:String,
        default: ''
    },
    isScroll: {
        type: Boolean,
        default: false
    },
    notInitName: {
        type:Boolean,
        default: false
    }
},
computed: {
    getActive:{
        get(){
            if(this.$attrs.value !==undefined && this.$attrs.value !==''){
                return this.$attrs.value
            };
            return this.activeName || this.tabs?.[0]?.name || ''
        },
        set(val){
            this.activeName = val;
            if(this.getActive === val){return}
            this.$emit('input',val);
        }
    },
    tabsHide(){
        return this.tabs.map(o=>{
            return this.show(o);
        })
    },
    compereInfo(){
        return this.$store.getters.getCompereInfo
    }
},
watch: {},
methods: {
    handleClick(item){
        // this.getActive = item.name;
        this.$emit('click',item.name);
        this.$emit('input', item.name);
    },
    isReload(item){
        if(this.reload){
            return item.name === this.activeName
        }else{
            return true
        }
    },
    show(item){
        if(typeof item.hidden === 'function'){
            return !item.hidden(item);
        }else if(typeof item.show === 'function'){
            return item.show(item);
        }else{
            return true
        }
    }
},
created() {

},
mounted() {
    if(this.notInitName){return}
    this.$nextTick(()=>{
        this.getActive = this.$attrs.value || this.tabs[0]?.name || '';
    })
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
::v-deep(.el-tabs__active-bar){
    height: 3px;
    background: var(--color-main);
    border-radius: 29px;
    margin: 0;
}
.is-scroll{
    ::v-deep(.el-tabs__content){
        height: calc(100vh - 105px);
        overflow: hidden;
        overflow-y: auto;
    }
}
.value-class{
    ::v-deep(.el-tabs__item){
        padding-right: 60px!important;
    }
}

</style>