<!--
/**
 * @description 已开通服务图标组（弹幕/数据看板/诊断/笔记/总结/标记/本地删除标识）。
 */
-->

<template>
    <div class="activatedService">
        <img v-if="item.existBarrage === 1" src="@/assets/imgs/dan.png" class="compereResourceTypeImg">
        <img v-if="item.existDataBoard === 1 && replayType !== 'replayShort'" src="@/assets/imgs/shu.png" class="compereResourceTypeImg">
        <img v-if="item.hasDiagnosisReport === 1" src="@/assets/imgs/zhen.png" class="compereResourceTypeImg">
        <img v-if="item.existsNotes === 1" src="@/assets/imgs/bi.png" class="compereResourceTypeImg">
        <img v-if="item.notesSummary === 1" src="@/assets/imgs/jie.png" class="compereResourceTypeImg">
        <img v-if="item.existsMark === 1" src="@/assets/imgs/zhu.png" class="compereResourceTypeImg">
        <img v-if="!!item.localVideoStatus" src="@/assets/imgs/del.png" class="compereResourceTypeImg">
    </div>
</template>

<script>
export default {
    props: {
        item: {
            type: Object,
            default: () => ({})
        },
        replayType: {
            type: String,
            default: ''
        }
    }
}
</script>

<style lang="scss" scoped>
.activatedService {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
}

.compereResourceTypeImg {
    width: 16px;
    height: 16px;
    margin: 0 1px;
}
</style>

