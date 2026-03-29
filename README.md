# dongfang2

## 项目简介

`dongfang2` 是一个面向 A 股行情数据的本地化抓取、存储、查询、分析与展示系统。

系统目标不是简单转发东方财富接口，而是把下面这些能力串成一条完整链路：

- 数据抓取
- 反爬与稳定性控制
- 原始数据落库
- 定时任务更新
- 查询缓存与补刷
- 实时选股分析
- 前端可视化看盘

当前项目采用前后端分离架构：

- 前端：Vue 3 + TypeScript + Vite + Element Plus + ECharts
- 后端：Spring Boot 2.7 + Spring Web + Spring Data JPA
- 数据库：MySQL

---

## 当前系统能力总览

当前系统已经具备以下核心能力：

- 全量分页抓取行业板块基础行情
- 全量分页抓取股票池数据
- 批量抓取行业历史 K 线
- 批量抓取个股实时盘口和个股 K 线
- 原始 JSON 持久化到 MySQL，并记录 `fetched_at`
- 所有查询接口统一返回数据库抓取时间 `_meta.fetchedAt`
- 页面直接展示数据库抓取时间，方便判断数据实时性
- 查询链路采用“优先返回本地数据，过期后后台异步刷新”的策略
- 定时任务内置补差机制，并额外提供完整性修复任务
- 抓取层具备限速、随机延迟、重试、冷却、代理回退等反爬能力
- 定时任务支持同类任务不重叠执行
- 按 A 股交易时段执行“盘中高频、盘后低频”的调度策略
- 提供实时选股分析页
- 提供分钟级分时分析页内联展示
- 已接入第三阶段分析：盘口结构分、日线技术分进入候选评分

---

## 页面功能

前端路由定义见 [qianduan/src/router/index.ts](/F:/work/workspace/dongfang2/qianduan/src/router/index.ts)。

### 1. 行业板块基础数据 `/industry-base`

页面文件： [qianduan/src/views/IndustryBase.vue](/F:/work/workspace/dongfang2/qianduan/src/views/IndustryBase.vue)

功能包括：

- 展示行业板块基础行情总表
- 展示行业数量和最近数据库抓取时间
- 展示涨跌幅、成交额、主力净流入、上涨家数等字段
- 支持跳转到行业 K 线详情页

### 2. 行业板块 K 线 `/industry-kline`

页面文件： [qianduan/src/views/IndustryKline.vue](/F:/work/workspace/dongfang2/qianduan/src/views/IndustryKline.vue)

功能包括：

- 根据 `industryCode` 查询行业历史 K 线
- 展示行业基础信息
- 展示数据库抓取时间
- 支持日期过滤
- 展示开盘、收盘、最高、最低、成交量、成交额、振幅、涨跌幅、涨跌额、换手率等图表数据

### 3. 股票池 `/stock-pool`

页面文件： [qianduan/src/views/StockPool.vue](/F:/work/workspace/dongfang2/qianduan/src/views/StockPool.vue)

功能包括：

- 展示全市场股票池分页数据
- 展示总数、当前页、数据库抓取时间
- 展示最新价、涨跌幅、成交额、市值、行业等字段
- 支持从股票池跳转到个股实盘页
- 跳转时携带股票代码和市场信息

### 4. 个股实盘数据 `/stock-real`

页面文件： [qianduan/src/views/StockReal.vue](/F:/work/workspace/dongfang2/qianduan/src/views/StockReal.vue)

功能包括：

- 支持手动输入股票代码查询
- 支持从股票池页跳转进入
- 展示个股基础信息和盘口摘要
- 展示买一到买五、卖一到卖五、委差、委比、量比、均价、换手率等数据
- 同时展示 `Quote Fetched` 和 `Kline Fetched`
- 展示个股历史 K 线图及相关指标
- 支持日期筛选

### 5. 实时选股分析 `/stock-analysis`

页面文件： [qianduan/src/views/StockAnalysis.vue](/F:/work/workspace/dongfang2/qianduan/src/views/StockAnalysis.vue)

当前已实现三阶段能力：

