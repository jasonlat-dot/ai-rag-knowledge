#!/bin/bash

# AI知识问答系统 - Docker部署脚本
# 支持Linux和macOS系统

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_header() {
    echo "========================================"
    echo "   AI知识问答系统 - Docker部署脚本"
    echo "========================================"
    echo
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        return 1
    fi
    return 0
}

# 检查Docker环境
check_docker() {
    print_info "检查Docker环境..."
    
    if ! check_command docker; then
        print_error "未检测到Docker，请先安装Docker"
        echo "安装指南：https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # 检查Docker Compose是否可用
    if command -v "docker compose" &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        print_error "未检测到Docker Compose"
        echo "安装指南：https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    # 检查Docker服务是否运行
    if ! docker info &> /dev/null; then
        print_error "Docker服务未运行，请启动Docker服务"
        exit 1
    fi
    
    print_success "Docker环境检查通过"
}

# 检查JAR包
check_jar() {
    print_info "检查JAR包文件..."
    
    if [ ! -f "backend/ai-rag-knowledge-app.jar" ]; then
        print_error "未找到JAR包文件"
        echo "请确保以下文件存在："
        echo "    backend/ai-rag-knowledge-app.jar"
        echo
        echo "💡 构建JAR包的步骤："
        echo "    1. 在项目根目录执行：mvn clean package -DskipTests"
        echo "    2. 复制JAR包：cp ai-rag-knowledge-app/target/ai-rag-knowledge-app.jar deployment/backend/"
        echo
        exit 1
    fi
    
    print_success "JAR包文件检查通过"
}

# 检查端口占用
check_ports() {
    print_info "检查端口占用情况..."
    
    if lsof -i :80 &> /dev/null; then
        print_warning "端口80已被占用，可能会导致前端服务启动失败"
    fi
    
    if lsof -i :8087 &> /dev/null; then
        print_warning "端口8087已被占用，可能会导致后端服务启动失败"
    fi
}

# 部署服务
deploy_services() {
    print_info "开始部署服务..."
    echo
    
    # 停止可能存在的旧服务
    print_info "停止旧服务..."
    $COMPOSE_CMD down &> /dev/null || true
    
    # 构建并启动服务
    print_info "构建并启动服务..."
    if ! $COMPOSE_CMD up -d --build; then
        print_error "部署失败，请检查错误信息"
        exit 1
    fi
    
    echo
    print_info "等待服务启动..."
    sleep 10
    
    # 检查服务状态
    print_info "检查服务状态..."
    $COMPOSE_CMD ps
}

# 验证部署
verify_deployment() {
    echo
    print_info "验证部署状态..."
    
    # 检查后端健康状态
    if curl -f http://localhost:8087/api/v1/health &> /dev/null; then
        print_success "后端服务健康检查通过"
    else
        print_warning "后端服务健康检查失败，请查看日志"
    fi
    
    # 检查前端访问
    if curl -f http://localhost &> /dev/null; then
        print_success "前端服务访问正常"
    else
        print_warning "前端服务访问失败，请查看日志"
    fi
}

# 显示部署结果
show_result() {
    echo
    print_success "部署完成！"
    echo
    echo "🌐 访问地址："
    echo "    前端页面：http://localhost"
    echo "    后端API：http://localhost:8087/api/v1/"
    echo "    健康检查：http://localhost/health"
    echo
    echo "📋 常用命令："
    echo "    查看日志：$COMPOSE_CMD logs -f"
    echo "    停止服务：$COMPOSE_CMD down"
    echo "    重启服务：$COMPOSE_CMD restart"
    echo
    echo "📖 详细文档请查看：README.md"
    echo
}

# 主函数
main() {
    print_header
    
    # 检查是否在正确的目录
    if [ ! -f "docker-compose.yml" ]; then
        print_error "请在deployment目录下运行此脚本"
        exit 1
    fi
    
    check_docker
    check_jar
    check_ports
    deploy_services
    verify_deployment
    show_result
}

# 错误处理
trap 'print_error "部署过程中发生错误，退出码: $?"' ERR

# 执行主函数
main "$@"