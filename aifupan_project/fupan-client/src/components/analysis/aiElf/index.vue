<template>
    <div>
        <draggable v-if="isTour" v-model="sprites" :options="{ draggable: '.sprite' }" @start="onDragStart"
            @end="onDragEnd">
            <div v-for="(sprite, index) in sprites" :key="index" class="sprite" :class="sprite.className"
                 :style="{ left: sprite.left + 'px', top: sprite.top + 'px' }" >
                <div class="sprite-content-box flex-ai-c" @mouseenter="toBorderShow">
                    
                    <div class="h100" v-if="!isToBorder" @mouseleave="toBorderHide">
                        <div class="shaking-element aiElf-box aiElf5 font-s14">
                            <span class="icon-text icon-text5" @click="elfClick('multiDiagnose')">
                                <span class="aiElfTag">AI</span>
                                <span class="aiElfLabel">诊断多场</span>
                            </span>
                        </div>
                        <div v-if="showCurrentVideoAgentEntry" class="shaking-element aiElf-box aiElf6 font-s14">
                            <span class="icon-text icon-text6" @click="elfClick('currentDiagnoseNew')">
                                <span class="aiElfTag">AI</span>
                                <span class="aiElfLabel">诊断本场（新）</span>
                            </span>
                        </div>
                        <div class="shaking-element aiElf-box aiElf2 font-s14">
                            <span class="icon-text icon-text2" @click="elfClick('assistant')">
                                <span class="aiElfTag">AI</span>
                                <span class="aiElfLabel">诊断本场（旧）</span>
                            </span>
                        </div>
                        <div v-if="!isCompare" class="shaking-element aiElf-box aiElf1 font-s14">
                            <span class="icon-text icon-text1" @click="elfClick('violation')">
                                <span class="aiElfTag">AI</span>
                                <span class="aiElfLabel">查违规</span>
                            </span>
                        </div>
                        <div v-if="!isCompare && isVideo && isScrolling && !isSliceShortVideo" class="shaking-element aiElf-box aiElf3 font-s14">
                            <span class="icon-text icon-text3" @click="elfClick('scrolling')">
                                <span class="aiElfTag">AI</span>
                                <span class="aiElfLabel">分析弹幕</span>
                            </span>
                        </div>
                        <!-- <div v-if="!isCompare && !isSliceShortVideo" class="shaking-element aiElf-box aiElf4 font-s14">
                            <span class="icon-text icon-text4" @click="elfClick('textAssistant')">
                                <span class="aiElfTag">AI</span>
                                <span class="aiElfLabel">提取话术</span>
                            </span>
                        </div> -->
                    </div>
                    <img src="@/assets/imgs/aiElf.png" class="toBorder-img" v-if="isToBorder" @mouseenter="toBorderShow" alt="">
                    <div class="btn-box">
                        <el-button type="text" class="font-s26" icon="el-icon-caret-right"
                            @click="toBorder"></el-button>
                    </div>
                </div>
                <!-- <div class="aiElf-bts main-bg pd-4">
                    <span class="aiElf-bt cs-p font-s12"><span class="aiElf-bt-text">运营助手</span></span>
                    <span class="aiElf-bt cs-p font-s12"><span class="aiElf-bt-text">违规助手</span></span>
                </div> -->
            </div>
        </draggable>

        <aiTour ref="aiTour" @hide="tourHide"></aiTour>
    </div>
</template>

