# AI知识问答系统 - Docker部署指南

## 📋 部署概述

本文档提供AI知识问答系统的完整Docker部署方案，包含前端静态页面和后端Spring Boot服务的容器化部署。

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   用户浏览器     │───▶│  Nginx前端服务   │───▶│ Spring Boot后端  │
│   (Port 80)     │    │   (Port 80)     │    │   (Port 8087)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📁 目录结构

```
deployment/
├── README.md                    # 本部署文档
├── docker-compose.yml           # Docker Compose配置文件
├── backend/                     # 后端服务配置
│   ├── Dockerfile              # 后端Docker镜像构建文件
│   └── ai-rag-knowledge-app-1.0.jar  # ⚠️ 需要手动放置的JAR包
├── config/                      # 配置文件目录
│   └── nginx.conf              # Nginx反向代理配置
├── frontend/                    # 前端配置目录（预留）
└── logs/                        # 日志目录（自动创建）
```

## ⚠️ 重要提醒：JAR包准备

**在开始部署之前，您必须完成以下步骤：**

### 1. 构建JAR包

在项目根目录执行以下命令构建JAR包：

```bash
# 清理并编译项目
mvn clean package -DskipTests

# 或者包含测试
mvn clean package
```

### 2. 复制JAR包到部署目录

构建完成后，将生成的JAR包复制到deployment目录：

```bash
# 从构建目录复制JAR包
cp ai-rag-knowledge-app/target/ai-rag-knowledge-app-1.0.jar deployment/backend/
```

**📌 注意事项：**
- JAR包名称必须为：`ai-rag-knowledge-app-1.0.jar`
- JAR包必须放置在：`deployment/backend/` 目录下
- 如果JAR包名称不同，请修改 `deployment/backend/Dockerfile` 中的对应行

## 🚀 部署步骤

### 前置要求

1. **Docker 和 Docker Compose**
   ```bash
   # 检查Docker版本
   docker --version
   # 新版Docker Desktop使用以下命令
   docker compose version
   # 或者旧版docker-compose
   docker-compose --version
   ```
   
   **注意**: 如果系统中没有安装Docker，请先安装Docker Desktop:
   - Windows: 下载并安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
   - 安装完成后重启系统
   - 确保Docker服务正在运行

2. **系统要求**
   - Docker Engine 20.10+
   - Docker Compose 2.0+
   - 至少2GB可用内存
   - 端口80和8087未被占用

### 1. 验证JAR包

确保JAR包已正确放置：

```bash
ls -la deployment/backend/ai-rag-knowledge-app-1.0.jar
```

### 2. 启动服务

在deployment目录下执行：

```bash
# 进入部署目录
cd deployment

# 构建并启动所有服务（新版Docker Desktop）
docker compose up -d --build

# 或者使用旧版docker-compose命令
# docker-compose up -d --build
```

### 3. 验证部署

```bash
# 查看服务状态
docker compose ps
# 或 docker-compose ps

# 查看服务日志
docker compose logs -f
# 或 docker-compose logs -f

# 检查后端健康状态
curl http://localhost:8087/api/v1/health

# 检查前端访问
curl http://localhost:80
```

## 🔧 配置说明

### JVM参数配置

后端服务已配置以下JVM参数：

```bash
-Djasypt.encryptor.password=lijiaqiang1024@wt1314520  # 加密配置密码
-Xms512m                                              # 初始堆内存
-Xmx1024m                                             # 最大堆内存
-XX:+UseG1GC                                          # 使用G1垃圾收集器
-XX:MaxGCPauseMillis=200                              # GC暂停时间目标
-XX:+UseContainerSupport                              # 容器支持
-XX:MaxRAMPercentage=75.0                             # 最大内存使用比例
-Dspring.profiles.active=prod                         # 生产环境配置
```

### 端口映射

- **前端服务**: `http://localhost:80`
- **后端API**: `http://localhost:8087/api/v1/`
- **健康检查**: `http://localhost:80/health`

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| SPRING_PROFILES_ACTIVE | prod | Spring Boot环境配置 |
| TZ | Asia/Shanghai | 时区设置 |
| JAVA_OPTS | (见上方JVM参数) | Java虚拟机参数 |

## 📊 监控和日志

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs backend
docker-compose logs frontend

# 实时查看日志
docker-compose logs -f --tail=100
```

### 健康检查

系统提供多层健康检查：

1. **Docker容器健康检查**：自动检测服务状态
2. **应用健康检查**：`/api/v1/health` 端点
3. **Nginx健康检查**：前端服务可用性检测

## 🛠️ 常见问题排查

### 1. 后端服务启动失败

**可能原因**：
- JAR包未正确放置
- 端口8087被占用
- 内存不足

**解决方案**：
```bash
# 检查JAR包
ls -la deployment/backend/ai-rag-knowledge-app-1.0.jar

# 检查端口占用
netstat -tlnp | grep 8087

# 查看详细错误日志
docker-compose logs backend
```

### 2. 前端无法访问后端API

**可能原因**：
- 网络配置问题
- Nginx配置错误
- 后端服务未启动

**解决方案**：
```bash
# 检查网络连接
docker-compose exec frontend ping backend

# 检查Nginx配置
docker-compose exec frontend nginx -t

# 重启前端服务
docker-compose restart frontend
```

### 3. 内存不足

**解决方案**：
```bash
# 调整JVM内存参数（修改Dockerfile）
-Xms256m -Xmx512m

# 重新构建
docker-compose up -d --build
```

## 🔄 服务管理

### 启动服务
```bash
docker-compose up -d
```

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart
```

### 更新服务
```bash
# 重新构建并启动
docker-compose up -d --build

# 仅重新构建特定服务
docker-compose up -d --build backend
```

### 清理资源
```bash
# 停止并删除容器、网络
docker-compose down

# 同时删除数据卷
docker-compose down -v

# 删除镜像
docker-compose down --rmi all
```

## 🔒 安全建议

1. **生产环境部署**：
   - 修改默认的jasypt加密密码
   - 使用HTTPS协议
   - 配置防火墙规则
   - 定期更新Docker镜像

2. **网络安全**：
   - 仅暴露必要的端口
   - 使用反向代理
   - 配置访问控制

3. **数据安全**：
   - 定期备份数据
   - 使用加密存储
   - 监控访问日志

## 📞 技术支持

如果在部署过程中遇到问题，请：

1. 查看本文档的常见问题排查部分
2. 检查Docker和Docker Compose版本
3. 确认系统资源充足
4. 查看详细的错误日志

---

**部署完成后，您可以通过 http://localhost 访问AI知识问答系统！** 🎉