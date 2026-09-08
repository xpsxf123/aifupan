<template>
    <el-dialog
        :title="isEdit?'编辑分组':'创建分组'"
        :visible.sync="visible"
        width="320px"
        :close-on-press-escape="false"
        :destroy-on-close="true"
        :close-on-click-modal="false"
        :before-close="handleClose">
        <el-input
            type="textarea"
            :rows="2"
            placeholder="请输入内容"
            maxlength="8"
            show-word-limit
            v-model="groupName">
        </el-input>
        <div slot="footer" style="text-align: center">
            <afp-button type="primary" size="default" @click="addItem">{{ isEdit ? '编辑分组' : '添加分组' }}
            </afp-button>
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
            groupName: ''
        };
    },
    computed: {
        isEdit() {
            return !isEmpty(this.editItem)
        }
    },
    watch: {
        editItem: {
            handler(val) {
               this.groupName = val.groupName;
            },
            immediate: true,
            deep: true
        },
    },
    methods: {
        handleClose() {
            this.$emit('update:visible', false);
            this.groupName = ''
        },
        addItem() {
            this.$emit('addItem', {isEdit: this.isEdit, ...this.editItem, groupName: this.groupName});
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
.subscribe-expert-group-container {

}
</style>