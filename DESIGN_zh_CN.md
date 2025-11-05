# KafkaDesk 设计文档 v1.0

## 1. 项目概述

### 1.1 项目简介
KafkaDesk 是一款基于 Java 和 JavaFX 开发的 Kafka 桌面客户端工具，旨在为开发者和运维人员提供可视化的 Kafka 集群管理和操作界面。通过直观的图形界面，用户可以轻松管理 Kafka 集群、主题、消费者组等资源，并进行消息的生产和消费操作。

### 1.2 核心目标
- 提供友好的图形化界面，降低 Kafka 使用门槛
- 支持多集群管理，方便开发和生产环境切换
- 实现消息的可视化生产和消费
- 提供集群监控和性能分析功能
- 跨平台支持（Windows、macOS、Linux）

### 1.3 目标用户
- Kafka 开发人员
- 系统运维人员
- 数据工程师
- 测试人员

## 2. 技术架构

### 2.1 技术栈
- **开发语言**: Java 17+
- **UI 框架**: JavaFX 17+
- **构建工具**: Maven
- **Kafka 客户端**: Apache Kafka Clients 3.6+
- **JSON 处理**: Jackson
- **日志框架**: SLF4J + Logback
- **单元测试**: JUnit 5

### 2.2 架构设计

#### 2.2.1 整体架构
```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  (JavaFX Views, Controllers, FXML)              │
├─────────────────────────────────────────────────┤
│              Business Logic Layer                │
│  (Service Layer, Business Models)               │
├─────────────────────────────────────────────────┤
│              Data Access Layer                   │
│  (Kafka Client Wrapper, Configuration Manager)  │
├─────────────────────────────────────────────────┤
│              External Dependencies               │
│  (Kafka Cluster, Local Storage)                 │
└─────────────────────────────────────────────────┘
```

#### 2.2.2 模块划分
1. **UI 模块** (kafkadesk-ui)
   - 主窗口和布局管理
   - 集群管理界面
   - 主题管理界面
   - 消费者组管理界面
   - 消息生产界面
   - 消息消费界面
   - 设置界面

2. **核心服务模块** (kafkadesk-core)
   - 集群连接服务
   - 主题管理服务
   - 消费者组管理服务
   - 消息生产服务
   - 消息消费服务
   - 配置管理服务

3. **数据模型模块** (kafkadesk-model)
   - 集群配置模型
   - 主题信息模型
   - 消费者组模型
   - 消息模型

4. **工具模块** (kafkadesk-utils)
   - 序列化/反序列化工具
   - 日期时间工具
   - 字符串处理工具

## 3. 核心功能设计

### 3.1 集群管理

#### 3.1.1 功能描述
- 添加/编辑/删除集群连接配置
- 测试集群连接
- 查看集群信息（版本、节点、控制器等）
- 支持多集群管理和快速切换

#### 3.1.2 数据模型
```java
public class ClusterConfig {
    private String id;
    private String name;
    private String bootstrapServers;
    private String saslMechanism;
    private String securityProtocol;
    private Map<String, String> properties;
    private boolean autoConnect;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 3.1.3 界面设计
- 左侧：集群列表树形视图
- 右侧：集群详情面板
- 工具栏：添加、编辑、删除、刷新按钮

### 3.2 主题管理

#### 3.2.1 功能描述
- 查看主题列表
- 创建新主题（配置分区数、副本数等）
- 删除主题
- 查看主题详细信息（分区、副本、配置）
- 修改主题配置
- 增加分区数

#### 3.2.2 数据模型
```java
public class TopicInfo {
    private String name;
    private int partitions;
    private int replicationFactor;
    private Map<String, String> config;
    private List<PartitionInfo> partitionDetails;
}

public class PartitionInfo {
    private int partition;
    private Node leader;
    private List<Node> replicas;
    private List<Node> isr;
}
```

#### 3.2.3 界面设计
- 主区域：主题列表表格（名称、分区数、副本数）
- 详情面板：显示选中主题的详细信息
- 工具栏：创建、删除、刷新、搜索功能

### 3.3 消息生产

#### 3.3.1 功能描述
- 选择目标主题和分区
- 支持手动输入消息内容
- 支持批量发送消息
- 支持多种消息格式（JSON、文本、Avro）
- 支持自定义 Key 和 Headers
- 发送结果反馈

#### 3.3.2 数据模型
```java
public class ProducerMessage {
    private String topic;
    private Integer partition;
    private String key;
    private String value;
    private Map<String, String> headers;
    private MessageFormat format;
}

public enum MessageFormat {
    JSON, TEXT, AVRO, XML
}
```

#### 3.3.3 界面设计
- 顶部：主题选择、分区选择
- 中部：消息编辑器（支持语法高亮）
- 底部：Key 输入、Headers 配置
- 右侧：发送按钮、发送历史

### 3.4 消息消费

#### 3.4.1 功能描述
- 选择主题和分区
- 支持从最早、最新、指定位置开始消费
- 实时消费消息流
- 消息过滤和搜索
- 导出消息到文件
- 支持多种格式化显示

#### 3.4.2 数据模型
```java
public class ConsumerMessage {
    private String topic;
    private int partition;
    private long offset;
    private String key;
    private String value;
    private Map<String, String> headers;
    private long timestamp;
}

