@echo off
chcp 65001 >nul
echo ========================================
echo   AI知识问答系统 - Docker部署脚本
echo ========================================
echo.

:: 检查Docker是否安装
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到Docker，请先安装Docker Desktop
    echo 下载地址：https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

:: 检查Docker Compose是否可用
echo 检查Docker Compose...
docker compose version >nul 2>&1
if %errorlevel% neq 0 (
    docker-compose --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ❌ 错误：未检测到Docker Compose
        pause
        exit /b 1
    ) else (
        set COMPOSE_CMD=docker-compose
    )
) else (
    set COMPOSE_CMD=docker compose
)

echo ✅ Docker环境检查通过
echo.

:: 检查JAR包是否存在
if not exist "backend\ai-rag-knowledge-app-1.0.jar" (
    echo ❌ 错误：未找到JAR包文件
    echo 请确保以下文件存在：
    echo    backend\ai-rag-knowledge-app-1.0.jar
    echo.
    echo 💡 构建JAR包的步骤：
    echo    1. 在项目根目录执行：mvn clean package -DskipTests
    echo    2. 复制JAR包：copy ai-rag-knowledge-app\target\ai-rag-knowledge-app-1.0.jar deployment\backend\
    echo.
    pause
    exit /b 1
)

echo ✅ JAR包文件检查通过
echo.

:: 检查端口占用
echo 🔍 检查端口占用情况...
netstat -an | findstr ":80 " >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  警告：端口80已被占用，可能会导致前端服务启动失败
)

netstat -an | findstr ":8087 " >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  警告：端口8087已被占用，可能会导致后端服务启动失败
)

echo.
echo 🚀 开始部署服务...
echo.

:: 停止可能存在的旧服务
echo 📋 停止旧服务...
%COMPOSE_CMD% down >nul 2>&1

:: 构建并启动服务
echo 🔨 构建并启动服务...
%COMPOSE_CMD% up -d --build

if %errorlevel% neq 0 (
    echo ❌ 部署失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo ⏳ 等待服务启动...
timeout /t 10 /nobreak >nul

:: 检查服务状态
echo 📊 检查服务状态...
%COMPOSE_CMD% ps

echo.
echo ✅ 部署完成！
echo.
echo 🌐 访问地址：
echo    前端页面：http://localhost
echo    后端API：http://localhost:8087/api/v1/
echo    健康检查：http://localhost/health
echo.
echo 📋 常用命令：
echo    查看日志：%COMPOSE_CMD% logs -f
echo    停止服务：%COMPOSE_CMD% down
echo    重启服务：%COMPOSE_CMD% restart
echo.
echo 📖 详细文档请查看：README.md
echo.
pause