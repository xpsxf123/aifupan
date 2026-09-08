export default {
    data() {
        return {
            pollingTimeout: null,
        }
    },
    computed: {
        allItemsCompleted() {
            const list = this.tableConfig?.tableData || []
            return list.every(item => [0, 3, 4].includes(item.extractStatus))
        },
    },
    methods: {
        stopPolling() {
            if (this.pollingTimeout) {
                clearTimeout(this.pollingTimeout)
                this.pollingTimeout = null
            }
        },
        scheduleNextPoll(time, callBack) {
            this.stopPolling()
            if (!this.allItemsCompleted) {
                this.pollingTimeout = setTimeout(() => {
                    callBack?.()
                }, time || 1000)
            }
        },
    },
    beforeDestroy() {
        this.stopPolling()
    },
}


