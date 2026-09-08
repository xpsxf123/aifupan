<template>
    <div :class="{'search-container': true,'has-history-search':!!actionType}">
        <div class="header">
            <div class="header-tips text-sm flex items-center" v-if="config.type==='hotItem'">
                <span>哪些关键词搜索量大？</span>
                <div style="color: var(--color-main)" class="flex items-center cursor-pointer" @click="openUrl">
                    <span>点我了解</span>
                    <img src="@/assets/imgs/rightgif.gif" class="arrow" alt="">
                </div>
            </div>
            <div class="header-title">
                <div class="h1">{{ config.searchTitle }}</div>
                <div class="resources">
                    <span>{{ config.searchResource }}：</span>
                    <span style="color: var(--color-main)">
                            {{
                            config.type === 'expert' ? `${userProperty?.searchInfluencerNum || 0} 个` : `${userProperty?.searchHotVideoNum || 0} 次`
                        }}
                        </span>
                </div>
                <div class="h3">{{ config.searchSubtitle }}</div>
            </div>
            <div class="header-input flex items-center justify-center">
                <el-autocomplete
                    class="input-with-select"
                    popper-class="suggestions-list"
                    v-model="searchValue"
                    :fetch-suggestions="querySearch"
                    :placeholder="config.searchPlaceholder"
                >
                    <template slot-scope="{ item }">
                        <div>{{ item.label }}</div>
                    </template>
                </el-autocomplete>
                <el-button type="primary" class="search" :disabled="loading" @click="searchAction">搜索
                </el-button>
                <div class="history-icon" v-if="actionType!=='history'">
                    <i class="el-icon-time"></i>
                    <span @click="getHistory">历史记录</span>
                </div>
            </div>
            <div class="header-input-sub flex items-center justify-between" v-if="config.type==='hotItem'">
                <div class="left" @click="openUrlToDouYin">打开抖音</div>
                <div class="right">没有您想要的结果？请多搜索几次</div>
            </div>

            <div class="history" v-if="actionType==='history'">
                <i class="el-icon-arrow-left"></i>
                <span @click="goBack">返回</span>
                <span class="text-xs">仅展示近7天的搜索记录</span>
            </div>
        </div>
        <el-card class="search-content brs-10" :body-style="{ height: '100%' }" :style="getMargin" v-if="!!actionType">
            <slot></slot>
        </el-card>
    </div>
</template>

<script>
import {uniqBy} from 'lodash'
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";

