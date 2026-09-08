<template>
    <div class="wordsItemContainer flex-ai-c justify-between">
        <div class="wordsItemColorContainer flex-ai-c">
            <div class="wordsItemText wordsItemBox font-s12 text-color2">
                成交量：{{ deal ? getDeal || '-' : '-' }}
            </div>
        </div>
        <afp-button :plain="false" size="small" class="elBt" :class="deal ? 'cancelIndiciaBtn' : 'indiciaBtn'" @click="markClick"
                   >{{
                deal ? '隐藏成交量' : '点击分析'
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
            deal: true
        };
    },
    mounted() {

    },
    computed: {
        getDeal() {
            const {
                purchaseCountStart,
                purchaseCountEnd,
            } = this.sentenceMarkData
            if (purchaseCountStart === purchaseCountEnd) {
                return `${myUtils.fnw(purchaseCountStart, 2)}`
            } else {
                return `${myUtils.fnw(purchaseCountStart, 2)} ~ ${myUtils.fnw(purchaseCountEnd, 2)}`
            }
        },
    },
    created() {

    },
    methods: {
        async markClick() {
            const {
                purchaseCountStart,
                purchaseCountEnd,
            } = this.sentenceMarkData
            const resultData = myUtils.isGreaterThanZero(purchaseCountStart) || myUtils.isGreaterThanZero(purchaseCountEnd)
            await this.buyInAuth(this.sentenceMarkData?.anchorInfo?.SecUid, resultData ? [purchaseCountStart, purchaseCountEnd] : null, () => {
                this.deal = !this.deal
                this.$emit('click', this.deal)
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
    color: #2E3742 !important;
    background: transparent;
}

.indiciaBtn {
    border: none !important;
    color: var(--color-main) !important;
    background: transparent;
}
</style>