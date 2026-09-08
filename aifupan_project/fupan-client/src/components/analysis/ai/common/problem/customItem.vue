<template>
    <div class="flex-jc-sb pd-8 w100 brs-8 cs-p" :class="{'b-1-c1':flodModel,'problem-item':!flodModel}" :style="{backgroundColor: flodModel?'#fff':'#F4EDFF'}" @click="onClick">
        <div :style="{width: flodModel?'calc(100% - 16px)':'100%'}">
            <h4 class="mg-0 font-s12 text-color3" :class="{'problem-item-content':flodModel,'flod-problem-item-content':!flodModel}" style="font-weight: normal;">
                <span class="flex-ji-c text-center ding">
                    <span style="line-height: 18px;display: inline-block;" :style="{color:'#fff'}">定</span>
                </span>
                <span class="font-s14 text-color1" style="font-weight: bolder;padding-left: 5px">
                    <span v-if="flodModel">{{ item.content }}</span>
                    <span v-else>{{ item.outline || item.content }}</span>
                </span>
            </h4>
        </div>
        <div class="flex-ji-c" v-if="flodModel">
            <i class="font_family icon-daohanglanjiantou16px-2"></i>
        </div>
    </div>
</template>

<script>

import aiTypeMixin from "@/mixins/aiTypeMixin";

export default {
    components: {},
    mixins: [aiTypeMixin],
    props:{
        item: {
            type: Object,
            default: () => {
                return {}
            }
        },
        flod: {
            type: Boolean,
            default: false
        },
        type: {
            type: String,
            default: ''
        },
        index: {
            type: Number,
            default: -1
        }
    },
    data() {
        return {

        };
    },
    computed: {
        flodModel(){
            return this.flod||this.isViolation
        }
    },
    watch: {},
    methods: {
        onClick() {
            this.$emit('click', {
                data: this.item,
                index: this.index
            })
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
.problem-item {
    border: 1px #F4EDFF solid;

    &:hover {
        border: 1px #9976FF solid;
    }
}
.ding{
    display: inline-block;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    vertical-align: top;
    background: #9976FF;
    font-size: 12px;
}
.problem-item-content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    text-overflow: ellipsis;
    height: 38px;
}

.flod-problem-item-content {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>