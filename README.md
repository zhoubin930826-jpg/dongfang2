# dongfang2

## 项目简介

`dongfang2` 是一个前后端分离的东方财富数据看板项目，主要用于展示：

- 行业板块基础数据
- 行业板块 K 线数据
- 股票池数据
- 个股实盘委托买卖数据
- 个股 K 线数据

前端负责数据展示和图表渲染，后端负责请求东方财富接口、处理反爬细节、做数据持久化，并对前端提供本地查询接口。

---

## 当前项目结构

```text
dongfang2/
├── houduan/          # Spring Boot 后端
├── qianduan/         # Vue 3 + Vite 前端
├── README.md         # 当前说明
└── README2.md        # 原始需求说明
```

---

## 主要功能

### 前端页面

- `/industry-base`
  - 展示行业板块基础数据表格
- `/industry-kline`
  - 展示行业板块历史 K 线和多维度折线图
- `/stock-pool`
  - 展示股票池列表
- `/stock-real`
  - 展示个股盘口数据和历史 K 线图

### 后端能力

- 对接东方财富接口
- 自动维护 Cookie、请求头、代理和重试逻辑
- 定时抓取数据并保存到数据库
- 前端接口优先查询数据库，数据库无数据或数据过期时自动补抓

---

## 技术栈

### 前端

| 技术 | 版本 |
|------|------|
| Vue | ^3.5.25 |
| Vue Router | ^4.6.4 |
| Element Plus | ^2.13.2 |
| ECharts | ^6.0.0 |
| Axios | ^1.13.5 |
| TypeScript | ~5.9.3 |
| Vite | ^7.3.1 |

### 后端

| 技术 | 版本 |
|------|------|
| Spring Boot | 2.7.18 |
| Spring Web | 2.7.18 |
| Spring Data JPA | 2.7.18 |
| MySQL Connector/J | 8.0.33 |
| Flyway | 已保留依赖，当前配置关闭 |

说明：

- 项目原先后端只做接口转发，现在已经扩展为“采集 + 持久化 + 查询”模式。
- 由于 Spring Boot 2.7 自带 Flyway 对 MySQL 8.4 识别不兼容，当前 MySQL 版运行时关闭了 Flyway，表结构通过 SQL 脚本手工初始化。

---

## 本次已完成的环境和项目改造记录

### 1. 本地代码与基础环境

- 已将项目下载到 `F:\work\workspace\dongfang2`
- 已安装 JDK 17
- 已验证前端 `Node.js` 和 `npm` 可用
- 已修复前端若干 TypeScript 构建问题，当前前端可以正常 `build`

### 2. 后端持久化改造

后端已从“直接请求东方财富并返回给前端”改造成“优先查库，必要时补抓并入库”。

新增内容包括：

- JPA 实体
- Repository 数据访问层
- 数据采集服务
- 数据查询服务
- 定时采集任务
- 数据库表结构脚本

主要代码位置：

- `houduan/src/main/java/com/example/houduan/controller/EastMoneyController.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyApiClient.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyStorageService.java`
- `houduan/src/main/resources/db/migration/V1__create_eastmoney_tables.sql`

### 3. MySQL 安装与配置

已安装：

- MySQL Server 8.4

已完成：

- 初始化 MySQL 数据目录
- 安装并启动 Windows 服务 `MySQL84`
- 创建项目数据库 `dongfang2`
- 创建项目账号 `dongfang2_app`
- 将后端数据源从 H2 切换为 MySQL

### 4. 当前数据库信息

MySQL 服务：

- 服务名：`MySQL84`
- 端口：`3306`

数据库：

- 库名：`dongfang2`

账号：

- root
  - 用户名：`root`
  - 密码：`Root@20260325!`
- 项目账号
  - 用户名：`dongfang2_app`
  - 密码：`Dongfang2App@20260325!`

建议：

- 这些密码是本地开发阶段配置，后续如果要长期使用，建议尽快改掉。

---

## 当前数据库表

当前已创建以下表：

- `industry_base_response`
- `industry_kline_response`
- `stock_pool_response`
- `stock_real_response`
- `stock_kline_response`
- `crawl_job_log`

表结构脚本位置：

- `houduan/src/main/resources/db/migration/V1__create_eastmoney_tables.sql`

---

## 定时采集说明

当前后端已经启用定时任务，配置在：

- `houduan/src/main/resources/application.properties`

当前调度频率：

- 行业板块基础数据：每 15 分钟
- 股票池：每 15 分钟
- 行业 K 线：每 6 小时
- 个股明细和个股 K 线：每 6 小时

相关配置项：

```properties
collector.industry-base.cron=0 */15 * * * *
collector.stock-pool.cron=30 */15 * * * *
collector.industry-kline.cron=0 0 */6 * * *
collector.stock-detail.cron=0 10 */6 * * *
collector.max-stock-pool-pages=1
collector.query-freshness-minutes=60
```

说明：

- 当前 `stock-pool` 只采集第 1 页
- 当前查询新鲜度窗口为 60 分钟
- 当前接口会先查数据库，若无数据或数据过期，则会实时补抓并入库

---

## 当前启动方式

### 后端

工作目录：

- `F:\work\workspace\dongfang2\houduan`

启动命令：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin;' + $env:Path
.\mvnw.cmd spring-boot:run
```

### 前端

工作目录：

- `F:\work\workspace\dongfang2\qianduan`

首次安装依赖：

```powershell
F:\work\workspace\nodejs\npm.cmd install
```

启动命令：

```powershell
F:\work\workspace\nodejs\npm.cmd run dev -- --host 0.0.0.0
```

---

## 当前访问地址

- 前端首页：`http://localhost:5173`
- 后端接口：`http://localhost:8080`

可直接测试的接口：

- `http://localhost:8080/api/industry/base`
- `http://localhost:8080/api/industry/kline?industryCode=BK0486`
- `http://localhost:8080/api/stock/pool?pn=1`
- `http://localhost:8080/api/stock/real?stockCode=600960`
- `http://localhost:8080/api/stock/kline?stockCode=600960`

---

## 重要配置文件

- 前端代理配置：
  - `qianduan/vite.config.ts`
- 前端入口：
  - `qianduan/src/main.ts`
- 后端主配置：
  - `houduan/src/main/resources/application.properties`
- 后端 Maven 配置：
  - `houduan/pom.xml`

---

## 日志文件

运行时日志文件位置：

- 后端标准输出：`houduan/backend.log`
- 后端错误日志：`houduan/backend.err.log`
- 前端标准输出：`qianduan/frontend.log`
- 前端错误日志：`qianduan/frontend.err.log`

---

## 当前已验证通过的事项

- 前端可以成功构建
- 前端开发服务可以正常访问
- 后端 Spring Boot 服务可以正常启动
- 后端已成功切换到 MySQL
- MySQL 服务可以正常运行
- 行业板块基础数据接口已验证返回成功并写入数据库
- 股票池接口已验证返回成功并写入数据库
- 个股实时数据接口已验证返回成功并写入数据库
- 个股 K 线接口已验证返回成功并写入数据库
- 前端通过 `/api` 代理访问后端接口正常

---

## 后续建议

建议后续继续完善以下事项：

- 将数据库密码移出 `application.properties`
- 升级到兼容 MySQL 8.4 的 Flyway 方案，恢复自动迁移
- 为定时任务增加监控和失败告警
- 将股票池采集扩展到更多页
- 将原始 JSON 之外的关键字段做结构化拆表，便于后续统计分析

