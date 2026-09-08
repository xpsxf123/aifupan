<template>
    <el-dialog
        :title="currentItemAnchor?.anchorInfo?.anchorName"
        append-to-body
        :close-on-press-escape="false"
        :close-on-click-modal="false"
        destroy-on-close
        :visible.sync="dialogVisible"
        width="75%"
        :before-close="handleClose">
        <Session :currentItem="currentItemAnchor" :accountType="''" :eHeight="300" :isSubVideo="true"/>
    </el-dialog>
</template>

<script>
import Session from './session.vue'

export default {
    props: {
        currentItemAnchor: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    components: {Session},
    data() {
        return {
            dialogVisible: false,
        };
    },
    computed: {},
    watch: {},
    methods: {
        handleClose() {
            this.dialogVisible = false
            this.$emit('changeCurrentItemAnchor')
        },
        open() {
            this.dialogVisible = true
        }
    },
    created() {

    },
    mounted() {
    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>