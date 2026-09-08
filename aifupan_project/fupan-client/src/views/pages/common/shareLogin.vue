<template>
    <div>
        <dialogBox :visible.sync="dialogVisible" :show-close="false" :close-on-click-modal="false" width="380px">
            <loginMain @changeType="onChangeType"></loginMain>
        </dialogBox>
    </div>
</template>

<script>
import dialogBox from '@/components/dialog/index.vue'
import dialogMixin from '@/mixins/dialog'
import loginMain from '@/views/pages/login/component/main.vue'
export default {
    components: {
        dialogBox,
        loginMain
    },
    mixins: [dialogMixin],
    props:{
        
    },
    data() {
        return {
            dialogVisible:true
        };
    },
    computed: {},
    watch: {},
    methods: {
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
