// composables/useTable.js
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export function useMixinTable(getDataList, paramsRecord = true) {
    if (typeof getDataList !== 'function') {
        throw new Error('useTable 需要传入 getDataList 函数')
    }

    const route = useRoute()
    const router = useRouter()

    const form = ref({
        page: 1,
        limit: 10
    })
    const total = ref(0)

    // 设置 url 参数
    const setUrl = () => {
        const query = {...route.query, ...form.value}
        router.replace({query}).catch((err) => {
            if (err.name !== 'NavigationDuplicated') {
                console.error(err)
            }
        })
    }

    // 初始化 url 参数
    const setUrlParams = () => {
        if (!paramsRecord) return
        const params = route.query
        for (const key in params) {
            let value = params[key]
            if (value !== null && value !== undefined) {
                if (key === 'page' || key === 'limit') {
                    value = Number(value)
                }
                form.value[key] = value
            }
        }
    }

    // 修改每页数量
    const toSize = (val) => {
        form.value.limit = val
        form.value.page = 1
        getDataList()
    }

    // 修改当前页
    const toPage = (val) => {
        form.value.page = val
        getDataList()
    }

    onMounted(() => {
        setUrlParams()
    })

    return {
        form,
        total,
        setUrl,
        setUrlParams,
        toSize,
        toPage
    }
}
