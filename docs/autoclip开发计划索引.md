# Autoclip MCP 服务开发文档索引

## 📋 主要规划文档

### 核心计划

- **[autoclip 开发计划.md](./autoclip开发计划.md)** - 完整开发计划（主文档，70+ MCP 工具）
- **[autoclip_plan_comparison.md](./autoclip_plan_comparison.md)** - 对比分析和整合建议

## 🔍 CapCut/VectCut 参考分析

### 源码分析（作为开发参考）

- **[capcut_analysis_01_architecture.md](./capcut_analysis_01_architecture.md)** - 整体架构
- **[capcut_analysis_02_mcp_integration.md](./capcut_analysis_02_mcp_integration.md)** - MCP 集成
- **[capcut_analysis_03_core_functions.md](./capcut_analysis_03_core_functions.md)** - 核心功能
- **[capcut_analysis_04_java_migration.md](./capcut_analysis_04_java_migration.md)** - Java 移植策略

## 🎯 核心技术路线

1. **直接操作 JSON 模板**（不移植 pyJianYingDraft 库）
2. **生成剪映/CapCut 草稿格式**
3. **延迟下载模式**（保存时才下载媒体）
4. **与 XHS 模块保持架构一致**

## 📚 阅读顺序

1. 先读 `autoclip开发计划.md` 了解完整规划
2. 再读 `capcut_analysis_04_java_migration.md` 理解技术路线
3. 其他分析文档作为开发参考

---

**最后更新**: 2025-12-24
