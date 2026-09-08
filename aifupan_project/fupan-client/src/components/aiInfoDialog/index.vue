<template>
    <dialog-box :visible.sync="dialogVisible" title="查看分析" class="ai-window-dialog-box"
        :close-on-click-modal="false" width="900px">
        <div>
            <div class="ai-window-box overflow_hidden overflow_auto_y">
                <aiContent :els="dialogData.list" readonly></aiContent>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import aiContent from '@/components/analysis/ai/common/aiContent.vue';
export default {
    components: {
        DialogBox,
        aiContent
    },
    mixins: [dialogMixin],
    props: {
        targetType: {
            type: String,
            default: ''
        }
    },
    provide() {
        return {
            AiVnode: this
        }
    },
    data() {
        return {
            formData: {}
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        onCancel() {
            this.hide();
        },
        async showCallback() {
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss'>
.ai-window-dialog-box {
    .ai-window-box {
        height: calc(100vh - 220px);
        position: relative;
        .ai-box {
            padding-top: 0;
            height: auto;
            // .deepThinking {
            //     display: none;
            // }
            // .deepThinkingTitle {
            //     .font_family {
            //         display: none;
            //     }
            // }
        }
        .ai-content-box{
            padding-top: 10px;
        }
        .aiCustom-box{
            margin-top: 0;
            margin-bottom: 10px;
        }
    }


}
</style>