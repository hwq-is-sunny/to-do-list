---
name: MVP开发方案
overview: 基于现有 UI 图和页面说明，先定义第一版可运行 MVP 的最小范围，优先保证首页、任务新增/编辑、日历时间展示和本地保存可用。方案采用本地数据库 + 单机运行，不引入后端与云同步。
todos:
  - id: restore-project
    content: 先恢复 Android 工程基础文件与模块结构（Gradle、app、Manifest、基础入口）
    status: pending
  - id: mvp-screens
    content: 先打通首页、任务新增/编辑、日历时间轴三页与底部导航
    status: pending
  - id: mvp-data
    content: 用 Room 完成任务本地存储与基本 CRUD
    status: pending
  - id: mvp-polish
    content: 完成日期切换、任务完成状态切换、排序与重启后数据保留验证
    status: pending
isProject: false
---

# 第一版可运行 MVP 开发方案

## 前置说明（不写代码阶段）
- 当前工作区里应用源码处于缺失状态（只剩 UI 图和说明文档），后续实现前需要先恢复 Android 工程文件。
- 下面方案按“最容易实现、先跑起来”的原则制定，不改现有 UI 风格和布局。

## 1) 第一版先做哪些页面
- 首页代办页（核心）：对应 `ui-screens/代办页.JPG`，建议落地到 [D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/screens/today/TodayScreen.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/screens/today/TodayScreen.kt)
- 任务新增/编辑页（核心）：建议在现有候选页基础上收敛为任务表单（可复用 [D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/screens/inbox/AddCandidateScreen.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/screens/inbox/AddCandidateScreen.kt) 或新增 `TaskEditScreen`）
- 日历时间轴页（核心）：对应 `ui-screens/课表页.jpg`，建议落地到 [D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/screens/calendar/CalendarScreen.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/screens/calendar/CalendarScreen.kt)
- 底部导航壳层（支撑页面切换）：[D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/navigation/CampusTodoNavHost.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/navigation/CampusTodoNavHost.kt)

- 暂缓页面（第二阶段）：设置页、登录/注册页、月历课程页（`ui-screens/日历页.jpg`）

## 2) 第一版先做哪些功能
- 首页任务列表可展示（按选中日期显示任务）
- 新增任务（标题 + 日期 + 时间段 + 备注可选）
- 编辑任务（进入任务详情后修改并保存）
- 删除任务
- 勾选任务完成/取消完成
- 日期切换（首页日期条 + 日历页日期切换）
- 日历时间轴展示（当天任务按开始时间排序）
- 本地持久化（App 重启后数据仍在）

- 明确不做（第一版先不做）：账号后端、云同步、AI Planning、Tomato Focus、复杂筛选/统计

## 3) 需要哪些核心数据字段
- `id: Long`（主键）
- `title: String`（任务标题，必填）
- `note: String?`（备注，可空）
- `date: LocalDate` 或 `dateEpochDay: Long`（所属日期）
- `startTime: LocalTime?` 或 `startMinute: Int?`（开始时间，可空）
- `endTime: LocalTime?` 或 `endMinute: Int?`（结束时间，可空）
- `status: Enum`（`TODO` / `DONE`）
- `priority: Enum`（`NORMAL` / `HIGH`，第一版可简单两级）
- `createdAt: Long`（创建时间戳）
- `updatedAt: Long`（更新时间戳）

- 第一版最小可运行必需字段：`id/title/date/start/end/status/updatedAt`

## 4) 代码文件大概怎么组织
- 入口与导航
  - [D:/clone/todolist old/app/src/main/java/com/campus/todo/MainActivity.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/MainActivity.kt)
  - [D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/navigation/NavRoutes.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/navigation/NavRoutes.kt)
  - [D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/navigation/CampusTodoNavHost.kt](D:/clone/todolist old/app/src/main/java/com/campus/todo/ui/navigation/CampusTodoNavHost.kt)

- UI 层（先做核心三页）
  - 首页：`ui/screens/today/TodayScreen.kt` + `TodayViewModel.kt`
  - 日历：`ui/screens/calendar/CalendarScreen.kt` + `CalendarViewModel.kt`
  - 任务新增/编辑：`ui/screens/tasks/TaskEditScreen.kt` + `TaskEditViewModel.kt`（或复用 inbox 现有页面）
  - 公共组件：`ui/components/CampusCards.kt`（保持现有视觉）

- 数据层（本地优先）
  - 实体：`data/db/entity/Task.kt`
  - DAO：`data/db/dao/TaskDao.kt`
  - 数据库：`data/db/AppDatabase.kt` + `data/db/Converters.kt`
  - 仓库：`data/repo/TodoRepository.kt`

- 建议的最小调用链
  - `Screen -> ViewModel -> Repository -> TaskDao(Room) -> SQLite`

- 设置页和登录页在第一版仅保留路由占位或暂不接入主流程，避免拖慢“可运行 MVP”交付。