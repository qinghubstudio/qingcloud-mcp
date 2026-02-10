# 本地 Agent Skills 实现

## 概念澄清

### Claude Agent Skills vs 本地 Skills

**Claude Agent Skills（官方）：**

```
用户 → Claude API → Agent Skills (沙箱) → 执行
              ↑
        预置 + 自定义 Skills
```

**本地 Agent Skills：**

```
用户 → 本地 LLM → MCP Tools → 执行
              ↑
        Skills = 组织化的 MCP Tools
```

---

## Skills 的本质

在本地方案中，**Skills 就是有组织的 MCP Tools 集合**：

```python
# MCP Tool - 原子能力
def search_notes(keyword):
    """搜索笔记的单一功能"""
    pass

# Agent Skill - 组合能力
class ContentResearchSkill:
    """内容研究技能（组合多个工具）"""

    def research_topic(self, topic):
        # 1. 搜索相关内容
        notes = search_notes(topic)

        # 2. 分析用户画像
        users = get_user_profiles(notes)

        # 3. 提取热门标签
        tags = analyze_tags(notes)

        # 4. 生成研究报告
        return generate_report(notes, users, tags)
```

**关键区别：**

- **MCP Tool** = 单一功能（如搜索）
- **Agent Skill** = 多工具组合的业务能力（如内容研究）

---

## 实现本地 Skills

### 1. 定义 Skill 接口

```python
# skills/base.py
from abc import ABC, abstractmethod
from typing import Dict, Any

class Skill(ABC):
    """Skill 基类"""

    @property
    @abstractmethod
    def name(self) -> str:
        """技能名称"""
        pass

    @property
    @abstractmethod
    def description(self) -> str:
        """技能描述"""
        pass

    @abstractmethod
    def execute(self, **kwargs) -> Any:
        """执行技能"""
        pass

    def to_tool_schema(self) -> Dict:
        """转换为工具 Schema（供 LLM 调用）"""
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": self.get_parameters()
            }
        }

    @abstractmethod
    def get_parameters(self) -> Dict:
        """获取参数定义"""
        pass
```

### 2. 创建具体 Skill

```python
# skills/content_research.py
from skills.base import Skill
from mcp_client import MCPClient

class ContentResearchSkill(Skill):
    """内容研究技能"""

    def __init__(self, mcp_client: MCPClient):
        self.mcp = mcp_client

    @property
    def name(self) -> str:
        return "content_research"

    @property
    def description(self) -> str:
        return "研究指定主题的内容趋势、用户画像和热门标签"

    def get_parameters(self) -> Dict:
        return {
            "type": "object",
            "properties": {
                "topic": {
                    "type": "string",
                    "description": "研究主题"
                },
                "depth": {
                    "type": "string",
                    "enum": ["basic", "detailed"],
                    "default": "basic",
                    "description": "研究深度"
                }
            },
            "required": ["topic"]
        }

    def execute(self, topic: str, depth: str = "basic") -> Dict:
        """执行内容研究"""

        # 1. 搜索相关笔记
        notes_result = self.mcp.call_tool("searchNotes", {
            "keyword": topic,
            "page_size": 50 if depth == "detailed" else 20
        })

        notes = notes_result["data"]["items"]

        # 2. 分析数据
        analysis = self._analyze_notes(notes)

        # 3. 如果需要详细分析，获取用户信息
        if depth == "detailed":
            top_users = analysis["top_users"]
            user_profiles = []

            for user_id in top_users:
                profile = self.mcp.call_tool("getUserProfile", {
                    "userId": user_id
                })
                user_profiles.append(profile)

            analysis["user_details"] = user_profiles

        return analysis

    def _analyze_notes(self, notes) -> Dict:
        """分析笔记数据"""
        # 提取标签、统计互动等
        tags = {}
        top_users = []

        for note in notes:
            # 统计逻辑...
            pass

        return {
            "total": len(notes),
            "hot_tags": tags,
            "top_users": top_users
        }
```

### 3. 注册 Skills

```python
# skills/registry.py
class SkillRegistry:
    """Skills 注册表"""

    def __init__(self, mcp_client):
        self.mcp = mcp_client
        self.skills = {}

    def register(self, skill: Skill):
        """注册技能"""
        self.skills[skill.name] = skill

    def get_skill(self, name: str) -> Skill:
        """获取技能"""
        return self.skills.get(name)

    def list_skills(self) -> list:
        """列出所有技能"""
        return list(self.skills.values())

    def to_tool_schemas(self) -> list:
        """转换为工具 Schema 列表"""
        return [skill.to_tool_schema() for skill in self.skills.values()]

# 使用
registry = SkillRegistry(mcp_client)
registry.register(ContentResearchSkill(mcp_client))
registry.register(CompetitorAnalysisSkill(mcp_client))
```

