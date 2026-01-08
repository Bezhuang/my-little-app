# My Little App 全栈项目

<div align="center">

**版本**: v1.0.0

[![Java](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3-yellow)](https://vuejs.org/)
[![uni-app](https://img.shields.io/badge/uniapp-3-orange)](https://uniapp.dcloud.io/)

</div>

## 项目简介

My Little App 是一个基于前后端分离架构的全栈应用系统，支持多端部署（移动端 H5、微信小程序、管理后台 Web）。系统提供了完整的用户认证、管理功能，并采用现代化的技术栈进行开发。

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            客户端层                                      │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────┐  │
│  │   H5/小程序端        │  │   管理后台 Web        │  │   第三方接入     │  │
│  │ my-little-app-      │  │ my-little-app-      │  │     (API)       │  │
│  │    frontend         │  │     admin           │  │                 │  │
│  │ (uni-app + Vue3)    │  │ (Vue3 + Element)    │  │                 │  │
│  │   + Node.js         │  │   + Node.js         │  │                 │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────────────┘  │
└─────────────┼────────────────────────┼──────────────────────────────────┘
              │                        │
              ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       网关/中间件层                                      │
│  ┌─────────────────────┐              ┌─────────────────────────────┐   │
│  │       Nginx         │              │          JWT Auth           │   │
│  │  (反向代理/负载均衡)  │              │      (Token 验证/鉴权)       │   │
│  └─────────────────────┘              └─────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          应用服务层 (API)                                │
│                   my-little-app-backend (Spring Boot)                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Spring Security │ WebFlux HTTP Client │ Druid │ MyBatis │ Redis │   │
│  │ (认证授权)      │ (HTTP客户端)         │ (连接池)│ (ORM)   │ (缓存) │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            数据持久层                                    │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                      MySQL 5.7+                                   │  │
│  │                                                                   │  │
│  │   user │ admin │ role │ permission │ user_role │ operation_log    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 技术栈概览

| 层级             | 技术选型        | 版本   | 说明             |
| ---------------- | --------------- | ------ | ---------------- |
| **后端基础框架** | Spring Boot     | 3.4.1  | 企业级后端框架   |
| **编程语言**     | Java            | 17     | JDK 版本要求 17+ |
| **数据库**       | MySQL           | 5.7+   | 关系型数据库     |
| **连接池**       | Druid           | 1.2.23 | 数据库连接池     |
| **ORM 框架**     | MyBatis         | 3.0.4  | 持久层框架       |
| **缓存层**       | Redis           | 7.x    | 缓存             |
| **安全框架**     | Spring Security | -      | 认证授权         |
| **认证令牌**     | JWT (jjwt)      | 0.12.6 | 无状态认证       |
| **数据校验**     | Validation      | -      | 参数校验         |
| **邮件服务**     | Spring Mail     | -      | 邮件发送         |
| **HTTP 客户端**  | WebFlux         | -      | API 调用         |
| **移动端框架**   | uni-app         | 3.0    | 跨平台开发       |
| **管理后台 UI**  | Vue             | 3.x    | 响应式框架       |
| **管理后台组件** | Element Plus    | 2.13   | UI 组件库        |
| **状态管理**     | Pinia           | 3.0    | Vue 状态管理     |
| **路由管理**     | Vue Router      | 4.6    | 路由配置         |
| **HTTP 客户端**  | Axios           | 1.13   | HTTP 请求        |
| **构建工具**     | Maven           | 3.8+   | 后端构建工具     |
| **前端构建**     | Vite            | 7.x    | 前端构建工具     |
| **国际化**       | vue-i18n        | 9.1    | 多语言支持       |

## 项目结构

### 1. 后端服务 (my-little-app-backend)

```
my-little-app-backend/
├── pom.xml                                    # Maven 构建配置
└── src/
    ├── main/
    │   ├── java/com/bezhuang/my_little_app_backend/
    │   │   ├── config/                        # 配置类
    │   │   ├── controller/                    # REST 控制器
    │   │   ├── entity/                        # 实体类
    │   │   ├── mapper/                        # MyBatis Mapper
    │   │   ├── service/                       # 服务层
    │   │   │   └── impl/                      # 服务实现
    │   │   ├── dto/                           # 数据传输对象
    │   │   └── utils/                         # 工具类
    │   └── resources/
    │       ├── application.properties         # 主配置（敏感）
    │       ├── application.properties.template # 配置模板
    │       └── mapper/                        # SQL 映射文件
    └── test/                                  # 测试代码
```

### 2. 移动端前端 (my-little-app-frontend)

```
my-little-app-frontend/
├── package.json                               # pnpm 配置
├── vite.config.js                             # Vite 构建配置
├── manifest.json                              # uni-app 配置
└── src/
    ├── components/                            # 公共组件
    ├── pages/                                 # 页面
    ├── static/                                # 静态资源
    ├── stores/                                # Pinia 状态管理
    ├── utils/                                 # 工具函数
    ├── App.vue                                # 根组件
    └── main.js                                # 入口文件
```

### 3. 管理后台 (my-little-app-admin)

```
my-little-app-admin/
├── package.json                               # pnpm 配置
├── vite.config.js                             # Vite 构建配置
├── index.html                                 # HTML 模板
└── src/
    ├── api/                                   # API 接口封装
    ├── components/                            # 公共组件
    ├── layouts/                               # 布局组件
    ├── router/                                # Vue Router 配置
    ├── stores/                                # Pinia 状态管理
    ├── views/                                 # 页面视图
    ├── App.vue                                # 根组件
    └── main.js                                # 入口文件
```

## 子系统说明

### 1. my-little-app-backend（后端服务）

| 属性     | 值                                                              |
| -------- | --------------------------------------------------------------- |
| 框架     | Spring Boot 3.4.1                                               |
| Java     | 17                                                              |
| 打包方式 | JAR (my-little-app-backend-0.0.1-SNAPSHOT.jar)                  |
| 端口     | 8080 (默认)                                                     |
| 主要依赖 | Spring Security, MyBatis, JWT, Druid, Validation, Mail, WebFlux |

### 2. my-little-app-frontend（移动端）

| 属性       | 值                                                                                              |
| ---------- | ----------------------------------------------------------------------------------------------- |
| 框架       | uni-app 3.0 + Vue 3                                                                             |
| 构建工具   | Vite 5.x                                                                                        |
| 支持平台   | H5, 微信小程序, 支付宝小程序, 百度小程序, 字节跳动小程序, 快手小程序, 小红书, QQ 小程序, 鸿蒙等 |
| 样式预处理 | Sass                                                                                            |
| 国际化     | vue-i18n                                                                                        |

### 3. my-little-app-admin（管理后台）

| 属性        | 值                   |
| ----------- | -------------------- |
| 框架        | Vue 3 + Element Plus |
| 状态管理    | Pinia                |
| 路由        | Vue Router 4         |
| 构建工具    | Vite 7.x             |
| HTTP 客户端 | Axios                |

## 初始化配置

本节包含项目启动前所有必要的配置步骤。

### 1. 数据库初始化

项目提供了 `my-little-app-backend/src/main/resources/db/init.sql` 文件用于初始化数据库。

**初始化数据库：**

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/your/project/my-little-app-backend/src/main/resources/db/init.sql
```

或者使用 MySQL 客户端工具直接导入 init.sql 文件。

> **⚠️ 超级管理员配置说明：**
> init.sql 中 t_admin 表预置了超级管理员账号，相关信息（用户名、邮箱、密码（md5加密后））在数据库初始化时写入，**无法通过后台界面修改**。如需修改，请直接编辑 init.sql 文件后重新初始化数据库。


### 2. 后端配置

```bash
# 复制配置模板
cd my-little-app-backend/src/main/resources
cp application.properties.template application.properties

# 编辑配置（修改数据库连接、密钥等）
vim application.properties
```

### 3. 前端配置（后端 API 地址）

**admin** 和 **frontend** 访问后端的 `base_url` 都在 `@utils/constants.js` 文件中，按需修改：

| 文件路径                                        | 配置项     |
| ----------------------------------------------- | ---------- |
| `my-little-app-admin/src/utils/constants.js`    | `BASE_URL` |
| `my-little-app-frontend/src/utils/constants.js` | `BASE_URL` |

```javascript
// 开发环境示例
export const BASE_URL = 'http://localhost:8080/api'
```

## 环境要求

### 基础环境

| 工具    | 版本要求 | 说明          |
| ------- | -------- | ------------- |
| JDK     | 17+      | Java 运行环境 |
| Node.js | 18+      | 前端运行环境  |
| MySQL   | 5.7+     | 数据库        |
| Maven   | 3.8+     | 后端构建工具  |
| pnpm    | 8+       | 前端包管理    |

## 调试方法

### 1. 环境准备

```bash
# 克隆项目
git clone <repository-url>
cd my-little-app

# 安装后端依赖
cd my-little-app-backend
./mvnw install

# 安装前端依赖
cd ../my-little-app-frontend
pnpm install

cd ../my-little-app-admin
pnpm install
```

### 2. 启动服务

```bash
# 后端服务
cd my-little-app-backend
./mvnw spring-boot:run -Pdev

# 移动端 H5
cd my-little-app-frontend
pnpm run dev:h5

# 管理后台
cd my-little-app-admin
pnpm run dev
```

## 部署方法

### 后端部署

```bash
# 构建
cd my-little-app-backend
./mvnw clean package -DskipTests

# 运行
java -jar target/my-little-app-backend-0.0.1-SNAPSHOT.jar

# 后台运行
nohup java -jar target/my-little-app-backend-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
```

### 前端部署

**H5/管理后台**:
```bash
# 构建
cd my-little-app-frontend
pnpm run build:h5

# 部署
# 将 dist 目录部署到 Nginx
```

**微信小程序**:
```bash
cd my-little-app-frontend
pnpm run build:mp-weixin
# 上传至微信公众平台
```

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /var/www/my-little-app/admin;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Docker 部署（可选）

```dockerfile
# 后端 Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/my-little-app-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# 构建镜像
docker build -t my-little-app-backend ./my-little-app-backend

# 运行容器
docker run -d -p 8080:8080 --name backend my-little-app-backend
```

## 开发指南

### 安全注意事项

1. **敏感信息保护**
   - `application.properties` 已加入 `.gitignore`，切勿提交
   - 生产环境必须修改默认密钥（JWT、数据库密码等）
   - 定期轮换 API Key 和数据库密码

2. **生产环境配置**
   - 关闭 MyBatis SQL 日志 (`log-impl`)
   - 配置合适的日志级别 (INFO/WARN)
   - 启用 HTTPS
   - 配置 CORS 白名单

3. **依赖安全**
   - 定期检查依赖漏洞：`./mvnw audit`
   - 使用最新稳定版本
   - 关注 Spring Security 更新

### 开发规范

| 类型         | 规范                                                  |
| ------------ | ----------------------------------------------------- |
| **后端命名** | 类名大驼峰，方法名/变量名小驼峰，常量全大写下划线分隔 |
| **前端命名** | 组件文件大驼峰，使用 kebab-case                       |
| **API 风格** | RESTful 设计，统一响应格式 `{ code, message, data }`  |
| **分支策略** | main → develop → feature/*, hotfix/*, release/*       |

### 数据库设计

| 规范         | 说明                                    |
| ------------ | --------------------------------------- |
| **表命名**   | 小写字母 + 下划线，如 `user_role`       |
| **字段命名** | 小驼峰，如 `userName`                   |
| **主键**     | `id` (BIGINT)                           |
| **时间字段** | `create_time`, `update_time` (DATETIME) |
| **软删除**   | `is_deleted` (TINYINT)                  |

**核心数据表**：user, admin, role, permission, user_role, role_permission, operation_log

**ID冲突解决**：t_admin 表最多 4 个管理员（ID 1-4），由应用逻辑强制约束。t_user 表用户 ID 从 5 开始自增（5, 6, 7...）

### 多端兼容

1. **移动端**
   - 使用 uni-app API，注意平台差异
   - 微信小程序需配置 AppID
   - H5 端无需配置即可运行

2. **数据库迁移**
   - 生产环境使用 Flyway/Liquibase 管理
   - 重大变更需备份数据库
   - 遵循先测试后上线的原则

3. **版本要求**
   - JDK 版本必须 17+
   - Node.js 版本 18+
   - Maven 版本 3.8+

### 常见问题

| 问题               | 解决方案                                                       |
| ------------------ | -------------------------------------------------------------- |
| 后端启动失败       | 检查 MySQL 服务、`application.properties` 配置、控制台错误日志 |
| 前端无法连接后端   | 检查 CORS 配置、后端服务、API 地址配置                         |
| 微信小程序真机调试 | 配置合法域名、检查 HTTPS、确认 AppID 正确                      |

## AI Tool Calling 功能说明

本项目集成了 DeepSeek API 的原生 Function Calling（函数调用）功能，支持 AI 模型在对话中调用外部工具获取实时信息。

### 可用工具

| 工具名称           | 功能说明                   | 触发场景                                             |
| ------------------ | -------------------------- | ---------------------------------------------------- |
| `get_current_time` | 获取当前日期和时间         | 用户询问"现在几点了"、"今天是几号"等问题             |
| `web_search`       | 联网搜索互联网获取实时信息 | 用户需要实时新闻、知识库外的内容时（需开启联网搜索） |

### 工具控制机制

**前端控制开关**：
- 移动端前端（`my-little-app-frontend`）聊天界面提供"联网搜索"开关
- 开关开启时，AI 可使用 `web_search` 工具
- 开关关闭时，AI 无法使用 `web_search` 工具

**AI 自主决策**：
- AI 根据用户问题自主判断是否需要调用工具
- 无需预搜索逻辑，AI 自行决定工具调用时机

### 第三方 API

#### 1. DeepSeek API
- 官方文档：https://platform.deepseek.com/api-docs
- 用于 AI 对话生成和工具调用

#### 2. 博查AI Web Search API
- 官方文档：https://bocha-ai.feishu.cn/wiki/RXEOw02rFiwzGSkd9mUcqoeAnNK
- 用于实现 `web_search` 工具的联网搜索功能

## 版本历史

| 版本   | 日期       | 说明         |
| ------ | ---------- | ------------ |
| v1.0.0 | 2025-12-31 | 初始版本发布 |

## License

本项目仅供学习和研究使用。

---