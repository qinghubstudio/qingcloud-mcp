# 文件处理

## 概述

Skills 可以读取上传的文件，也可以生成新文件。本文档介绍如何处理文件的上传和下载。

## 上传文件到容器

### 基本上传

```python
import anthropic

client = anthropic.Anthropic()

# 上传文件
with open("data.csv", "rb") as f:
    file_upload = client.beta.files.content.create(
        file=f,
        beta=["files-api-2025-04-14"]
    )

file_id = file_upload.id
print(f"Uploaded file ID: {file_id}")
```

### 在消息中引用文件

```python
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
            "skill_id": "xlsx"
        }]
    },
    messages=[{
        "role": "user",
        "content": [
            {
                "type": "file",
                "file_id": file_id,
                "filename": "data.csv"
            },
            {
                "type": "text",
                "text": "Analyze this CSV file"
            }
        ]
    }]
)
```

---

## 下载生成的文件

### 提取文件 ID

```python
def extract_file_ids(response):
    """从响应中提取文件 ID"""
    file_ids = []

    for block in response.content:
        if hasattr(block, 'file_id'):
            file_ids.append({
                'file_id': block.file_id,
                'filename': getattr(block, 'filename', 'unknown')
            })

    return file_ids

# 使用
files = extract_file_ids(response)
for file in files:
    print(f"File: {file['filename']} ({file['file_id']})")
```

### 下载文件

```python
def download_file(client, file_id, save_path):
    """下载文件"""

    # 获取文件内容
    file_content = client.beta.files.content.retrieve(
        file_id=file_id,
        beta=["files-api-2025-04-14"]
    )

    # 保存到本地
    with open(save_path, "wb") as f:
        f.write(file_content.read())

    print(f"✅ Downloaded to {save_path}")

# 使用
download_file(client, "file_abc123", "output.xlsx")
```

---

## 完整示例：处理 Excel

```python
import anthropic
import os

def process_excel_file(input_path, output_path):
    """上传、处理、下载 Excel 文件"""

    client = anthropic.Anthropic(
        api_key=os.environ.get("ANTHROPIC_API_KEY")
    )

    # 1. 上传输入文件
    print("Uploading input file...")
    with open(input_path, "rb") as f:
        file_upload = client.beta.files.content.create(
            file=f,
            beta=["files-api-2025-04-14"]
        )

    input_file_id = file_upload.id
    print(f"✅ Uploaded: {input_file_id}")

    # 2. 处理文件
    print("Processing with Skills...")
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
                "skill_id": "xlsx"
            }]
        },
        messages=[{
            "role": "user",
            "content": [
                {
                    "type": "file",
                    "file_id": input_file_id,
                    "filename": os.path.basename(input_path)
                },
                {
                    "type": "text",
                    "text": """
                        Read this Excel file and:
                        1. Add a summary sheet
                        2. Create pivot tables
                        3. Add charts
                        Save as a new file
                    """
                }
            ]
        }]
    )

    # 3. 提取输出文件 ID
    output_file_id = None
    for block in response.content:
        if hasattr(block, 'file_id'):
            output_file_id = block.file_id
            break

    if not output_file_id:
        print("❌ No output file generated")
        return None

    # 4. 下载输出文件
    print("Downloading output...")
    file_content = client.beta.files.content.retrieve(
        file_id=output_file_id,
        beta=["files-api-2025-04-14"]
    )

    with open(output_path, "wb") as f:
        f.write(file_content.read())

    print(f"✅ Saved to {output_path}")
    return output_path

# 使用
if __name__ == "__main__":
    process_excel_file("input.xlsx", "processed.xlsx")
```

---

## 批量文件处理

```python
def batch_process_files(input_files, instruction):
    """批量处理多个文件"""

    client = anthropic.Anthropic()

    # 上传所有文件
    uploaded_files = []
    for filepath in input_files:
        with open(filepath, "rb") as f:
            file_upload = client.beta.files.content.create(
                file=f,
                beta=["files-api-2025-04-14"]
            )
        uploaded_files.append({
            "file_id": file_upload.id,
            "filename": os.path.basename(filepath)
        })

    # 构建消息内容
    content = []
    for file in uploaded_files:
        content.append({
            "type": "file",
            "file_id": file["file_id"],
            "filename": file["filename"]
        })
    content.append({
        "type": "text",
        "text": instruction
    })

    # 处理
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
            "preloaded": [
                {"type": "anthropic", "skill_id": "xlsx"}
            ]
        },
        messages=[{"role": "user", "content": content}]
    )

    return response

# 使用
files = ["sales_q1.xlsx", "sales_q2.xlsx", "sales_q3.xlsx"]
result = batch_process_files(
    files,
    "Combine these quarterly reports into one annual report"
)
```

---

## 支持的文件类型

### Excel (.xlsx)

```python
container={"preloaded": [{"type": "anthropic", "skill_id": "xlsx"}]}
```

### PowerPoint (.pptx)

```python
container={"preloaded": [{"type": "anthropic", "skill_id": "pptx"}]}
```

### Word (.docx)

```python
container={"preloaded": [{"type": "anthropic", "skill_id": "docx"}]}
```

### PDF (.pdf)

```python
container={"preloaded": [{"type": "anthropic", "skill_id": "pdf"}]}
```

---

## 错误处理

```python
def safe_file_processing(file_path):
    """带错误处理的文件处理"""

    try:
        # 检查文件存在
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")

        # 检查文件大小（限制 8MB）
        file_size = os.path.getsize(file_path)
        if file_size > 8 * 1024 * 1024:
            raise ValueError(f"File too large: {file_size} bytes")

        # 处理文件
        client = anthropic.Anthropic()

        with open(file_path, "rb") as f:
            file_upload = client.beta.files.content.create(
                file=f,
                beta=["files-api-2025-04-14"]
            )

        return file_upload.id

    except FileNotFoundError as e:
        print(f"❌ {e}")
        return None
    except ValueError as e:
        print(f"❌ {e}")
        return None
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        return None
```

---

## 最佳实践

- ✅ 处理后及时下载文件
- ✅ 检查文件大小限制（8MB）
- ✅ 使用有意义的文件名
- ✅ 保存重要文件的 file_id
- ✅ 批量处理时控制文件数量

---

[返回主目录](./04-skills-integration-guide.md) | [下一篇：代码示例 →](./04e-api-examples.md)
