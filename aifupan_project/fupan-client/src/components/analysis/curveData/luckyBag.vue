<template>
    <div class="lucky-bag">
        <div class="header flex-jcai-sb">
            <div class="flex-jcai-sb">
                <img src="@/assets/imgs/lucky_bag.png" alt="" style="height:15px;margin-right:5px;">
                <span class="title">发布福袋</span>
            </div>
            <span class="time">{{luckyBagData.blessBagTime}}</span>
        </div>
        <div class="content">
            <el-table
                :show-header="false"
                :data="tableData"
                
                style="width: 100%;font-weight: 500">
                <el-table-column
                    prop="name"
                    width="70"
                    label="名称">
                </el-table-column>
                <el-table-column
                    prop="info"
                    align="left"
                    label="信息">
                </el-table-column>
            </el-table>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    props: {
        luckyBagData: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            tableData: []
        }
    },
    computed: {},
    watch: {
        luckyBagData: {
            immediate: true,
            deep:true,
            handler(newVal) {
                if (newVal) {
                    this.tableData= [{
                        name: '福袋奖励:',
                        info: newVal.blessBagReward
                    }, {
                        name: '领取条件:',
                        info: newVal.getCondition
                    }, {
                        name: '参与人数:',
                        info: newVal.candidateNum
                    }]
                }
            }
        }
    },
    methods: {},
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
.lucky-bag {
    width: 320px;
    font-family: Arial, sans-serif;
    padding: 12px;
    background: #fff;
    border-radius: 18px;;

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;

        .title {
            font-weight: bold;
            font-size: 14px;
        }

        .time {
            font-size: 14px;
            color: #888;
        }
    }

    .content {

        ::v-deep(.el-table td.el-table__cell) {
            border: none;
            padding: 2px 0;
            vertical-align: top;
        }

        ::v-deep(.el-table::before) {
            height: 0
        }

        ::v-deep(.el-table .cell) {
            padding: 0;
            font-size: 14px;
        }
    }
}
</style>