- 第一阶段：基于股票池、行业快照、核心指数生成候选股票列表
- 第二阶段：加入质量分、风险标签、风险等级、分钟级分时分析
- 第三阶段：把本地 `stock-real` 和 `stock-kline` 解析成盘口分和技术分，进入候选评分

页面展示内容包括：

- 市场情绪分数、市场状态、股票池快照时间
- 热点行业与市场宽度概览
- 候选股票综合评分、质量分、盘口分、技术分
- 评分拆解、风险标签、入选理由
- 盘口抓取时间、日线抓取时间
- 分时价格线、均价线、尾盘变化、均价偏离、收盘强度等

### 6. 全局样式与时间展示

相关文件：

- [qianduan/src/App.vue](/F:/work/workspace/dongfang2/qianduan/src/App.vue)
- [qianduan/src/style.css](/F:/work/workspace/dongfang2/qianduan/src/style.css)
- [qianduan/src/utils/responseMeta.ts](/F:/work/workspace/dongfang2/qianduan/src/utils/responseMeta.ts)

功能包括：

- 统一工作台风格布局
- 统一表格、卡片、图表风格
- 统一格式化 `_meta.fetchedAt`
- 页面直接展示数据库抓取时间

---

## 后端接口

控制器文件： [houduan/src/main/java/com/example/houduan/controller/EastMoneyController.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/controller/EastMoneyController.java)

当前提供的接口如下：

- `GET /api/industry/base`
- `GET /api/industry/kline?industryCode=BKxxxx`
- `GET /api/stock/pool?pn=1`
- `GET /api/stock/real?stockCode=600000&market=1`
- `GET /api/stock/kline?stockCode=600000&market=1`
- `GET /api/analysis/opportunities?limit=30`
- `GET /api/analysis/intraday?stockCode=600000&market=1`

查询类接口会附带：

- `_meta.fetchedAt`
- `_meta.isFresh`

---

## 数据抓取与存储

核心服务文件：

- [houduan/src/main/java/com/example/houduan/service/EastMoneyApiClient.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/EastMoneyApiClient.java)
- [houduan/src/main/java/com/example/houduan/service/EastMoneyStorageService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/EastMoneyStorageService.java)
- [houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java)
- [houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java)

当前后端支持抓取：

- 行业基础行情
- 行业历史 K 线
- 股票池分页数据
- 个股实时盘口
- 个股历史 K 线
- 个股分钟级分时数据
- 核心指数实时行情

数据存储策略：

- 原始响应 JSON 直接入库
- 为每份数据记录 `fetched_at`
- 从原始 JSON 中提取行业代码、股票代码、市场信息、总数、总页数等辅助字段

---

## 三阶段分析能力

分析服务文件：

- [houduan/src/main/java/com/example/houduan/service/StockOpportunityAnalysisService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/StockOpportunityAnalysisService.java)
- [houduan/src/main/java/com/example/houduan/service/StockIntradayAnalysisService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/StockIntradayAnalysisService.java)
- [houduan/src/main/java/com/example/houduan/service/StockFactorEnrichmentService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/StockFactorEnrichmentService.java)

### 第一阶段：机会候选排序

输入：

- 本地股票池快照
- 本地行业快照
- 上证指数、深证成指、创业板指、沪深 300

输出：

- 市场情绪分数
- 市场状态
- 热点行业列表
- 候选股票综合评分
- 评分拆解与入选理由

### 第二阶段：质量与风险分析

在第一阶段基础上补充：

- 质量分
- 风险标签
- 风险等级
- 分钟级分时分析

### 第三阶段：盘口分与技术分

第三阶段新增内容：

- 从本地 `stock-real` 提取盘口结构信号
- 从本地 `stock-kline` 提取日线趋势信号
- 对前排候选股票做二次增强评分
- 输出 `scoreDetail.quote`
- 输出 `scoreDetail.technical`
- 输出 `quoteFetchedAt`
- 输出 `klineFetchedAt`

默认配置：

```properties
analysis.stage3.preselect-limit=180
analysis.stage3.quote-freshness-minutes=25
analysis.stage3.kline-freshness-hours=96
```

说明：

