<template>
    <div class="wordsBodyContentText2">
        <span class="text-colorc2" style="display: flex;align-items: center">
            弹幕数量：{{ countNum }}
                 <img v-if="countNum>0 && !hideIcon" src="@/assets/imgs/dan.png" alt=""
                      class="dan-icon"
                      @click="()=>viewBullet(true)">
        </span>
        <div v-if="bullet" class="content_container" v-dialogDrag>
            <div class="content_bullet">
                <div class="move-header">
                    <i class="icon el-icon-circle-close" @click="()=>viewBullet(false)"></i>
                    <div>{{ toformatTime(item.startTime) }}</div>
                </div>
                <div class="content" v-if="bulletList.length">
                    <scrolling v-for="(_item, index) in bulletList" :key="index" :item="_item" :isShowTime="true"
                               :deductionTime="getInitStartTime">
                    </scrolling>
                </div>
                <img src="@/assets/imgs/chartEmpty.png" alt="" v-else class="image-style"/>
            </div>
        </div>
    </div>
</template>

<script>

import scrolling from "@/components/analysis/scrolling/index.vue";
import myUtils from '/src/utils/utils';

export default {
    components: {scrolling},
    props: {
        countNum: {
            type: [String, Number],
            default: 0
        },
        hideIcon: {
            type: Boolean,
            default: false
        },
        item: {
            type: Object,
            default: () => {
            }
        },
        textData: {
            type: Object,
            default: () => {
            }
        }
    },
    data() {
        return {
            bullet: false,
            bulletList: []
        };
    },
    computed: {
        getInitStartTime() {
            return (new Date(this.textData?.videoInfo?.StartTime)).getTime()
        },
        toformatTime() {
            return (val) => {
                return myUtils.toformatTime(val);
            }
        },
    },
    watch: {},
    methods: {
        async viewBullet(status) {
            if (status) {
                const {data: result, code} = await this.$httpBack.v2100.queryDanMuData({
                    limit: 9999,
                    page: 1,
                    queryType: 2,
                    startTime: this.item.startTimeSecond * 1000,
                    endTime: this.item.endTimeSecond * 1000,
                    videoId: this.item.videoId,
                    batchNumber: this.item.batchNumber,
                    recordDate: this.item.startTimeSecond * 1000
                }, {load: false})
                if (code !== 0) return
                this.bulletList = result?.list
            }
            this.bullet = status
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
.wordsBodyContentText2 {
    font-weight: 400;
    font-size: 12px;
    position: relative;
    
    .dan-icon {
        margin-left: 3px;
        width: 14px;
        height: 14px;
        cursor: pointer;
    }

    .content_bullet {
        width: 320px;
        height: 183px;
        position: absolute;
        background: #fff;
        top: 20px;
        left: 20px;
        z-index: 2000;
        box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.5);
        border-radius: 8px;

        .icon {
            font-size: 18px;
            cursor: pointer;
        }

        .content {
            overflow-x: hidden;
            overflow-y: scroll;
            height: 150px;
            padding: 0 6px
        }

        .move-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 6px;
            box-shadow: 0 2px 4px -2px rgba(0, 0, 0, 0.2);
        }

        .image-style {
            display: block;
            width: 130px;
            margin: 10px auto;
        }
    }
}


</style>
