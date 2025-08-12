

## 二、一键部署脚本

小傅哥，这里为你准备一键安装 Docker 环境的脚本文件，你可以非常省心的完成 Docker 部署。使用方式如下。

<div align="center">
    <img src="https://bugstack.cn/images/roadmap/tutorial/road-map-docker-install-02.png" width="650px">
</div>

- **地址**：<https://github.com/fuzhengwei/xfg-dev-tech-docker-install>
- **地址**：<https://gitcode.com/Yao__Shun__Yu/xfg-dev-tech-docker-install>

本文档介绍如何执行项目中的各个脚本，包括权限设置和执行步骤。操作视频：[https://www.bilibili.com/video/BV1oaNazEEf5](https://www.bilibili.com/video/BV1oaNazEEf5)

### 1. 脚本权限设置

在执行任何脚本之前，需要先为脚本文件添加可执行权限：

```
# 为所有脚本添加可执行权限
chmod +x environment/jdk/install-java.sh
chmod +x environment/jdk/remove-java.sh
chmod +x run_install_docker_local.sh
chmod +x run_install_software.sh
chmod +x install-maven.sh
chmod +x remove-maven.sh

```
或者一次性为所有脚本添加权限：

```
find . -name "*.sh" -type f -exec chmod +x {} \;
```

### 2. JDK 安装脚本

#### 2.1 安装 JDK

脚本位置： environment/jdk/install-java.sh

功能： 支持安装 JDK 8 和 JDK 17

执行方式：

```
# 交互式安装（推荐）
sudo ./environment/jdk/install-java.sh

# 指定版本安装
sudo ./environment/jdk/install-java.sh -v 8    # 安装 JDK 8
sudo ./environment/jdk/install-java.sh -v 17   # 安装 JDK 17

# 强制安装（覆盖已有安装）
sudo ./environment/jdk/install-java.sh -f -v 8

# 静默安装
sudo ./environment/jdk/install-java.sh -q -v 8

# 自定义安装目录
sudo ./environment/jdk/install-java.sh -d /opt/java -v 8
```
注意事项：

- 需要 root 权限执行
- 脚本会提示手动下载 JDK 包到 /dev-ops/java 目录
- 支持的版本：JDK 8 (1.8.0_202) 和 JDK 17 (17.0.14)
- 安装完成后环境变量会自动配置

#### 2.2 卸载 JDK

脚本位置： environment/jdk/remove-java.sh

功能： 彻底清理 JDK 安装和环境配置

执行方式：

```
# 交互式删除（推荐）
sudo ./environment/jdk/remove-java.sh

# 强制删除
sudo ./environment/jdk/remove-java.sh -f

# 静默删除
sudo ./environment/jdk/remove-java.sh -f -q

# 指定安装目录删除
sudo ./environment/jdk/remove-java.sh -d /opt/java

# 删除时不备份配置文件
sudo ./environment/jdk/remove-java.sh --no-backup
```
注意事项：

- 需要 root 权限执行
- 会自动备份配置文件（除非使用 --no-backup）
- 清理系统和用户级环境变量配置

#### 2.3 Maven 安装脚本

##### 2.3.1 安装 Maven

脚本位置：`environment/maven/install-maven.sh`

功能：自动安装 Apache Maven 3.8.8

执行方式：

```bash
# 交互式安装（推荐）
sudo ./environment/maven/install-maven.sh

# 自定义安装目录
sudo ./environment/maven/install-maven.sh -d /opt/maven

# 使用本地Maven包
sudo ./environment/maven/install-maven.sh -p /path/to/apache-maven-3.8.8.zip

# 强制安装（覆盖已有安装）
sudo ./environment/maven/install-maven.sh -f

# 静默安装
sudo ./environment/maven/install-maven.sh -q

# 强制静默安装
sudo ./environment/maven/install-maven.sh -f -q
```

##### 2.3.2 卸载 Maven

```bash
# 交互式删除（推荐）
sudo ./environment/jdk/remove-maven.sh

# 强制删除
sudo ./environment/jdk/remove-maven.sh -f

# 静默删除
sudo ./environment/jdk/remove-maven -f -q
```

### 3. Docker 安装脚本

脚本位置： run_install_docker_local.sh

功能： 使用本地的 install_docker.sh 脚本安装 Docker

执行方式：

```
# 执行 Docker 安装
./run_install_docker_local.sh
```
注意事项：

- 脚本会自动检查 install_docker.sh 文件是否存在
- 如果需要 root 权限会自动请求
- 安装完成后会询问是否安装 Portainer 容器管理界面
- Portainer 访问地址： http://服务器IP:9000

### 4. 软件安装脚本

脚本位置： run_install_software.sh

功能： 使用 Docker Compose 安装各种开发软件

执行方式：

```
# 执行软件安装
sudo ./run_install_software.sh
```

支持的软件：

- nacos - 服务注册与发现
- mysql - 数据库
- phpmyadmin - MySQL 管理界面
- redis - 缓存数据库
- redis-admin - Redis 管理界面
- rabbitmq - 消息队列
- elasticsearch - 搜索引擎
- logstash - 日志处理
- kibana - 日志分析界面
- xxl-job-admin - 任务调度
- prometheus - 监控系统
- grafana - 监控面板
- ollama - AI 模型服务
- pgvector - 向量数据库
- pgvector-admin - 向量数据库管理界面
  注意事项：

- 需要 root 权限执行
- 需要先安装 Docker 和 docker-compose
- 脚本会检查磁盘空间并显示预计占用
- 支持选择原始配置或阿里云镜像配置
- 可以多选软件进行批量安装

### 5. 常见问题

#### 5.1 权限问题

如果遇到权限拒绝错误：

```
# 确保脚本有执行权限
ls -la *.sh
# 如果没有 x 权限，重新添加
chmod +x script_name.sh
```

#### 5.2 环境变量生效

JDK 安装后，环境变量在当前会话中已生效，新开终端需要：

```
# 重新加载配置
source /etc/profile
# 或者重新登录
```

#### 5.3 Docker 相关

确保 Docker 服务正在运行：

```
# 检查 Docker 状态
sudo systemctl status docker
# 启动 Docker 服务
sudo systemctl start docker
```

### 6. 执行顺序建议

1. 首先安装 JDK （如果需要）：

   ```
   sudo ./environment/jdk/install-java.sh -v 8
   ```
   
2. 然后安装 Docker ：

   ```
   ./run_install_docker_local.sh
   ```

3. 然后安装 Docker ：

   ```
   ./install-maven.sh
   ```
   
4. 最后安装开发软件 ：

   ```
   sudo ./run_install_software.sh
   ```
   按照以上步骤，您就可以成功执行所有脚本并搭建完整的开发环境。


