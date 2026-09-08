<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                成交率：{{ dealRate ? getDealRate || '-' : '-' }}
            </div>
        </div>
        <afp-button :plain="false" size="small" class="elBt" :class="dealRate ? 'cancelIndiciaBtn' : 'indiciaBtn'" @click="markClick"
                   >{{
                dealRate ? '隐藏成交率' : '点击分析'
            }}
        </afp-button>
    </div>
</template>

<script>
import buyIn from '@/mixins/buyIn.js'
import myUtils from "@/utils/utils";

export default {
    mixins: [buyIn],
    props: {
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            dealRate: true,
        };
    },
    computed: {
        getDealRate() {
            const {
                purchaseCountStart,
                purchaseCountEnd,
                totalWatchNum
            } = this.sentenceMarkData
            if (myUtils.isGreaterThanZero(totalWatchNum) && (myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd))) {
                if (purchaseCountStart === purchaseCountEnd) {
                    return `${myUtils.fnw((purchaseCountStart / totalWatchNum) * 100, 2)}%`
                } else {
                    return `${myUtils.fnw((purchaseCountStart / totalWatchNum) * 100, 2)}% ~ ${myUtils.fnw((purchaseCountEnd / totalWatchNum) * 100, 2)}%`
                }
            } else {
                return '0.00%'
            }
        }
    },
    mounted() {

    },
    created() {

    },
    methods: {
        async markClick() {
            const currentData = this.getDealRate !== '0.00%' ? this.getDealRate : undefined
            await this.buyInAuth(this.sentenceMarkData?.anchorInfo?.SecUid, currentData, () => {
                this.dealRate = !this.dealRate;
                this.$emit('click', this.dealRate)
            })
        }
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
.wordsBodyContentText2 {
    font-weight: 400;
    font-size: 12px;
}

.elBt {
    padding: 5px;
    margin-left: 6px;
}

.cancelIndiciaBtn {
    border: none !important;
    background: transparent;
    color: #2E3742 !important;
}

.indiciaBtn {
    border: none !important;
    background: transparent;
    color: var(--color-main) !important;
}
</style>