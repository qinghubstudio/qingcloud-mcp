# 完整代码示例

## Python 示例

### 基础集成

```python
#!/usr/bin/env python3
"""
Claude Skills 基础集成示例
"""
import os
from anthropic import Anthropic

class SkillsClient:
    """封装 Skills API 调用"""

    def __init__(self, api_key=None):
        self.client = Anthropic(
            api_key=api_key or os.environ.get("ANTHROPIC_API_KEY")
        )
        self.model = "claude-3-7-sonnet-20250219"

    def call_with_skills(self, message, skills):
        """使用 Skills 调用 Claude"""

        response = self.client.messages.create(
            model=self.model,
            max_tokens=4096,
            beta=[
                "code-execution-2025-08-25",
                "skills-2025-10-02",
                "files-api-2025-04-14"
            ],
            tools=[{"type": "code_execution"}],
            container={
                "preloaded": skills
            },
            messages=[{
                "role": "user",
                "content": message
            }]
        )

        return response

    def extract_text(self, response):
        """提取文本响应"""
        texts = []
        for block in response.content:
            if block.type == "text":
                texts.append(block.text)
        return "\n".join(texts)

# 使用示例
if __name__ == "__main__":
    client = SkillsClient()

    response = client.call_with_skills(
        message="Create a simple Excel budget",
        skills=[{
            "type": "anthropic",
            "skill_id": "xlsx",
            "version": "latest"
        }]
    )

    print(client.extract_text(response))
```

---

## Java 示例

```java
package com.example.skills;

import com.anthropic.client.AnthropicClient;
import com.anthropic.types.*;
import java.util.List;

public class SkillsExample {

    private final AnthropicClient client;
    private static final String MODEL = "claude-3-7-sonnet-20250219";

    public SkillsExample(String apiKey) {
        this.client = AnthropicClient.builder()
            .apiKey(apiKey)
            .build();
    }

    public MessageResponse callWithSkills(
        String userMessage,
        List<Skill> skills
    ) {
        return client.messages().create(
            MessageRequest.builder()
                .model(MODEL)
                .maxTokens(4096)
                .beta(List.of(
                    "code-execution-2025-08-25",
                    "skills-2025-10-02",
                    "files-api-2025-04-14"
                ))
                .tools(List.of(
                    Tool.codeExecution()
                ))
                .container(Container.builder()
                    .preloaded(skills)
                    .build()
                )
                .messages(List.of(
                    Message.ofUser(userMessage)
                ))
                .build()
        );
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        SkillsExample example = new SkillsExample(apiKey);

        Skill excelSkill = Skill.builder()
            .type("anthropic")
            .skillId("xlsx")
            .version("latest")
            .build();

        MessageResponse response = example.callWithSkills(
            "Create an Excel budget",
            List.of(excelSkill)
        );

        response.getContent().forEach(block -> {
            if (block instanceof TextBlock) {
                System.out.println(((TextBlock) block).getText());
            }
        });
    }
}
```

---

## Node.js 示例

```javascript
// skills-client.js
const Anthropic = require("@anthropic-ai/sdk");

class SkillsClient {
  constructor(apiKey) {
    this.client = new Anthropic({
      apiKey: apiKey || process.env.ANTHROPIC_API_KEY,
    });
    this.model = "claude-3-7-sonnet-20250219";
  }

  async callWithSkills(message, skills) {
    const response = await this.client.messages.create({
      model: this.model,
      max_tokens: 4096,
      beta: [
        "code-execution-2025-08-25",
        "skills-2025-10-02",
        "files-api-2025-04-14",
      ],
      tools: [{ type: "code_execution" }],
      container: {
        preloaded: skills,
      },
      messages: [
        {
          role: "user",
          content: message,
        },
      ],
    });

    return response;
  }

  extractText(response) {
    return response.content
      .filter((block) => block.type === "text")
      .map((block) => block.text)
      .join("\n");
  }
}

// 使用示例
async function main() {
  const client = new SkillsClient();

  const response = await client.callWithSkills("Create a simple Excel budget", [
    {
      type: "anthropic",
      skill_id: "xlsx",
      version: "latest",
    },
  ]);

  console.log(client.extractText(response));
}

main().catch(console.error);
```

---

## Spring Boot 集成

```java
// SkillsService.java
package com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.anthropic.client.AnthropicClient;

@Service
public class SkillsService {

    private final AnthropicClient client;

    public SkillsService(@Value("${anthropic.api.key}") String apiKey) {
        this.client = AnthropicClient.builder()
            .apiKey(apiKey)
            .build();
    }

    public String processWithExcelSkill(String instruction) {
        Skill excelSkill = Skill.builder()
            .type("anthropic")
            .skillId("xlsx")
            .version("latest")
            .build();

        MessageResponse response = client.messages().create(
            // ... 构建请求
        );

        return extractText(response);
    }
}

// SkillsController.java
@RestController
@RequestMapping("/api/skills")
public class SkillsController {

    @Autowired
    private SkillsService skillsService;

    @PostMapping("/excel")
    public ResponseEntity<String> processExcel(
        @RequestBody String instruction
    ) {
        String result = skillsService.processWithExcelSkill(instruction);
        return ResponseEntity.ok(result);
    }
}
```

---

## FastAPI 集成

```python
# main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import anthropic
import os

app = FastAPI()

client = anthropic.Anthropic(
    api_key=os.environ.get("ANTHROPIC_API_KEY")
)

class SkillRequest(BaseModel):
    message: str
    skill_id: str = "xlsx"

@app.post("/api/skills/process")
async def process_with_skills(request: SkillRequest):
    try:
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
                    "type": "anthropic",
                    "skill_id": request.skill_id,
                    "version": "latest"
                }]
            },
            messages=[{
                "role": "user",
                "content": request.message
            }]
        )

        # 提取文本
        text = "\n".join([
            block.text for block in response.content
            if block.type == "text"
        ])

        return {"result": text}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

---

## 测试工具类

```python
# test_skills.py
import unittest
from unittest.mock import Mock, patch
from skills_client import SkillsClient

class TestSkillsClient(unittest.TestCase):

    def setUp(self):
        self.client = SkillsClient(api_key="test-key")

    @patch('anthropic.Anthropic')
    def test_call_with_skills(self, mock_anthropic):
        # Mock 响应
        mock_response = Mock()
        mock_response.content = [
            Mock(type="text", text="Test response")
        ]

        mock_anthropic.return_value.messages.create.return_value = mock_response

        # 测试
        response = self.client.call_with_skills(
            "Test message",
            [{"type": "anthropic", "skill_id": "xlsx"}]
        )

        self.assertIsNotNone(response)

    def test_extract_text(self):
        mock_response = Mock()
        mock_response.content = [
            Mock(type="text", text="Hello"),
            Mock(type="code", code="print()"),
            Mock(type="text", text="World")
        ]

        result = self.client.extract_text(mock_response)
        self.assertEqual(result, "Hello\nWorld")

if __name__ == '__main__':
    unittest.main()
```

---

[返回主目录](./04-skills-integration-guide.md) | [下一篇：MCP 概述 →](./04f-mcp-overview.md)
