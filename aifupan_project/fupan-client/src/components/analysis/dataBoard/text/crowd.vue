<template>
    <div class="text-data">
        <div class="flows" v-for="(item,key) in liveData.crowd" :key="item.title" :style="spanStyle">
            <div class="header-title">
                <span> {{ item?.title }}</span>
                <span class="after"
                      v-if="(key==='genderPortrait'&&isShipinhao)||!isShipinhao">（看播用户/成交用户）</span>
            </div>
            <div class="child-data" v-if="(key==='genderPortrait'&&isShipinhao)||!isShipinhao">
                <template v-if="item?.childData?.length">
                    <div v-for="_item in item?.childData" :key="_item.title" class="child">
                        <div class="title">
                            {{ _item?.label }}
                        </div>
                        <div class="value">
                            <span>{{ numberData(_item?.value1) }}</span>
                            <span class="after">丨</span>
                            <span style="color: #909499"> {{ numberData(_item?.value2) }}</span>
                        </div>
                    </div>
                </template>
                <div v-else
                     style="text-align: center;width: 100%;padding-top: 20px">
                    <div v-if="resData?.dataSourceType===1&&isDouYin">
                        成交数据过低，巨量百应-直播大屏-基础版没有提供人群画像数据
                    </div>
                    <div v-else>
                        暂无数据
                    </div>
                </div>
            </div>

            <!-- 视频号要单独处理人群画像的=>年龄分布-->
            <div class="child-data" style="display: block;padding-left: 24px" v-if="key==='agePortrait'&&isShipinhao">
                <template v-if="userPortrait[0]?.childData?.length||userPortrait[1]?.childData?.length">
                    <div v-for="(_item,_index) in userPortrait" :key="_item.title" class="flex items-start"
                         :style="{marginTop:_index===1?'24px':''}">
                        <div class="title whitespace-nowrap">
                            {{ _item.label }}：
                        </div>
                        <div class="flex items-center flex-wrap">
                            <div v-for="(__item,__index) in _item?.childData" :key="__item.title" class="child"
                                 :style="{paddingLeft:__index===0?'0':'18px'}">
                                <div class="title">
                                    {{ __item?.label }}
                                </div>
                                <div class="value">
                                    <span>{{ numberData(__item?.value) }}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
                <div v-else
                     style="text-align: center;width: 100%;padding-top: 20px">
                    <div v-if="resData?.dataSourceType===1&&isDouYin">
                        成交数据过低，巨量百应-直播大屏-基础版没有提供人群画像数据
                    </div>
                    <div v-else>
                        暂无数据
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>

import {isEmpty} from 'lodash'
import common from './common'

export default {
    mixins: [common],
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
            liveData: {},
            userPortrait: [],//视频号才有此字段
        }
    },
    computed: {},
    watch: {
        resData: {
            handler(newVal, oldVal) {
                if (!isEmpty(newVal)) {
                    this.userPortrait = [
                        {
                            label: '看播用户',
                            childData: JSON.parse(newVal?.watchUserPortrait || '{}')?.agePortrait || []
                        },
                        {
                            label: '成交用户',
                            childData: JSON.parse(newVal?.payUserPortrait || '{}')?.agePortrait || []
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
    display: block !important;

    .flows {
        &:last-child {
            margin-bottom: 40px;
        }
    }

    .child-data {
        flex-wrap: wrap
    }
}

</style>
