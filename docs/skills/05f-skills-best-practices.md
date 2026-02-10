# 本地 Skills 最佳实践

## Skills 设计原则

### 1. 单一职责

每个 Skill 应该专注于一个特定领域：

```python
# ✅ 好的设计
class ContentResearchSkill:
    """专注内容研究"""
    def execute(self, topic): ...

class UserAnalysisSkill:
    """专注用户分析"""
    def execute(self, user_id): ...

# ❌ 不好的设计
class EverythingSkill:
    """什么都做"""
    def research_content(self): ...
    def analyze_user(self): ...
    def publish_post(self): ...
```

### 2. 可组合性

Skills 应该可以相互组合：

```python
class MarketingCampaignSkill:
    """营销活动技能（组合其他 Skills）"""

    def __init__(self, content_skill, user_skill):
        self.content_skill = content_skill
        self.user_skill = user_skill

    def execute(self, campaign_topic):
        # 1. 研究内容趋势
        trends = self.content_skill.execute(campaign_topic)

        # 2. 分析目标用户
        audience = self.user_skill.execute(trends["top_users"])

        # 3. 生成营销策略
        return self._generate_strategy(trends, audience)
```

### 3. 清晰的接口

提供清晰的输入输出定义：

```python
class Skill:
    def get_parameters(self) -> Dict:
        """
        返回清晰的参数定义
        - 必需参数明确标注
        - 提供默认值
        - 类型约束
        - 描述清楚
        """
        return {
            "type": "object",
            "properties": {
                "topic": {
                    "type": "string",
                    "description": "主题关键词",
                    "minLength": 1,
                    "maxLength": 50
                },
                "depth": {
                    "type": "string",
                    "enum": ["basic", "detailed"],
                    "default": "basic"
                }
            },
            "required": ["topic"]
        }
```

---

## 常见 Skills 模板

### 数据收集类

```python
class DataCollectionSkill(Skill):
    """数据收集模板"""

    def execute(self, target, filters=None):
        # 1. 收集原始数据
        raw_data = self._collect(target, filters)

        # 2. 数据清洗
        clean_data = self._clean(raw_data)

        # 3. 数据验证
        self._validate(clean_data)

        # 4. 返回结构化数据
        return {
            "data": clean_data,
            "metadata": {
                "count": len(clean_data),
                "collected_at": datetime.now(),
                "filters": filters
            }
        }
```

### 分析类

```python
class AnalysisSkill(Skill):
    """分析类模板"""

    def execute(self, data, analysis_type="basic"):
        # 1. 数据预处理
        processed = self._preprocess(data)

        # 2. 执行分析
        if analysis_type == "basic":
            result = self._basic_analysis(processed)
        else:
            result = self._advanced_analysis(processed)

        # 3. 生成洞察
        insights = self._generate_insights(result)

        # 4. 返回报告
        return {
            "analysis": result,
            "insights": insights,
            "recommendations": self._generate_recommendations(insights)
        }
```

### 生成类

```python
class GenerationSkill(Skill):
    """内容生成模板"""

    def execute(self, requirements):
        # 1. 理解需求
        parsed_req = self._parse_requirements(requirements)

        # 2. 收集素材
        materials = self._gather_materials(parsed_req)

        # 3. 生成内容
        content = self._generate(materials, parsed_req)

        # 4. 质量检查
        self._quality_check(content)

        # 5. 返回内容
        return {
            "content": content,
            "metadata": {
                "word_count": len(content),
                "based_on": materials
            }
        }
```

---

## 错误处理

### 优雅降级

```python
class ResilientSkill(Skill):
    """具有容错能力的 Skill"""

    def execute(self, **kwargs):
        try:
            return self._execute_with_full_features(**kwargs)

        except APILimitError:
            # 降级：使用缓存数据
            logger.warning("API limit reached, using cached data")
            return self._execute_with_cache(**kwargs)

        except DataNotFoundError as e:
            # 部分失败：返回可用数据
            logger.error(f"Some data not found: {e}")
            return {
                "status": "partial",
                "data": self._get_available_data(**kwargs),
                "errors": [str(e)]
            }

        except Exception as e:
            # 完全失败：返回有用的错误信息
            logger.exception("Skill execution failed")
            return {
                "status": "failed",
                "error": str(e),
                "suggestions": self._get_fallback_suggestions()
            }
```

### 重试机制

```python
from tenacity import retry, stop_after_attempt, wait_exponential

class ReliableSkill(Skill):

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(min=1, max=10)
    )
    def _call_mcp_tool(self, name, args):
        """带重试的 MCP 调用"""
        return self.mcp.call_tool(name, args)

    def execute(self, **kwargs):
        # 使用带重试的调用
        result = self._call_mcp_tool("searchNotes", {"keyword": kwargs["topic"]})
        return self._process(result)
```

---

## 性能优化

### 缓存策略

