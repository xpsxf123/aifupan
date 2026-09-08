<template>
    <div class="text-data">
        <div class="flows" v-for="(item,key) in liveData.flow" :key="item.title">
            <div class="header-title">
                <span> {{ item?.title }}</span>
                <span class="after" v-if="!isShipinhao">（看播用户/成交用户）</span>
            </div>
            <div class="child-data" style="display: block;padding-left: 24px" v-if="isShipinhao">
                <template v-if="userFlowPortrait[0]?.childData?.length">
                    <div v-for="(_item,_index) in userFlowPortrait" :key="_item.title" class="flex items-start"
                         :style="{marginTop:_index===1?'24px':''}">
                        <div class="title whitespace-nowrap">
                            {{ _item.label }}：
                        </div>
                        <div class="flex items-center flex-wrap">
                            <div v-for="(__item,__index) in _item?.childData" :key="__item.title" class="child"
                                 :style="{paddingLeft:__index===0?'0':'18px'}">
                                <div class="title">
                                    {{ __item?.channelName }}
                                </div>
                                <div class="value">
                                    <span>{{ numberData(__item?.ratio) }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
                <div v-else
                     style="text-align: center;width: 100%;padding-top: 20px">
                    暂无数据
                </div>
            </div>
            <div class="child-data" v-else>
                <template v-if="item?.childData?.length">
                    <div v-for="_item in item?.childData" :key="_item.title" class="child">
                        <div class="title">
                            {{ _item?.channelName }}
                        </div>
                        <div class="value">
                            <span>{{ numberData(_item?.value1) }}</span>
                            <span class="after">丨</span>
                            <span style="color: #909499">{{ numberData(_item?.value2) }}</span>
                        </div>
                    </div>
                </template>
                <div v-else
                     style="text-align: center;width: 100%;padding-top: 20px">
                    暂无数据
                </div>
            </div>
        </div>
    </div>
</template>

<script>

import {isEmpty, isNumber} from 'lodash'
import common from './common'

export default {
    mixins: [common],
    components: {},
    props: {
        resData: {
            type: Object,
            default: () => {
                return {}
            }
        },
        span: {
            type: Number,
            default: 12
        },
        sentenceMarkData: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            userFlowPortrait: [],//视频号才有此字段
            liveData: {}
        }
    },
    computed: {},
    watch: {
        resData: {
            handler(newVal, oldVal) {
                if (!isEmpty(newVal)) {
                    this.userFlowPortrait = [
                        {
                            label: '看播用户',
                            childData: JSON.parse(newVal?.watchFlowList || '[]') || []
                        },
                        {
                            label: '成交用户',
                            childData: JSON.parse(newVal?.payFlowList || '[]') || []
                        }
                    ]
                    this.assemblyData(newVal)
                }
            },
            immediate: true,
        },
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
.text-data {
    .flows {
        width: 100%;
    }

    .child-data {
        flex-wrap: wrap
    }
}
</style>
