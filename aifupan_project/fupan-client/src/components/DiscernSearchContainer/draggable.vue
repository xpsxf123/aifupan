<template>
    <div>
        <draggable v-model="sprites" :options="{ draggable: '.sprite' }" @start="onDragStart"
            @end="onDragEnd">
            <div v-for="(sprite, index) in sprites" :key="index" class="sprite main-bg pd-4 brs-10" 
                :class="[collapseStatus?'brs-10 b-1-cTheme':'brs-0',sprite.className]"
                :style="{ left: sprite.left + 'px', top: sprite.top + 'px' }" >
                <div class="sprite-content-box flex-ai-c h100 w100" :style="{width: getW+'px', minHeight: getH+'px'}">
                    <slot :collapse="collapse" :status="collapseStatus" :unCollapse="unCollapse"></slot>
                </div>
            </div>
        </draggable>
    </div>
</template>

<script>
import draggable from 'vuedraggable';
import resize from '@/mixins/resize.js';
export default {
    components: {
        draggable
    },
    props: {
        width: {
            type: Number,
            default: 400
        },
        height:{
            type: Number,
            default: 160
        },
        collapseW:{
            type: Number,
            default: 110
        },
        collapseH:{
            type: Number,
            default: 80
        },
    },
    mixins: [],
    data() {
        return {
            sprites: [
                { left: document.body.clientWidth*0.5 - this.width/2, top: (document.body.clientHeight * 0.5) - this.height/2 }
            ],
            collapseStatus: false
        };
    },
    computed: {
        getW(){
            if(this.collapseStatus){
                return this.collapseW
            }else{
                return this.width
            }
        },
        getH(){
            if(this.collapseStatus){
                return this.collapseH
            }else{
                return this.height
            }
        }
    },
    methods: {
        onDragStart(e) {
            // 记录元素初始位置
            this.startX = e.originalEvent.clientX - e.item.getBoundingClientRect().left
            this.startY = e.originalEvent.clientY - e.item.getBoundingClientRect().top
        },
        onDragEnd(e) {
            // 拖拽结束时更新精灵的位置
            const { newIndex, originalEvent } = e;
            // this.sprites[newIndex].left = originalEvent.x - this.startX;
            // this.sprites[newIndex].top = originalEvent.y - this.startY;
            const sprite = this.sprites[newIndex];
            // 计算新位置
            let newLeft = originalEvent.x - this.startX;
            let newTop = originalEvent.y - this.startY;
            // 获取屏幕尺寸
            const wl = document.body.clientWidth;
            const wh = document.body.clientHeight;
            // 假设你知道精灵的宽度和高度（这里用spriteWidth和spriteHeight表示）
            const spriteWidth = this.getW; // 如果没有宽度属性，使用默认值
            const spriteHeight = this.getH; // 如果没有高度属性，使用默认值xs
            // 边界检查 - 确保精灵不会超出屏幕
            newLeft = Math.max(0, Math.min(newLeft, wl - spriteWidth));
            newTop = Math.max(0, Math.min(newTop, wh - spriteHeight));
            // 更新位置
            sprite.left = newLeft;
            sprite.top = newTop;
        },
        collapse(){
            this.collapseStatus = true
            this.sprites[0].left += this.width/2;
            this.sprites[0].top += this.height/2;
        },
        unCollapse(){
            this.collapseStatus = false
            this.sprites[0].left -= this.width/2;
            this.sprites[0].top -= this.height/2;
        },
        init(){

        }
    },
    mounted() {
    }
};
</script>

<style lang="scss" scoped>
.sprite{
    position: fixed;
    cursor: pointer;
    z-index: 1888;
    box-shadow: 2px 2px 10px rgba(0, 0, 0, 0.3);
}
</style>