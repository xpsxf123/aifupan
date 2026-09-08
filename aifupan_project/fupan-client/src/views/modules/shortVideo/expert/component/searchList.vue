<template>
    <div class="expert">
        <TableList :tableConfig="tableConfig" @paginationChange="paginationChange">
            <template #name="{ row: item }">
                <div class="flex items-center">
                    <el-avatar :src="item.avatar"></el-avatar>
                    <div class="video-title text-clamp2 line-clamp1">{{ item.nickname }}</div>
                </div>
            </template>
            <template #emptyText>
                <div class="empty-text">暂无数据</div>
            </template>
        </TableList>
    </div>
</template>

<script>
import TableList from "./../../component/tableList.vue";

export default {
    components: {TableList},
    props: {
        actionType: {
            type: String,
            default: ''
        },
        tableConfig: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {};
    },
    computed: {
    },
    watch: {},
    methods: {
        paginationChange(values){
            this.$emit('paginationChange', values)
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
.expert {
    .video-title {
        padding-left: 8px;
    }
}
</style>