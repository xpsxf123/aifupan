<template>
    <el-dialog
        title="提示"
        :visible.sync="visible"
        class="del-video"
        width="440px"
        :close-on-press-escape="false"
        :destroy-on-close="true"
        :close-on-click-modal="false"
        :before-close="handleClose">
        <div class="content-container">
            <div class="content">
                <i class="icon el-icon-warning-outline"></i>
                <span>将永久删除选中的文件，是否继续</span>
            </div>
            <div style="text-align: center">
                <el-radio-group v-model="selectType">
                    <el-radio :label="0" class="radio-as-checkbox">删除全部内容</el-radio>
                    <el-radio :label="1" class="radio-as-checkbox">仅删除视频文件（保留其他东西）</el-radio>
                </el-radio-group>
            </div>
        </div>
        <div slot="footer" class="text-center">
            <afp-button @click="handleClose" size="default">取 消</afp-button>
            <afp-button @click="delFileType" type="primary" style="margin-left: 30px" size="default" :plain="false">删 除</afp-button>
        </div>
    </el-dialog>
</template>

<script>

import {isEmpty} from "lodash";

export default {
    components: {},
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        editItem: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            selectType: 0
        };
    },
    computed: {
        isEdit() {
            return !isEmpty(this.editItem)
        }
    },
    watch: {},
    methods: {
        handleClose() {
            this.$emit('update:visible', false);
        },
        delFileType() {
            this.$emit('delFileType', this.selectType);
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
<style lang='scss' scoped>
.del-video {
    .content-container {
        .content {
            padding-bottom: 12px;
            display: flex;
            align-items: center;

            .icon {
                color: #e6a23c;
                font-size: 20px;
                margin-right: 6px;
            }
        }
    }

    ::v-deep(.el-dialog__body) {
        padding: 16px 20px;
    }
}
</style>