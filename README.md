# dongfang2

## 项目简介
`dongfang2` 是一个面向东方财富行情数据的本地化看盘与分析系统，采用前后端分离架构：

- 前端：Vue 3 + TypeScript + Vite + Element Plus + ECharts
- 后端：Spring Boot 2.7 + Spring Web + Spring Data JPA
- 数据库：MySQL

这个系统的目标不是简单转发东方财富接口，而是把“抓取、反爬、入库、定时更新、缓存查询、实时分析、页面展示”整合成一套可持续运行的本地数据工作台，用于辅助发现当前更强、更优的股票标的。

---

## 当前功能总览
当前系统已经具备以下能力：

- 全量分页抓取行业板块基础行情
- 全量分页抓取股票池数据
- 按行业代码批量抓取行业历史 K 线
- 按股票代码批量抓取个股实盘数据与个股历史 K 线
- 将原始返回 JSON 持久化到 MySQL，并记录 `fetched_at`
- 所有查询接口统一返回数据库抓取时间 `_meta.fetchedAt`
- 前端页面直接展示数据库抓取时间，便于判断实时性
- 查询链路采用“优先返回本地缓存，过期后后台异步刷新”的策略
- 抓取链路具备基础反爬与稳定性保护，包括 Cookie、限速、随机延迟、重试、冷却、代理失败回退
- 定时任务具备“同类任务不重叠”的保护机制
- 新增实时选股分析页，可基于本地股票池、行业快照和核心指数生成候选股票列表
- 新增第二阶段分析能力，支持质量评分、风险标签、风险等级和分钟级分时强弱分析

---

## 页面功能
前端路由定义见 `qianduan/src/router/index.ts`。

### 1. 行业板块基础数据 `/industry-base`
页面文件：`qianduan/src/views/IndustryBase.vue`

功能包括：

- 展示行业板块基础行情总表
- 展示行业数量、最近数据库抓取时间等摘要信息
- 展示涨跌幅、成交额、主力净流入、上涨家数等字段
- 每一行提供“历史详情”按钮
- 点击后跳转到行业 K 线页面，并携带 `industryCode`

### 2. 行业板块 K 线 `/industry-kline`
页面文件：`qianduan/src/views/IndustryKline.vue`

功能包括：

- 根据 `industryCode` 查询行业历史 K 线
- 展示行业基础信息，如代码、名称、市场、记录数等
- 展示数据库抓取时间 `DB Fetched`
- 支持开始日期、结束日期筛选
- 展示开盘、收盘、最高、最低、成交量、成交额、振幅、涨跌幅、涨跌额、换手率等图表

### 3. 股票池 `/stock-pool`
页面文件：`qianduan/src/views/StockPool.vue`

功能包括：

- 展示全市场股票池分页数据
- 展示股票总数、当前页、最近数据库抓取时间
- 展示股票基础行情字段，如最新价、涨跌幅、成交额、市值、板块信息等
- 点击“实盘数据”后跳转到个股实盘页面
- 将股票代码和市场信息一起带到下游页面

### 4. 个股实盘数据 `/stock-real`
页面文件：`qianduan/src/views/StockReal.vue`

功能包括：

- 支持手动输入股票代码查询
- 支持从股票池页面跳转进入
- 展示个股基础信息和盘口摘要
- 展示买一到买五、卖一到卖五、委差、委比、量比、均价、换手率等数据
- 展示两个数据库时间：
  - `Quote Fetched`
  - `Kline Fetched`
- 同时展示个股历史 K 线图及相关指标图
- 支持日期筛选

### 5. 实时选股分析 `/stock-analysis`
页面文件：`qianduan/src/views/StockAnalysis.vue`

功能包括：

- 基于本地股票池、行业快照和核心指数生成候选股票列表
- 展示市场情绪分数、市场状态、热点行业、涨跌家数、涨停跌停数量等概览
- 输出候选股票综合评分、质量分、风险等级、风险标签和评分拆解
- 支持从候选列表直接跳转到个股实盘页
- 支持按选中股票查看分钟级分时分析
- 展示分时价格线、均价线、尾盘变化、均价偏离、收盘强度、放量情况等指标

### 6. 全局展示能力
相关文件：

- `qianduan/src/App.vue`
- `qianduan/src/style.css`
- `qianduan/src/utils/responseMeta.ts`

功能包括：

- 全局行情工作台式布局
- 统一卡片、表格、图表风格
- 统一解析后端 `_meta.fetchedAt`
- 统一格式化数据库抓取时间

---

## 后端接口能力
控制器文件：
`houduan/src/main/java/com/example/houduan/controller/EastMoneyController.java`

当前提供的本地接口如下：

- `GET /api/industry/base`
  - 返回行业板块基础行情
- `GET /api/industry/kline?industryCode=BKxxxx`
  - 返回指定行业历史 K 线
- `GET /api/stock/pool?pn=1`
  - 返回股票池指定页数据
- `GET /api/stock/real?stockCode=600000&market=1`
  - 返回指定个股实盘数据
