# 小红书 AI 文案生成系统

## 项目简介

一个基于 Qwen Vision 图片理解能力的小红书文案生成平台。项目包含产品官网、AI 文案工作台、用户认证、个人历史记录和管理员后台，支持通过本地图片或公开图片 URL 生成不同风格的小红书标题、正文和话题标签。

## 功能列表

### 产品与内容生成

- 产品官网首页与功能介绍
- 本地图片上传、预览和重新选择
- 公开图片 URL 输入
- 支持 JPEG、PNG、WebP，单张图片最大 10MB
- 图片文件头校验、URL 超时与 SSRF 安全检查
- Qwen Vision 图片内容理解
- 生成图片分析、小红书标题、正文和 3～5 个话题标签
- 文案风格选择：日常分享、种草推荐、专业测评、情绪治愈、高级简约
- 任务状态闭环：`PROCESSING`、`COMPLETED`、`FAILED`
- 区分图片过大、格式错误、URL 无法访问、AI 失败和网络错误

### 用户与历史记录

- 本地用户注册、登录和退出
- BCrypt 密码加密存储
- JWT 无状态身份认证与过期校验
- `USER`、`ADMIN` 两级角色
- Generation 与用户绑定
- 普通用户只能查看自己的历史生成记录
- 历史记录详情、图片、标题、正文和标签展示

### 管理后台

- 仅 `ADMIN` 可以访问 `/admin` 和 `/api/admin/*`
- Dashboard 数据概览
- 用户总数、生成记录总数、今日生成次数
- 今日活跃用户统计：当天创建过生成任务的去重用户数
- 用户列表：用户名、角色、注册时间
- 全部生成记录：所属用户、图片信息、标题、状态、创建时间
- 未登录访问后台接口返回 HTTP 401
- 普通用户访问后台接口返回 HTTP 403

## 技术架构

### 前端

- Vue 3
- TypeScript
- Vite
- 原生 Fetch API
- `localStorage` 登录状态管理

### 后端

- Java 17
- Spring Boot 3.3.5
- Maven
- Spring Web
- Spring JDBC / `JdbcTemplate`
- Spring `RestClient`
- Spring Security Crypto / BCrypt
- 自实现 HMAC-SHA256 JWT
- `springboot3-dotenv`

### 数据库与部署

- MySQL 8.0
- Docker Desktop / Docker MySQL
- 本地文件系统图片存储
- Spring SQL 初始化

### AI

- 阿里云百炼 OpenAI 兼容接口
- Qwen Vision 视觉模型
- Base64 Data URL 图片输入
- Mock / Qwen Provider 配置切换

项目没有使用 JPA、Hibernate、MyBatis、Flyway、Liquibase 或第三方 AI SDK。

## 项目结构

```text
E:\AI-project
├── .env                              # 本地敏感配置，不提交 Git
├── .env.example                      # 环境变量示例
├── PROJECT_CONTEXT.md                # 项目背景与开发上下文
├── README.md
├── uploads\                          # 运行时图片存储目录
├── backend\
│   ├── admin-user.sql.example        # 管理员初始化 SQL 模板
│   ├── pom.xml
│   └── src\
│       ├── main\
│       │   ├── java\com\example\xhscopywriting\
│       │   │   ├── config\           # 应用配置
│       │   │   ├── controller\       # 普通、认证、管理员接口
│       │   │   ├── dto\              # 请求与响应 DTO
│       │   │   ├── exception\        # 业务异常与统一处理
│       │   │   ├── model\            # Generation、User 等模型
│       │   │   ├── repository\       # JdbcTemplate 数据访问
│       │   │   ├── security\         # JWT 与当前用户解析
│       │   │   └── service\          # 业务、AI、存储与后台服务
│       │   └── resources\
│       │       ├── application.yml
│       │       └── schema.sql
│       └── test\                     # 单元测试与集成测试
└── xiaohongshu-ai-frontend\
    ├── package.json
    ├── vite.config.ts                # 开发环境 /api 代理
    └── src\
        ├── components\               # 上传、风格、导航、后台布局
        ├── services\                 # Auth、Generation、Admin API
        ├── stores\                   # 简单认证状态
        ├── types\                    # TypeScript 类型
        └── views\                    # 官网、工作台、历史、认证、后台
```

## 环境准备

当前已验证的开发环境：

| 环境 | 版本或要求 |
| --- | --- |
| Java | JDK 17；当前验证版本 17.0.20 |
| Maven | 当前验证版本 3.9.16 |
| Node.js | 当前验证版本 24.19.0 |
| npm | 当前验证版本 11.17.0 |
| MySQL | MySQL 8.0，通过 Docker 运行 |
| Docker | Docker Desktop，WSL2 Linux Engine |

项目使用 Docker 中的 MySQL，不要求另外安装或手工启动本机 MySQL 服务。

## 环境变量

Spring Boot 启动时会自动读取项目根目录：

```text
E:\AI-project\.env
```

根据根目录 `.env.example` 准备本地配置：

```dotenv
AI_PROVIDER=qwen

QWEN_API_KEY=your_qwen_api_key
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_MODEL=your_qwen_vision_model

JWT_SECRET=replace-with-at-least-32-random-characters
JWT_EXPIRATION_MS=3600000

DB_URL=jdbc:mysql://localhost:3306/xiaohongshu_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
DB_USERNAME=root
MYSQL_ROOT_PASSWORD=replace-with-your-local-password
UPLOAD_DIR=uploads
```

