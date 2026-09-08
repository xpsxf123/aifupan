<template>
    <div class="authorize">
        <div class="authorize-header">
            <div class="warning-text">检测到以下账号巨量百应授权过期，会导致无法同步数据以进行AI数据诊断</div>
        </div>
        <el-table :data="getList" class="authorize-table" max-height="300">
            <el-table-column prop="AnchorName" label="账号" align="center"></el-table-column>
            <el-table-column prop="juliangAuthStatus" label="状态" align="center">
                <template slot-scope="{row}">
                    <div class="flex items-center justify-center">
                        <div style="color: var(--color-main);cursor: pointer" @click="()=>handleAuthorize(row)">
                            点我授权
                        </div>
                        <div :class="buyInClass(row.juliangAuthStatus||0)" style="padding: 0 6px">
                            （{{ textRender(row.juliangAuthStatus) }}）
                        </div>
                    </div>
                </template>
            </el-table-column>
        </el-table>
    </div>
</template>

<script>
import buyIn from "@/mixins/buyIn";

export default {
    components: {},
    mixins: [buyIn],
    props: {
        unAuthorizedList: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return {
            buyInStatus: new Map([
                [0, '点我授权'],
                [1, '已授权'],
                [2, '授权过期'],
                [3, '授权失败'],
                [4, '授权中'],
                [5, '授权账号不匹配'],
                [6, '子账号无权限']
            ])
        };
    },
    computed: {
        textRender() {
            return (status) => {
                return this.buyInStatus.get(status)
            }
        },
        buyInClass() {
            return (buyIn) => {
                if (buyIn === 0) {
                    return 'buyIn-default'
                } else if (buyIn === 1) {
                    return 'buyIn-success'
                } else if (buyIn === 2) {
                    return 'buyIn-time'
                } else if (buyIn === 3) {
                    return 'buyIn-error'
                } else if (buyIn === 4) {
                    return 'buyInIng'
                } else if (buyIn === 5) {
                    return 'buyIn-error'
                }
            }
        },
        getList() {
            return this.unAuthorizedList.filter(item => item.AccountType === 0 && [2, 3, 5, 6].includes(item.juliangAuthStatus))
        }
    },
    watch: {},
    methods: {
        async handleAuthorize(row) {
            await this.buyInFront(row.SecUid)
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
.authorize {
    border-radius: 8px;

    .warning-text {
        font-size: 14px;
        color: #333;
        padding: 8px 20px;
    }

    &-table {
        width: 100%;
        background-color: #fff;
        border-radius: 4px;
    }

    &-btn {
        color: #4785B8;
    }

    .buyIn-default {
        color: #5BB1FF;
    }

    .buyIn-success {
        color: #61B593;
    }

    .buyIn-error {
        color: #f60808;
    }

    .buyInIng {
        color: #F4D05A;
    }

    .buyIn-time {
        color: #f60808;
    }
}
</style>