- 盘口分优先用于盘中实时判断
- 日线技术分用于判断趋势结构
- 第三阶段只对前排候选做增强，避免全市场全量读取带来额外压力

---

## 查询策略

查询逻辑位于 [houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java)。

当前策略为：

- 优先读取数据库最新结果
- 数据在新鲜窗口内时直接返回
- 数据过期但库里有旧数据时，先返回旧数据，再后台异步刷新
- 库里完全没有数据时，同步抓取并入库后返回

当前新鲜窗口配置：

```properties
collector.query-freshness-minutes=60
```

---

## 定时任务

定时任务实现位于 [houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java)。

A 股时钟服务位于 [houduan/src/main/java/com/example/houduan/service/AShareMarketClockService.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/service/AShareMarketClockService.java)。

当前一共 5 个定时任务：

### 1. `industry-base`

- 盘中频率：每 5 分钟一次
- 盘后频率：工作日 15:05 后补一轮
- 策略：按总数分页全量抓行业基础数据，合并为完整快照入库

### 2. `stock-pool`

- 盘中频率：每 5 分钟一次
- 盘后频率：工作日 15:06 后补一轮
- 策略：按总数分页全量抓股票池，每页单独入库

### 3. `industry-kline`

- 频率：工作日 15:20 后跑一轮
- 策略：从最新行业基础快照提取全部行业代码，逐个抓取行业 K 线

### 4. `stock-detail`

- 频率：工作日 15:35 后跑一轮
- 策略：
  - 以股票池第一页最新快照为参考时间
  - 尽量按同一轮快照读取各页
  - 拼出全量股票代码集合
  - 逐只抓取 `stock-real` 和 `stock-kline`

### 5. `integrity-repair`

- 盘中频率：每 30 分钟一次
- 盘后频率：工作日 15:50 后补一轮
- 策略：
  - 检查股票池缺页
  - 检查行业 K 线缺行业
  - 检查个股实时数据缺股票
  - 检查个股 K 线缺股票
  - 自动补差

### 定时任务共性

- 同类任务不重叠执行
- 主任务内自带补查
- 额外完整性修复任务负责兜底
- 盘中高频、盘后低频，避免无效抓取

---

## 反爬与稳定性

相关文件：

- [houduan/src/main/java/com/example/houduan/utils/ClaudeEastMoneyApiUtil.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/utils/ClaudeEastMoneyApiUtil.java)
- [houduan/src/main/java/com/example/houduan/config/CrawlerConfig.java](/F:/work/workspace/dongfang2/houduan/src/main/java/com/example/houduan/config/CrawlerConfig.java)

当前已实现：

- Cookie 初始化与刷新
- User-Agent 池
- 最小请求间隔控制
- 随机延迟
- 请求失败重试
- 限频冷却
- 封禁冷却
- 连续失败保护冷却
- 代理支持
- 代理失败后直连回退
- 分页抓取中的主动休息

当前主要配置：

```properties
crawler.proxy.enabled=true
crawler.proxy.fail-open=true
crawler.request.min-interval-ms=900
crawler.request.base-delay-ms=150
crawler.request.random-delay-ms=350
crawler.retry.max-attempts=4
crawler.cooldown.rate-limit-ms=30000
crawler.cooldown.forbidden-ms=60000
crawler.cooldown.failure-threshold=6
crawler.cooldown.failure-ms=45000
collector.stock-pool.pause-after-pages=10
collector.stock-pool.pause-ms=5000
collector.stock-detail.pause-after-symbols=40
collector.stock-detail.pause-ms=15000
analysis.market-index.cache-ms=60000
analysis.intraday.cache-ms=60000
```

---

## 数据库表

建表脚本： [houduan/src/main/resources/db/migration/V1__create_eastmoney_tables.sql](/F:/work/workspace/dongfang2/houduan/src/main/resources/db/migration/V1__create_eastmoney_tables.sql)

当前主要表：

- `industry_base_response`
- `industry_kline_response`
- `stock_pool_response`
- `stock_real_response`
- `stock_kline_response`
- `crawl_job_log`

说明：

- 前五张表存放原始抓取数据和 `fetched_at`
- `crawl_job_log` 记录定时任务执行日志，不存行情本体