- `GET /api/stock/kline?stockCode=600000&market=1`
  - 返回指定个股历史 K 线
- `GET /api/analysis/opportunities?limit=30`
  - 返回实时选股候选分析结果
- `GET /api/analysis/intraday?stockCode=600000&market=1`
  - 返回指定股票的分钟级分时分析结果

所有查询类接口都会附带：

- `_meta.fetchedAt`
- `_meta.isFresh`

注入逻辑见：
`houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java`

---

## 数据抓取与持久化能力
核心服务文件：

- `houduan/src/main/java/com/example/houduan/service/EastMoneyApiClient.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyStorageService.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java`
- `houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java`

当前后端具备以下数据抓取能力：

- 请求东方财富行业基础行情接口
- 请求东方财富行业历史 K 线接口
- 请求东方财富股票池接口
- 请求东方财富个股实盘接口
- 请求东方财富个股历史 K 线接口
- 请求东方财富分钟级分时趋势接口
- 请求核心指数实时行情接口
- 将原始 JSON 直接存入数据库
- 为每条抓取结果记录 `fetched_at`
- 从原始 JSON 中提取行业代码、股票代码、市场信息、总条数、总页数等关键字段

---

## 分析能力
分析服务文件：

- `houduan/src/main/java/com/example/houduan/service/StockOpportunityAnalysisService.java`
- `houduan/src/main/java/com/example/houduan/service/StockIntradayAnalysisService.java`

### 第一阶段：候选股票机会分析
`StockOpportunityAnalysisService` 当前会整合：

- 本地股票池快照
- 本地行业基础快照
- 上证指数、深证成指、创业板指、沪深 300 等核心指数

当前输出包括：

- 市场情绪分数
- 市场状态
- 涨跌家数、涨停跌停数量、平均涨跌幅
- 热点行业列表
- 候选股票综合评分
- 每只股票的评分拆解和入选理由

### 第二阶段：质量与风险分析
在第一阶段基础上，当前还补充了：

- 质量分
- 风险等级
- 风险标签
- 估值与市值维度的基础质量因子
- ST、负 PE、高波动、高换手、涨跌停等风险识别

### 分钟级分时分析
`StockIntradayAnalysisService` 当前支持：

- 按需抓取分钟级分时数据
- 本地内存缓存，减少重复请求
- 输出分时价格、均价、量能和金额序列
- 输出尾盘变化、近 30 分钟变化、近 60 分钟变化、均价偏离、收盘强度、午后强弱等指标
- 输出积极信号与风险标签

说明：

- 分钟级分时分析当前为“按需抓取 + 本地内存缓存”
- 这部分数据目前还没有正式落库到 MySQL

---

## 查询策略
查询逻辑由
`houduan/src/main/java/com/example/houduan/service/EastMoneyQueryService.java`
实现。

当前行为：

- 优先从数据库读取最新结果
- 如果数据仍在新鲜窗口内，直接返回
- 如果数据已过期，但数据库里已有旧数据，则先返回旧数据，同时后台异步刷新
- 如果数据库中完全没有数据，则同步抓取并入库后返回

当前新鲜窗口配置：

- `collector.query-freshness-minutes=60`

配置文件：
`houduan/src/main/resources/application.properties`

---

## 定时任务
定时任务实现位于：
`houduan/src/main/java/com/example/houduan/service/EastMoneyCollectorService.java`

### 1. 行业基础数据

- 配置：`collector.industry-base.cron=0 */15 * * * *`
- 频率：每 15 分钟
- 策略：
  - 分页抓取行业基础数据
  - 根据第一页返回的 `total` 自动计算总页数
  - 将所有页合并成一份完整行业快照后入库
- 用途：
  - 供行业基础页面查询
  - 供 `industry-kline` 定时任务提取全部行业代码

### 2. 股票池

- 配置：`collector.stock-pool.cron=30 */15 * * * *`
- 频率：每 15 分钟
- 策略：
  - 先抓第一页
  - 读取 `total`
  - 按 `collector.stock-pool.page-size=100` 计算总页数
  - 当前 `collector.max-stock-pool-pages=0`，表示不限制页数，按总页数全量抓取
  - 每页单独存一条记录
- 用途：
  - 供股票池页面分页查询
  - 供 `stock-detail` 定时任务按页拼出全量股票代码集合

### 3. 行业 K 线

- 配置：`collector.industry-kline.cron=0 0 */6 * * *`
- 频率：每 6 小时
- 策略：
  - 读取最新行业基础快照
  - 提取全部行业代码
  - 逐个行业抓取历史 K 线并入库

### 4. 个股明细与个股 K 线

- 配置：`collector.stock-detail.cron=0 10 */6 * * *`
- 频率：每 6 小时
- 策略：
  - 先读取股票池第 1 页的最新记录，作为参考快照时间
  - 再按页读取“该时间点及之前最近的一条记录”，避免分页快照新旧混杂
  - 从所有页中提取股票代码和市场信息
  - 汇总并按股票代码去重
  - 对每只股票分别抓取：
    - `stock-real`
    - `stock-kline`

