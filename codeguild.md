# 装备能力指标本体库与规则引擎一体化平台需求文档（面向 Codex 开发）

> 基于：Protege + OWL 本体库 + OWLAPI + Neo4j + Drools + Spring Boot + Vue3/React
> 目标：形成一个“本体建模 → 知识图谱可视化 → 规则管理 → 仿真校验 → 结果分析”的一体化系统

------

# 1. 项目目标

构建一个支持：

- 装备能力指标体系本体建模
- 本体关系可视化编辑
- 基于本体自动生成知识图谱
- Drools规则管理
- 仿真数据接入
- 能力指标约束校验
- 违规链路分析
- 影响传播分析

的一体化平台。

系统需要解决：

> “本体只能描述语义关系，但无法表达复杂量化约束”的问题。

因此：

- OWL负责：
  - 概念定义
  - 指标体系
  - 指标关系
  - 语义分类
- Drools负责：
  - 数值约束
  - 联动规则
  - 影响传播
  - 阈值校验
  - 组合条件判断

------

# 2. 系统总体架构

```text
                ┌──────────────────────┐
                │      前端平台         │
                │ Vue3 / React         │
                └─────────┬────────────┘
                          │ REST/WebSocket
──────────────────────────┼─────────────────────────
                          │
                ┌─────────▼────────────┐
                │     Spring Boot       │
                │    统一业务后端        │
                └─────────┬────────────┘
                          │
        ┌─────────────────┼──────────────────┐
        │                 │                  │
        ▼                 ▼                  ▼
┌──────────────┐ ┌────────────────┐ ┌────────────────┐
│ OWLAPI模块    │ │ Drools规则引擎 │ │ Neo4j图数据库   │
│ 本体解析       │ │ 规则执行       │ │ 图谱查询         │
└──────────────┘ └────────────────┘ └────────────────┘
        │
        ▼
┌────────────────┐
│ Protege OWL文件 │
└────────────────┘
```

------

# 3. 核心业务模型

------

# 3.1 核心实体

## 3.1.1 能力指标（CapabilityIndicator）

| 字段         | 类型   | 说明     |
| ------------ | ------ | -------- |
| id           | String | 唯一ID   |
| iri          | String | OWL IRI  |
| name         | String | 指标名称 |
| domain       | String | 作战域   |
| category     | String | 装备类别 |
| description  | String | 描述     |
| unit         | String | 单位     |
| dataType     | String | 数据类型 |
| thresholdMin | Double | 最小值   |
| thresholdMax | Double | 最大值   |

------

## 3.1.2 关系实体（CapabilityRelationship）

采用“关系重化”设计。

| 字段               | 类型    | 说明 |
| ------------------ | ------- | ---- |
| id                 | String  |      |
| sourceIndicatorId  | String  |      |
| targetIndicatorId  | String  |      |
| relationshipType   | String  |      |
| droolsRuleName     | String  |      |
| weight             | Double  |      |
| priority           | Integer |      |
| influenceDirection | String  |      |
| enabled            | Boolean |      |

------

## 3.1.3 Drools规则

| 字段       | 类型     |
| ---------- | -------- |
| ruleName   | String   |
| drlContent | Text     |
| version    | String   |
| status     | String   |
| createTime | DateTime |
| updateTime | DateTime |

------

## 3.1.4 仿真数据

| 字段        | 类型     |
| ----------- | -------- |
| indicatorId | String   |
| value       | Double   |
| timestamp   | DateTime |
| source      | String   |

------

# 4. 技术选型

------

# 4.1 后端

| 模块       | 技术                  |
| ---------- | --------------------- |
| Web框架    | Spring Boot 3         |
| ORM        | MyBatis Plus          |
| 本体解析   | OWLAPI                |
| 图数据库   | Neo4j                 |
| 规则引擎   | Drools 8              |
| 权限       | Spring Security + JWT |
| 缓存       | Redis                 |
| MQ（可选） | RabbitMQ/Kafka        |

------

# 4.2 前端

推荐：

| 技术           | 说明      |
| -------------- | --------- |
| Vue3 + TS      | 推荐      |
| 或 React + TS  | 可选      |
| Ant Design Vue | UI        |
| Cytoscape.js   | 图谱渲染  |
| Monaco Editor  | DRL编辑器 |
| ECharts        | 分析图    |
| Pinia          | 状态管理  |

------

# 5. 前端详细需求（重点）

------

# 5.1 本体库可视化管理

这是系统核心。

需要类似：

- Neo4j Browser
- yEd
- GraphXR
- Protégé Graph View

的体验。

------

# 5.1.1 功能要求

