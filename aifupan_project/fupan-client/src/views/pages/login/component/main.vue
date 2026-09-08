<template>
    <div class="pd-l20 pd-r20">
        <login v-if="type === 'login'" @loginOrRegisterChange="loginOrRegisterChange" @changeType="onChangeType"></login>
        <register v-else @loginOrRegisterChange="loginOrRegisterChange"></register>
    </div>
</template>

<script>
import login from './login.vue';
import register from './register.vue'
export default {
    components: {
        login,
        register
    },
    props:{
        
    },
    data() {
        return {
            type: 'login'
        };
    },
    computed: {},
    watch: {},
    methods: {
        loginOrRegisterChange(type){
            this.type = type;
        },
        onChangeType(val){
            this.$emit('changeType',val)
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