---

## 集成到 Agent

### 增强版 Agent

```python
# local_skills_agent.py (增强版)
import ollama
from skills.registry import SkillRegistry
from mcp_client import MCPClient

class LocalSkillsAgent:
    """支持 Skills 的本地 Agent"""

    def __init__(
        self,
        mcp_server_url: str = "http://localhost:8080/mcp",
        llm_model: str = "qwen2.5:7b"
    ):
        self.mcp_client = MCPClient(mcp_server_url)
        self.llm_model = llm_model
        self.skill_registry = SkillRegistry(self.mcp_client)

        # 注册 Skills
        self._register_skills()

        # 获取所有可用工具（MCP Tools + Agent Skills）
        self.tools = self._load_all_tools()
        self.messages = []

    def _register_skills(self):
        """注册所有 Skills"""
        from skills.content_research import ContentResearchSkill
        from skills.competitor_analysis import CompetitorAnalysisSkill

        self.skill_registry.register(ContentResearchSkill(self.mcp_client))
        self.skill_registry.register(CompetitorAnalysisSkill(self.mcp_client))

    def _load_all_tools(self):
        """加载所有工具（MCP + Skills）"""

        # 1. 基础 MCP Tools
        mcp_tools = self._convert_mcp_tools(
            self.mcp_client.list_tools()
        )

        # 2. Agent Skills
        skill_tools = self.skill_registry.to_tool_schemas()

        # 合并
        return mcp_tools + skill_tools

    def chat(self, user_message: str) -> str:
        """对话（支持 Skills）"""

        self.messages.append({
            "role": "user",
            "content": user_message
        })

        response = ollama.chat(
            model=self.llm_model,
            messages=self.messages,
            tools=self.tools
        )

        assistant_msg = response["message"]
        self.messages.append(assistant_msg)

        if assistant_msg.get("tool_calls"):
            return self._handle_tool_calls(assistant_msg["tool_calls"])

        return assistant_msg["content"]

    def _handle_tool_calls(self, tool_calls):
        """处理工具调用（区分 Skill 和 MCP Tool）"""

        for tool_call in tool_calls:
            func_name = tool_call["function"]["name"]
            func_args = tool_call["function"]["arguments"]

            # 检查是否是 Skill
            skill = self.skill_registry.get_skill(func_name)

            if skill:
                print(f"⭐ 执行 Skill: {func_name}")
                result = skill.execute(**func_args)
            else:
                print(f"🔧 调用 MCP Tool: {func_name}")
                result = self.mcp_client.call_tool(func_name, func_args)

            self.messages.append({
                "role": "tool",
                "content": str(result)
            })

        # 生成最终回答
        final_response = ollama.chat(
            model=self.llm_model,
            messages=self.messages
        )

        return final_response["message"]["content"]
```

---

## 使用示例

```python
# main.py
from local_skills_agent import LocalSkillsAgent

agent = LocalSkillsAgent()

# 使用 Skill
response = agent.chat("研究一下'咖啡'主题的内容趋势")

# Agent 会：
# 1. 识别需要使用 content_research Skill
# 2. 调用 Skill，Skill 内部会调用多个 MCP Tools
# 3. 返回综合分析结果

print(response)
# 输出：根据分析，'咖啡'主题的内容趋势...
```

---

## 预置 Skills 示例

```python
# skills/competitor_analysis.py
class CompetitorAnalysisSkill(Skill):
    """竞品分析技能"""

    def execute(self, competitors: list) -> Dict:
        """分析竞争对手"""

        results = []

        for comp_id in competitors:
            # 获取用户信息
            profile = self.mcp.call_tool("getUserProfile", {
                "userId": comp_id
            })

            # 分析发布频率、互动情况等
            analysis = self._深度分析(profile)
            results.append(analysis)

        return {
            "competitors": results,
            "recommendations": self._generate_recommendations(results)
        }
```

---

## Skills vs MCP Tools 对比

| 特性   | MCP Tools   | Agent Skills    |
| ------ | ----------- | --------------- |
| 粒度   | 单一功能    | 组合能力        |
| 复杂度 | 简单        | 可复杂          |
| 示例   | searchNotes | contentResearch |
| 实现   | 固定接口    | 自定义逻辑      |
| 调用   | 直接调用    | 编排多个 Tools  |

---

[下一篇：Skills 最佳实践 →](./05f-skills-best-practices.md)

[返回本地方案目录](./05-local-skills-solution.md)
