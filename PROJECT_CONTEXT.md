# AI 图片小红书文案生成平台 - 项目上下文

## 重要说明

这是一个正在开发中的 AI 应用项目。

请不要重新初始化环境。

请根据当前进度继续开发。

开发方式要求：

- 一次只完成一个小功能
- 每一步先解释原理
- 修改代码前说明修改哪些文件
- 完成后说明如何测试
- 遇到错误先分析原因，不要盲目修改


# 一、项目目标

项目名称：

AI 图片小红书文案生成平台


核心业务：

用户上传图片

↓

后端调用多模态 AI 模型识别图片内容

↓

生成小红书风格文案

↓

展示结果

↓

保存历史记录


核心功能：

1. 图片上传
2. 图片内容识别
3. AI生成标题
4. AI生成正文
5. AI生成话题标签
6. 历史记录保存


# 二、技术栈

## 前端

Vue 3

TypeScript

Vite


## 后端

Java

Spring Boot


## 数据库

MySQL 8.0


## 部署

Docker


## 版本管理

Git


# 三、开发原则

禁止：

- 硬编码数据库密码
- 硬编码模型 API Key
- 上传敏感配置文件


所有敏感信息：

必须使用环境变量。


例如：

数据库：

DB_URL

DB_USERNAME

DB_PASSWORD


模型：

MODEL_API_KEY


配置文件：

加入 .gitignore


# 四、已经完成的软件环境


## VS Code

状态：

已安装


用途：

- 前端开发
- 后端开发
- 终端操作


## Git

状态：

已安装


已经配置：

user.name = 朱长清


## Node.js

状态：

已安装


用途：

Vue 前端运行环境。


## Vue + TypeScript

状态：

已创建成功。


使用：

Vite


曾经运行成功：

http://localhost:5173/


## Java

状态：

JDK 17 已安装


版本：

openjdk version 17.0.20


## Maven

状态：

已安装


路径：

E:\DevTools\apache-maven-3.9.16


## Spring Boot

VS Code 插件：

已安装


# 五、Docker环境


Docker Desktop：

已安装


运行方式：

WSL2 Linux Engine


状态：

正常


已经验证：

docker info


Docker 数据位置：

E:\DockerData\DockerDesktopWSL


# 六、MySQL Docker状态


MySQL镜像：

mysql:8.0


容器：

mysql


当前状态：

正常运行


端口：

3306:3306


数据库登录：

root


密码：

来自：

.env


环境变量：

MYSQL_ROOT_PASSWORD=我的数据库密码（保存在 .env 中）


注意：

密码不能写入代码。


# 七、已经解决的问题


## MySQL root密码问题


现象：

root登录失败。


原因：

Docker MySQL 使用 Volume 保存初始化数据。

删除容器不会删除数据库数据。


解决：

重新初始化 MySQL 容器。


当前：

root登录成功。


# 八、当前开发进度


已完成：

✅ 开发环境准备

✅ Docker配置

✅ MySQL部署

✅ MySQL登录验证


当前阶段：

准备进入：

Spring Boot 后端开发。


下一步：

1. 创建 Spring Boot 项目
2. 配置 MySQL连接
3. 使用环境变量管理数据库配置
4. 创建数据库表
5. 开发后端接口


# 九、AI Agent工作要求


请像导师一样指导开发。

用户是 AI 初学者。


每一步说明：

1. 这是什么软件/环境
2. 对应项目哪个部分
3. 这个环境有什么作用
4. 背后的原理
5. 具体操作步骤


不要：

一次输出大量代码。

不要：

直接修改大量文件。


优先：

小步骤

解释

验证

再继续。


# 十、当前目标

完成：

Vue + TypeScript 前端

↓

Spring Boot 后端

↓

MySQL数据库

↓

AI模型调用

↓

图片生成小红书文案完整流程。

