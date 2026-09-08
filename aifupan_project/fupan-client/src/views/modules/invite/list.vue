<template>
    <div class="invite-list">
        <el-table
            :data="result.list||[]"
            border
            max-height="320">
            <el-table-column
                prop="userNickName"
                width="110"
                label="用户名">
            </el-table-column>
            <el-table-column
                prop="phone"
                width="110"
                label="手机号">
            </el-table-column>
            <el-table-column
                prop="progress"
                width="120"
                label="使用类型">
            </el-table-column>
            <el-table-column
                prop="rewardDetailList"
                label="奖励类型">
                <template slot-scope="{row}">
                    <div v-for="(reward,index) in row.rewardDetailList" :key="index">
                        {{ `${reward.rewardType} ${formatNum(reward.rewardNumber)} ${reward.rewardUnit}` }}
                    </div>
                </template>
            </el-table-column>
            <el-table-column
                width="180"
                prop="rewardDate"
                label="奖励时间">
            </el-table-column>
        </el-table>
        <el-pagination
            style="margin: 12px;float:right"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="currentPage"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="currentSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="result.totalCount">
        </el-pagination>
    </div>
</template>

<script>
import myUtils from "@/utils/utils";

export default {
    data() {
        return {
            currentPage: 1,
            currentSize: 10
        }
    },
    props: {
        result: {
            type: Object,
            default: () => {
            }
        }
    },
    computed:{
        formatNum() {
            return (val) => {
                return myUtils.fnw(val)
            }
        }
    },
    methods: {
        handleSizeChange(val) {
            this.currentSize = val
            this.$emit('pageChange', {
                limit: val,
                page: this.currentPage
            })
        },
        handleCurrentChange(val) {
            this.currentPage = val
            this.$emit('pageChange', {
                limit: this.currentSize,
                page: val
            })
        }
    }
}
</script>

<style lang="less" scoped>
.invite-list {

}
</style>