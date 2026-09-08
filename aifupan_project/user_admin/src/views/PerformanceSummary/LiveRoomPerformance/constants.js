export const searchConfig = {
  items: [
    {
      type: 'input',
      prop: 'name',
      placeholder: '请输入名称',
      label: '',
      width: '300px'
    }
  ]
}

export const tableColumns = [
  {
    label: '排行',
    prop: 'rank',
    width: 80,
    slotName: 'rank',
    align: 'center'
  },
  {
    label: '直播间',
    prop: 'name',
    minWidth: 200,
    slotName: 'liveRoom'
  },
  {
    label: '场观',
    prop: 'viewCount',
    sortable: true,
    minWidth: 100
  },
  {
    label: '销售额',
    prop: 'salesRevenue',
    sortable: true,
    minWidth: 120
  },
  {
    label: '退款',
    prop: 'refund',
    sortable: true,
    minWidth: 100
  },
  {
    label: '净销售额',
    prop: 'netSales',
    sortable: true,
    minWidth: 120
  },
  {
    label: '投放',
    prop: 'investment',
    sortable: true,
    minWidth: 100
  },
  {
    label: 'ROI',
    prop: 'roi',
    sortable: true,
    minWidth: 80
  }
]
