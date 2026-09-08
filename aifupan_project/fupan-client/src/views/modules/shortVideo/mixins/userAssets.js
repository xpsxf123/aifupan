import {isEmpty} from "lodash";

export default {
    computed: {
        userProperty() {
            return this.$store.getters.getUserproperty
        },
        isAuthenticated() {
            return (item) => {
                const {userType, id: currentUserId} = this.$store.getters.getUserInfo;
                if (userType === 0) {
                    return false
                } else {
                    return item?.userId !== currentUserId
                }
            }
        }
    },
    methods: {
        async getUserProperty() {
            await this.$httpBack.userProperty.getUserProperty();
        },
        manualUpdate(item) {
            this.currentItem = item
            this.$refs.tipMessage?.open({
                content: `<div>
                    <div>频繁更新数据，有可能会引发风控问题。</div>
                    <div style="margin-top: 8px">建议每个关键词每天使用更新按钮，更新数据<span
                        style="color:red;">不超过3次</span>，间隔<span
                        style="color:red;">1分钟以上！</span>
                    </div>
                </div> `,
                confirmButtonText: '确定更新',
                cancelButtonText: '取消更新'
            })
        },
        async confirmEvent() {
            if (isEmpty(this.currentItem)) return
            try {
                const result = await this.$httpClient.shortVideo.updateHotSearchData({
                    platformType: this.currentItem.platformType,
                    keyword: this.currentItem.keyword,
                    industryId: this.currentItem.industryId,
                    groupId: this.currentItem.groupId,
                    likeCountMin: this.currentItem.subscriptionLikeCountThreshold,
                    autoSyncEnabled: this.currentItem.autoSyncEnabled,
                    likeCountThreshold: this.currentItem.likeCountThreshold,
                    updateTimeCondition: this.currentItem.updateTimeCondition
                })
                if (result.code === 0) {
                    this.$message.success('数据已更新，请勿频繁更新，可能会引发风控问题')
                }
            } catch (e) {
            } finally {
                this.currentItem = {}
                await this.refreshData?.()
            }
        },
        cancelEvent() {
            this.currentItem = {}
        },
    },
}


