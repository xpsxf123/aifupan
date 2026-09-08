<template>
    <div class="popover-container">
        <div class="item">
            <div class="child" style="background: #FF85C0"></div>
            人次：{{ getNum(item.viewersNum) }}
        </div>
        <div class="item">
            <div class="child" style="background: #FF4D4F"></div>
            在线：{{ getNum(item.onlineMaxNum) }}
        </div>
        <div class="item">
            <div class="child" style="background: #FADB14"></div>
            <span>{{ dataView(item, ['volumeStart', 'volumeEnd']).indexOf('~')!==-1 ? '¥：' : '销售额：' }}</span>
            {{ dataView(item, ['volumeStart', 'volumeEnd'])}}
        </div>
    </div>
</template>

<script>
import myUtils from "@/utils/utils";

export default {
    components: {},
    props: {
        item: {
            type: Object,
            default: () => {
                return {};
            }
        }
    },
    data() {
        return {};
    },
    computed: {
        getNum() {
            return (value) => {
                return myUtils.fnw(value);
            };
        },
        dataView() {
            return (resData, keys, length) => {
                return myUtils.dataView(resData, keys, length)
            }
        },
    },
    watch: {},
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
.popover-container {
    .item {
        font-size: 12px;
        display: flex;
        align-items: center;
        line-height: 1.6;

        .child {
            flex-shrink: 0;
            height: 6px;
            width: 6px;
            border-radius: 50%;
            margin-inline: 5px;
        }
    }
}

</style>