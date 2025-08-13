# AI知识问答系统 - 后端服务Docker部署文件
# 基于OpenJDK 17的Spring Boot应用

# 使用官方OpenJDK 17镜像
FROM openjdk:17-jdk-slim

# 维护者信息
LABEL maintainer="AI Knowledge System Backend"
LABEL description="AI知识问答系统后端服务"
LABEL version="1.0"

# 设置工作目录
WORKDIR /app

# 创建应用用户（安全最佳实践）
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 安装必要的系统工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制jar包到容器中
# 注意：jar包需要提前放置在deployment目录下
COPY ./ai-rag-knowledge-app.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 切换到应用用户
USER appuser

# 暴露应用端口
EXPOSE 8087

# 健康检查已移除

# 设置JVM参数和启动应用
# 包含用户指定的jasypt加密密码参数
ENTRYPOINT ["java", \
    "-Djasypt.encryptor.password=lijiaqiang1024@wt1314520", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dspring.profiles.active=prod", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=Asia/Shanghai", \
    "-jar", "app.jar"]