### 5. 不重叠保护
所有定时任务都具备“同类任务不重叠”保护：

- 如果上一轮同类任务还没执行完，下一轮会直接跳过

---

## 反爬与稳定性能力
抓取工具相关文件：

- `houduan/src/main/java/com/example/houduan/utils/ClaudeEastMoneyApiUtil.java`
- `houduan/src/main/java/com/example/houduan/config/CrawlerConfig.java`

当前已实现能力：

- Cookie 初始化与定期刷新
- User-Agent 池
- 请求最小间隔控制
- 随机延迟
- 请求失败重试
- 频控冷却
- 封禁冷却
- 连续失败保护冷却
- 代理支持
- 代理失败后的直连回退
- 单代理冷却
- 分页抓取过程中的主动休息

当前主要配置如下：

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
analysis.market-index.cache-ms=120000
analysis.intraday.cache-ms=120000
```

---

## 数据库表
数据库建表脚本：
`houduan/src/main/resources/db/migration/V1__create_eastmoney_tables.sql`

当前表包括：

- `industry_base_response`
- `industry_kline_response`
- `stock_pool_response`
- `stock_real_response`
- `stock_kline_response`
- `crawl_job_log`

说明：

- 前五张表用于存放原始抓取结果和 `fetched_at`
- `crawl_job_log` 用于记录每次定时任务执行结果，不存行情本体

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
├─ README.md                    # 当前说明文档
└─ README2.md                   # 原始需求说明
```

---

## 技术栈

### 前端
来源：`qianduan/package.json`

- Vue `^3.5.25`
- Vue Router `^4.6.4`
- Element Plus `^2.13.2`
- ECharts `^6.0.0`
- Axios `^1.13.5`
- TypeScript `~5.9.3`
- Vite `^7.3.1`

### 后端
来源：`houduan/pom.xml`

- Java 17
- Spring Boot `2.7.18`
- Spring Web
- Spring Data JPA
- MySQL Connector/J `8.0.33`
- Flyway 依赖保留，但当前运行配置关闭

---

## 启动方式

### 后端启动
工作目录：`houduan`

前台启动：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
.\mvnw.cmd spring-boot:run
```

后台启动并写日志：

```powershell
Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot" && cd /d F:\work\workspace\dongfang2\houduan && .\mvnw.cmd spring-boot:run > backend.log 2>&1' -WorkingDirectory 'F:\work\workspace\dongfang2\houduan'
```

### 前端启动
工作目录：`qianduan`

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

常用测试接口：

- [http://localhost:8080/api/industry/base](http://localhost:8080/api/industry/base)
- [http://localhost:8080/api/industry/kline?industryCode=BK0428](http://localhost:8080/api/industry/kline?industryCode=BK0428)
- [http://localhost:8080/api/stock/pool?pn=1](http://localhost:8080/api/stock/pool?pn=1)
- [http://localhost:8080/api/stock/real?stockCode=600000&market=1](http://localhost:8080/api/stock/real?stockCode=600000&market=1)
- [http://localhost:8080/api/stock/kline?stockCode=600000&market=1](http://localhost:8080/api/stock/kline?stockCode=600000&market=1)
- [http://localhost:8080/api/analysis/opportunities?limit=10](http://localhost:8080/api/analysis/opportunities?limit=10)
- [http://localhost:8080/api/analysis/intraday?stockCode=600000&market=1](http://localhost:8080/api/analysis/intraday?stockCode=600000&market=1)

---

## 日志文件

常见日志位置：

- `houduan/backend.log`
- `qianduan/frontend.log`

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

---

## 当前已落地的关键改造
相对于最初的原型版本，当前系统已经完成以下重要改造：

- 股票池从单页样本改为全量分页抓取
- 行业基础数据从单页样本改为全量分页抓取
- 个股接口支持基于市场信息构造正确 `secid`
- 查询接口支持返回数据库抓取时间
- 前端页面统一展示数据库抓取时间
- 个股实盘页支持手工输入股票代码
- 查询链路采用“缓存优先 + 过期后台刷新”
- 抓取链路增加了多层反爬和稳定性保护
- 股票池分页读取改为尽量锚定同一轮快照时间，减少页间新旧混杂
- 新增实时选股分析页，支持候选股票综合评分
- 新增质量评分、风险标签、风险等级
- 新增分钟级分时分析能力，并已体现在页面中

---

## 注意事项

- 当前数据库账号、代理地址等环境配置直接写在 `houduan/src/main/resources/application.properties` 中，适合本地开发，不建议直接用于生产环境
- `spring.flyway.enabled=false`，表示当前运行时不自动执行 Flyway 迁移
- 股票池和行业基础都已经按分页全量抓取，但单轮任务仍依赖外部接口可用性
- `stock-detail` 读取的是“股票池各页同一参考时间点及之前最近记录”拼出的全量股票集合，而不是扫描整张历史表
- 分钟级分时分析当前为按需抓取和内存缓存，尚未沉淀为可回测的分钟级数据库
