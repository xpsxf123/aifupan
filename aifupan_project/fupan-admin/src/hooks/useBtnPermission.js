// hooks/usePermission.js
import { useUserInfoStore } from '@/store'

export const useBtnPermission = () => {
    const userInfoStore = useUserInfoStore()
    const menuList = userInfoStore.loginResultData.menuTreeList
    console.log('menuList', menuList)

    // 递归查找当前页面的 metaList
    const findMetaList = (menuItems, currentPath) => {
        for (const item of menuItems) {
            if (item.url === currentPath) {
                return item.metaList
            }
            if (item.children?.length) {
                const result = findMetaList(item.children, currentPath)
                if (result) return result
            }
        }
        return null
    }

    const checkPermission = (permissionName, currentPath) => {
        if (!permissionName && !currentPath) {
            console.warn('权限名称不能为空')
            return false
        }
        const metaList = findMetaList(menuList, currentPath)
        console.log('metaList', metaList)

        const a = metaList?.some((meta) => meta.name === permissionName) ?? false
        console.log('权限控制', a)
        return a
    }

    return {
        checkPermission
    }
}
