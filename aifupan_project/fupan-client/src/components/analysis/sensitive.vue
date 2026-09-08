<template>
    <div class="wordsItemContainer" :class="{'flex-jc-sb':isColumn}">
        <div class="wordsItemColorContainer">
            <template>
                <div class="wordsItemColor" style="background: pink;"></div>
                <div class="wordsItemText">
<!--                    <span>{{isColumn ? '敏感词':'智能敏感词'}}</span>-->
                    <span>敏感词</span>
                </div>
            </template>
            <el-popover  v-if="!isColumn"  placement="bottom-start" width="400" trigger="hover" :close-delay="30" popper-class="myPopover">
                <div>
                    任何平台的审核机制都是先机器预审核，再人工审核。敏感词即容易被选入机审的词，敏感词被判罚后，即为违禁词；机器是精准匹配加上一定的智能化，如果敏感词本身违规级别较高、<span
                        style="color: red;font-weight: bold;">被频繁举报</span>或者<span
                        style="color: red;font-weight: bold;">风控严格</span>的时候，机审会直接判罚。</br>
                    人工审核主要是语义审核，会结合前后的语境、直播间类目、直播间流量层级、是否有商业行为和风控严格程度来进行判断是否判罚。</br>
                    敏感词是否判罚，和敏感词违规级别和出现密度、是否被频繁举报及直播间流量层级有关，因此<span
                        style="color: red;font-weight: bold;">密度低和流量层级低</span>的时候，敏感词没有被判罚，不代表流量高和被举报的时候不会被判罚。而且机器审核的时候，敏感词密度高的话，或被频繁举报，如果平台人力不足、即使没有判罚通知，也会存在<span
                        style="color: red;font-weight: bold;">隐性限流、无法破流量层级</span>的情况。</br>
                    平台的风控是波动性的、会随着运营要求和政策等因素，时松时紧，因此敏感词是否违禁，需要动态的去看。但是，养成良好的合规习惯，对于账号的长期稳定运营有重要意义，因为如果直播间多次命中敏感词，即使结合前后语境没有被判罚，也有可能触发<span
                        style="color: red;font-weight: bold;">人工盯审机制</span>，从而可能暴露直播间其他方面的问题！
                </div>
                <div slot="reference" style="display: flex; align-items: center;">
                    <img src="@/assets/imgs/help.png" style="width: 16px;" />
                </div>
            </el-popover>
            <!-- <template v-if="isColumn">
                <div class="wordsItemColor mg-l2" style="background: pink;"></div>
                <div class="wordsItemText">
                    <span>敏感词</span>
                </div>
            </template> -->
            <div class="wordsItemText">
                <span>：{{ count }}次</span>
            </div>
        </div>
        <div v-if="!ai">
            <Toggle @change="markClick" :customStyle="{background: '#fff'}" v-model="$attrs.value" @input="(v) => $emit('input', v)" :default="defaultState"
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
        };
    },
    mounted() {

    },
    created() {

    },
    methods: {
        markClick(val) {
            this.$emit('markers', 'sensitive', val)
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