<template>
    <el-image
        :src="validImage"
        class="video-thumbnail">
    </el-image>
</template>

<script>
export default {
    name: "ImageValidator",
    props: {
        urls: {
            type: Array,
            required: true
        }
    },
    data() {
        return {
            validImage: ''
        }
    },
    methods: {
        checkImage(url) {
            return new Promise((resolve) => {
                const img = new Image()
                img.onload = () => resolve(true)
                img.onerror = () => resolve(false)
                img.src = url
            })
        },
        async validateImages() {
            this.validImage = ''
            for (let url of this.urls) {
                const isValid = await this.checkImage(url)
                if (isValid) {
                    this.validImage = url
                    break
                }
            }
        }
    },
    watch: {
        urls: {
            handler() {
                this.validateImages()
            },
            deep: true,
            immediate: true
        }
    }
}
</script>

<style lang="scss" scoped>
.video-thumbnail {
    width: 100%;
    height: 100%;
}
</style>