public class ConsumerConfig {
    private String topic;
    private List<Integer> partitions;
    private OffsetResetStrategy offsetReset;
    private int maxMessages;
    private boolean autoCommit;
}

public enum OffsetResetStrategy {
    EARLIEST, LATEST, SPECIFIC
}
```

#### 3.4.3 界面设计
- 顶部：主题选择、消费配置
- 中部：消息列表表格（显示消息详情）
- 底部：消息详情面板（JSON 格式化显示）
- 工具栏：开始/停止、清除、导出、搜索

### 3.5 消费者组管理

#### 3.5.1 功能描述
- 查看所有消费者组
- 查看消费者组详情（成员、订阅主题）
- 查看消费者组的消费进度（lag）
- 重置消费者组 offset
- 删除消费者组

#### 3.5.2 数据模型
```java
public class ConsumerGroupInfo {
    private String groupId;
    private String state;
    private String protocolType;
    private List<MemberInfo> members;
    private Map<TopicPartition, OffsetAndMetadata> offsets;
    private Map<TopicPartition, Long> lag;
}

public class MemberInfo {
    private String memberId;
    private String clientId;
    private String host;
    private List<TopicPartition> assignments;
}
```

#### 3.5.3 界面设计
- 左侧：消费者组列表
- 右侧：消费者组详情（成员、订阅、Lag）
- 工具栏：刷新、重置 Offset、删除

## 4. 用户界面设计

### 4.1 主窗口布局
```
┌────────────────────────────────────────────────────┐
│  菜单栏: 文件 | 编辑 | 视图 | 工具 | 帮助           │
├────────────────────────────────────────────────────┤
│  工具栏: [连接] [刷新] [设置] [关于]               │
├──────────┬─────────────────────────────────────────┤
│          │                                         │
│  集群树   │         主工作区                        │
│          │   (Tab 切换不同功能模块)                │
│  - 集群1  │                                         │
│    - 主题 │   ┌─────────────────────────────────┐  │
│    - 消费者│   │ 主题管理 | 消息生产 | 消息消费 │  │
│  - 集群2  │   └─────────────────────────────────┘  │
│          │                                         │
│          │                                         │
│          │                                         │
├──────────┴─────────────────────────────────────────┤
│  状态栏: 连接状态 | 当前集群 | 版本信息             │
└────────────────────────────────────────────────────┘
```

### 4.2 UI 设计原则
- **简洁性**: 界面简洁清晰，避免信息过载
- **一致性**: 保持操作和视觉风格的一致性
- **响应性**: 提供及时的操作反馈
- **易用性**: 符合用户习惯，降低学习成本
- **美观性**: 采用现代化的设计风格

### 4.3 主题和样式
- 支持亮色和暗色主题切换
- 使用 JavaFX CSS 进行样式定制
- 采用统一的颜色方案和图标库

## 5. 数据存储设计

### 5.1 配置存储
- 使用 JSON 格式存储配置文件
- 配置文件位置: `~/.kafkadesk/config.json`
- 存储内容：
  - 集群连接配置
  - 用户偏好设置
  - 窗口位置和大小
  - 最近使用的集群

### 5.2 配置文件结构
```json
{
  "version": "1.0",
  "clusters": [
    {
      "id": "cluster-1",
      "name": "开发环境",
      "bootstrapServers": "localhost:9092",
      "properties": {}
    }
  ],
  "preferences": {
    "theme": "light",
    "language": "zh_CN",
    "autoConnect": true,
    "lastSelectedCluster": "cluster-1"
  },
  "window": {
    "width": 1200,
    "height": 800,
    "maximized": false
  }
}
```

## 6. 安全性设计

### 6.1 认证支持
- 支持 SASL/PLAIN
- 支持 SASL/SCRAM
- 支持 SSL/TLS 加密连接

### 6.2 敏感信息处理
- 密码加密存储
- 支持密码本地加密
- 提示用户不要在生产环境使用明文密码

## 7. 性能优化

### 7.1 UI 响应性
- 所有 Kafka 操作在后台线程执行
- 使用 JavaFX Task 和 Service 进行异步处理
- 避免 UI 线程阻塞

### 7.2 数据加载优化
- 主题和消息列表支持分页加载
- 大量数据展示使用虚拟滚动
- 消息消费支持流式处理

### 7.3 内存管理
- 限制消息缓存数量
- 及时释放不再使用的资源
- 监控内存使用情况

## 8. 错误处理

### 8.1 异常处理策略
- 所有 Kafka 操作包裹在 try-catch 块中
- 友好的错误提示信息
- 记录详细的错误日志

### 8.2 常见错误处理
- 连接失败：提示用户检查网络和配置
- 权限不足：提示用户检查认证信息
- 超时：提供重试选项
- 资源不存在：友好提示并刷新列表

## 9. 开发计划

### 9.1 第一阶段（MVP - 最小可行产品）
**目标**: 实现基础的集群连接和主题管理功能

**任务**:
1. 搭建项目基础架构（Maven 配置、模块划分）
2. 实现主窗口框架和基础布局
3. 实现集群连接管理
4. 实现主题列表查看
5. 实现简单的消息生产功能
6. 实现简单的消息消费功能

**预计时间**: 2-3 周

### 9.2 第二阶段（功能完善）
**目标**: 完善核心功能，提升用户体验

**任务**:
1. 完善主题管理（创建、删除、配置修改）
2. 增强消息生产（支持多种格式、批量发送）
3. 增强消息消费（过滤、搜索、导出）
4. 实现消费者组管理
5. 添加配置持久化
6. 优化 UI 交互和样式

**预计时间**: 3-4 周

### 9.3 第三阶段（高级功能）
**目标**: 添加高级特性和监控功能

**任务**:
1. 实现集群监控功能
2. 添加性能统计和图表展示
3. 支持多语言（中文、英文）
4. 实现主题和暗色主题切换
5. 添加快捷键支持
6. 完善文档和帮助系统

**预计时间**: 2-3 周

### 9.4 第四阶段（测试和发布）
**目标**: 全面测试和正式发布

**任务**:
1. 单元测试和集成测试
2. 跨平台测试（Windows、macOS、Linux）
3. 性能测试和优化
4. 用户文档编写
5. 打包和发布

**预计时间**: 1-2 周

## 10. 项目结构

```
KafkaDesk/
├── pom.xml                          # Maven 主配置文件
├── README.md                        # 项目说明
├── DESIGN.md                        # 设计文档
├── LICENSE                          # 许可证
├── .gitignore                       # Git 忽略配置
│
├── kafkadesk-model/                 # 数据模型模块
│   ├── pom.xml
│   └── src/
│       └── main/java/com/kafkadesk/model/
│           ├── ClusterConfig.java
│           ├── TopicInfo.java
│           ├── ConsumerGroupInfo.java
│           └── Message.java
│
├── kafkadesk-core/                  # 核心服务模块
│   ├── pom.xml
│   └── src/
│       └── main/java/com/kafkadesk/core/
│           ├── service/
│           │   ├── ClusterService.java
│           │   ├── TopicService.java
│           │   ├── ProducerService.java
│           │   ├── ConsumerService.java
│           │   └── ConsumerGroupService.java
│           └── config/
│               └── ConfigManager.java
│
├── kafkadesk-ui/                    # UI 模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/kafkadesk/ui/
│       │   ├── KafkaDeskApplication.java
│       │   ├── controller/
│       │   │   ├── MainController.java
│       │   │   ├── ClusterController.java
│       │   │   ├── TopicController.java
│       │   │   ├── ProducerController.java
│       │   │   └── ConsumerController.java
│       │   └── util/
│       │       └── FXMLUtil.java
│       └── resources/
│           ├── fxml/
│           │   ├── main.fxml
│           │   ├── cluster.fxml
│           │   ├── topic.fxml
│           │   ├── producer.fxml
│           │   └── consumer.fxml
│           ├── css/
│           │   ├── light-theme.css
│           │   └── dark-theme.css
│           └── images/
│               └── icons/
│
└── kafkadesk-utils/                 # 工具模块
    ├── pom.xml
    └── src/
        └── main/java/com/kafkadesk/utils/
            ├── JsonUtil.java
            ├── DateTimeUtil.java
            └── StringUtil.java
