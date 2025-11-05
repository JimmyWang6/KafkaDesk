# KafkaDesk
A desktop client tool for Kafka, offering visualized functions for Kafka cluster management and operations.

## 功能特性

- **集群管理**: 支持多集群配置和管理
- **主题管理**: 查看、创建和删除 Kafka 主题
- **消息生产**: 可视化界面发送消息到 Kafka
- **消息消费**: 实时消费和查看 Kafka 消息
- **跨平台**: 支持 Windows、macOS 和 Linux

## 技术栈

- Java 17+
- JavaFX 17+
- Apache Kafka Clients 3.6.0
- Maven

## 构建和运行

### 前置要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本

### 构建项目

```bash
mvn clean package
```

### 运行应用

```bash
cd kafkadesk-ui
mvn javafx:run
```

或者运行打包后的 JAR:

```bash
java -jar kafkadesk-ui/target/kafkadesk-ui-1.0.0-SNAPSHOT.jar
```

## 快速开始

1. 启动应用程序
2. 点击"添加集群"按钮
3. 输入集群名称和 Bootstrap Servers (例如: localhost:9092)
4. 在左侧集群树中选择集群进行连接
5. 在主题管理、消息生产、消息消费标签页中进行操作

## 项目结构

```
KafkaDesk/
├── kafkadesk-model/     # 数据模型
├── kafkadesk-utils/     # 工具类
├── kafkadesk-core/      # 核心服务
└── kafkadesk-ui/        # JavaFX 用户界面
```

## 配置文件

配置文件位于: `~/.kafkadesk/config.json`

## 许可证

Apache License 2.0
