export default {
    data() {
        return {
        }
    },
    methods: {
        // 判断是否超过15天
        isDay15 (createTime) {
            const now = Date.now()
            let createMs = 0
            if (typeof createTime === 'number') {
                createMs = createTime
            } else if (typeof createTime === 'string') {
                const parsed = new Date(createTime).getTime()
                createMs = isNaN(parsed) ? new Date(createTime.replace(/-/g, '/')).getTime() : parsed
            } else if (createTime instanceof Date) {
                createMs = createTime.getTime()
            }
            if (!createMs || isNaN(createMs)) {
                return false
            }
            const diffDays = (now - createMs) / (1000 * 60 * 60 * 24)
            return diffDays > 15
        },
        hintDay15 (createTime) {
            if (this.isDay15(createTime)) {

                this.$cNotify.notify({
                    title: '提示',
                    message: '录制超过15天，加载较慢，请稍等!',
                    duration: 5000
                })
            }
        },
    }
}