# Container 参数详解

## 概述

`container` 参数是使用 Skills 的核心配置，控制 Skills 的加载、执行和管理。

##基本结构

```python
container={
    # 预加载的 Skills
    "preloaded": [
        {
            "type": "anthropic",      # 或 "custom"
            "skill_id": "xlsx",
            "version": "latest"       # 或具体版本号
        }
    ],

    # 可选：重用已有容器
    "id": "container_abc123"
}
```

---

## Preloaded Skills

### 单个 Skill

```python
container={
    "preloaded": [{
        "type": "anthropic",
        "skill_id": "xlsx",
        "version": "latest"
    }]
}
```

### 多个 Skills（最多 8 个）

```python
container={
    "preloaded": [
        {"type": "anthropic", "skill_id": "xlsx", "version": "latest"},
        {"type": "anthropic", "skill_id": "pptx", "version": "latest"},
        {"type": "anthropic", "skill_id": "pdf", "version": "latest"},
        {"type": "custom", "skill_id": "skill_01abc", "version": "latest"}
    ]
}
```

---

## Skill 类型

### Anthropic 预置 Skills

```python
# 可用的预置 Skills
ANTHROPIC_SKILLS = {
    "xlsx": "Excel 处理",
    "pptx": " PowerPoint 创建",
    "docx": "Word 文档",
    "pdf": "PDF 处理"
}

# 使用示例
container={
    "preloaded": [{
        "type": "anthropic",
        "skill_id": "xlsx"
    }]
}
```

### 自定义 Skills

```python
# 使用上传的自定义 Skill
container={
    "preloaded": [{
        "type": "custom",
        "skill_id": "skill_01AbCdEfGhIjKlMnOpQrStUv",
        "version": "1759178010641129"  # epoch 时间戳
    }]
}
```

---

## 版本管理

### Latest 版本

```python
# 始终使用最新版本
container={
    "preloaded": [{
        "type": "anthropic",
        "skill_id": "xlsx",
        "version": "latest"  # 推荐用于开发
    }]
}
```

### 固定版本

```python
# Anthropic Skills：使用日期格式
container={
    "preloaded": [{
        "type": "anthropic",
        "skill_id": "xlsx",
        "version": "20251013"  # 固定到特定版本
    }]
}

# Custom Skills：使用时间戳
container={
    "preloaded": [{
        "type": "custom",
        "skill_id": "skill_01abc",
        "version": "1759178010641129"
    }]
}
```

### 版本选择建议

```yaml
开发环境: version="latest" # 获取最新特性
测试环境: version="20251013" # 固定版本测试
生产环境: version="20251013" # 稳定版本
```

---

## 容器重用

### 为什么重用容器？

```python
# ❌ 每次创建新容器（慢）
for i in range(10):
    response = client.messages.create(
        container={"preloaded": [...]},  # 每次都初始化
        messages=[...]
    )

# ✅ 重用容器（快）
# 第一次请求
response1 = client.messages.create(
    container={"preloaded": [...]},
    messages=[...]
)
container_id = response1.container_id

# 后续请求重用
for i in range(9):
    response = client.messages.create(
        container={"id": container_id},  # 重用
        messages=[...]
    )
```

### 完整示例

```python
def multi_turn_with_container():
    """多轮对话重用容器"""

    client = anthropic.Anthropic()

    # 第一轮：创建容器
    response1 = client.messages.create(
        model="claude-3-7-sonnet-20250219",
        max_tokens=4096,
        beta=[
            "code-execution-2025-08-25",
            "skills-2025-10-02"
        ],
        tools=[{"type": "code_execution"}],
        container={
            "preloaded": [
                {"type": "anthropic", "skill_id": "xlsx"}
            ]
        },
        messages=[{
            "role": "user",
            "content": "Create a sales report"
        }]
    )

    container_id = response1.container_id
    print(f"Container ID: {container_id}")

    # 第二轮：重用容器
    response2 = client.messages.create(
        model="claude-3-7-sonnet-20250219",
        max_tokens=4096,
        beta=[
            "code-execution-2025-08-25",
            "skills-2025-10-02"
        ],
        tools=[{"type": "code_execution"}],
        container={
            "id": container_id  # ✅ 重用
        },
        messages=[
            {"role": "user", "content": "Create a sales report"},
            {"role": "assistant", "content": response1.content},
            {"role": "user", "content": "Now add a chart"}
        ]
    )

    return response2
```

