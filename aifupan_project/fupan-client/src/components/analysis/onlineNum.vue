<template>
    <div class="onlineNumContainer not-select-text not-word">
        <div v-if="item.onlinePeopleObj" class="text-colorc3 not-select-text">{{ labelText }}{{ item.onlinePeopleObj?.number }}</div>
        <div v-if="item.onlinePeopleObj" class="onlineNumDifferenceContainer">
            <div v-if="item.onlinePeopleObj.difference > 0"
                style="color: #FC4F52;display: flex;align-items: center;">
                <!-- <span v-if="isAi">+</span>
                <img v-else src="@/assets/imgs/plus.png"
                    style="width: 8px;height: 8px;margin-right: 1px"> -->
                <span class="not-select-text">比上段：+{{ item.onlinePeopleObj.difference }}</span>
                <img class="online-num-img" src="@/assets/imgs/up.png" style="width: 16px;height: 16px;">
            </div>
            <div v-else-if="item.onlinePeopleObj.difference < 0"
                style="color: #28BD6C;display: flex;align-items: center;">
                <!-- <span v-if="isAi">-</span>
                <img v-else src="@/assets/imgs/minus.png"
                    style="width: 8px;height: 8px;margin-right: 1px"> -->
                <span class="not-select-text">比上段：-{{ Math.abs(item.onlinePeopleObj.difference) }}</span>
                <img class="online-num-img" src="@/assets/imgs/down.png" style="width: 16px;height: 16px;">
            </div>
            <div v-else style="display: flex">
                <img class="online-num-img" src="@/assets/imgs/flat.png" style="width: 8px;height: 8px;">
            </div>
        </div>
    </div>
</template>

<script>

export default {
    components: {},
    props:{
        item: {
            type: Object,
            default: () => { return {} }
        },
        isAi: {
            type: Boolean,
            default: false
        },
        label: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
        
        };
    },
    computed: {
        labelText() {
            if (this.label) return this.label
            return this.isAi ? '本段在线人数：' : '在线人数：'
        }
    },
    watch: {},
    methods: {
        
    },
    created() {
        
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.onlineNumDifferenceContainer {
    margin-left: 7px;
    font-size: 12px;
}

.onlineNumContainer {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #4D4D4D;
}
</style>