export default {
    mixins: [userAssets],
    props: {
        config: {
            type: Object,
            default: () => {
                return {}
            }
        },
        loading: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            searchValue: '',
            actionType: '',
            restaurants: []
        };
    },
    computed: {
        getMargin() {
            return this.config.type === 'hotItem' || this.actionType === 'history' ? {margin: '0 10px 10px 10px'} : {margin: '15px 10px 10px 10px'}
        }
    },
    watch: {
        'config.type': {
            handler(val) {
                if (!val) return
                this.getHistoryList(val)
            },
            deep: true,
            immediate: true
        },
        loading: {
            handler(val) {
                if (!val) this.getHistoryList()
            },
            deep: true,
        }
    },
    methods: {
        searchAction() {
            if (!this.searchValue) {
                if (this.config.type === "expert") {
                    return this.$message.warning('请输入抖音号进行搜索')
                }
                return this.$message.warning('请输入搜索关键字')
            }

            if (this.config.pattern) {
                if (!this.config.pattern.test(this.searchValue)) {
                    this.searchValue = ''
                    return this.$message.error('抖音号格式不正确，请重新输入')
                }
            }
            this.actionType = 'search'
            this.$emit('getAction', {action: 'search', searchValue: this.searchValue})
        },
        getHistory() {
            this.searchValue = ''
            this.actionType = 'history'
            this.$emit('getAction', {action: 'history', searchValue: this.searchValue})
        },
        goBack() {
            this.actionType = ""
            this.searchValue = ""
            this.$emit('getAction', {action: '', searchValue: ''})
        },
        async getHistoryList() {
            try {
                const httpServer = this.config.type === 'hotItem' ? this.$httpBack.shortVideo.hotHistory : this.$httpBack.shortVideo.expertHistory
                const serverResult = await httpServer({page: 1, limit: 100})
                if (serverResult.code !== 0) return
                serverResult.data?.list?.forEach(_item => {
                    _item.label = _item.keyword || _item.nickname
                    _item.value = _item.keyword || _item.platformAccount
                })
                this.restaurants = uniqBy(serverResult.data?.list || [], 'label')
            } catch (e) {
            }
        },
        querySearch(queryString, cb) {
            const restaurants = this.restaurants;
            const results = queryString ? restaurants.filter(this.createFilter(queryString)) : restaurants;
            // 调用 callback 返回建议列表的数据
            cb(results);
        },
        createFilter(queryString) {
            return (restaurant) => {
                return restaurant.value.toLowerCase().indexOf(queryString.toLowerCase()) !== -1;
            };
        },
        openUrlToDouYin() {
            window.open('https://www.douyin.com', '_blank')
            // this.$httpClient.system.openUrl({url: 'https://www.douyin.com'});
        },
        openUrl() {
            this.$httpClient.system.openUrl({url: 'https://aidso.douchacha.com/?dso_from=aifupan'});
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
.suggestions-list {
    li {
        > div {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
    }
}

.search-container {
    background: url("~@/assets/imgs/search_back.png") no-repeat;
    background-size: cover;
    background-position-y: top;
    position: absolute;
    top: 0;
    right: 0;
    left: 0;
    bottom: 0;
    display: flex;
    min-height: 254px;
    flex-direction: column;

    .header {
        z-index: 10;
        position: relative;
        margin-top: 70px;

        .header-tips {
            position: absolute;
            top: -48px;
            left: 12px;

            .arrow {
                max-width: 24px;
                transform: rotate(180deg);
                margin-top: -2px
            }
        }

        .header-title {
            padding-top: 10px;
            text-align: center;
            width: 50vw;
            margin: 0 auto;
            position: relative;

            .resources {
                position: absolute;
                font-size: 13px;
                padding: 6px 20px;
                bottom: 0;
            }

            .h1 {
                font-size: 32px;
                font-weight: bold;
            }

            .h3 {
                padding-block: 12px;
                font-size: 14px;
            }
        }

        .header-input {
            margin: 0 auto;
            width: 50vw;
            position: relative;
            box-shadow: 0px 0px 11px 0px rgba(0, 119, 255, 0.25);
            border-radius: 24px;

            .input-with-select {
                width: 100%;
                height: 50px;
                border-radius: 12px;

                ::v-deep(.el-input) {
                    background: linear-gradient(138deg, #09cfff, #942dfe);
                    padding: 1px;
                    border-radius: 24px;
                }

                ::v-deep(.el-input__inner) {
                    height: 50px;
                    border-radius: 24px;
                    border: none;
                    padding-right: 100px;
                    white-space: nowrap !important;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    word-break: break-all;
                }
            }

            .search {
                border-radius: 24px;
                height: 44px;
                padding: 6px 0;
                width: 88px;
                z-index: 1;
                position: absolute;
                right: 4px;
                top: 4px;
            }

            .history-icon {
                color: #7A7C80;
                font-size: 14px;
                position: absolute;
                z-index: 10;
                right: -95px;

                > span {
                    padding: 0 4px;
                    cursor: pointer;
                }
            }
        }

        .header-input-sub {
            margin: 0 auto;
            width: 50vw;
            padding: 6px 20px;

            .left {
                color: var(--color-main);
                font-size: 12px;
                cursor: pointer;
            }

            .right {
                color: #ABAEB3;
                font-size: 12px;
            }
        }

        .history {
            text-align: left;
            padding: 12px 20px;
            color: #7A7C80;
            font-size: 14px;
            position: relative;
            z-index: 10;

            > span {
                padding: 0 4px;
                cursor: pointer;
            }
        }
    }

    &::after {
        content: "";
        position: absolute;
        top: 254px;
        left: 12px;
        right: 12px;
        bottom: 16vh;
        z-index: 0;
        background: url("~@/assets/imgs/expert.png") no-repeat;
        background-size: auto;
        background-position-x: center;
        background-position-y: bottom;
        border-radius: 12px;
    }

    .search-content {
        height: 100%;
        flex: 1;
        overflow: auto;
        z-index: 1;

        ::v-deep(.el-card__body) {
            padding: 0;
        }
    }
}

.has-history-search {
    &::after {
        background: none;
    }
}
</style>