<script>
import draggable from 'vuedraggable';
import aiTour from './aiTour.vue';
import resize from '@/mixins/resize.js';
export default {
    components: {
        draggable,
        aiTour
    },
    props: {
        isCompare: {
            type: Boolean,
            default: false
        },
        type: {
            type: String,
            default: 'video'
        },
        isScrolling: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: ()=>{
                return {}
            }
        }
    },
    mixins: [resize],
    data() {
        return {
            sprites: [
                { left: document.body.clientWidth - 50, top: (document.body.clientHeight * 0.7) - 50 }
            ],
            iconW: 134,
            iconH: 236,
            isTour: false,
            startX: '',
            startY: '',
            isToBorder: false,
        };
    },
    computed: {
        isVideo() {
            return this.type === 'video'
        },
        isSliceShortVideo() {
            const {videoInfo, uploadFile} = this.sentenceMarkData
            const {videoSliceType} = videoInfo || {}
            const {fileSliceType} = uploadFile || {}
            return [videoSliceType, fileSliceType].includes(2)
        },
        showCurrentVideoAgentEntry() {
            const secUid = String(this.sentenceMarkData?.anchorInfo?.SecUid || this.sentenceMarkData?.anchorInfo?.secUid || '').trim()
            const videoId = String(this.sentenceMarkData?.videoInfo?.VideoId || this.sentenceMarkData?.videoInfo?.videoId || '').trim()
            return !this.isCompare && !!(secUid && videoId)
        }
    },
    methods: {
        runAiTour() {
            // localStorage.setItem('isAiTour','');
            if (localStorage.getItem('isAiTour')) {
                this.isTour = true;
                this.runAIElf();
            } else {
                this.$nextTick(() => {
                    this.$refs.aiTour.show();
                })
            }
        },
        tourHide() {
            localStorage.setItem('isAiTour', '1');
            this.isTour = true;
            this.runAIElf();
        },
        runAIElf() {
            this.$nextTick(() => {
                const w = document.body.clientWidth;
                const h = document.body.clientHeight;
                this.sprites[0].left = w - (this.iconW + 50);
                this.sprites[0].top = (h * 0.7) - (this.iconH - 50);
                this.ifBorder();
            });
        },
        elfClick(type) {
            this.$emit('click', type);
        },
        onDragStart(e) {
            // 记录元素初始位置
            this.startX = e.originalEvent.clientX - e.item.getBoundingClientRect().left
            this.startY = e.originalEvent.clientY - e.item.getBoundingClientRect().top
        },
        onDragEnd(e) {
            // 拖拽结束时更新精灵的位置
            const { newIndex, originalEvent } = e;
            this.sprites[newIndex].left = originalEvent.x - this.startX;
            this.sprites[newIndex].top = originalEvent.y - this.startY;

            this.ifBorder();
        },
        toBorder() {
            this.sprites[0].className = 'toBorder';
            this.isToBorder = true
            this.sprites[0].left = document.body.clientWidth - this.iconW - 16;
        },
        toBorderShow() {
            this.sprites[0].className = '';
            this.isToBorder = false
        },
        toBorderHide() {
            this.ifBorder()
        },
        ifBorder() {
            const wl = document.body.clientWidth - this.iconW;
            const sl = this.sprites[0].left + 20;
            if (wl <= sl) {
                this.sprites[0].left = wl - 20;
                this.sprites[0].className = 'toBorder';
                this.isToBorder = true
            } else {
                this.sprites[0].className = '';
                this.isToBorder = false
            }
        }
    },
    mounted() {
        this.runAiTour();
        this.$nextTick(() => {
            this.addResizeFns('runAIElf', () => {
                this.runAIElf()
            }, 10)
        })
    }
};
</script>

