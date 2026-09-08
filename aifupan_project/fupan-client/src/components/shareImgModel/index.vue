<template>
    <div ref="imgWindowBox" class="img-window-box overflow_auto_y pd-8" style="background-color:  #e2e2e2;">
        <div style="min-height: 100%;margin: 0 auto;background-color:  #EEF0F6;max-width: 794px;width: 100%;box-sizing: border-box;" :style="width ? {width: width} : {}" ref="aiShareImgBox">
            <slot name="title">
                <div class="pd-t40 pd-b40 share-title">
                    <slot :show="isShowPdfHead" name="title-before"></slot>
                    <div v-if="isShowPdfHead" class="flex-ji-c">
                        <img class="share-title-img" style="max-width: 20px;" src="@/assets/imgs/new_logoT.png" alt="">
                        <span class="mg-l6">{{ getTitle }}</span>
                    </div>
                    <slot :show="isShowPdfHead" name="title-after"></slot>
                </div>
            </slot>
            <div v-if="refresh" ref="watermarkBox" class="ai-share-watermark-box">
                <div ref="watermark" :style="{'backgroundImage': `url(${watermarkUrl})`}" class="ai-share-watermark"></div>
                <div ref="ai-share-watermark-content" style="min-height: 400px; position: relative; z-index: 1;">
                    <slot></slot>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import myUtils from '@/utils/utils';
import env from '/src/env';
import aiFupanSY from '@/assets/imgs/aifpanSY.png';
export default {
    components: {},
    props:{
        watermarkImg: {
            type: String,
            default: ''
        },
        titleFn:{
            type: Function,
            default: null
        },
        sponsorship:{
            type: [String,Array],
            default: 'Deepseek'
        },
        title: {
            type: String,
            default: ''
        },
        width:{
            type: String,
            default: ''
        },
        showTitle: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            refresh: true,
            isDev: env.dev,
            aiFupanSY,
            aiFupanSYBase64: '',
            showPdfHead: 1
        };
    },
    computed: {
        getUserInfo(){
            return this.$store.getters.getUserInfo;
        },
        getHtml(){
            return this.$refs.aiShareImgBox;
        },
        getWatermarkStyle(){
            let styleObj =this.watermarkImg ? {
               'backgroundImage': `url(${this.watermarkImg})`
            }: {};
            return myUtils.objectFommatEmpty(styleObj);
        },
        watermarkUrl(){
            return this.watermarkImg || this.aiFupanSYBase64 || this.aiFupanSY;
        },
        getTitle(){
            let s = this.sponsorship;
            let ts = Array.isArray(s)? s.join('+') : s;
            return this.title || `以下内容由爱复盘${ts?`+${ts}`: ''}+${this.getUserInfo.nickName}联合提供`
        },
        isShowPdfHead(){
            return this.showPdfHead !== 0;
        },
    },
    watch: {},
    methods: {
        async toBase64(url){
            try{
                const res = await fetch(url);
                const blob = await res.blob();
                return await new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = () => resolve(reader.result);
                    reader.onerror = reject;
                    reader.readAsDataURL(blob);
                });
            }catch(e){ return ''; }
        },
        async ensureDefaultWatermarkBase64(){
            if (this.watermarkImg) return;
            if (this.aiFupanSYBase64) return;
            const b64 = await this.toBase64(this.aiFupanSY);
            if (b64) this.aiFupanSYBase64 = b64;
        },
        async fetchTenantConfig(){
            try{
                const res = await this.$httpBack?.tenant?.tenantConfig?.();
                const v = res?.data?.showPdfHead;
                if (v === 0 || v === '0' || v === 1 || v === '1') {
                    this.showPdfHead = Number(v);
                }
            }catch(e){
            }
        },
        addWatermark(callBack) {
            this.refresh = false
            this.$nextTick(() => {
                this.refresh = true;
                this.$nextTick(() => {
                    this.$refs.imgWindowBox.scrollTo({top: 0});
                    let el = this.$refs.watermarkBox;
                    let watermark = this.$refs.watermark;
                    watermark.style.height = this.isDev? 0 : el.scrollHeight + 'px';
                    callBack?.()
                })
            });
        },
        getShareHtml() {
            return new Promise(resolve => {
                this.addWatermark(() => {
                    resolve(this.$refs.aiShareImgBox)
                })
            })
        },
    },
    created() {
        
    },
    mounted() {
        Promise.all([
            this.fetchTenantConfig(),
            this.ensureDefaultWatermarkBase64()
        ]).then(() => {
            this.addWatermark();
        });
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.img-window-box {
    height: 100%;
    position: relative;
}


.ai-share-watermark-box {
    position: relative;
}

.ai-share-watermark {
    position: absolute;
    width: 100%;
    z-index: 0;
    left: 0;
    top: 0;
    height: 100% !important;
    pointer-events: none;
    background-color: transparent;
    // background-image: url('~@/assets/imgs/aifpanSY.png');
    // opacity: 0.5;
    background-size: 50%;
    background-repeat: repeat;
    background-position: 0 0;
    /* 起始位置 */
    /* 计算平铺间距 */
    /* 图片宽度 + 水平间距 */
    // background-size: calc(200px + 20px) auto;
    // background-size: 100px 100px; /* 图片显示尺寸 */
    // background-position: 0 0, 400px 0, 800px 0; /* 创建间距 */
    // background-color: rgba(0,0,0,0.05);
}
</style>
