# 小红书 AI 文案生成系统

一个基于图片内容生成小红书风格文案的前后端项目。用户可以上传本地图片或输入公开图片 URL，后端通过 Qwen 视觉模型理解图片，并生成图片分析、标题、正文和话题标签。

## 核心功能

- 上传本地图片并预览
- 使用公开图片 URL 作为输入
- 支持 JPEG、PNG、WebP
- 单张图片最大 10MB
- 图片文件头与 Content-Type 校验
- 图片 URL 超时、重定向和 SSRF 安全检查
- Qwen 视觉模型图片分析
- 生成小红书标题、正文和 3～5 个话题标签
- 生成任务状态管理：`PROCESSING`、`COMPLETED`、`FAILED`
- 生成结果持久化到 MySQL
- 区分图片过大、格式错误、URL 无法访问、AI 失败和网络错误

## 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- 原生 Fetch API

### 后端

- Java 17
- Spring Boot 3.3.5
- Maven
- Spring Web
- Spring JDBC / `JdbcTemplate`
- Spring `RestClient`
- `springboot3-dotenv`

### 数据与运行环境

- MySQL 8.0
- Docker Desktop
- 本地文件系统图片存储
- 阿里云百炼 OpenAI 兼容接口 / Qwen 视觉模型

项目没有使用 JPA、Hibernate、MyBatis、Flyway、Liquibase 或 AI SDK。

## 项目结构

```text
E:\AI-project
├── .env                         # 本地敏感配置，不提交 Git
├── .env.example                 # 根目录环境变量示例
├── PROJECT_CONTEXT.md           # 项目背景与开发上下文
├── README.md
├── uploads\                     # 运行时图片存储目录
├── backend\
│   ├── pom.xml
│   └── src\
│       ├── main\
│       │   ├── java\com\example\xhscopywriting\
│       │   │   ├── config\
│       │   │   ├── controller\
│       │   │   ├── dto\
│       │   │   ├── exception\
│       │   │   ├── model\
│       │   │   ├── repository\
│       │   │   └── service\
│       │   └── resources\
│       │       ├── application.yml
│       │       └── schema.sql
│       └── test\
└── xiaohongshu-ai-frontend\
    ├── package.json
    └── src\
        ├── components\
        ├── services\
        ├── types\
        └── views\
```

## 环境变量

后端启动时会自动读取项目根目录：

```text
E:\AI-project\.env
```

可以根据根目录 `.env.example` 准备本地配置：

```dotenv
AI_PROVIDER=qwen

QWEN_API_KEY=your_qwen_api_key
QWEN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_MODEL=your_qwen_vision_model

DB_URL=jdbc:mysql://localhost:3306/xiaohongshu_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
DB_USERNAME=root
MYSQL_ROOT_PASSWORD=replace-with-your-local-password
UPLOAD_DIR=uploads
```

| 变量 | 用途 |
| --- | --- |
| `DB_URL` | MySQL JDBC 连接地址 |
| `DB_USERNAME` | MySQL 用户名 |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 密码及后端数据库密码 |
| `UPLOAD_DIR` | 图片保存目录，可使用相对路径 `uploads` |
| `AI_PROVIDER` | AI 实现，可选 `mock` 或 `qwen`；未配置时默认 `mock` |
| `QWEN_API_KEY` | 阿里云百炼 API Key，仅在 `qwen` 模式下使用 |
| `QWEN_BASE_URL` | 阿里云百炼 OpenAI 兼容接口地址 |
| `QWEN_MODEL` | 当前账号可用的 Qwen 视觉模型名称 |

不要把真实密码或 API Key 写入源码，也不要提交真实 `.env`。

## Docker MySQL

项目使用 Docker 中的 MySQL 8.0，不使用本机手动安装的 MySQL。当前环境使用：

- 容器名称：`mysql`
- 镜像：`mysql:8.0`
- 端口映射：`3306:3306`
- 业务数据库：`xiaohongshu_ai`
- 密码来源：项目根目录 `.env` 中的 `MYSQL_ROOT_PASSWORD`

电脑重启后，先启动 Docker Desktop，再启动现有 MySQL 容器：

```powershell
docker start mysql
```

可以检查容器状态：

```powershell
docker ps
```

数据库由 Docker MySQL 环境提供，不需要手工执行 `CREATE DATABASE`。后端配置了 Spring SQL 初始化，应用启动时会自动使用 `backend/src/main/resources/schema.sql` 检查并初始化所需表，不需要用户手工执行该文件。

## 启动后端

启动前确认：

1. Docker Desktop 已运行。
2. `mysql` 容器已启动。
3. 根目录 `.env` 已配置数据库信息。
4. 使用真实 Qwen 时，已配置 `AI_PROVIDER=qwen`、`QWEN_API_KEY` 和 `QWEN_MODEL`。

执行完整命令：

```powershell
cd E:\AI-project\backend
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

修改后端代码后需要重新启动正在运行的 Spring Boot 进程，旧进程不会自动加载新接口或新逻辑。

## 启动前端

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

前端默认访问地址：

```text
http://localhost:5173
```

开发环境中的 `/api` 请求会由 Vite 转发到 `http://localhost:8080`，因此使用真实生成流程时需要同时启动后端。

## 使用流程

### 本地图片

```text
选择本地图片
→ POST /api/generations 创建任务
→ POST /api/generations/{id}/image 上传图片
→ ImageStorageService 保存图片
→ Qwen 视觉分析与文案生成
→ GET /api/generations/{id} 查询结果
```

### 图片 URL

```text
输入公开图片 URL
→ POST /api/generations 创建任务
→ POST /api/generations/{id}/generate 异步触发
→ 下载并校验图片
→ ImageStorageService 保存图片
→ Qwen 视觉分析与文案生成
→ GET /api/generations/{id} 轮询结果
```

图片 URL 必须是可公开访问的 HTTP/HTTPS 地址，并且响应类型必须是 `image/jpeg`、`image/png` 或 `image/webp`。

## 测试与构建

后端测试：

```powershell
cd E:\AI-project\backend
mvn test
```

最近一次完整验证结果：

```text
Tests run: 45
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

测试环境使用 Mock AI 或 HTTP 测试替身，不会自动调用真实 Qwen API。

前端生产构建：

```powershell
cd E:\AI-project\xiaohongshu-ai-frontend
npm run build
```

最近一次验证结果：

```text
vue-tsc 类型检查通过
Vite 构建成功
32 modules transformed
```

## 当前范围

当前版本已经完成图片输入、图片 URL 输入、视觉理解、文案生成、任务状态和结果持久化。暂未包含登录、用户系统、历史记录页面和管理后台。