```python
from functools import lru_cache
import hashlib

class CachedSkill(Skill):
    """带缓存的 Skill"""

    def __init__(self, mcp_client):
        super().__init__(mcp_client)
        self.cache = {}

    def execute(self, **kwargs):
        # 生成缓存键
        cache_key = self._make_cache_key(kwargs)

        # 检查缓存
        if cache_key in self.cache:
            logger.info(f"Cache hit for {cache_key}")
            return self.cache[cache_key]

        # 执行并缓存
        result = self._do_execute(**kwargs)
        self.cache[cache_key] = result

        return result

    def _make_cache_key(self, kwargs):
        """生成缓存键"""
        sorted_items = sorted(kwargs.items())
        key_str = str(sorted_items)
        return hashlib.md5(key_str.encode()).hexdigest()
```

### 并行执行

```python
import asyncio

class ParallelSkill(Skill):
    """支持并行的 Skill"""

    async def execute_async(self, targets):
        """并行处理多个目标"""

        tasks = [
            self._process_one(target)
            for target in targets
        ]

        results = await asyncio.gather(*tasks, return_exceptions=True)

        # 过滤成功的结果
        successes = [r for r in results if not isinstance(r, Exception)]
        failures = [r for r in results if isinstance(r, Exception)]

        return {
            "successes": successes,
            "failures": len(failures),
            "total": len(targets)
        }

    async def _process_one(self, target):
        """处理单个目标"""
        # ... 实现
        pass
```

---

## 测试

### 单元测试

```python
import unittest
from unittest.mock import Mock, patch

class TestContentResearchSkill(unittest.TestCase):

    def setUp(self):
        # Mock MCP 客户端
        self.mcp_client = Mock()
        self.skill = ContentResearchSkill(self.mcp_client)

    def test_basic_research(self):
        """测试基础研究"""

        # 模拟 MCP 返回
        self.mcp_client.call_tool.return_value = {
            "data": {
                "items": [
                    {"title": "测试笔记", "likes": 100}
                ]
            }
        }

        # 执行
        result = self.skill.execute(topic="测试", depth="basic")

        # 验证
        self.assertIn("total", result)
        self.assertEqual(result["total"], 1)
        self.mcp_client.call_tool.assert_called_once()

    def test_error_handling(self):
        """测试错误处理"""

        # 模拟错误
        self.mcp_client.call_tool.side_effect = Exception("API Error")

        # 执行（不应抛出异常）
        result = self.skill.execute(topic="测试")

        # 验证返回了错误信息
        self.assertEqual(result["status"], "failed")
```

### 集成测试

```python
class TestSkillIntegration(unittest.TestCase):
    """测试 Skill 与真实 MCP 的集成"""

    @classmethod
    def setUpClass(cls):
        # 启动测试 MCP Server
        cls.mcp_client = MCPClient("http://localhost:8080/mcp")
        cls.skill = ContentResearchSkill(cls.mcp_client)

    def test_real_mcp_call(self):
        """测试真实 MCP 调用"""
        result = self.skill.execute(topic="咖啡", depth="basic")

        self.assertIsNotNone(result)
        self.assertIn("total", result)
        self.assertGreater(result["total"], 0)
```

---

## 文档化

### Skill 文档模板

```python
class DocumentedSkill(Skill):
    """
    内容研究技能

    功能：
        分析指定主题的内容趋势、热门标签和用户画像

    参数：
        topic (str): 研究主题，必需
        depth (str): 研究深度，可选值 'basic' 或 'detailed'，默认 'basic'

    返回：
        {
            "total": int,           # 分析的笔记总数
            "hot_tags": dict,       # 热门标签统计
            "top_users": list,      # 热门用户列表
            "user_details": list    # 用户详情（仅 detailed 模式）
        }

    示例：
        >>> skill = ContentResearchSkill(mcp_client)
        >>> result = skill.execute(topic="咖啡", depth="detailed")
        >>> print(result["total"])
        50

    依赖的 MCP Tools:
        - searchNotes: 搜索笔记
        - getUserProfile: 获取用户信息（detailed 模式）

    注意事项：
        - detailed 模式会增加更多 API 调用
        - 建议对结果进行缓存
    """
    pass
```

---

## Skills 生命周期管理

```python
class SkillLifecycleManager:
    """Skills 生命周期管理"""

    def __init__(self):
        self.registry = SkillRegistry()
        self.metrics = {}

    def register_skill(self, skill: Skill):
        """注册 Skill"""
        self.registry.register(skill)
        self.metrics[skill.name] = {
            "calls": 0,
            "errors": 0,
            "total_time": 0
        }

    def execute_skill(self, name: str, **kwargs):
        """执行 Skill 并记录指标"""
        skill = self.registry.get_skill(name)

        start = time.time()
        try:
            result = skill.execute(**kwargs)
            self.metrics[name]["calls"] += 1
            return result

        except Exception as e:
            self.metrics[name]["errors"] += 1
            raise

        finally:
            duration = time.time() - start
            self.metrics[name]["total_time"] += duration

    def get_metrics(self, name: str):
        """获取 Skill 指标"""
        m = self.metrics.get(name, {})
        if m.get("calls", 0) > 0:
            m["avg_time"] = m["total_time"] / m["calls"]
        return m
```

---

## 检查清单

创建新 Skill 时确认：

- [ ] 单一职责原则
- [ ] 清晰的参数定义
- [ ] 完善的错误处理
- [ ] 适当的缓存策略
- [ ] 单元测试覆盖
- [ ] 文档完整
- [ ] 日志记录
- [ ] 性能优化

---

[返回本地方案目录](./05-local-skills-solution.md)
