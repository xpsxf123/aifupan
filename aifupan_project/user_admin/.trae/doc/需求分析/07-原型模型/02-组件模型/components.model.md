# 原型模型｜组件模型（code 数据）

```json
{
  "version": "v0",
  "componentTypes": [
    {
      "type": "AppShell",
      "propsSchema": {
        "topBar": { "type": "object" },
        "sideMenu": { "type": "object" },
        "breadcrumb": { "type": "object" },
        "content": { "type": "array" }
      }
    },
    {
      "type": "Page",
      "propsSchema": {
        "id": { "type": "string" },
        "title": { "type": "string" },
        "route": { "type": "string" },
        "permission": { "type": "string" },
        "layout": { "type": "string", "enum": ["AppShell"] },
        "regions": { "type": "array" }
      }
    },
    {
      "type": "Region",
      "propsSchema": {
        "id": { "type": "string" },
        "name": { "type": "string" },
        "layout": { "type": "string", "enum": ["vertical", "horizontal", "grid"] },
        "children": { "type": "array" }
      }
    },
    {
      "type": "InfoCard",
      "propsSchema": {
        "title": { "type": "string" },
        "fields": { "type": "array", "items": { "type": "object" } }
      }
    },
    {
      "type": "FilterBar",
      "propsSchema": {
        "filters": { "type": "array", "items": { "type": "object" } },
        "actions": { "type": "array", "items": { "type": "object" } }
      }
    },
    {
      "type": "Tabs",
      "propsSchema": {
        "tabs": { "type": "array", "items": { "type": "object" } },
        "activeTab": { "type": "string" }
      }
    },
    {
      "type": "DataTable",
      "propsSchema": {
        "columns": { "type": "array", "items": { "type": "object" } },
        "dataSource": { "type": "string" },
        "rowActions": { "type": "array", "items": { "type": "object" } },
        "pagination": { "type": "boolean" }
      }
    },
    {
      "type": "CalendarWeekGrid",
      "propsSchema": {
        "hourStart": { "type": "number" },
        "hourEnd": { "type": "number" },
        "defaultHourWindow": { "type": "array", "items": { "type": "number" } },
        "dateRangeSource": { "type": "string" },
        "itemsSource": { "type": "string" },
        "interactions": { "type": "array", "items": { "type": "object" } }
      }
    },
    {
      "type": "ScheduleGrid",
      "propsSchema": {
        "roles": { "type": "array", "items": { "type": "string" } },
        "hourRange": { "type": "array", "items": { "type": "number" } },
        "dateRangeSource": { "type": "string" },
        "itemsSource": { "type": "string" },
        "interactions": { "type": "array", "items": { "type": "object" } }
      }
    },
    {
      "type": "Chart",
      "propsSchema": {
        "chartType": { "type": "string", "enum": ["bar", "line", "funnel", "pie"] },
        "dataSource": { "type": "string" },
        "metricSelector": { "type": "object" },
        "dateRangeSelector": { "type": "object" }
      }
    },
    {
      "type": "MetricCards",
      "propsSchema": {
        "metrics": { "type": "array", "items": { "type": "object" } },
        "datePreset": { "type": "string" }
      }
    },
    {
      "type": "FormModal",
      "propsSchema": {
        "title": { "type": "string" },
        "fields": { "type": "array", "items": { "type": "object" } },
        "submitAction": { "type": "string" },
        "validators": { "type": "array", "items": { "type": "string" } }
      }
    },
    {
      "type": "Drawer",
      "propsSchema": {
        "title": { "type": "string" },
        "content": { "type": "array" }
      }
    }
  ]
}
```
