<template>
    <div class="subscribe-expert-group-container">
        <div class="subscribe-expert-group flex flex-col">
            <el-card class="box-card">
                <div class="header flex items-center justify-between">
                    <div>
                        <afp-button type="primary" size="default" icon="el-icon-plus" @click="addItemVisible=true">创建分组
                        </afp-button>
                        <afp-button type="primary" @click="addSubscribe" plain size="default"
                                   v-if="tableConfig?.tableData.length">添加订阅
                        </afp-button>
                    </div>
                    <div>
                        本账号订阅数(剩余/总)：{{ userProperty?.userSubscribeHotVideoNum || 0 }}/{{userProperty?.totalUserSubscribeHotVideoNum || 0}}个
                        共享订阅数：{{ userProperty?.subscribeHotVideoNum || 0 }}/{{ userProperty?.totalSubscribeHotVideoNum || 0}}个
                    </div>
                </div>
            </el-card>
            <el-card class="box-card-content" style="margin-top: 12px;flex: 1">
                <TableList :tableConfig="tableConfig">
                    <template #emptyText>
                        <div class="empty-text">暂无数据</div>
                    </template>
                </TableList>
            </el-card>
        </div>
        <AddItem :visible.sync="addItemVisible" @addItem="addItem" :editItem="editItem"/>
    </div>
</template>

<script>
import TableList from './../../component/tableList.vue'
import AddItem from './../../component/addItem.vue'
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";

export default {
    components: {TableList, AddItem},
    mixins: [userAssets],
    props: {},
    data() {
        return {
            addItemVisible: false,
            editItem: {},
            groupText: '',
            tableConfig: {
                eHeight: 215,
                column: [{
                    label: "分组名称",
                    prop: 'groupName',
                    option: {
                        width: '200',
                        align: 'left'
                    }
                }, {
                    label: "关键词个数",
                    prop: 'memberCount',
                }, {
                    label: "视频总数",
                    prop: 'videoCount'
                }, {
                    label: "创建人",
                    prop: 'creator'
                }],
                options: [{
                    label: '查看详情',
                    show: (item) => {
                        return true
                    },
                    click: (item) => {
                        this.$emit('viewList',item.groupId)
                    }
                }, [{
                    label: '编辑',
                    show: (item) => {
                        return item.groupId && !this.isAuthenticated(item)
                    },
                    click: (item) => {
                        this.editItem = item
                        this.addItemVisible = true
                    }
                }, {
                    label: '删除',
                    show: (item) => {
                        return item.groupId && !this.isAuthenticated(item)
                    },
                    click: (item) => {
                        this.$confirm('确认要删除该分组？', '友情提示', {
                            confirmButtonText: '确定删除',
                            cancelButtonText: '再想想',
                            type: 'warning'
                        }).then(() => {
                            this.delGroup(item.groupId)
                        })
                    }
                }]],
                tableData: []
            }
        };
    },
    watch: {
        addItemVisible: {
            handler(val) {
                if (!val) this.editItem = {}
            },
            deep: true,
        }
    },
    methods: {
        async getGroupList() {
            const result = await this.$httpBack.shortVideo.groupList({groupType: 2})
            if (result.code !== 0) return
            this.tableConfig = {
                ...this.tableConfig,
                tableData: result.data?.list || []
            }
        },
        addSubscribe() {
            this.$emit('addSubscribe', {}, this.getGroupList)
        },
        async addItem({isEdit, groupType = 2, groupId = '', groupDescription = '', groupName}) {
            if (!groupName) return
            const httpServer = isEdit ? this.$httpBack.shortVideo.editGroup : this.$httpBack.shortVideo.addGroup
            try {
                const result = await httpServer({
                    groupId,
                    groupName,
                    groupType,
                    groupDescription
                })
                if (result.code !== 0) return this.$message.error(result.msg)
                this.$message.success(isEdit ? '修改' : '添加' + '成功')
                this.addItemVisible = false
                await this.getGroupList()
            } catch (e) {
            }
        },
        async delGroup(groupId) {
            const result = await this.$httpBack.shortVideo.delGroup(groupId)
            if (result.code !== 0) return
            this.$message.success('删除成功');
            await this.getGroupList()
        },
    },
    created() {

    },
    mounted() {
        this.getGroupList()
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
    height: 100%;

    .subscribe-expert-group {
        height: 100%;

        ::v-deep(.el-card__body) {
            padding: 0;
        }

        .box-card {
            .header {
                padding: 24px 12px;
            }
        }
    }
}
</style>