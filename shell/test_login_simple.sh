#!/bin/bash
# 简单测试脚本 - 只测试登录流程

set -e

MCP_URL="http://localhost:8089/mcp"

echo "=== 测试登录流程 ==="
echo ""

# 1. 初始化会话
echo "1. 初始化MCP会话..."
init_resp=$(curl -i -s "$MCP_URL" \
    -H "Accept: application/json, text/event-stream" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}')

session_id=$(echo "$init_resp" | grep -i "mcp-session-id:" | sed 's/.*: //;s/\r//' | tr -d '\n\r')
if [ -z "$session_id" ]; then
    echo "✗ 无法获取 session ID"
    echo "响应头:"
    echo "$init_resp" | head -20
    exit 1
fi

echo "✓ Session ID: $session_id"
echo ""

# 2. 检查登录状态
echo "2. 检查登录状态..."
check_resp=$(curl -s "$MCP_URL" \
    -H "Accept: application/json, text/event-stream" \
    -H "Content-Type: application/json" \
    -H "Mcp-Session-Id: $session_id" \
    -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"checkLoginStatus","arguments":{}},"id":2}')

echo "$check_resp" | grep -q '"isLoggedIn":true' && echo "✓ 已登录" || echo "✗ 未登录"
echo ""

# 3. 如果未登录,显示登录说明
if ! echo "$check_resp" | grep -q '"isLoggedIn":true'; then
    echo "=========================================="
    echo "要测试登录功能,请运行:"
    echo "  bash search_xsc.sh \"重庆XSC试卷\""
    echo ""
    echo "登录时:"
    echo "  1. 浏览器窗口会自动打开"
    echo "  2. 在浏览器中扫描二维码"
    echo "  3. 等待最多120秒"
    echo "=========================================="
fi

echo ""
echo "=== 测试完成 ==="
