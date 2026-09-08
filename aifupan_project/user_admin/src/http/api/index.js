/**
 * @file index.js
 * @description HTTP API 模块聚合导出
 */
import user from './user'
import role from './role'
import position from './position'
import employee from './employee'
import subCompany from './subCompany'
import dept from './dept'
import team from './team'
import org from './org'
import liveRoom from './liveRoom'
import trade from './trade'
import performanceSummary from './performanceSummary'
import employeeSchedule from './employeeSchedule'
import roomSchedule from './roomSchedule'
import productPerformance from './productPerformance'
import liveRoomPerformance from './liveRoomPerformance'
import employeeProfile from './employeeProfile'
import employeePerformance from './employeePerformance'

export default {
  user,
  role,
  position,
  employee,
  subCompany,
  dept,
  team,
  org,
  liveRoom,
  trade,
  performanceSummary,
  employeeSchedule,
  roomSchedule,
  productPerformance,
  liveRoomPerformance,
  employeeProfile,
  employeePerformance
}
