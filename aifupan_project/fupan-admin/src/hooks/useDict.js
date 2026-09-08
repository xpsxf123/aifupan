// useDict.js
import { reactive } from 'vue'

export const useDict = () => {
  const dicts = reactive({
    // 用户模块的字典
    userDict: {
      userType: [
        { value: 0, label: '普通用户' },
        { value: 1, label: '后台管理员' },
        { value: 2, label: '子账号' },
      ],
      sex: [
        { value: 1, label: '男' },
        { value: 2, label: '女' },
      ],
      anchorType: [
        { value: 0, label: '个人主播' },
        { value: 1, label: '公司' },
      ],
      loginLogType: [
        { value: 0, label: '登录' },
        { value: 1, label: '离线' },
        { value: 2, label: '上线' },
        { value: 3, label: '退出' },
      ],
    },

    // word 模块的字典
    wordDict: {
      cueType: [
        { value: 0, label: '运营提示词' },
        { value: 1, label: '违规提示词' },
        { value: 2, label: '弹幕提示词' },
        { value: 3, label: '数据截图助手' },
        { value: 4, label: '数据看板助手' },
        { value: 5, label: '重要弹幕提示词' },
        { value: 6, label: 'Ai话术提示词' },
        { value: 7, label: '自然原文提示词' },
        { value: 8, label: '优化原文提示词' },
      ],
      applyTo: [
        { value: 0, label: '单个分析' },
        { value: 1, label: '对比分析' },
      ],
      cueScope: [
        { value: 0, label: '全文' },
        { value: 1, label: '段落' },
      ],
      cueScene: [
        { value: 0, label: '直接提示' },
        { value: 1, label: '弹框操作' },
      ],
    },

    // 订单模块的字典
    orderDict: {
      timeUnit: [
        { value: 0, label: '小时', isDelete: 1 },
        { value: 1, label: '天', isDelete: 0 },
        { value: 2, label: '月' },
        { value: 3, label: '季度' },
        { value: 4, label: '半年' },
        { value: 5, label: '年' },
      ],
      orderStatus: [
        { value: 0, label: '未支付' },
        { value: 1, label: '未开始' },
        { value: 2, label: '生效中' },
        { value: 3, label: '已过期' },
        { value: 4, label: '已退款' },
        { value: 5, label: '已结束' },
        { value: 6, label: '超时未支付关闭' },
        { value: 7, label: '冻结' },
        { value: 8, label: '手动取消订单' },
      ],
      orderType: [
        { value: 0, label: '免费订单' },
        { value: 1, label: '升级订单' },
        { value: 2, label: '免费版换收费版' },
        { value: 3, label: '续费订单' },
        { value: 4, label: '增量包订单' },
        { value: 5, label: '版本活动订单' },
        { value: 6, label: '编辑订单' },
        { value: 7, label: '商品活动订单' },
      ],
      orderSource: [
        { value: '0', label: '客户端下单' },
        { value: '1', label: 'pc端下单' },
        { value: '2', label: '邀请码下单' },
        { value: '3', label: '活动下单' },
      ],
      orderPayStatus: [
        { value: 0, label: '未支付' },
        { value: 1, label: '已支付' },
        { value: 2, label: '支付失败' },
        { value: 3, label: '已退款' },
      ],
      payType: [
        { value: 0, label: '微信支付' },
        { value: 1, label: '支付宝支付' },
      ],
      commodityType: [
        { value: 0, label: '增量包' },
        { value: 1, label: '正常版本' },
        { value: 2, label: '活动版本' },
        { value: 3, label: '邀请活动订单' },
      ],
      refundStatus: [
        { value: 0, label: '未申请退款' },
        { value: 1, label: '申请退款中' },
        { value: 2, label: '退款成功' },
        { value: 3, label: '退款失败' },
      ],
      signs: [
        { value: 0, label: '减' },
        { value: 1, label: '加' },
      ],
    },
  })

  const getLabel = (dict, value) => {
    if (value === null || value === undefined) return ''
    if (!dict) return ''
    return dict.find((item) => item.value == value)?.label ?? ''
  }

  return {
    ...dicts,
    getLabel,
  }
}
