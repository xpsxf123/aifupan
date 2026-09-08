/**
 * @description: 清洗数据
 * @param {Array} schedulesData 直播间排班数据
 * @param {Array} listData 直播录制列表数据
 * @return {Array} 清洗后的数据
*/
export default function (schedulesData, listData) {
    schedulesData?.forEach(scheduleItem => {
        listData?.forEach(listItem => {
            if (listItem.secUid === scheduleItem.secUid) {
                listItem.schedulesData = scheduleItem
            }
        })
    })
}
