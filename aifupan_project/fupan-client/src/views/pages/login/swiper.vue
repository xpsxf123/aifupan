<template>
    <div class="flex-jc-sb flex-ai-c">
        <el-button type="text" icon="el-icon-caret-left pd-0 font-s30" @click="slidePrev"></el-button>
        <div class="swiper-container">
            <div v-show="swiperObj" class="swiper-wrapper">
                <div class="swiper-slide slide-content slh" v-for="(item, index) in slides" :key="index">
                    <div style="max-width: 100%;">
                        <img style="max-width: 100%; max-height: 110px;border-radius: 10px;" :src="item.url" alt="" srcset="">
                    </div>
                    <div style="height: 20px; width: 80%;margin: 0 auto;" class="font-s12 slh">{{ item.content }}</div>
                </div>
            </div>
            <!-- <div class="swiper-pagination"></div> -->
            <!-- <div class="swiper-button-prev"></div>
            <div class="swiper-button-next"></div> -->
        </div>
        <el-button type="text" icon="el-icon-caret-right  pd-0 font-s30" @click="slideNext"></el-button>
    </div>

</template>

<script>
import Swiper from 'swiper';
import 'swiper/css/swiper.min.css';
export default {
    components: {
        Swiper
    },
    props: {
        // 传入的 slides 数据
        slides: {
            type: Array,
            default: () => []
        },
        // 每组显示多少个 slide
        groupSize: {
            type: Number,
            default: 0
        }
    },
    data() {
        return {
            swiperObj: null,
            slidesPerView: 3, // 同时显示的 slide 数量
            spaceBetween: 30, // slide 之间的间距
            navigation: {
                nextEl: '.swiper-button-next',
                prevEl: '.swiper-button-prev',
            },
            pagination: {
                el: '.swiper-pagination',
                clickable: true
            },
            swiperInstance: null
        };
    },
    computed: {
    },
    mounted() {
        // this.$nextTick(() => {
        //     this.initSwiper();
        // })
    },
    methods: {
        setSlides(slides) {
            this.slides = slides;
            this.initSwiper(200);
        },
        onSwiper(swiper) {
            this.swiperInstance = swiper;
        },
        onSlideChange() {
            console.log('slide changed');
        },
        initSwiper(time) {
            setTimeout(() => {
                this.swiperObj = new Swiper('.swiper-container', {
                    spaceBetween: this.spaceBetween,
                    slidesPerView: this.groupSize || this.slidesPerView,
                    // 配置选项
                    // pagination: this.pagination,
                    // navigation: this.navigation,
                    lazy: true,
                    loop: this.slides?.length > 5,
                    autoplay: {
                        delay: 2500,
                        disableOnInteraction: false
                    },
                })
            }, time || 1000);
        },
        slidePrev() {
            if (this.swiperObj) {
                this.swiperObj.slidePrev();
            }
        },
        slideNext() {
            if (this.swiperObj) {
                this.swiperObj.slideNext();
            }
        }
    }
};
</script>

<style scoped lang="scss">
.swiper-container {
    position: relative;
    width: 100%;
    height: 130px;
    overflow: hidden;
}

.slide-content {
    height: 100%;
    text-align: center;
    // background: #ccc;
}

//   .swiper-button-prev,
//   .swiper-button-next {
//     color: #333;
//   }

//   .swiper-pagination {
//     position: absolute;
//     bottom: 10px;
//   }</style>