export default {
    computed: { },
    methods: {
        viewVideo(item, type) {
            this.$router.push({
                path: this.$route.path + '/analysisReadonly/' + type,
                query: {id: item.videoId, analysisStatus: item.analysisStatus}
            })
        },
        // viewOnlyVideo(item, type) {
        //     this.$router.push({
        //         path: this.$route.path + '/analysisReadonly/' + type,
        //         query: {id: item.videoId, analysisStatus: this.isShowViewVideo(item)?item.analysisStatus}
        //     })
        // },
        ifFreeAndNotTime(row) {
            return this.$store.getters.isFree && row.analysisStatus === 3;
        },
        // 免费版/激活版   智能分析时长不足时，提取时间充足  按钮是：查看话术脚本 ，状态是：提取完成
        // 免费版/激活版   智能分析时长不足时，提取时间也不足  按钮是：查看视频，状态是：提取失败
        // 免费版 智能分析充足，按钮：查看整场分析，状态：分析完成
        // 其他版本 都是 【查看整场分析】按钮，但是在分析状态显示，分析余额不足
        //useAnalysisPropertyType  0：智能分析时长 1：文案提取
        isShowScript(row) {
            return row.useAnalysisPropertyType === 1 && row.analysisStatus === 2
        },
        isShowViewAnalysis(row) {
            return row.useAnalysisPropertyType === 0 && row.analysisStatus === 2
        },
        isShowViewVideo(row) {
            return row.analysisStatus === 3
        },
        isAnalysisFail(row) {
            return row.analysisStatus === 4
        },

        editFileName(item, callback,targetType) {
            const {userType, id: currentUserId} = this.$store?.state?.userInfo;
            if(userType!==0 && item?.userId !== currentUserId) return this.$message.error('暂无权限')

            const _this = this;
            _this.$prompt('', '修改文件名', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                customClass:'edit-file-name',
                inputPlaceholder:'限制15字以内',
                inputPattern:/^.{0,15}$/,
                inputErrorMessage: '限制15字以内',
                showClose:false,
                closeOnClickModal:false,
                closeOnPressEscape:false,
                center:true
            }).then(({ value }) => {
                let httpSever = null
                let params={}
                if(!(value.trim())) return
                if(targetType === 'online'){
                    httpSever = _this.$httpBack.video.updateCloudRename;
                    params = item.fileId ? {fileId: item.fileId, newFileName: value} : {
                        videoId: item.videoId,
                        cloudRename: value
                    }
                }else {
                    httpSever = item.fileId ? _this.$httpClient.video.renameFile : _this.$httpClient.video.reNameVideo;
                    params = item.fileId ? {fileId: item.fileId, newFileName: value} : {
                        videoId: item.videoId,
                        newVideoName: value
                    }
                }
                httpSever?.(params).then((res)=>{
                    if(res.code===0){
                        _this.$message.success('名称修改成功')
                        callback?.()
                    }else {
                        _this.$message.error(res.msg || '名称修改失败')
                    }
                }).catch(()=>{
                })
            }).catch(() => {

            });
        }
    }
}