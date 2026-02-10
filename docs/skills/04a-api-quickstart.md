# Claude API 快速入门

## 前置准备

### 1. 获取 API 密钥

访问 [Anthropic Console](https://platform.claude.com/settings/keys) 创建 API 密钥：

1. 登录账号
2. 进入 Settings → API Keys
3. 点击 "Create Key"
4. 保存密钥（只显示一次）

```bash
# 将密钥保存为环境变量（推荐）
export ANTHROPIC_API_KEY="sk-ant-api03-..."
```

### 2. 安装 SDK

选择你的编程语言：

**Python:**

```bash
pip install anthropic
```

**Node.js:**

```bash
npm install @anthropic-ai/sdk
```

**Java:**

```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-sdk-java</artifactId>
    <version>0.4.0</version>
</dependency>
```

---

## 第一个 Skills 调用

### 无 Skills 的基础调用

先看一个不使用 Skills 的简单例子：

```python
import anthropic

client = anthropic.Anthropic(api_key="your-api-key")

response = client.messages.create(
    model="claude-3-7-sonnet-20250219",
    max_tokens=4096,
    messages=[{
        "role": "user",
        "content": "Hello, Claude!"
    }]
)

print(response.content[0].text)
```

---

## 使用 Skills 的调用

### 启用 Code Execution

Skills 依赖 Code Execution，需要：

1. 添加必需的 Beta 头
2. 启用 `code_execution` 工具
3. 指定 Skills

```python
import anthropic

client = anthropic.Anthropic(api_key="your-api-key")

response = client.messages.create(
    model="claude-3-7-sonnet-20250219",
    max_tokens=4096,

    # ✅ 必需：Beta 头
    beta=[
        "code-execution-2025-08-25",  # 启用代码执行
        "skills-2025-10-02",           # 启用 Skills
        "files-api-2025-04-14"         # 启用文件 API
    ],

    # ✅ 必需：启用 code_execution
    tools=[{
        "type": "code_execution"
    }],

    # ✅ 指定要使用的 Skills
    container={
        "preloaded": [{
            "type": "anthropic",
            "skill_id": "xlsx",  # 使用 Excel skills
            "version": "latest"
        }]
    },

    messages=[{
        "role": "user",
        "content": "Create an Excel file with a simple budget table"
    }]
)

# 处理响应
for block in response.content:
    if block.type == "text":
        print(block.text)
    elif block.type == "code_execution":
        # Skills 执行的代码
        print(f"Code: {block.code}")
        print(f"Output: {block.output}")
```

---

##关键参数说明

### `beta` 头

必需的三个 beta 标识：

```python
beta=[
    "code-execution-2025-08-25",  # 代码执行能力
    "skills-2025-10-02",           # Skills API
    "files-api-2025-04-14"         # 文件操作
]
```

### `tools` 参数

必须启用 `code_execution`：

```python
tools=[{
    "type": "code_execution"
}]
```

### `container` 参数

指定要加载的 Skills：

```python
container={
    "preloaded": [
        {
            "type": "anthropic",      # 类型：anthropic 或 custom
            "skill_id": "xlsx",       # Skill ID
            "version": "latest"       # 版本：latest 或具体日期
        },
        # 可以加载多个 Skills（最多8个）
        {
            "type": "anthropic",
            "skill_id": "pptx",
            "version": "latest"
        }
    ]
}
```

---

## 完整示例：创建 Excel 文件

```python
import anthropic
import os

def create_excel_with_skills():
    """使用 Skills 创建 Excel 文件"""

    client = anthropic.Anthropic(
        api_key=os.environ.get("ANTHROPIC_API_KEY")
    )

    response = client.messages.create(
        model="claude-3-7-sonnet-20250219",
        max_tokens=4096,
        beta=[
            "code-execution-2025-08-25",
            "skills-2025-10-02",
            "files-api-2025-04-14"
        ],
        tools=[{"type": "code_execution"}],
        container={
            "preloaded": [{
                "type": "anthropic",
                "skill_id": "xlsx",
                "version": "latest"
            }]
        },
        messages=[{
            "role": "user",
            "content": """
                Create an Excel file with:
                - Sheet1: Monthly budget
                - Columns: Category, Budget, Actual, Variance
                - 5 expense categories
                - Use formulas for Variance column
                - Add totals row
            """
        }]
    )

    # 提取生成的文件 ID
    file_id = None
    for block in response.content:
        if hasattr(block, 'file_id'):
            file_id = block.file_id
            break

    if file_id:
        # 下载文件（见 04d-file-handling.md）
        file_content = client.files.content(file_id)

        with open("budget.xlsx", "wb") as f:
            f.write(file_content.read())

        print(f"✅ Excel 文件已创建：budget.xlsx")
    else:
        print("❌ 未找到生成的文件")

    return response

if __name__ == "__main__":
    create_excel_with_skills()
```

---

## 响应结构

Skills 调用的响应包含多个内容块：

```json
{
  "content": [
    {
      "type": "text",
      "text": "I'll create the Excel file for you."
    },
    {
      "type": "code_execution",
      "code": "import openpyxl\\n...",
      "output": "Created budget.xlsx"
    },
    {
      "type": "file",
      "file_id": "file_abc123",
      "filename": "budget.xlsx"
    }
  ]
}
```

---

## 常见错误

### ❌ 未添加 beta 头

```python
# 错误
response = client.messages.create(
    model="claude-3-7-sonnet-20250219",
    messages=[...]
)
# Error: beta headers required
```

### ❌ 未启用 code_execution

```python
# 错误
response = client.messages.create(
    model="claude-3-7-sonnet-20250219",
    beta=["skills-2025-10-02"],
    # 缺少 tools 参数
    messages=[...]
)
# Error: code_execution tool required
```

### ❌Skill ID 不存在

```python
# 错误
container={
    "preloaded": [{
        "type": "anthropic",
        "skill_id": "invalid_skill",  # 不存在
        "version": "latest"
    }]
}
# Error: Skill not found
```

---

## 调试技巧

### 1. 打印完整响应

```python
import json

response = client.messages.create(...)

# 查看完整响应结构
print(json.dumps(response.model_dump(), indent=2))
```

### 2. 检查执行日志

```python
for block in response.content:
    if block.type == "code_execution":
        print("执行代码：")
        print(block.code)
        print("\n输出：")
        print(block.output)
```

### 3. 启用详细日志

```python
import logging

logging.basicConfig(level=logging.DEBUG)
```

---

## 下一步

- **学习容器参数** → [Container 参数详解](./04b-api-container.md)
- **创建自定义 Skills** → [自定义 Skills](./04c-custom-skills.md)
- **处理文件** → [文件处理](./04d-file-handling.md)

---

[返回主目录](./04-skills-integration-guide.md)
