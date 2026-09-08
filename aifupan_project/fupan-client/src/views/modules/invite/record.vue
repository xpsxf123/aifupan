<template>
    <div class="invite-record">
        <el-dialog
            title="邀请记录"
            width="50vw"
            :close-on-click-modal="false"
            :visible.sync="dialogVisible">
            <List v-if="result?.list?.length>0" :result="result" @pageChange="pageChange"/>
            <div class="record-content" v-else>
                <img src="~@/assets/imgs/invite-record-empty.png" alt="" class="invite-img"/>
                <p>暂未有记录，快去邀请好友吧~</p>
                <afp-button @click="inviteClick" class="invite-btn">邀请好友</afp-button>
            </div>
        </el-dialog>
    </div>
</template>
<script>
import List from './list.vue'
import share from '@/mixins/share'

export default {
    components: {List},
    mixins: [share],
    data() {
        return {
            dialogVisible: false,
            result: {}
        }
    },
    methods: {
        changeDialogStatus(status) {
            this.dialogVisible = status
            if (status) this.getList()
        },
        inviteClick() {
            this.copyToClipboard()
            this.dialogVisible = false
        },
        async getList(data) {
            const {data: result, code} = await this.$httpBack.v2500?.clientGetUserRewardList(data ?? {
                page: 1,
                limit: 10
            })
            if (code !== 0) return this.$message.error('获取记录失败')
            this.result = result
        },
        pageChange(data) {
            this.getList(data)
        }
    }
}
</script>

<style lang="less" scoped>
.invite-record {
    .record-content {
        text-align: center;
        margin-bottom: 20px;

        .invite-img {
            width: 160px;
        }

        .invite-btn {
            background: linear-gradient(90deg, #936B00 0%, #613B0E 100%);
            border-radius: 8px;
            color: #DAB078;
        }
    }

    ::v-deep(.el-dialog__body) {
        padding: 0 12px 12px 12px;
    }
}

</style>