const useGetRoleColor = () => {
  // 定义岗位对应的颜色
  const roleColorMap = {
    主播: '#444DFF',
    副播: '#008931',
    运营: '#9158DB',
    中控: '#9158DB',
    投手: '#008484',
    剪辑: '#DC3537',
    嘉宾: '#C04D84'
  }

  const getRoleColor = (roleName) => {
    return roleColorMap[roleName] || '#606266'
  }
  return { getRoleColor }
}

export default useGetRoleColor
