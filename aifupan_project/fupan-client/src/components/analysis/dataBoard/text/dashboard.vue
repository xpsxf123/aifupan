<template>
    <div class="text-data">
        <div class="flows" v-for="(item,key) in liveData.dashboard" :key="item.title" :style="spanStyle">
            <div class="header-title flex items-center justify-start">
                {{ item?.title }}

                <div v-if="key==='popularityCount'" style="margin-left: 30px"
                     class="font-s12 flex-ji-c text-colorTheme cs-p" @click="toOfficialWebsite">
                    <img src="@/assets/imgs/rightgif.gif" style="max-width: 24px;" alt="" srcset="">
                    <span style="width: 160px;font-weight: 400">查看更多场次数据？点我查看</span>
                </div>
<!--                <span style="color: #7A7C80;font-weight: 500"></span>-->
                <div v-if="key==='salesCount'&&isEmptyType===8" class="status-8">
                    注意：本场数据可能稍有误差，下播后6到8小时会进行校准
                </div>
            </div>
            <div class="child-data">
                <template v-if="isTakeProductData(item,key)">
                    <div v-for="_item in item?.childData" :key="_item.title" class="child">
                        <div class="title">{{ _item?.label }}</div>
                        <div class="value">{{ _item?.value }}</div>
                    </div>
                </template>
                <div v-if="key==='salesCount'&&isEmptyType===1&&!item.isTakeProduct"
                     style="text-align: center;width: 100%;padding-top: 20px">
                    本场没有进行电商商品销售
                </div>
            </div>
        </div>
    </div>
</template>

<script>

import {isEmpty} from 'lodash'
import common from './common'

export default {
    components: {},
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
        sentenceMarkData:{
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            liveData: {}
        }
    },
    computed: {},
    watch: {
        resData: {
            handler(newVal, oldVal) {
                if (!isEmpty(newVal)) {
                    this.assemblyData(newVal)
                }
            },
            immediate: true,
        },
    },
    methods: {
        toOfficialWebsite() {
            if (this.$isWeb) {
                window.open('https://www.douchacha.com', "_blank");
            } else {
                this.$httpClient.system.openUrl({url: 'https://www.douchacha.com'});
            }
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
.status-8 {
    font-size: 12px;
    font-weight: 400;
    color: red;
    padding-left: 12px;
}
</style>
