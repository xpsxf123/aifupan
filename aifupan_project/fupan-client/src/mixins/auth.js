/**
 * 权限认证混入
 * 提供权限验证相关的数据和方法
 * @mixin
 */
export default {
    /**
     * 组件数据
     * @returns {Object} 包含权限信息的数据对象
     */
    data(){
        return {
            /** @type {Object} 权限信息对象 */
            authInfo: {}
        }
    },
    computed:{
        /**
         * 获取当前用户是否有权限
         * @returns {boolean} 是否有权限
         */
        isSlefAuth(){
            const { videoInfo, uploadFile, fileInfo } = this.sentenceMarkData || this.textData || {};
            return this.getSlefAuth(videoInfo || uploadFile || fileInfo || this.authInfo)
        },
        /**
         * 获取当前用户权限ID数组
         * @returns {Array<string>} 权限ID数组
         */
        selfIds(){
            const { videoInfo, uploadFile, fileInfo } = this.sentenceMarkData || this.textData || {};
            return this.getSelfIds(videoInfo || uploadFile || fileInfo || this.authInfo)
        },
        /**
         * 获取当前用户ID
         * @returns {string} 用户权限ID
         */
        selfUId(){
            const { videoInfo, uploadFile, fileInfo } = this.sentenceMarkData || this.textData || {};
            return this.getSelfUId(videoInfo || uploadFile || fileInfo || this.authInfo)
        },
        /**
         * 获取当前租户ID
         * @returns {string} 租户权限ID
         */
        selfTId(){
            const { videoInfo, uploadFile, fileInfo } = this.sentenceMarkData || this.textData || {};
            return this.getSelfTId(videoInfo || uploadFile || fileInfo || this.authInfo)
        }
    },
    methods:{
        /**
         * 从数据对象中提取租户ID和用户ID
         * @param {Object} data - 包含用户和租户信息的数据对象
         * @param {string|number} [data.TenantId] - 租户ID（大写）
         * @param {string|number} [data.UserId] - 用户ID（大写）
         * @param {string|number} [data.tenantId] - 租户ID（小写）
         * @param {string|number} [data.userId] - 用户ID（小写）
         * @returns {Object} 包含tenantId和userId的对象
         */
        getDataIds(data){
            const { TenantId, UserId, tenantId, userId } = data || {};
            let resolvedTenantId = tenantId || TenantId
            let resolvedUserId = userId || UserId
            const sourceKind = String(this.$route?.query?.sourceKind || '')
            const path = String(this.$route?.path || '')
            const isUploadSource = sourceKind === 'uploadFile'
                || path.startsWith('/uploadVideo')
                || path.startsWith('/uploadText')
                || path.startsWith('/uploadSlice')
                || path.startsWith('/uploadShort')
                || (!!data?.fileId && !data?.VideoId && !data?.videoId)
            if (isUploadSource && (!resolvedTenantId || !resolvedUserId)) {
                const storeUser = this.$store?.getters?.getUserInfo || {}
                if (!resolvedTenantId) resolvedTenantId = storeUser.activeTenantId
                if (!resolvedUserId) resolvedUserId = storeUser.id
            }
            return {
                tenantId: resolvedTenantId,
                userId: resolvedUserId
            }
        },
        /**
         * 生成权限验证ID数组
         * @param {Object} data - 包含用户和租户信息的数据对象
         * @returns {Array<string>} 权限ID数组，格式为["userId_userType", "tenantId_userType"]
         */
        getAuthIds(data){
            const { tenantId, userId } = this.getDataIds(data);
            return [`${userId}_${this.$store.getters.getUserType}`,`${tenantId}_${this.$store.getters.getUserType}`]
        },
        /**
         * 验证当前用户是否有权限
         * @param {Object} data - 包含用户和租户信息的数据对象
         * @returns {boolean} 是否有权限
         */
        getSlefAuth(data){
            return this.$auth(this.getAuthIds(data));
        },
        /**
         * 获取权限ID数组
         * @param {Object} data - 包含用户和租户信息的数据对象
         * @returns {Array<string>} 权限ID数组
         */
        getSelfIds(data){
            return this.getAuthIds(data)
        },
        /**
         * 获取用户权限ID
         * @param {Object} data - 包含用户和租户信息的数据对象
         * @returns {string} 用户权限ID，格式为"userId_userType"
         */
        getSelfUId(data){
            return this.getAuthIds(data)?.[0]
        },
        /**
         * 获取租户权限ID
         * @param {Object} data - 包含用户和租户信息的数据对象
         * @returns {string} 租户权限ID，格式为"tenantId_userType"
         */
        getSelfTId(data){
            return this.getAuthIds(data)?.[1]
        }
    }
}
