# 前端静态文件部署 - Nginx容器
# 基于轻量级的Nginx Alpine镜像

FROM nginx:alpine

# 设置维护者信息
LABEL maintainer="AI-RAG-Knowledge Team"
LABEL description="AI知识问答系统前端静态文件服务"

# 设置工作目录
WORKDIR /usr/share/nginx/html

# 删除默认的nginx静态文件
RUN rm -rf /usr/share/nginx/html/*

# 复制前端静态文件到nginx默认目录
COPY ../index.html /usr/share/nginx/html/
COPY ../chat.js /usr/share/nginx/html/
COPY ../src /usr/share/nginx/html/src/

# 设置文件权限
RUN chmod -R 755 /usr/share/nginx/html

# 暴露80端口
EXPOSE 80

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]