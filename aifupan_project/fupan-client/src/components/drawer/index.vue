<template>
    <el-drawer
        class="drawer-box"
        :visible.sync="getVisible"
        :with-header="getWithHeader"
        :size="width"
        :show-close="showClose"
        :wrapperClosable="wrapperClosable"
        :title="getTitle"
        v-bind="{
            ...$attrs,
            ...$props
        }"
        >
        <template #title>
            <slot name="title">
                <div>
                    <span>{{ getTitle }}</span>
                </div>
            </slot>
        </template>
        <div class="pd-16">
            <slot></slot>
        </div>
    </el-drawer>
</template>

<script>
export default {
    components: {},
    props:{
        title: {
            type: String,
            default: ''
        },
        visible: {
            type: [Boolean,undefined],
            default: undefined
        },
        width: {
            type: [String,Number],
            default: '50%'
        },
        showClose: {
            type: Boolean,
            default: true
        },
        wrapperClosable:{
            type: Boolean,
            default: false
        },
        withHeader: {
            type: [Boolean,undefined],
            default: undefined
        }
    },
    data() {
        return {
            drawer: false,
            titleStr: '',
            drawerData: {}
        };
    },
    computed: {
        getTitle(){
            return this.titleStr || this.title
        },
        getVisible:{
            get(){
                if(typeof this.visible !== 'undefined'){
                    return this.visible
                }else{
                    return this.drawer;
                }
            },
            set(val){
                this.drawer = val;
                this.$emit('update:visible', val);
            }
        },
        getWithHeader(){
            if(typeof this.withHeader !='undefined'){
                return this.withHeader
            }else{
                return !!this.getTitle
            }
        }
    },
    watch: {},
    methods: {
        init(){
            this.titleStr = '';
            this.drawerData = {}
        },
        show(data,title){
            this.drawer = true;
            this.drawerData = data;
            this.titleStr = title;
        },
        hide(){
            this.drawer = false
            this.init();
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
.drawer-box{
    ::v-deep(.el-drawer__header){
        margin: 0;
        padding: 16px 16px 0 16px;
    }
}
</style>