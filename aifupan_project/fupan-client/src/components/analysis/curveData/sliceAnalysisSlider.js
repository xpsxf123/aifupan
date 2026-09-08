export default {
    data() {
        return {
            currentSliceIndex: -1,//切片位置
            currentSliceVisible: false,
            hideTimer: null,
        }
    },
    methods: {
        setCurrentSliceAnalysisPosition(left, index, ref, key) {
            this.currentSliceVisible = true
            this.setCurrentToolPosition(left, index, ref, key)
        },
        leaveSliceAnalysis() {
            this.currentSliceVisible = false
            clearTimeout(this.hideTimer)
            this.hideTimer = setTimeout(() => {
                if (!this.currentSliceVisible) {
                    this.currentSliceIndex = -1
                }
            }, 300)
        },
        enterPopover() {
            this.currentSliceVisible = true
            clearTimeout(this.hideTimer)
        },
        leavePopover() {
            this.currentSliceVisible = false
            this.hideTimer = setTimeout(() => {
                if (!this.currentSliceVisible) {
                    this.currentSliceIndex = -1
                }
            }, 300)
        },
    }
}