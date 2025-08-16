# AI知识问答系统 - 部署指南

## 📋 完整部署流程

### 第一步：获取源代码
```bash
# 克隆项目代码
git clone https://github.com/jasonlat-dot/ai-rag-knowledge

```

### 第二步：后端打包
```bash
# 进入目录
cd ai-rag-knowledge

# 使用Maven打包（确保已安装Java 8+和Maven）可以在本地打包再上传到服务器
mvn clean package -DskipTests

# 将生成的jar包重命名并移动到backend文件夹
```

### 第三步：准备部署文件
```bash
# 回到项目根目录
cd ..

# 确认目录结构
# deployment/
# ├── backend/
# │   └── ai-rag-knowledge-app.jar
# |   └── Dockerfile
# └── web/
#     └── deplay/
```

### 第四步：上传到服务器
```bash
# 将deployment目录和backend目录一起上传到服务器
# 使用scp命令（替换your-server为实际服务器地址）
scp -r deployment/ user@your-server:/path/to/
```

### 第五步：服务器部署
```bash
# 登录服务器
ssh user@your-server

# 进入部署目录
cd /path/to/deployment/stream-rag-ui/deplay

# 给脚本添加执行权限
chmod +x deploy.sh

# 确保Docker和Docker Compose已安装
docker --version
docker-compose --version

# 运行部署脚本
./deploy.sh
```

## 🚀 一键部署（服务器端）

### Linux/Mac服务器
```bash
# 运行部署脚本
./deploy.sh
```

### Windows服务器
```bash
# 运行部署脚本
deploy.bat
```

## 📋 部署选项

1. **仅部署前端** - Vue单页应用，访问 http://localhost
2. **部署完整系统** - 前端+后端，前端 http://localhost，后端API http://localhost:8080
3. **停止所有服务** - 清理所有运行的容器
4. **查看服务状态** - 检查容器运行状态
5. **查看服务日志** - 实时监控服务日志

## 🔧 环境要求

### 开发环境
- Java 17+
- Maven 3.6+
- Node.js 18+ （本地运行需要 Linux 不需要，是通过docker部署的）
- Git

### 服务器环境
- Docker Desktop (Windows/Mac) 或 Docker Engine (Linux)
- Docker Compose
- 足够的磁盘空间（建议2GB+）

## 📁 文件说明

- `deploy.bat` - Windows一键部署脚本
- `deploy.sh` - Linux/Mac一键部署脚本
- `docker-compose.yml` - Docker Compose配置
- `Dockerfile` - Vue前端Docker构建文件
- `nginx.conf` - Nginx服务器配置
- `DEPLOYMENT.md` - 详细部署文档

## 🆘 常见问题

**Q: Docker未安装怎么办？**
A: 环境脚本在 ai-rag-knowledge/docs/dev-ops 文件夹下
```ssh
   cd ai-rag-knowledge/docs/dev-ops
   # 给予权限
   chmod +x *.sh
   # 安装docker 和 docker-compose
   ./run_install_docker.sh 或 ./run_install_docker_local.sh
   # 安装其他的软件
   ./run_install_software.sh
```

**Q: 端口被占用怎么办？**
A: 修改 `docker-compose.yml` 中的端口映射，或停止占用端口的服务

**Q: 后端jar包找不到怎么办？**
A: 确保backend目录在deployment目录的下级，且jar包名为ai-rag-knowledge-app.jar

**Q: Maven打包失败怎么办？**
A: 检查Java版本，确保使用Java 17+，并检查网络连接

**Q: 如何查看详细日志？**
A: 运行部署脚本选择选项5，或使用 `docker-compose logs -f`

**Q: 服务器权限不够怎么办？**
A: 使用sudo运行Docker命令，或将用户添加到docker组

---

更多详细信息请查看 [DEPLOYMENT.md](deplay/DEPLOYMENT.md)