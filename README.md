# 项目说明

## 项目结构
```
dongfang2/
├── houduan/          # 后端项目
├── qianduan/         # 前端项目
├── README.md         # 原始README文件
└── README2.md        # 本说明文件
```

## 前端项目 (qianduan)

### 环境搭建
- **Node.js**：需要安装Node.js环境
- **包管理器**：npm

### 技术栈及版本
| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.5.25 | 前端框架 |
| vue-router | ^4.6.4 | 路由管理 |
| element-plus | ^2.13.2 | UI组件库 |
| echarts | ^6.0.0 | 图表库 |
| axios | ^1.13.5 | HTTP客户端 |
| typescript | ~5.9.3 | 类型系统 |
| vite | ^7.3.1 | 构建工具 |

### 项目结构
```
qianduan/
├── public/           # 静态资源
├── src/
│   ├── assets/       # 资源文件
│   ├── components/   # 组件
│   ├── router/       # 路由配置
│   ├── views/        # 页面
│   │   ├── IndustryBase.vue     # 行业板块基础数据
│   │   ├── IndustryKline.vue    # 行业板块 K 线数据
│   │   ├── StockPool.vue        # 读取股票池
│   │   └── StockReal.vue        # 实盘委托买卖数据
│   ├── App.vue       # 根组件
│   ├── main.ts       # 入口文件
│   └── style.css     # 全局样式
├── package.json      # 项目配置
└── vite.config.ts    # Vite配置
```

### 路由配置
| 路径 | 名称 | 组件 | 标题 |
|------|------|------|------|
| / | - | 重定向到 /industry-base | - |
| /industry-base | IndustryBase | IndustryBase.vue | 行业板块基础数据 |
| /industry-kline | IndustryKline | IndustryKline.vue | 行业板块 K 线数据 |
| /stock-real | StockReal | StockReal.vue | 实盘委托买卖数据 |
| /stock-pool | StockPool | StockPool.vue | 读取股票池 |

### 命令
- **开发环境启动**：`npm run dev`
- **构建生产版本**：`npm run build`
- **预览生产版本**：`npm run preview`

## 后端项目 (houduan)

### 环境搭建
- **JDK**：Java 1.8
- **构建工具**：Maven

### 技术栈及版本
| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 后端框架 |
| Spring Web | - | Web支持 |
| Spring Test | - | 测试支持 |

### 项目结构
```
houduan/
├── src/
│   ├── main/
│   │   ├── java/com/example/houduan/
│   │   │   ├── controller/      # 控制器
│   │   │   │   └── EastMoneyController.java
│   │   │   ├── utils/           # 工具类
│   │   │   │   └── EastMoneyApiUtil.java
│   │   │   └── HouduanApplication.java  # 应用入口
│   │   └── resources/
│   │       └── application.properties    # 配置文件
│   └── test/                    # 测试代码
└── pom.xml                      # Maven配置
```

### 主要组件
1. **EastMoneyController**：控制器，处理前端请求
2. **EastMoneyApiUtil**：工具类，处理东方财富API相关操作
3. **HouduanApplication**：应用主类，启动Spring Boot应用

### 命令
- **构建项目**：`mvn clean package`
- **运行项目**：`java -jar target/houduan-0.0.1-SNAPSHOT.jar`
- **使用Maven运行**：`mvn spring-boot:run`

## 项目启动流程

### 1. 启动后端服务
1. 进入后端目录：`cd houduan`
2. 运行项目：`mvn spring-boot:run`

### 2. 启动前端服务
1. 进入前端目录：`cd qianduan`
2. 安装依赖（首次运行）：`npm install`
3. 启动开发服务器：`npm run dev`

### 3. 访问项目
- 前端服务默认地址：`http://localhost:5173`
- 后端API默认地址：`http://localhost:8080`

## 构建部署

### 前端构建
1. 进入前端目录：`cd qianduan`
2. 执行构建：`npm run build`
3. 构建产物位于 `dist` 目录

### 后端构建
1. 进入后端目录：`cd houduan`
2. 执行构建：`mvn clean package`
3. 构建产物位于 `target` 目录
