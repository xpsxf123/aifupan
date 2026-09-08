export default {
    data() {
        return {
            anchorSchedulePositionName: '',
            anchorScheduleEmployeeNames: '',
            anchorScheduleRequestKey: '',
        }
    },
    watch: {
        sentenceMarkData: {
            handler() {
                this.loadAnchorSchedulePlan()
            },
            deep: true,
        }
    },
    mounted() {
        this.loadAnchorSchedulePlan()
    },
    activated() {
        this.loadAnchorSchedulePlan()
    },
    methods: {
        clearAnchorScheduleInfo() {
            this.anchorSchedulePositionName = ''
            this.anchorScheduleEmployeeNames = ''
        },
        getAnchorScheduleParams() {
            if (this.isCompare) return null
            const sourceKind = String(this.$route?.query?.sourceKind || '')
            const path = String(this.$route?.path || '')
            const hasUploadFileData = !!(this.sentenceMarkData?.uploadFile?.fileId || this.sentenceMarkData?.fileInfo?.fileId)
            const isUploadSource = sourceKind === 'uploadFile'
                || hasUploadFileData
                || path.startsWith('/uploadVideo')
                || path.startsWith('/uploadText')
                || path.startsWith('/uploadSlice')
                || path.startsWith('/uploadShort')
            if (isUploadSource) return null
            if (!this.$store?.getters?.largeEnterprises) return null

            const videoInfo = this.sentenceMarkData?.videoInfo || {}
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}

            const livePlatformType = anchorInfo?.platform ?? anchorInfo?.Platform
            const videoId = videoInfo?.VideoId || videoInfo?.videoId || this.$route?.query?.videoId || this.$route?.query?.id
            const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId
            const startTime = videoInfo?.StartTime || videoInfo?.startTime || ''
            const endTime = videoInfo?.EndTime || videoInfo?.endTime || ''

            if (!videoId) return null

            return {
                livePlatformType,
                videoId,
                secUid,
                startTime,
                endTime
            }
        },
        normalizeSchedulePlanResponse(data) {
            if (!data) return []
            if (Array.isArray(data)) return data
            if (Array.isArray(data?.list)) return data.list
            if (Array.isArray(data?.records)) return data.records
            if (Array.isArray(data?.rows)) return data.rows
            return [data]
        },
        extractScheduleEmployeeNames(position) {
            const direct = position?.employeeNames || position?.employeeName
            if (direct) return String(direct)
            const employees = Array.isArray(position?.employees) ? position.employees : []
            return employees
                .map(employee => employee?.employeeName || employee?.name || employee?.nickName || employee?.employeeNickName)
                .filter(Boolean)
                .join('、')
        },
        setAnchorScheduleInfoByResponse(res) {
            if (res?.code !== 0) {
                this.clearAnchorScheduleInfo()
                return
            }
            
            const list = this.normalizeSchedulePlanResponse(res?.data)
            const schedulePositions = list
                .filter(Boolean)
                .map(item => (Array.isArray(item?.positions) ? item.positions : []))
                .flat()

            const hasAnchorPositionFlag = schedulePositions.some(item => item && Object.prototype.hasOwnProperty.call(item, 'anchorPosition'))
            const anchorPosition = schedulePositions.find(item => item?.anchorPosition === true)
                || (!hasAnchorPositionFlag ? schedulePositions.find(item => String(item?.positionName || '') === '主播') : null)
            if (!anchorPosition) {
                this.clearAnchorScheduleInfo()
                return
            }

            const positionName = anchorPosition?.positionName || ''
            const employeeNames = this.extractScheduleEmployeeNames(anchorPosition)
            if (positionName && employeeNames) {
                this.anchorSchedulePositionName = positionName
                this.anchorScheduleEmployeeNames = employeeNames
                return
            }

            this.clearAnchorScheduleInfo()
        },
        async loadAnchorSchedulePlan() {
            const params = this.getAnchorScheduleParams()
            if (!params) {
                this.clearAnchorScheduleInfo()
                return
            }

            const requestKey = JSON.stringify(params)
            if (requestKey === this.anchorScheduleRequestKey) return
            this.anchorScheduleRequestKey = requestKey
            
            try {
                const request = this.$httpBack2?.liveRoom?.plan
                if (typeof request !== 'function') {
                    this.clearAnchorScheduleInfo()
                    return
                }
                let res = await request(params)
                this.setAnchorScheduleInfoByResponse(res)
            } catch (e) {
                this.clearAnchorScheduleInfo()
            }
        }
    }
}