---

## Skills 组合策略

### 按场景组合

```python
# 办公文档处理
office_skills = {
    "preloaded": [
        {"type": "anthropic", "skill_id": "xlsx"},
        {"type": "anthropic", "skill_id": "pptx"},
        {"type": "anthropic", "skill_id": "docx"}
    ]
}

# PDF 处理
pdf_skills = {
    "preloaded": [
        {"type": "anthropic", "skill_id": "pdf"}
    ]
}

# 自定义业务流程
custom_workflow = {
    "preloaded": [
        {"type": "custom", "skill_id": "skill_data_analysis"},
        {"type": "custom", "skill_id": "skill_report_gen"},
        {"type": "anthropic", "skill_id": "xlsx"}
    ]
}
```

### 动态组合

```python
def get_container_config(task_type):
    """根据任务类型动态选择 Skills"""

    configs = {
        "data_analysis": {
            "preloaded": [
                {"type": "anthropic", "skill_id": "xlsx"},
                {"type": "custom", "skill_id": "skill_stats"}
            ]
        },
        "presentation": {
            "preloaded": [
                {"type": "anthropic", "skill_id": "pptx"},
                {"type": "custom", "skill_id": "skill_charts"}
            ]
        },
        "report": {
            "preloaded": [
                {"type": "anthropic", "skill_id": "docx"},
                {"type": "anthropic", "skill_id": "pdf"}
            ]
        }
    }

    return configs.get(task_type, {"preloaded": []})

# 使用
container = get_container_config("data_analysis")
response = client.messages.create(
    container=container,
    messages=[...]
)
```

---

## 高级配置

### 环境变量配置

```python
import os
import json

# 从环境变量加载配置
SKILLS_CONFIG = json.loads(os.getenv('CLAUDE_SKILLS_CONFIG', '{}'))

container = SKILLS_CONFIG.get('default_container', {
    "preloaded": [{"type": "anthropic", "skill_id": "xlsx"}]
})
```

### 配置文件

```yaml
# skills_config.yaml
containers:
  default:
    preloaded:
      - type: anthropic
        skill_id: xlsx
        version: latest

  advanced:
    preloaded:
      - type: anthropic
        skill_id: xlsx
      - type: anthropic
        skill_id: pptx
      - type: custom
        skill_id: skill_01abc
```

```python
import yaml

def load_container_config(config_file, profile="default"):
    """从配置文件加载容器配置"""
    with open(config_file) as f:
        config = yaml.safe_load(f)
    return config['containers'][profile]

# 使用
container = load_container_config('skills_config.yaml', 'advanced')
```

---

## 错误处理

```python
def create_message_with_skills(skills_config, user_message):
    """带错误处理的 Skills 调用"""

    try:
        response = client.messages.create(
            model="claude-3-7-sonnet-20250219",
            max_tokens=4096,
            beta=[
                "code-execution-2025-08-25",
                "skills-2025-10-02"
            ],
            tools=[{"type": "code_execution"}],
            container=skills_config,
            messages=[{
                "role": "user",
                 "content": user_message
            }]
        )
        return response

    except anthropic.BadRequestError as e:
        if "skill not found" in str(e):
            print("❌ Skill 不存在，请检查 skill_id")
        elif "version not found" in str(e):
            print("❌ 版本不存在，尝试使用 'latest'")
        raise

    except anthropic.APIError as e:
        print(f"❌ API 错误: {e}")
        raise
```

---

## 最佳实践

- ✅ 开发时使用 `latest`，生产时固定版本
- ✅ 重用容器以提升性能
- ✅ 按场景组合 Skills
- ✅ 限制同时加载的 Skills 数量（最多 8 个）
- ✅ 使用配置文件管理复杂配置

---

[返回主目录](./04-skills-integration-guide.md) | [下一篇：自定义 Skills →](./04c-custom-skills.md)
