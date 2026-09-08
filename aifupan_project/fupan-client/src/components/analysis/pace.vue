<template>
    <div class="wordsItemContainer">
        <div class="wordsItemColorContainer">
            <div class="wordsItemText wordsItemBox font-s12"><span v-if="!isColumn" class="font-s12">语速：</span>{{ analysisChar ?
                parseInt((charConut / videoTime)* 60):'**' }}字/分钟</div>
        </div>
        <Toggle @change="markClick" :default="analysisChar" :active="isColumn?'语速分析':'点击分析'" inactive='隐藏分析'></Toggle>
    </div>
</template>

<script>
import Toggle from './toggle.vue'
export default {
    name: "",
    props: {
        charConut: {
            type: [Number, String],
            default: 0
        },
        videoTime: {
            type: [Number, String],
            default: 0
        },
        isColumn: {
            type: Boolean,
            default: false
        },
    },
    components: {
        Toggle
    },
    computed: {

    },
    data() {
        return {
            analysisChar: true
        };
    },
    mounted() {

    },
    created() {

    },
    methods: {
        markClick() {
            this.analysisChar = !this.analysisChar
            this.$emit('click', this.analysisChar)
        }
    }

};
</script>

<style scoped>
.wordsItemBox {
    text-align: right;
}

.wordsItemContainer {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.wordsItemColorContainer {
    display: flex;
    align-items: center;
}

.wordsItemText {
    font-weight: 500;
    font-size: 13px;
    color: #2E3742;
}
</style>