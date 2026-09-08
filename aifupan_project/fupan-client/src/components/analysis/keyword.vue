<template>
    <div class="wordsItemContainer"  :class="{'flex-jc-sb':isColumn}">
        <div class="wordsItemColorContainer">
            <template>
                <div class="wordsItemColor" style="background: paleturquoise;"></div>
                <div class="wordsItemText">
                    <span>{{isColumn ? '关键词':'运营关键词'}}</span>
                </div>
            </template>
            <el-popover  v-if="!isColumn" placement="bottom-start" width="400" trigger="hover" :close-delay="30" popper-class="myPopover">
                <div>
                    直播的节奏和话术框架是整场直播的<span style="color: red;font-weight: bold;">
                        骨架</span>，而骨架是由各种关键的<span style="color: red;font-weight: bold;">骨节</span>组成的。</br>
                    关键词就是直播框架的骨节，设置好适合自己直播间相匹配的关键词，能快速的定位到直播过程中最<span
                        style="color: red;font-weight: bold;">关键的话术结构</span>有哪些，密度如何，例如什么时候拉粉拉团，拉粉团的钩子是怎么样的，拉粉团的密度是怎么样的、塑造人设和塑品密度如何。</br>
                    直播框架比较好的直播间，直播流程会很顺畅，可以兼顾好直播当中各阶段的基础数据，从而获得更大的推流。</br>
                    优秀的运营操盘手或者主播，都会使用<span style="color: red;font-weight: bold;">关键词</span>来定位整场<span
                        style="color: red;font-weight: bold;">话术框架</span>，从而提高复盘效率，找到<span
                        style="color: red;font-weight: bold;">竟品的核心要点</span>。</br>
                    当一个直播间的流量突然上升和下降的时候，极有可能是直播话术结构产生了变化，可以重点看各种关键词的<span
                        style="color: red;font-weight: bold;">时间点和密度</span>。
                </div>
                <div slot="reference" style="display: flex; align-items: center;">
                    <img src="@/assets/imgs/help.png" style="width: 16px;" />
                </div>
            </el-popover>
            <!-- <template v-if="isColumn">
                <div class="wordsItemColor mg-l2" style="background: paleturquoise;"></div>
                <div class="wordsItemText">
                    <span>关键词</span>
                </div>
            </template> -->
            <div class="wordsItemText">
                <span>：{{ count }}次</span>
            </div>
        </div>
        <div v-if="!ai">
            <Toggle @change="markClick" v-model="$attrs.value" @input="(v) => $emit('input', v)" :default="defaultState"
                active="点击标注" inactive='取消标注' :readonly="readonly"></Toggle>
        </div>
    </div>
</template>

<script>
import Toggle from './toggle.vue'
export default {
    name: "",
    props: {
        count: {
            type: [Number, String],
            default: 0
        },
        defaultState: {
            type: Boolean,
            default: false
        },
        isColumn: {
            type: Boolean,
            default: false
        },
        ai: {
            type: Boolean,
            default: false
        },
        readonly: {
            type: Boolean,
            default: false
        }
    },
    components: {
        Toggle
    },
    computed: {

    },
    data() {
        return {
            btState: false
        };
    },
    mounted() {

    },
    created() {

    },
    methods: {
        markClick(val) {
            this.$emit('markers', 'crux', val)
            this.$emit('markClick', val)
        }
    }

};
</script>

<style scoped>
.wordsItemContainer {
    display: flex;
    align-items: center;
    justify-items: center;
    padding: 0 12px;
}

.wordsItemColorContainer {
    display: flex;
    align-items: center;
    justify-items: center;
}

.wordsItemColor {
    width: 24px;
    height: 16px;
    border-radius: 4px;
}

.wordsItemText {
    font-weight: 500;
    font-size: 12px;
    color: #2E3742;
    margin-left: 4px;
}
</style>