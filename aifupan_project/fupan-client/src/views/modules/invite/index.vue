<template>
    <div>
        <div class="invite-container">
            <div class="invite-header">
                <img class="invite-header-img" src="@/assets/imgs/invite/invite-header.png" alt=""/>
                <div class="header-content">
                    <!--                <h1 style="color: #fff;font-size: 32px">邀请好友，多邀多得：最高送价值300元增量包！</h1>-->
                    <!--                <div class="invite-steps">-->
                    <!--                    <div class="step">-->
                    <!--                        <div class="step-number">-->
                    <!--                            <div class="b_g">1</div>-->
                    <!--                        </div>-->
                    <!--                        <div class="step-text">发送邀请链接给好友</div>-->
                    <!--                    </div>-->
                    <!--                    <div class="step">-->
                    <!--                        <div class="step-number">-->
                    <!--                            <div class="b_g">2</div>-->
                    <!--                        </div>-->
                    <!--                        <div class="step-text">好友注册爱复盘</div>-->
                    <!--                    </div>-->
                    <!--                    <div class="step">-->
                    <!--                        <div class="step-number">-->
                    <!--                            <div class="b_g">3</div>-->
                    <!--                        </div>-->
                    <!--                        <div class="step-text">获得爱复盘增量包奖励</div>-->
                    <!--                    </div>-->
                    <!--                    <div class="step">-->
                    <!--                        <div class="step-number">-->
                    <!--                            <div class="b_g">4</div>-->
                    <!--                        </div>-->
                    <!--                        <div class="step-text">好友获得7天试用资格</div>-->
                    <!--                    </div>-->
                    <!--                </div>-->
                </div>
            </div>
            <div style="display: flex;align-items: center;justify-content: center;margin-top: -36px">
                <img src="@/assets/imgs/invite/share-btn.png" class="invite-btn" alt="" @click="generateInviteLink">
            </div>

            <div class="invite-content">
                <div class="reward-summary">
                    <div class="header-title">
                        <div style="display:flex;align-items: center;justify-content: space-between">
                            <div>
                                <span>奖励汇总</span>
                                <img src="~@/assets/imgs/star.png" alt="" class="star">
                            </div>
                            <span style="font-size: 13px;cursor: pointer" @click="viewRecord">邀请记录 <i
                                class="el-icon-arrow-right"></i></span>
                        </div>
                    </div>
                    <div class="reward" v-if="rewardSummary.length"
                         :style="{width : 180 * rewardSummary.length + rewardSummary.length + 'px'}">
                        <div class="reward-col" v-for="(item,index) in rewardSummary" :key="index">
                            <div class="summary-item">
                                <div class="summary-label">{{ item.rewardLabel }}:</div>
                                <div class="summary-value">{{ formatNum(item.rewardNum) }} {{ item.rewardUnit }}</div>
                            </div>
                        </div>
                    </div>
                    <img src="@/assets/imgs/chartEmpty.png" alt="" v-else
                         class="image-style"/>
                </div>

                <div class="reward-rules">
                    <div class="header-title">
                        <span>奖励规则</span>
                        <img src="~@/assets/imgs/star.png" alt="" class="star">
                    </div>
                    <el-table :data="progressList" style="width: 100%" size="medium"
                              :header-cell-style="{backgroundColor: '#FAFAFA'}">
                        <el-table-column prop="inviteProgressTitle" label="好友使用条件" width="160"></el-table-column>
                        <el-table-column prop="inviteProgressRequire" label="要求" width="200"></el-table-column>
                        <el-table-column prop="reward" label="你将获得奖励">
                            <template slot-scope="{row}">
                                <div v-for="item in row.rewardList" :key="item.id" style="white-space: pre-line;">
                                    {{ item.commodityTypeName }}
                                    <span style="color:#FF9866"> {{ formatNum(item.commodityNumber) }}</span>
                                    {{ item.commodityTypeUnit }}
                                </div>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>

                <div class="invite-notes">
                    <div class="header-title">
                        <span>邀请攻略</span>
                        <img src="~@/assets/imgs/star.png" alt="" class="star">
                    </div>
                    <ol style="font-size: 14px">
                        <li v-for="item in platformList" :key="item.id">{{ item.value }}</li>
                    </ol>
                </div>
            </div>
            <Record ref="invite_record"/>
        </div>
        <div style="height: 10px"></div>
    </div>
</template>

