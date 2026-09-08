<template>
    <dialog-box :visible.sync="dialogVisible" title="文件备注" append-to-body
                :close-on-click-modal="false" width="600px" @closed="closed">
        <div style="height: 500px;">
            <div style="height: calc(500px - 40px);" class="ai-window-box overflow_hidden overflow_auto_y">
                <div v-html="fileRemark"></div>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';

export default {
    components: {
        DialogBox
    },
    mixins: [dialogMixin],
    props: {},
    data() {
        return {
            fileRemark: ''
        };
    },
    computed: {},
    watch: {},
    methods: {
        async showCallback({data}) {
            this.fileRemark = data.cloudRemarks
        },
        closed() {
            this.fileRemark = '';
        },
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
<style lang='scss'>
.edit-content-box {

}
</style>