## 功能1：本体树结构展示

左侧：

```text
水面作战域
 ├── 舰艇
 │    ├── 驱逐舰
 │    ├── 护卫舰
 │    └── 航母
 ├── 雷达
 └── 武器系统
```

支持：

- 展开/折叠
- 拖拽
- 搜索
- 标签过滤

------

## 功能2：指标节点可视化

中央画布：

节点：

```text
[雷达探测距离]
[预警响应时间]
[火控精度]
```

边：

```text
直接影响
阈值约束
间接影响
```

------

## 功能3：关系编辑（重点）

用户可：

### 创建关系

```text
雷达探测距离
    ───直接影响──▶
预警响应时间
```

### 编辑关系属性

弹窗：

| 属性         | 输入   |
| ------------ | ------ |
| 关系类型     | 下拉框 |
| Drools规则名 | 输入框 |
| 权重         | 数字   |
| 优先级       | 数字   |
| 是否启用     | Switch |

------

## 功能4：关系重化可视化

系统内部：

```text
Relationship Node
```

但前端不能展示“关系节点”。

必须：

### 自动渲染成边

即：

内部：

```text
RelationshipEntity
  ├── hasSource
  ├── hasTarget
```

前端：

```text
source ───▶ target
```

用户无感知。

------

# 5.1.2 图谱交互需求

支持：

| 功能     | 要求     |
| -------- | -------- |
| 缩放     | 鼠标滚轮 |
| 平移     | 拖拽     |
| 框选     | 支持     |
| 多选     | Ctrl     |
| 自动布局 | 力导向   |
| 分层布局 | DAG      |
| 路径高亮 | 支持     |
| 节点搜索 | 支持     |
| 边过滤   | 支持     |
| 类型过滤 | 支持     |

------

# 5.1.3 图谱布局

至少支持：

## 力导向布局

适合：

- 影响关系分析

------

## DAG布局

适合：

- 指标依赖链

------

## 树布局

适合：

- 本体分类

------

# 5.2 知识图谱可视化模块

------

# 5.2.1 图数据库同步

系统启动：

```text
OWL → Neo4j
```

自动同步。

------

# 5.2.2 图谱查询

支持：

## 查询某指标的：

- 上游影响
- 下游影响
- 所有关联规则
- 违规路径

------

# 5.2.3 图谱分析

支持：

| 分析       | 说明       |
| ---------- | ---------- |
| 最短路径   | 指标影响链 |
| 中心性分析 | 核心指标   |
| 聚类分析   | 指标簇     |
| 连通域分析 | 孤立指标   |

------

# 5.2.4 图谱高亮

当规则违规：

例如：

```text
雷达距离不足
  ↓
预警超时
  ↓
拦截失败
```

需要：

- 整条链红色高亮
- 动态闪烁
- 显示违规原因

------

# 5.3 Drools规则管理（重点）

------

# 5.3.1 DRL在线编辑器

必须：

## Monaco Editor

支持：

| 功能         | 要求 |
| ------------ | ---- |
| Java语法高亮 | 必须 |
| 自动补全     | 必须 |
| 错误提示     | 必须 |
| 折叠         | 必须 |
| 搜索         | 必须 |

------

# 5.3.2 规则管理功能

## 列表页

| 字段     |
| -------- |
| 规则名   |
| 状态     |
| 版本     |
| 更新时间 |

支持：

- 启用
- 禁用
- 发布
- 回滚

------

## 规则版本管理

支持：

```text
RULE_001_v1
RULE_001_v2
RULE_001_v3
```

可回滚。

------

# 5.3.3 规则与图谱联动（核心）

点击图谱边：

自动打开：

```text
对应Drools规则
```

反之：

点击规则：

高亮：

```text
对应图谱关系
```

这是核心需求。

------

# 5.3.4 规则测试台

用户输入：

```json
{
  "Radar_Detection_Range": 300,
  "Warning_Response_Time": 15
}
```

点击：

```text
执行规则
```

输出：

```text
规则违反：
RULE_RadarCoverageToWarning

原因：
探测距离不足导致响应超时
```

------

# 5.4 仿真数据接入模块

支持：

| 类型      | 格式 |
| --------- | ---- |
| HTTP      | JSON |
| Kafka     | 流式 |
| CSV       | 文件 |
| WebSocket | 实时 |

------

# 5.4.1 实时校验

仿真数据进入后：

自动：

```text
触发相关规则
```

------

# 5.4.2 实时图谱刷新

若某指标异常：

图谱：

- 自动变红
- 传播高亮

------

# 6. 后端详细设计

------

# 6.1 模块划分