| 变量 | 用途 |
| --- | --- |
| `DB_URL` | `xiaohongshu_ai` 业务数据库 JDBC 地址 |
| `DB_USERNAME` | MySQL 用户名 |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 密码及后端连接密码 |
| `UPLOAD_DIR` | 图片保存目录，可使用相对路径 `uploads` |
| `JWT_SECRET` | JWT HMAC 签名密钥，至少 32 个字符 |
| `JWT_EXPIRATION_MS` | JWT 有效期，默认 3600000 毫秒 |
| `AI_PROVIDER` | AI Provider，可选 `mock` 或 `qwen`，默认 `mock` |
| `QWEN_API_KEY` | 阿里云百炼 API Key，仅在 `qwen` 模式使用 |
| `QWEN_BASE_URL` | 阿里云百炼 OpenAI 兼容接口地址 |
| `QWEN_MODEL` | 当前账号可用的 Qwen 视觉模型名称 |

不要把数据库密码、JWT 密钥或 Qwen API Key 写入源码，也不要提交真实 `.env`。

## 启动方式

### 1. 启动 Docker Desktop

先打开 Docker Desktop，等待 Docker Engine 正常运行。

### 2. 启动 MySQL 容器

当前项目使用已有的 `mysql` 容器：

```powershell
docker start mysql
docker ps
```

当前数据库环境：

- 镜像：`mysql:8.0`
- 容器名称：`mysql`
- 端口：`3306:3306`
- 业务数据库：`xiaohongshu_ai`
- 密码来源：根目录 `.env`

数据库由 Docker MySQL 环境提供，不需要手工执行 `CREATE DATABASE`。应用启动时会通过 `schema.sql` 检查并初始化所需表，不要求手工执行该文件。

### 3. 启动后端

确认 Docker MySQL 和根目录 `.env` 已准备好，然后执行：

```powershell
cd E:\AI-project\backend
mvn spring-boot:run
```

启动成功后访问：

```text
http://localhost:8080
```

修改后端代码后必须重新启动 Spring Boot，旧进程不会自动加载新增接口。

### 4. 启动前端

第一次运行时安装依赖：

```powershell
cd E:\AI-project\xiaohongshu-ai-frontend
npm install
```

之后启动开发服务器：

```powershell
cd E:\AI-project\xiaohongshu-ai-frontend
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 会把 `/api` 请求代理到 `http://localhost:8080`。修改 `vite.config.ts` 后需要重启前端开发服务器。

## 核心使用流程

### 本地图片生成

```text
用户登录
→ 选择图片和文案风格
→ POST /api/generations 创建用户任务
→ POST /api/generations/{id}/image 上传图片
→ ImageStorageService 校验并保存图片
→ Qwen Vision 分析图片并生成文案
→ 结果保存到 MySQL
→ 前端查询并展示结果
```

### 图片 URL 生成

```text
用户登录
→ 输入公开图片 URL 和文案风格
→ POST /api/generations 创建用户任务
→ POST /api/generations/{id}/generate 异步触发
→ 下载并校验远程图片
→ 复用 ImageStorageService 保存流程
→ Qwen Vision 分析并生成文案
→ 前端轮询最终结果
```

图片 URL 必须使用 HTTP/HTTPS，并返回 `image/jpeg`、`image/png` 或 `image/webp`，最大 10MB。

## 用户认证说明

认证接口：

| 接口 | 用途 |
| --- | --- |
| `POST /api/auth/register` | 注册普通用户 |
| `POST /api/auth/login` | 登录并获取 JWT |
| `POST /api/auth/logout` | 无状态退出确认 |

- 普通注册始终创建 `USER`，前端不能传入角色或 `user_id`。
- 密码通过 BCrypt 加密后保存，数据库不保存明文密码。
- 登录成功返回 JWT、用户名和角色。
- 前端把 JWT 保存到 `localStorage`，调用受保护接口时发送 `Authorization: Bearer <token>`。
- 后端验证 JWT 签名和过期时间，并根据用户名重新读取数据库用户身份。
- Generation 创建时绑定数据库中的用户 ID；历史列表只查询当前用户数据。

## 管理后台说明

管理员页面：

| 页面 | 功能 |
| --- | --- |
| `/admin` | Dashboard 与用量统计 |
| `/admin/users` | 用户列表与角色 |
| `/admin/generations` | 全部生成记录管理 |

管理员接口：

| 接口 | 功能 |
| --- | --- |
| `GET /api/admin/access` | 管理员权限检查 |
| `GET /api/admin/dashboard` | 用户数、生成数、今日生成、今日活跃用户 |
| `GET /api/admin/users` | 查询用户列表 |
| `GET /api/admin/generations` | 查询全部生成记录 |

当前开发数据库已创建用户名为 `admin` 的管理员账号。管理员密码属于本地开发凭据，不写入 README 或源码。

普通注册用户不能自行成为管理员。创建新的管理员时：

1. 使用 Spring `BCryptPasswordEncoder` 生成密码哈希。
2. 复制并填写 `backend/admin-user.sql.example`。
3. 手工向 `users` 表插入 `role=ADMIN` 的记录。
4. 不要把明文密码写入 SQL 文件或 Git。

## 测试与构建

### 后端测试

```powershell
cd E:\AI-project\backend
mvn test
```

最近一次完整验证：

```text
Tests run: 68
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

测试环境使用 Mock AI 或 HTTP 测试替身，不会自动请求真实 Qwen API。测试覆盖数据库持久化、图片处理、URL 下载、AI 成功与失败、认证、用户隔离、管理员权限、统计、用户列表和生成记录管理。

### 前端构建

```powershell
cd E:\AI-project\xiaohongshu-ai-frontend
npm run build
```

最近一次验证：

```text
vue-tsc 类型检查通过
Vite 构建成功
62 modules transformed
```

## 当前版本状态

当前版本已完成官网、图片与图片 URL 文案生成、文案风格、历史记录、用户认证、JWT、用户数据隔离、管理员权限、Dashboard、用户管理、生成记录管理和基础用量统计，形成了完整的课程项目 MVP。
