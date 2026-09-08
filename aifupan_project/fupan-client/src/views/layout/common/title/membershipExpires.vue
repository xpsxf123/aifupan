<template>
    <div class="memberCard pd-6 mg-r10 flex-ai-c" style="font-size: 14px;">
        <span>会员到期时间：</span>
        <span>{{getUserInfo?.expirationDate?.substring(0, 10)}}</span>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        
    },
    data() {
        return {
            expiresTime: '2023-12-31'
        };
    },
    computed: {
        getUserInfo() {
            return this.$store.state.userInfo;
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
.memberCard{
    background: url("~@/assets/imgs/theme/expirationDate.png") no-repeat center center;
    background-size: cover;
    border-radius: 6px;
    height: 100%;
}
</style>