```text
backend
 ├── ontology-service
 ├── graph-service
 ├── rule-service
 ├── simulation-service
 ├── auth-service
 └── gateway
```

------

# 6.2 ontology-service

负责：

- OWL解析
- OWL导入
- 本体同步
- 本体查询

------

## API

### 获取所有指标

```http
GET /api/ontology/indicators
```

------

### 获取关系

```http
GET /api/ontology/relationships
```

------

### 保存关系

```http
POST /api/ontology/relationship
```

------

# 6.3 graph-service

负责：

- Neo4j同步
- 图谱查询
- 路径分析

------

## API

### 查询影响路径

```http
GET /api/graph/path
```

参数：

```text
source
target
```

------

### 获取邻居节点

```http
GET /api/graph/neighbors/{id}
```

------

# 6.4 rule-service

负责：

- DRL管理
- 动态编译
- 动态加载
- 规则执行

------

# 6.4.1 动态规则加载（重点）

必须支持：

```text
无需重启服务
```

动态更新规则。

------

# 6.4.2 KieContainer动态刷新

采用：

```java
KieFileSystem
KieBuilder
KieContainer
```

动态编译。

------

# 6.5 simulation-service

负责：

- 接收仿真数据
- 转换CapabilityData
- 触发规则

------

# 7. 数据库设计

------

# 7.1 MySQL

------

## rule_definition

| 字段        |
| ----------- |
| id          |
| rule_name   |
| drl_content |
| version     |
| status      |

------

## ontology_relationship

| 字段      |
| --------- |
| id        |
| source_id |
| target_id |
| rule_name |
| weight    |

------

# 7.2 Neo4j

节点：

```cypher
(:Indicator)
(:Relationship)
(:Domain)
```

边：

```cypher
[:AFFECTS]
[:BELONGS_TO]
[:CONSTRAINS]
```

------

# 8. 关键技术实现（必须重点实现）

------

# 8.1 OWL → Neo4j同步

必须支持：

```text
OWL更新后自动同步图数据库
```

------

# 8.2 关系重化自动转换

后端：

```text
CapabilityRelationship
```

自动转：

```text
Graph Edge
```

------

# 8.3 Drools动态编译

支持：

- 在线修改规则
- 即时生效

------

# 8.4 图谱联动规则

这是系统灵魂。

必须实现：

```text
图谱边 ↔ Drools规则
```

双向映射。

------

# 9. 性能要求

------

# 9.1 图谱规模

支持：

| 类型     | 数量  |
| -------- | ----- |
| 指标节点 | 10万  |
| 关系边   | 100万 |
| 规则     | 1万   |

------

# 9.2 性能指标

| 操作     | 指标   |
| -------- | ------ |
| 图谱加载 | <3秒   |
| 规则执行 | <100ms |
| 路径查询 | <1秒   |

------

# 10. 安全要求

------

# 10.1 权限

角色：

| 角色           | 权限     |
| -------------- | -------- |
| Admin          | 全部     |
| OntologyEditor | 本体编辑 |
| RuleEditor     | 规则编辑 |
| Viewer         | 查看     |

------

# 10.2 审计日志

记录：

- 谁修改了规则
- 谁修改了关系
- 修改前后内容

------

# 11. 推荐开发顺序

------

# Phase 1：核心打通

先实现：

- OWL加载
- OWL解析
- Drools执行
- 规则映射

------

# Phase 2：图谱化

实现：

- Neo4j同步
- Cytoscape前端

------

# Phase 3：规则管理

实现：

- DRL编辑器
- 动态编译

------

# Phase 4：实时校验

实现：

- WebSocket
- 仿真数据流

------

# 12. Codex任务拆分建议

------

# 后端任务

## Task 1

```text
SpringBoot + OWLAPI
解析CapabilityRelationship
```

------

## Task 2

```text
OWL同步Neo4j
```

------

## Task 3

```text
Drools动态规则加载
```

------

## Task 4

```text
规则执行结果回传
```

------

# 前端任务

## Task 1

```text
Cytoscape知识图谱
```

------

## Task 2

```text
关系编辑弹窗
```

------

## Task 3

```text
Monaco DRL编辑器
```

------

## Task 4

```text
图谱与规则联动
```

------

# 13. 最终目标

最终形成：

```text
本体库（语义）
   +
知识图谱（关系）
   +
Drools（约束）
   +
仿真数据（动态）
```

的一体化能力指标分析平台。

核心能力：

- 指标关系推理
- 规则自动校验
- 违规链分析
- 影响传播分析
- 实时仿真联动
- 可视化知识图谱分析