---

## 项目结构

```text
dongfang2/
├─ houduan/                     # Spring Boot 后端
│  ├─ src/main/java/...
│  ├─ src/main/resources/...
│  └─ pom.xml
├─ qianduan/                    # Vue 3 前端
│  ├─ src/
│  ├─ package.json
│  └─ vite.config.ts
├─ README.md
└─ README2.md                   # 原始需求说明
```

---

## 技术栈

### 前端

来源： [qianduan/package.json](/F:/work/workspace/dongfang2/qianduan/package.json)

- Vue 3
- Vue Router
- Element Plus
- ECharts
- Axios
- TypeScript
- Vite

### 后端

来源： [houduan/pom.xml](/F:/work/workspace/dongfang2/houduan/pom.xml)

- Java 17
- Spring Boot 2.7.18
- Spring Web
- Spring Data JPA
- MySQL Connector/J

---

## 启动方式

### 后端

工作目录： [houduan](/F:/work/workspace/dongfang2/houduan)

前台启动：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
.\mvnw.cmd spring-boot:run
```

后台启动并写日志：

```powershell
Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot" && cd /d F:\work\workspace\dongfang2\houduan && .\mvnw.cmd spring-boot:run > backend.log 2>&1' -WorkingDirectory 'F:\work\workspace\dongfang2\houduan'
```

### 前端

工作目录： [qianduan](/F:/work/workspace/dongfang2/qianduan)

前台启动：

```powershell
cmd /c npm.cmd run dev
```

后台启动并写日志：

```powershell
Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'cd /d F:\work\workspace\dongfang2\qianduan && npm.cmd run dev > frontend.log 2>&1' -WorkingDirectory 'F:\work\workspace\dongfang2\qianduan'
```

---

## 默认访问地址

- 前端首页：[http://localhost:5173/](http://localhost:5173/)
- 实时选股页：[http://localhost:5173/stock-analysis](http://localhost:5173/stock-analysis)
- 后端服务：[http://localhost:8080/](http://localhost:8080/)

常用接口：

- [http://localhost:8080/api/industry/base](http://localhost:8080/api/industry/base)
- [http://localhost:8080/api/industry/kline?industryCode=BK0428](http://localhost:8080/api/industry/kline?industryCode=BK0428)
- [http://localhost:8080/api/stock/pool?pn=1](http://localhost:8080/api/stock/pool?pn=1)
- [http://localhost:8080/api/stock/real?stockCode=600000&market=1](http://localhost:8080/api/stock/real?stockCode=600000&market=1)
- [http://localhost:8080/api/stock/kline?stockCode=600000&market=1](http://localhost:8080/api/stock/kline?stockCode=600000&market=1)
- [http://localhost:8080/api/analysis/opportunities?limit=10](http://localhost:8080/api/analysis/opportunities?limit=10)
- [http://localhost:8080/api/analysis/intraday?stockCode=600000&market=1](http://localhost:8080/api/analysis/intraday?stockCode=600000&market=1)

---

## 构建与验证

后端编译：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
.\mvnw.cmd -DskipTests compile
```

前端构建：

```powershell
cmd /c npm.cmd run build
```

本次代码更新已验证：

- 后端 `.\mvnw.cmd -DskipTests compile` 通过
- 前端 `npm.cmd run build` 通过

---

## 日志文件

常见日志位置：

- [houduan/backend.log](/F:/work/workspace/dongfang2/houduan/backend.log)
- [qianduan/frontend.log](/F:/work/workspace/dongfang2/qianduan/frontend.log)

---

## 当前注意事项

- 当前交易日判断基于周一到周五和 A 股常规交易时段，暂未接入法定节假日日历
- 分时分析当前为按需抓取和内存缓存，尚未沉淀为分钟级历史库
- 第三阶段盘口分依赖本地 `stock-real` 覆盖率；如果盘后或本地缺少实盘快照，盘口分可能回退为中性分
- 第三阶段技术分依赖本地 `stock-kline` 覆盖率
- 当前仍以原始 JSON 存储为主，后续如果要做更强的回测和因子验证，建议继续推进结构化因子落库
