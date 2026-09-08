# [OPEN] notes-annotation-disappear

## 症状
- 初始进入详情页：批注段落红色下划线正常显示
- 进入“笔记模式”：批注样式消失
- 切换到“批注 tab”：依然不显示批注样式（等待也不出现）
- 退出笔记/批注模式：分钟段落页面也出现异常（用户反馈为“不显示”）

## 期望
- 进入笔记模式与切换批注 tab 不应影响正文批注的显示
- 退出笔记模式后应恢复到进入前的正文展示与批注显示

## 复现步骤
1. 打开详情页（分钟段落）
2. 观察批注段落红线存在
3. 点击“笔记+批注”进入笔记模式
4. 在笔记模式中切换到“纠正和批注汇总（annotation）”
5. 再切回/退出笔记模式

## 假设（可证伪）
- A：进入笔记模式/切 tab 触发了文本区域 refresh 或 textType 切换，导致 annotationMap 被清空或未重新 setAnnotationHtml。
- B：切到批注 tab 时 isAnnotation/notes 相关标记导致 TextParagraph 渲染分支切换（text vs 非text），批注包裹/批字定位依赖 DOM 时序失败。
- C：getAnnotation()/initAnnotationText/getAnnotationText 在某次调用链中写入了异常区间（NaN/越界），导致后续渲染全部失效。
- D：退出笔记模式时 notesTabsClick('') 未恢复正文状态（textType/refresh/currentParagraphIndex），导致正文容器未渲染或被覆盖。
- E：TextParagraph.setAnnotationHtml/setAnnotationMark 在重复调用时存在非幂等行为，导致 contentText/contentHtml 变为空或被错误覆盖。

## 需要采集的运行时证据
- notes/annotation 切换时：notes、isAnnotation、textType、refresh、currentParagraphIndex 的变化轨迹
- annotationMap 的 keys/长度是否被清空、是否重建成功
- TextParagraph 收到的 annotation 数量、是否执行 setAnnotationHtml、是否能找到 annotation-item DOM

