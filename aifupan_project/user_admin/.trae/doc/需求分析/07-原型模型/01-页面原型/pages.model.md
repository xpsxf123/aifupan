# 原型模型｜页面模型（code 数据）

```json
{
  "version": "v0",
  "pages": [
    {
      "id": "MySchedule",
      "title": "个人排班",
      "route": "/my/schedule",
      "permission": "schedule.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_header",
            "name": "Header",
            "layout": "vertical",
            "children": [
              {
                "type": "InfoCard",
                "props": {
                  "title": "个人信息",
                  "fields": [
                    { "key": "avatarUrl", "label": "头像", "type": "image", "source": "me.avatarUrl" },
                    { "key": "name", "label": "昵称/姓名", "type": "text", "source": "me.name" },
                    { "key": "position", "label": "岗位", "type": "text", "source": "me.positionName" },
                    { "key": "mobile", "label": "手机号", "type": "text", "source": "me.mobile" }
                  ]
                }
              },
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    {
                      "key": "datePreset",
                      "type": "segmented",
                      "options": ["今日", "明日", "昨日", "本周", "下周", "本月"]
                    }
                  ],
                  "actions": []
                }
              }
            ]
          }
        },
        {
          "type": "Region",
          "props": {
            "id": "r_content",
            "name": "Content",
            "layout": "vertical",
            "children": [
              {
                "type": "CalendarWeekGrid",
                "props": {
                  "hourStart": 0,
                  "hourEnd": 23,
                  "defaultHourWindow": [0, 12],
                  "dateRangeSource": "state.dateRange",
                  "itemsSource": "api:/api/schedule/my",
                  "interactions": [
                    { "event": "onItemClick", "action": "navigate:/schedule/live-rooms/{liveRoomId}?date={date}" }
                  ]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "MyPerformance",
      "title": "个人业绩",
      "route": "/my/performance",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_filters",
            "name": "Filters",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    {
                      "key": "datePreset",
                      "type": "segmented",
                      "options": ["今日", "昨日", "本周", "上周", "本月", "上月"]
                    }
                  ],
                  "actions": [
                    {
                      "label": "导出",
                      "permission": "performance.export",
                      "action": "api:/api/performance/employees/{me.employeeId}/export"
                    }
                  ]
                }
              }
            ]
          }
        },
        {
          "type": "Region",
          "props": {
            "id": "r_main",
            "name": "Main",
            "layout": "vertical",
            "children": [
              {
                "type": "MetricCards",
                "props": {
                  "metrics": [
                    { "key": "pv", "label": "场观" },
                    { "key": "sales", "label": "销售" },
                    { "key": "refund", "label": "退款" },
                    { "key": "netSales", "label": "净销售" },
                    { "key": "adCost", "label": "投放" },
                    { "key": "roi", "label": "ROI" }
                  ],
                  "datePreset": "state.datePreset"
                }
              },
              {
                "type": "Chart",
                "props": {
                  "chartType": "funnel",
                  "dataSource": "api:/api/performance/employees/{me.employeeId}/trend",
                  "metricSelector": {
                    "key": "metric",
                    "options": ["pv", "sales", "refund", "adCost", "conversionRate"]
                  },
                  "dateRangeSelector": { "key": "dateRange", "source": "state.dateRange" }
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/employees/{me.employeeId}/daily",
                  "pagination": true,
                  "columns": [
                    { "key": "date", "title": "日期" },
                    { "key": "pv", "title": "场观" },
                    { "key": "sales", "title": "销售" },
                    { "key": "refund", "title": "退款" },
                    { "key": "netSales", "title": "净销售" },
                    { "key": "adCost", "title": "投放" },
                    { "key": "roi", "title": "ROI" }
                  ],
                  "rowActions": []
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "MyProfile",
      "title": "个人信息",
      "route": "/my/profile",
      "permission": "profile.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_profile",
            "name": "Profile",
            "layout": "vertical",
            "children": [
              {
                "type": "FormModal",
                "props": {
                  "title": "个人信息",
                  "fields": [
                    { "key": "avatarUrl", "label": "头像", "type": "upload", "required": true },
                    { "key": "email", "label": "邮箱", "type": "input", "required": false, "validator": "email" }
                  ],
                  "submitAction": "api:/api/me",
                  "validators": ["email"]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "OrgManage",
      "title": "部门和人员",
      "route": "/org/manage",
      "permission": "org.manage",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_tabs",
            "name": "Tabs",
            "layout": "vertical",
            "children": [
              {
                "type": "Tabs",
                "props": {
                  "activeTab": "query.tab",
                  "tabs": [
                    { "id": "employees", "title": "人员", "contentPage": "OrgManageEmployees" },
                    { "id": "liveRooms", "title": "直播间", "contentPage": "OrgManageLiveRooms" },
                    { "id": "orgUnits", "title": "小组和部门", "contentPage": "OrgManageOrgUnits" },
                    { "id": "subsidiaries", "title": "子公司", "contentPage": "OrgManageSubsidiaries" },
                    { "id": "positions", "title": "岗位", "contentPage": "OrgManagePositions" }
                  ]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "LiveRoomScheduleList",
      "title": "直播间排班",
      "route": "/schedule/live-rooms",
      "permission": "schedule.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_filter_list",
            "name": "Filter+List",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    { "key": "platform", "type": "select" },
                    { "key": "keyword", "type": "search", "placeholder": "直播间名称/ID" },
                    { "key": "orgPath", "type": "orgCascade" }
                  ],
                  "actions": [
                    {
                      "label": "添加直播间",
                      "permission": "liveRoom.create",
                      "action": "navigate:/org/manage?tab=liveRooms"
                    }
                  ]
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/live-rooms",
                  "pagination": true,
                  "columns": [
                    { "key": "name", "title": "直播间" },
                    { "key": "platform", "title": "平台" },
                    { "key": "groupName", "title": "小组" },
                    { "key": "today", "title": "今日排班" },
                    { "key": "tomorrow", "title": "明日排班" },
                    { "key": "week", "title": "本周排班" },
                    { "key": "month", "title": "本月排班" }
                  ],
                  "rowActions": [
                    { "label": "查看排班", "action": "navigate:/schedule/live-rooms/{liveRoomId}" },
                    { "label": "首次配置", "action": "navigate:/schedule/live-rooms/{liveRoomId}/config" }
                  ]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "LiveRoomScheduleDetail",
      "title": "直播间排班详情",
      "route": "/schedule/live-rooms/:liveRoomId",
      "permission": "schedule.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_schedule",
            "name": "Schedule",
            "layout": "vertical",
            "children": [
              {
                "type": "InfoCard",
                "props": {
                  "title": "直播间信息",
                  "fields": [{ "key": "name", "label": "直播间", "type": "text", "source": "liveRoom.name" }]
                }
              },
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    {
                      "key": "dateRange",
                      "type": "dateRangePreset",
                      "options": ["本周", "下周", "本月", "下月", "历史(一年)"]
                    }
                  ],
                  "actions": [{ "label": "新增排班", "permission": "schedule.edit", "action": "openModal:ShiftCreate" }]
                }
              },
              {
                "type": "ScheduleGrid",
                "props": {
                  "roles": ["主播", "助播", "场控"],
                  "hourRange": [0, 23],
                  "dateRangeSource": "state.dateRange",
                  "itemsSource": "api:/api/schedule/live-rooms/{liveRoomId}/shifts",
                  "interactions": [
                    { "event": "onEmptyCellClick", "action": "openModal:ShiftCreate" },
                    { "event": "onItemClick", "action": "openModal:ShiftEdit" }
                  ]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "LiveRoomPerformanceList",
      "title": "直播间业绩",
      "route": "/performance/live-rooms",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_perf_list",
            "name": "PerfList",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    { "key": "platform", "type": "select" },
                    { "key": "departmentOrgId", "type": "select" },
                    { "key": "groupOrgId", "type": "select" }
                  ],
                  "actions": []
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/live-rooms",
                  "pagination": true,
                  "columns": [
                    { "key": "name", "title": "直播间" },
                    { "key": "platform", "title": "平台" },
                    { "key": "orgPath", "title": "组织" },
                    { "key": "today", "title": "今日" },
                    { "key": "yesterday", "title": "昨日" },
                    { "key": "week", "title": "本周" },
                    { "key": "month", "title": "本月" },
                    { "key": "statsAccounts", "title": "业绩统计账号" }
                  ],
                  "rowActions": [
                    { "label": "查看详情", "action": "navigate:/performance/live-rooms/{liveRoomId}" },
                    { "label": "编辑", "permission": "liveRoom.edit", "action": "openModal:LiveRoomEdit" }
                  ]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "LiveRoomPerformanceDetail",
      "title": "直播间数据详情",
      "route": "/performance/live-rooms/:liveRoomId",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_perf_detail",
            "name": "PerfDetail",
            "layout": "vertical",
            "children": [
              {
                "type": "InfoCard",
                "props": {
                  "title": "直播间基础信息",
                  "fields": [{ "key": "name", "label": "直播间", "type": "text", "source": "liveRoom.name" }]
                }
              },
              {
                "type": "FilterBar",
                "props": {
                  "filters": [{ "key": "dateRange", "type": "dateRangePreset", "options": ["近30天", "自定义"] }],
                  "actions": [
                    {
                      "label": "导出",
                      "permission": "performance.export",
                      "action": "api:/api/performance/live-rooms/{liveRoomId}/export"
                    },
                    { "label": "场次明细", "action": "navigate:/performance/live-rooms/{liveRoomId}/sessions" }
                  ]
                }
              },
              {
                "type": "MetricCards",
                "props": {
                  "metrics": [
                    { "key": "pv", "label": "场观" },
                    { "key": "sales", "label": "销售" },
                    { "key": "refund", "label": "退款" },
                    { "key": "netSales", "label": "净销售" },
                    { "key": "adCost", "label": "投放" },
                    { "key": "roi", "label": "ROI" }
                  ],
                  "datePreset": "state.datePreset"
                }
              },
              {
                "type": "Chart",
                "props": {
                  "chartType": "bar",
                  "dataSource": "api:/api/performance/live-rooms/{liveRoomId}/trend",
                  "metricSelector": { "key": "metric", "options": ["pv", "sales", "refund", "adCost", "netSales"] },
                  "dateRangeSelector": { "key": "dateRange", "source": "state.dateRange" }
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/live-rooms/{liveRoomId}/daily",
                  "pagination": true,
                  "columns": [
                    { "key": "date", "title": "日期" },
                    { "key": "pv", "title": "场观" },
                    { "key": "sales", "title": "销售" },
                    { "key": "refund", "title": "退款" },
                    { "key": "netSales", "title": "净销售" },
                    { "key": "adCost", "title": "投放" },
                    { "key": "roi", "title": "ROI" }
                  ],
                  "rowActions": []
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "ProductRank",
      "title": "商品排行榜",
      "route": "/products/rank",
      "permission": "productRank.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_rank",
            "name": "Rank",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    { "key": "keyword", "type": "search", "placeholder": "商品名称" },
                    {
                      "key": "dateRange",
                      "type": "dateRangePreset",
                      "options": ["今日", "昨日", "近7日", "近30日", "自定义"]
                    }
                  ],
                  "actions": []
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/product-rank",
                  "pagination": true,
                  "columns": [
                    { "key": "rank", "title": "排名" },
                    { "key": "product", "title": "商品信息" },
                    { "key": "org", "title": "来源组织" },
                    { "key": "salesCount", "title": "销量" },
                    { "key": "refundCount", "title": "退单" },
                    { "key": "salesAmount", "title": "销售额" },
                    { "key": "refundAmount", "title": "退款额" },
                    { "key": "ctr", "title": "曝光点击率" },
                    { "key": "payRate", "title": "点击付款率" },
                    { "key": "conversionRate", "title": "曝光成交率" }
                  ],
                  "rowActions": [{ "label": "查看商品", "action": "navigate:/products/{productId}" }]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "CompanyCompass",
      "title": "公司业绩罗盘",
      "route": "/summary/company-compass",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_compass",
            "name": "Compass",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    {
                      "key": "dateRange",
                      "type": "dateRangePreset",
                      "options": ["今日", "昨日", "本周", "上周", "本月", "上月", "近30天", "自定义"]
                    }
                  ],
                  "actions": [
                    {
                      "label": "导出",
                      "permission": "performance.export",
                      "action": "api:/api/performance/company/export"
                    }
                  ]
                }
              },
              {
                "type": "MetricCards",
                "props": {
                  "metrics": [
                    { "key": "pv", "label": "场观" },
                    { "key": "sales", "label": "销售" },
                    { "key": "refund", "label": "退款" },
                    { "key": "netSales", "label": "净销售" },
                    { "key": "adCost", "label": "投放" }
                  ],
                  "datePreset": "state.datePreset"
                }
              },
              {
                "type": "Chart",
                "props": {
                  "chartType": "bar",
                  "dataSource": "api:/api/performance/company/trend",
                  "metricSelector": { "key": "metric", "options": ["pv", "sales", "refund", "adCost", "netSales"] },
                  "dateRangeSelector": { "key": "dateRange", "source": "state.dateRange" }
                }
              },
              {
                "type": "Chart",
                "props": {
                  "chartType": "pie",
                  "dataSource": "api:/api/performance/company/subsidiary-share",
                  "metricSelector": { "key": "datePreset", "options": ["昨日", "近7日", "近30日"] },
                  "dateRangeSelector": { "key": "none", "source": "state.none" }
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/company/daily",
                  "pagination": true,
                  "columns": [
                    { "key": "date", "title": "日期" },
                    { "key": "pv", "title": "场观" },
                    { "key": "sales", "title": "销售" },
                    { "key": "refund", "title": "退款" },
                    { "key": "netSales", "title": "净销售" },
                    { "key": "adCost", "title": "投放" }
                  ],
                  "rowActions": []
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "SummaryByOrg",
      "title": "按组织查看",
      "route": "/summary/by-org",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_by_org",
            "name": "ByOrg",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    { "key": "orgId", "type": "orgCascade" },
                    { "key": "datePreset", "type": "segmented", "options": ["今日", "昨日", "本周", "本月"] }
                  ],
                  "actions": []
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/by-org",
                  "pagination": true,
                  "columns": [
                    { "key": "liveRoom", "title": "直播间" },
                    { "key": "group", "title": "小组" },
                    { "key": "range", "title": "时间段" },
                    { "key": "shiftOrSessionCount", "title": "班次/场次" },
                    { "key": "pv", "title": "总场观" },
                    { "key": "sales", "title": "总销售" },
                    { "key": "refund", "title": "总退款" },
                    { "key": "netSales", "title": "净销售" },
                    { "key": "adCost", "title": "总投放" },
                    { "key": "roi", "title": "ROI" }
                  ],
                  "rowActions": [{ "label": "查看每日业绩", "action": "navigate:/performance/live-rooms/{liveRoomId}" }]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "StaffPerformanceList",
      "title": "人员业绩",
      "route": "/staff/performance",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_staff_list",
            "name": "StaffList",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    { "key": "orgPath", "type": "orgCascade" },
                    { "key": "positionId", "type": "select" },
                    { "key": "name", "type": "search", "placeholder": "姓名" },
                    { "key": "mobile", "type": "input", "placeholder": "手机号" }
                  ],
                  "actions": []
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/employees",
                  "pagination": true,
                  "columns": [
                    { "key": "name", "title": "姓名" },
                    { "key": "position", "title": "岗位" },
                    { "key": "employmentType", "title": "类型" },
                    { "key": "orgPath", "title": "所属组织" },
                    { "key": "today", "title": "今日" },
                    { "key": "yesterday", "title": "昨日" },
                    { "key": "week", "title": "本周" },
                    { "key": "lastWeek", "title": "上周" },
                    { "key": "sessions", "title": "场次(数/时长)" }
                  ],
                  "rowActions": [{ "label": "查看详情", "action": "navigate:/staff/performance/{employeeId}" }]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "StaffPerformanceDetail",
      "title": "人员业绩详情",
      "route": "/staff/performance/:employeeId",
      "permission": "performance.view",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_staff_detail",
            "name": "StaffDetail",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    {
                      "key": "datePreset",
                      "type": "segmented",
                      "options": ["今日", "昨日", "本周", "上周", "本月", "上月"]
                    }
                  ],
                  "actions": [
                    {
                      "label": "导出",
                      "permission": "performance.export",
                      "action": "api:/api/performance/employees/{employeeId}/export"
                    }
                  ]
                }
              },
              {
                "type": "MetricCards",
                "props": {
                  "metrics": [
                    { "key": "pv", "label": "场观" },
                    { "key": "sales", "label": "销售" },
                    { "key": "refund", "label": "退款" },
                    { "key": "netSales", "label": "净销售" },
                    { "key": "adCost", "label": "投放" },
                    { "key": "roi", "label": "ROI" }
                  ],
                  "datePreset": "state.datePreset"
                }
              },
              {
                "type": "Chart",
                "props": {
                  "chartType": "funnel",
                  "dataSource": "api:/api/performance/employees/{employeeId}/trend",
                  "metricSelector": {
                    "key": "metric",
                    "options": ["pv", "sales", "refund", "adCost", "conversionRate"]
                  },
                  "dateRangeSelector": { "key": "dateRange", "source": "state.dateRange" }
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/employees/{employeeId}/daily",
                  "pagination": true,
                  "columns": [
                    { "key": "date", "title": "日期" },
                    { "key": "pv", "title": "场观" },
                    { "key": "sales", "title": "销售" },
                    { "key": "refund", "title": "退款" },
                    { "key": "netSales", "title": "净销售" },
                    { "key": "adCost", "title": "投放" },
                    { "key": "roi", "title": "ROI" }
                  ],
                  "rowActions": []
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "LiveRoomScheduleConfig",
      "title": "首次排班配置",
      "route": "/schedule/live-rooms/:liveRoomId/config",
      "permission": "schedule.config",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_config",
            "name": "Config",
            "layout": "vertical",
            "children": [
              {
                "type": "FormModal",
                "props": {
                  "title": "直播间时间轴配置",
                  "fields": [
                    { "key": "copyFromLiveRoomId", "label": "复制直播间", "type": "select", "required": false },
                    { "key": "dayStart", "label": "每日开始时间", "type": "timeOrUnlimited", "required": true },
                    { "key": "dayEnd", "label": "每日结束时间", "type": "timeOrUnlimited", "required": true },
                    { "key": "shiftDurations", "label": "班次时长集合(分钟)", "type": "multiNumber", "required": true },
                    {
                      "key": "timeGridMinutes",
                      "label": "时间轴单位",
                      "type": "select",
                      "required": true,
                      "options": [120, 60, 30, 15]
                    },
                    { "key": "breakEnabled", "label": "中间休息", "type": "switch", "required": true },
                    {
                      "key": "breakMinutes",
                      "label": "休息时长(分钟)",
                      "type": "number",
                      "requiredWhen": "breakEnabled=true"
                    },
                    { "key": "positions", "label": "岗位配置", "type": "multiSelect", "required": true }
                  ],
                  "submitAction": "api:/api/schedule/live-rooms/{liveRoomId}/config",
                  "validators": ["timeRange", "minArrayLength"]
                }
              }
            ]
          }
        }
      ]
    },
    {
      "id": "LiveRoomSessionDetail",
      "title": "场次明细",
      "route": "/performance/live-rooms/:liveRoomId/sessions",
      "permission": "session.edit",
      "layout": "AppShell",
      "regions": [
        {
          "type": "Region",
          "props": {
            "id": "r_sessions",
            "name": "Sessions",
            "layout": "vertical",
            "children": [
              {
                "type": "FilterBar",
                "props": {
                  "filters": [
                    { "key": "dateRange", "type": "dateRange" },
                    { "key": "hostEmployeeId", "type": "select" }
                  ],
                  "actions": [
                    { "label": "录入场次", "permission": "session.edit", "action": "openModal:SessionCreate" }
                  ]
                }
              },
              {
                "type": "DataTable",
                "props": {
                  "dataSource": "api:/api/performance/live-rooms/{liveRoomId}/sessions",
                  "pagination": true,
                  "columns": [
                    { "key": "date", "title": "日期" },
                    { "key": "timeRange", "title": "直播时间" },
                    { "key": "participants", "title": "人员" },
                    { "key": "pv", "title": "场观" },
                    { "key": "sales", "title": "销售" },
                    { "key": "refund", "title": "退款" },
                    { "key": "netSales", "title": "净销售" },
                    { "key": "adCost", "title": "投放" },
                    { "key": "roi", "title": "ROI" },
                    { "key": "source", "title": "来源" }
                  ],
                  "rowActions": [
                    { "label": "编辑", "permission": "session.edit", "action": "openModal:SessionEdit" },
                    {
                      "label": "删除",
                      "permission": "session.edit",
                      "action": "confirmThen:apiDelete:/api/performance/sessions/{sessionId}"
                    }
                  ]
                }
              }
            ]
          }
        }
      ]
    }
  ]
}
```
