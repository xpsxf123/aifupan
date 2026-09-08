<template>
    <el-card class="box-card">
        <div slot="header">
            <div class="card-title text-colorMain">
                <slot name="title">
                    <span class="card-title-icon"></span>{{title}}
                </slot>
            </div>
        </div>
        <slot></slot>
    </el-card>
</template>

<script>

export default {
    components: {},
    props:{
        title: {
            type: String,
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
::v-deep(.el-card__header){
    padding: 16px;
    padding-bottom: 12px;
    border-bottom: none;
}
::v-deep(.el-card__body){
    padding: 0;
}
.box-card{
    border-radius: 10px;
}
.card-title{
    // font-family: PingFang SC, PingFang SC;
    vertical-align: middle;
    display: flex;
    align-items: center;
    font-weight: 900;
    font-size: 16px;
    line-height: 24px;
    // text-align: left;
    // font-style: normal;
    // text-transform: none;
    .card-title-icon{
        margin-right: 8px;
        display: inline-block;
        width: 4px;
        height: 16px;
        background: #05D0FF;
        border-radius: 42px 42px 42px 42px;
    }
}
</style>