<script>
import Record from './record.vue'
import share from '@/mixins/share.js'
import myUtils from "@/utils/utils";

export default {
    name: 'InviteFriends',
    mixins: [share],
    components: {Record},
    data() {
        return {
            rewardSummary: [],
            progressList: [],
            platformList: []
        }
    },
    activated() {
        this.getClientGetRewardSummary()
        this.getClientInviteActivity()
        this.getPlatformList()
    },
    computed: {
        formatNum() {
            return (val) => {
                return myUtils.fnw(val)
            }
        }
    },
    methods: {
        generateInviteLink() {
            this.copyToClipboard();
        },
        async getClientGetRewardSummary() {
            const {data: result, code} = await this.$httpBack.v2500.clientGetRewardSummary()
            if (code !== 0) return
            this.rewardSummary = result || []
        },
        async getClientInviteActivity() {
            const {data: result, code} = await this.$httpBack.v2500.clientinviteactivity()
            if (code !== 0) return
            this.progressList = result?.progressList || []
        },
        async getPlatformList() {
            const {data: result, code} = await this.$httpBack.dictdata.list({
                typeLogo: 'activity_rule',
                limit: -1
            }, {load: false})
            if (code !== 0) return
            this.platformList = result?.list || []
        },
        viewRecord() {
            this.$refs.invite_record?.changeDialogStatus(true)
        },
    }
}
</script>

<style lang="less" scoped>
.invite-container {
    padding-bottom: 5px;
    background-color: #fff;
    border-radius: 20px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .invite-header {
        position: relative;

        .invite-header-img {
            width: 100%;
            display: block;
        }

        .header-content {
            position: absolute;
            width: 100%;
            top: 0;
            padding: 3% 25px 0 25px;
        }
    }

    .highlight {
        color: #FF6600;
        font-weight: bold;
    }

    .invite-steps {
        //width: 50%;
        display: flex;
        justify-content: flex-start;
        margin-bottom: 30px;
    }

    .step {
        display: flex;
        justify-content: flex-start;
        align-items: center;

    }

    .step:not(:first-child) {
        padding: 0 30px;
    }

    .step-number {
        width: 30px;
        height: 30px;
        color: #000;
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        font-weight: bold;

        .b_g {
            height: 15px;
            width: 25px;
            text-align: center;
            background: #fff;
            border-radius: 60px;
            line-height: 3px;
            margin-top: 12px;
        }
    }

    .invite-btn {
        height: 65px;
        z-index: 1;
        cursor: pointer;
    }

    .invite-content {
        width: 722px;
        margin-top: 12px;
        margin-left: 40px;
    }

    .image-style {
        display: block;
        width: 160px;
        margin: 0 auto;
    }

    .reward-summary, .reward-rules, .invite-notes {
        margin-bottom: 16px;
    }

    .reward {
        border: 1px solid #E0E0E0;
        border-radius: 10px;
        overflow: hidden;
        margin-top: 16px;
        display: flex;
        max-width: 720px;
        flex-wrap: wrap;
        box-sizing: content-box;
    }

    .summary-item {
        background-color: #F5F7FA;
        padding: 12px 18px;
        border-radius: 4px;
        height: 100%;
        position: relative;

        &:after {
            position: absolute;
            content: '';
            display: block;
            height: 60px;
            border-right: 1px solid #E0E0E0;
            top: 12px;
            right: 0;
        }
    }

    .reward-col {
        width: 180px;
    }

    .reward-col:nth-child(4n) {
        .summary-item {
            &:after {
                display: none;
            }
        }
    }

    .reward-col:last-child {
        .summary-item {
            &:after {
                display: none;
            }
        }
    }


    .summary-label {
        color: #606266;
        margin-bottom: 5px;
        font-size: 14px;
    }

    .summary-value {
        font-size: 18px;
        font-weight: bold;
        color: #303133;
        margin-top: 13px;
    }

    .invite-notes ol {
        padding-left: 20px;

    }

    .invite-notes li {
        margin-bottom: 10px;
        color: #606266;
    }

    .header-title {
        position: relative;
        font-size: 16px;
        margin-bottom: 5px;

        &:after {
            position: absolute;
            content: '';
            display: block;
            width: 65px;
            height: 5px;
            background: rgba(248, 227, 144, 0.5);
            top: 16px;
            left: 0;
        }

        .star {
            position: absolute;
            height: 10px;
            margin-left: 4px;
        }
    }
}

</style>