```

## 11. 依赖管理

### 11.1 主要依赖
```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17.0.8</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>17.0.8</version>
    </dependency>
    
    <!-- Kafka Client -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>3.6.0</version>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.3</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 12. 打包和发布

### 12.1 打包方式
- 使用 Maven 进行构建
- 使用 jpackage 创建原生安装包
- 支持创建可执行 JAR 文件

### 12.2 发布渠道
- GitHub Releases
- 官网下载
- 可能的软件包管理器（Homebrew、Chocolatey）

## 13. 后续扩展计划

### 13.1 功能扩展
- Schema Registry 集成
- Kafka Connect 管理
- Kafka Streams 监控
- ACL 权限管理
- 集群性能指标可视化
- 消息链路追踪

### 13.2 技术优化
- 支持插件化架构
- 提供 REST API 接口
- 增加自动化测试覆盖率
- 持续集成和持续部署（CI/CD）

## 14. 总结

KafkaDesk 致力于成为一款功能强大、易于使用的 Kafka 桌面客户端工具。通过 Java 和 JavaFX 技术栈，我们将提供跨平台的图形化界面，帮助用户更高效地管理和操作 Kafka 集群。

本设计文档为项目的第一版设计，后续将根据开发进度和用户反馈不断完善和优化。

---

**文档版本**: 1.0  
**创建日期**: 2025-11-05  
**作者**: KafkaDesk Team  
**状态**: 草稿
