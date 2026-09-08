export default {
    data() {
        return {}
    },
    inject: ['APP'],
    methods: {
        buyInFront(secUid) {
            this.APP.buyInAccount(secUid)
        },
        async buyInAuth(secUid, currentData, callback) {
            if (currentData !== undefined && currentData !== null) {
                callback?.()
                return
            }
            if (!secUid) return this.$message.error('获取主播信息失败，请稍后再试！')
            const result = await this.$httpClient.buyIn.getAnchorInfo({
                secUid: secUid
            })
            if (result.data.AccountType !== 0) {
                callback?.()
                return
            }
            if ([0, 2, 3, 5, 6].includes(result.data.juliangAuthStatus)) {
                this.$confirm('查看分钟成交量，请先进行巨量百应授权?', '友情提示', {
                    confirmButtonText: '点我授权',
                    cancelButtonText: '关闭',
                    showClose: false,
                    type: 'warning'
                }).then(async () => {
                    await this.buyInFront(secUid)
                }).catch(() => {
                });
            } else {
                callback?.()
            }
        },
        async buyInAuthForLive(secUid,userType) {
            if (!secUid) return this.$message.error('获取主播信息失败，请稍后再试！')
            const resultBuyIn = await this.$httpClient.buyIn.authorizedBuyIn({
                secUid: secUid,
                authType: userType
            })
            if (resultBuyIn.code !== 0) {
                this.$message.error(resultBuyIn.msg || '巨量百应授权失败，请稍后再试！')
            }
        },
        //巨量百应授权成功
        watchAuthorizedBuyInSuccess(callback) {
            this.$CSharpNotify.addTask('juliangAuthSuccess', (res, resolve) => {
                this.$nextTick(() => {
                    this.$message.success('巨量百应授权成功！')
                    callback?.()
                })
            });
        },
        //巨量百应授权失败
        watchAuthorizedBuyInError(callback) {
            this.$CSharpNotify.addTask('juliangAuthError', (res, resolve) => {
                this.$nextTick(() => {
                    this.$message.error(res?.msg)
                    callback?.()
                })
            });
        },
        //巨量百应授权过期
        watchAuthorizedBuyInExpired(callback) {
            this.$CSharpNotify.addTask('juliangAuthExpires', (res, resolve) => {
                this.$nextTick(() => {
                    callback?.()
                })
            });
        },
        //手动拉取的数据
        watchAuthorizedBuyInPullDataSuccess(callback) {
            this.$CSharpNotify.addTask('juliangPullDataSuccess', (res, resolve) => {
                this.$nextTick(() => {
                    callback?.()
                })
            });
        }
    },
}


