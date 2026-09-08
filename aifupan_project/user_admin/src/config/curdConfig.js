const config = {
  // 数据查询页面表格布局
  dataQueryLayout: [[['title'], ['add', 'option']], ['search'], ['table'], ['page']],
  searchTableLayout: [[['search'], ['option']], ['table'], ['page']],
  dataViewLayout: [['title'], [['search'], ['option']], ['table'], ['page']]
}

const dataQueryLayout = config.dataQueryLayout
export { dataQueryLayout }

const searchTableLayout = config.searchTableLayout
export { searchTableLayout }

const dataViewLayout = config.dataViewLayout
export { dataViewLayout }

export default config
