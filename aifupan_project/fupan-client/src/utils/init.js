import store from '@/store'
import httpClient from '@/utils/request-api-client'

export async function initApp() {
    const isAiFuPan = navigator.userAgent?.indexOf('aifupan') >= 0;
    if (isAiFuPan) {
        try {
            const {data: result, code} = await httpClient.setup.getMachineCode()
            if (code === 0) {
                store.commit("setMachineCode", result);
            }
        } catch (e) {
            console.log(e)
        }
    }
}