<style lang="scss" scoped>
.sprite {
    position: fixed;
    //width: 134px;
    //height: 194px;
    // background-color: #f00; /* 这里可以替换为精灵的背景图片 */
    cursor: pointer;
    z-index: 1888;

    .aiElf-box {
        background-image: none;
        height: 34px;
        width: 126px;
        margin: 6px 0;
        border-radius: 18px;
        display: flex;
        align-items: center;
        padding: 0 12px;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12);
        transition: transform .2s ease, box-shadow .2s ease;
    }

    .aiElf1 {
        background-color: #8B5CF6;
    }

    .aiElf2 {
        background-color: #3B82F6;
    }

    .aiElf3 {
        background-color: #22C55E;
    }

    .aiElf4 {
        background-color: #F59E0B;
    }

    .aiElf5 {
        background-color: #F59E0B;
    }

    .aiElf6 {
        background-color: #14B8A6;
    }

    .aiElf {
        &1:hover {
            box-shadow: 0 6px 14px rgba(139, 92, 246, 0.35);
            transform: translateX(-2px);
        }

        &2:hover {
            box-shadow: 0 6px 14px rgba(59, 130, 246, 0.35);
            transform: translateX(-2px);
        }

        &3:hover {
            box-shadow: 0 6px 14px rgba(34, 197, 94, 0.35);
            transform: translateX(-2px);
        }

        &4:hover {
            box-shadow: 0 6px 14px rgba(245, 158, 11, 0.35);
            transform: translateX(-2px);
        }

        &5:hover {
            box-shadow: 0 6px 14px rgba(245, 158, 11, 0.35);
            transform: translateX(-2px);
        }

        &6:hover {
            box-shadow: 0 6px 14px rgba(20, 184, 166, 0.35);
            transform: translateX(-2px);
        }
    }

    .icon-text {
        display: flex;
        align-items: center;
        height: 100%;
        width: 100%;
        color: #fff;
        user-select: none;
        &1 {
            color: #fff;
        }

        &2 {
            color: #fff;
        }

        &3 {
            color: #fff;
        }

        &4 {
            color: #fff;
        }

        &5 {
            color: #fff;
        }

        &6 {
            color: #fff;
        }

        //&1:hover {
        //    background: #DE286F;
        //    color: #fff;
        //}
        //
        //&2:hover {
        //    background: var(--color-main);
        //    color: #fff;
        //}
        //
        //&3:hover {
        //    background: #D73232;
        //    color: #fff;
        //}
        //
        //&4:hover {
        //    background: #E4770F;
        //    color: #fff;
        //}
    }

    .aiElfTag {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 18px;
        padding: 0;
        border-radius: 0;
        font-size: 16px;
        font-weight: 700;
        background: transparent;
        margin-right: 4px;
    }

    .aiElfLabel {
        font-size: 12px;
        font-weight: 400;
        letter-spacing: 0;
        white-space: nowrap;
    }

    .aiElf-bt-text {

        // text-fill-color: transparent;
        &:hover {
            background-image: linear-gradient(90deg, var(--color-main), #B947FF);
            -webkit-text-fill-color: transparent;
            -webkit-background-clip: text;
            background-clip: text;
        }
    }

    .aiElf-bts {
        width: 72px;
        background: #FFFFFF;
        box-shadow: 0px 1px 3px 0px rgba(173, 173, 173, 0.1), 0px 5px 5px 0px rgba(173, 173, 173, 0.09), 0px 11px 7px 0px rgba(173, 173, 173, 0.05), 0px 20px 8px 0px rgba(173, 173, 173, 0.01), 0px 31px 9px 0px rgba(173, 173, 173, 0);
        border-radius: 2px 2px 2px 2px;
        border: 1px solid #E6E6E6;
        padding: -5px 0;
        position: absolute;
        bottom: calc(100% - 10px);
        left: 20px;
        opacity: 0;
        transition: all 0.3s;
        overflow: hidden;

        .aiElf-bt {
            display: block;
            text-align: center;
            padding: 4px 0;
            transition: all 0.3s;

            &:hover {
                background: linear-gradient(90deg, #F5F2FF, #E8F7FF);
            }
        }
    }

    &:hover .aiElf-bts {
        opacity: 1;
        bottom: calc(100% + 5px);
    }
}

.sprite-content-box {
    .btn-box {
        display: none;
    }

    &:hover {
        .btn-box {
            display: block;
        }
    }
}

.ai-draggable-box {}

.toBorder {
    transition: all 0.3s;
    transform: translateX(95px);

    &:hover {
        transform: translateX(-10px);
    }
}
</style>
