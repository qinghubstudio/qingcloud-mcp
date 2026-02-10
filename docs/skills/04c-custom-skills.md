# 创建自定义 Skills

## 概述

自定义 Skills 让你为特定业务逻辑创建可复用的 AI 能力。

## Skill 结构

### 必需文件：SKILL.md

````markdown
---
name: my-data-processor
description: Process and analyze CSV data files
---

# Data Processor Skill

This skill processes CSV files and generates analysis reports.

## Usage

```python
import pandas as pd

# Read CSV file
df = pd.read_csv('/mnt/data/input.csv')

# Perform analysis
summary = df.describe()
print(summary)
```
````

## Dependencies

- pandas
- numpy

````

### YAML Frontmatter 要求

```yaml
---
name: my-skill-name          # 必需
  # 1-64 字符
  # 小写字母、数字、连字符
  # 不能使用 "anthropic", "claude"

description: Skill description  # 必需
  # 1-1024 字符
  # 不能包含 XML 标签
---
````

---

## 创建 Skill

### 方式一：目录上传

```python
import anthropic

client = anthropic.Anthropic()

# 从目录创建 Skill
skill = client.beta.skills.create(
    directory_path="./my-skill",  # 包含 SKILL.md 的目录
    beta=["skills-2025-10-02"]
)

print(f"Skill ID: {skill.id}")
print(f"Version: {skill.version}")
```

### 方式二：文件列表上传

```python
with open("./my-skill/SKILL.md", "rb") as skill_file,     open("./my-skill/helper.py", "rb") as helper_file:

    skill = client.beta.skills.create(
        files=[
            {
                "path": "SKILL.md",
                "content": skill_file
            },
            {
                "path": "helper.py",
                "content": helper_file
            }
        ],
        beta=["skills-2025-10-02"]
    )
```

---

## Skill 示例

### 示例 1：CSV 数据分析

````markdown
---
name: csv-analyzer
description: Analyze CSV files and generate insights
---

# CSV Analyzer

Analyzes CSV data and provides statistical insights.

## Usage

```python
import pandas as pd
import matplotlib.pyplot as plt

def analyze_csv(file_path):
    df = pd.read_csv(file_path)

    # Basic stats
    print("Data Summary:")
    print(df.describe())

    # Generate chart
    df.plot(kind='bar')
    plt.savefig('/mnt/data/chart.png')

    return df

# Use it
analyze_csv('/mnt/data/input.csv')
```
````

````

### 示例 2：JSON 转换器

```markdown
---
name: json-transformer
description: Transform and validate JSON data
---

# JSON Transformer

Transforms JSON structures and validates schemas.

```python
import json

def transform_json(input_file, output_file):
    with open(input_file) as f:
        data = json.load(f)

    # Transform logic
    transformed = {
        "version": "2.0",
        "data": data
    }

    with open(output_file, 'w') as f:
        json.dump(transformed, f, indent=2)
````

````

---

## 管理 Skills

### 列出所有 Skills

```python
# 列出自定义 Skills
skills = client.beta.skills.list(
    source="custom",
    beta=["skills-2025-10-02"]
)

for skill in skills.data:
    print(f"{skill.name} - {skill.id}")
````

### 获取 Skill 详情

```python
skill = client.beta.skills.retrieve(
    skill_id="skill_01AbCdEf",
    beta=["skills-2025-10-02"]
)

print(f"Name: {skill.name}")
print(f"Description: {skill.description}")
print(f"Version: {skill.version}")
```

### 删除 Skill

```python
# 先删除所有版本
versions = client.beta.skills.versions.list(
    skill_id="skill_01AbCdEf"
)

for version in versions.data:
    client.beta.skills.versions.delete(
        skill_id="skill_01AbCdEf",
        version=version.version
    )

# 再删除 Skill
client.beta.skills.delete(
    skill_id="skill_01AbCdEf"
)
```

---

## 版本管理

### 创建新版本

```python
# 更新 Skill 文件后创建新版本
new_version = client.beta.skills.versions.create(
    skill_id="skill_01AbCdEf",
    directory_path="./my-skill-updated",
    beta=["skills-2025-10-02"]
)

print(f"New version: {new_version.version}")
```

### 使用特定版本

```python
# 使用固定版本（生产环境推荐）
container={
    "preloaded": [{
        "type": "custom",
        "skill_id": "skill_01AbCdEf",
        "version": "1759178010641129"  # 具体版本
    }]
}

# 使用最新版本（开发环境）
container={
    "preloaded": [{
        "type": "custom",
        "skill_id": "skill_01AbCdEf",
        "version": "latest"
    }]
}
```

---

## 限制与注意事项

- ⚠️ 总大小上限：8MB
- ⚠️ 必须包含 SKILL.md
- ⚠️ 所有文件必须在同一根目录
- ⚠️ name 和 description 有格式要求

---

## 完整工作流

```python
def deploy_custom_skill():
    """完整的 Skill 部署流程"""

    client = anthropic.Anthropic()

    # 1. 创建 Skill
    print("Creating skill...")
    skill = client.beta.skills.create(
        directory_path="./my-skill",
        beta=["skills-2025-10-02"]
    )

    skill_id = skill.id
    print(f"✅ Created: {skill_id}")

    # 2. 测试 Skill
    print("Testing skill...")
    response = client.messages.create(
        model="claude-3-7-sonnet-20250219",
        max_tokens=4096,
        beta=[
            "code-execution-2025-08-25",
            "skills-2025-10-02"
        ],
        tools=[{"type": "code_execution"}],
        container={
            "preloaded": [{
                "type": "custom",
                "skill_id": skill_id,
                "version": "latest"
            }]
        },
        messages=[{
            "role": "user",
            "content": "Test the skill"
        }]
    )

    print("✅ Test completed")

    # 3. 发布到生产
    print("Deploy to production...")
    # 使用固定版本号
    prod_version = skill.version

    return skill_id, prod_version
```

---

[返回主目录](./04-skills-integration-guide.md) | [下一篇：文件处理 →](./04d-file-handling.md)
