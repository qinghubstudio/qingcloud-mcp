#!/bin/bash
# ==============================================================================
# search-xhs.sh - 搜索"重庆XSC试卷"并获取第一个用户的试卷/题目相关笔记
#
# 使用 MCP Streamable HTTP 协议与 qingcloud-mcp 服务器交互
# 流程: 登录检查 -> 搜索 -> 提取第一条 -> 获取帖子详情 -> 获取用户笔记 -> 过滤
# ==============================================================================

set -euo pipefail

MCP_URL="${MCP_URL:-http://localhost:8082/mcp}"
KEYWORD="${1:-重庆XSC试卷}"
OUTPUT_DIR="./search_output"
mkdir -p "$OUTPUT_DIR"

# Colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $*"; }
warn() { echo -e "${YELLOW}[$(date +%H:%M:%S)] ⚠${NC} $*"; }
err()  { echo -e "${RED}[$(date +%H:%M:%S)] ✗${NC} $*" >&2; }

# Check for dependencies
if ! command -v jq &> /dev/null; then
    err "jq is required but not installed. Please install it (e.g., sudo apt install jq)."
    exit 1
fi

# ------------------------------------------------------------------
# MCP Streamable HTTP helper
# ------------------------------------------------------------------
SESSION_ID=""
CALL_ID=0

mcp_call() {
    local method="$1"
    local params="$2"
    CALL_ID=$((CALL_ID + 1))

    local headers=(-H "Content-Type: application/json" -H "Accept: application/json, text/event-stream")
    if [ -n "$SESSION_ID" ]; then
        headers+=(-H "Mcp-Session-Id: $SESSION_ID")
    fi

    local body
    if [ "$params" = "null" ] || [ -z "$params" ]; then
        body="{\"jsonrpc\":\"2.0\",\"method\":\"$method\",\"id\":$CALL_ID}"
    else
        body="{\"jsonrpc\":\"2.0\",\"method\":\"$method\",\"params\":$params,\"id\":$CALL_ID}"
    fi

    local tmp_h="/tmp/xsc_h_$CALL_ID.txt"
    local tmp_b="/tmp/xsc_b_$CALL_ID.txt"

    curl -s --max-time 180 -D "$tmp_h" -X POST "$MCP_URL" "${headers[@]}" -d "$body" > "$tmp_b" 2>/dev/null || true

    # Extract session ID from headers if not already set (subshell scope safe)
    if [ -z "$SESSION_ID" ]; then
        SESSION_ID=$(grep -i "^Mcp-Session-Id:" "$tmp_h" 2>/dev/null | head -1 | awk -F': ' '{print $2}' | tr -d '\r\n')
    fi

    local content_type
    content_type=$(grep -i "^Content-Type:" "$tmp_h" 2>/dev/null | head -1 || echo "")

    # Parse response: if SSE, extract data: lines; otherwise return as-is
    if echo "$content_type" | grep -qi "text/event-stream"; then
        grep "^data: " "$tmp_b" | sed 's/^data: //' | head -1
    else
        cat "$tmp_b"
    fi
}

# Extract text content from MCP tool call result using jq (MUCH SAFER than Python for this)
extract_text() {
    local input=$(cat)
    if [ -z "$input" ]; then echo ""; return; fi
    echo "$input" | jq -r '.result.content[] | select(.type=="text") | .text' 2>/dev/null || echo ""
}

# ==============================================================================
# Step 1: Initialize MCP session
# ==============================================================================
log "Step 1: 初始化 MCP 会话..."
# Increment CALL_ID in parent shell to keep sync
CALL_ID=$((CALL_ID + 1))
tmp_h="/tmp/xsc_h_$CALL_ID.txt"
tmp_b="/tmp/xsc_b_$CALL_ID.txt"

curl -s --max-time 180 -D "$tmp_h" -X POST "$MCP_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json, text/event-stream" \
    -d '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"xsc-search","version":"1.0"}},"id":'$CALL_ID'}' > "$tmp_b"

init_response=$(cat "$tmp_b")
SESSION_ID=$(grep -i "Mcp-Session-Id:" "$tmp_h" | head -1 | awk -F': ' '{print $2}' | tr -d '\r\n')

if echo "$init_response" | jq -e '.result' >/dev/null 2>&1; then
    log "  ✓ 会话已建立 (Session: ${SESSION_ID:-unknown})"
    if [ -z "$SESSION_ID" ]; then
        err "  未能获取 Session ID"
        exit 1
    fi
else
    err "  初始化失败: $init_response"
    exit 1
fi

# ==============================================================================
# Step 2: Check login status
# ==============================================================================
log "Step 2: 检查登录状态..."
login_resp=$(mcp_call "tools/call" '{"name":"checkLoginStatus","arguments":{}}')
login_text=$(echo "$login_resp" | extract_text)
echo "$login_text" > "$OUTPUT_DIR/01_login_status.json"

IS_LOGGED_IN=$(echo "$login_text" | jq -r '.isLoggedIn // false' 2>/dev/null || echo "false")

if [ "$IS_LOGGED_IN" = "true" ]; then
    log "  ✓ 已登录"
else
    warn "  未登录，正在触发登录..."
    echo "  请配合完成登录操作系统将等待扫码 (最大等待 120 秒)..."
    
    # Force fresh login
    LOGIN_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{"forceRefresh":true}},"id":2}'

    login_resp_full=$(curl -s -X POST "$MCP_URL" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json, text/event-stream" \
      -H "Mcp-Session-Id: $SESSION_ID" \
      -d "$LOGIN_BODY" \
      --max-time 180)
    
    login_json=$(echo "$login_resp_full" | grep "^data: " | sed 's/^data: //' | head -1)
    login_text=$(echo "$login_json" | extract_text)
    
    # Final check
    check_again_resp=$(mcp_call "tools/call" '{"name":"checkLoginStatus","arguments":{}}')
    check_again_text=$(echo "$check_again_resp" | extract_text)
    IS_LOGGED_IN=$(echo "$check_again_text" | jq -r '.isLoggedIn // false' 2>/dev/null || echo "false")

    if [ "$IS_LOGGED_IN" = "true" ]; then
        log "  ✓ 登录成功"
    else
        err "  登录失败或超时"
        err "  最后状态: $check_again_text"
        exit 1
    fi
fi

# ==============================================================================
# Step 3: Search notes
# ==============================================================================
log "Step 3: 搜索关键词 '$KEYWORD'..."
SEARCH_PARAMS=$(printf '{"name":"searchNotes","arguments":{"keyword":"%s"}}' "$KEYWORD")
search_resp=$(mcp_call "tools/call" "$SEARCH_PARAMS")
search_text=$(echo "$search_resp" | extract_text)
echo "$search_text" > "$OUTPUT_DIR/02_search_results.json"

ITEM_COUNT=$(echo "$search_text" | jq '.data.total // .total // 0' 2>/dev/null || echo "0")
log "  搜索到 $ITEM_COUNT 条结果"

if [ "$ITEM_COUNT" = "0" ] || [ "$ITEM_COUNT" = "null" ]; then
    err "  未搜索到结果"
    log "  响应详情已保存到 $OUTPUT_DIR/02_search_results.json"
    exit 1
fi

# ==============================================================================
# Step 4: Extract first result info
# ==============================================================================
log "Step 4: 提取第一条结果..."
NOTE_ID=$(echo "$search_text" | jq -r '.data.items[0].noteId // .items[0].noteId // empty')
XSEC_TOKEN=$(echo "$search_text" | jq -r '.data.items[0].xsecToken // .items[0].xsecToken // empty')
USER_ID=$(echo "$search_text" | jq -r '.data.items[0].userId // .items[0].userId // empty')
TITLE=$(echo "$search_text" | jq -r '.data.items[0].title // .items[0].title // "NoTitle"' | tr ' ' '_')

if [ -z "$NOTE_ID" ]; then
    err "  未能提取 Note ID"
    exit 1
fi

log "  标题: $TITLE"
log "  noteId: $NOTE_ID"
log "  userId: ${USER_ID:-Unknown}"

# ==============================================================================
# Step 5: Get post detail if userId missing
# ==============================================================================
if [ -z "$USER_ID" ] || [ "$USER_ID" = "null" ]; then
    log "Step 5: 获取帖子详情 (提取 userId)..."
    detail_resp=$(mcp_call "tools/call" "$(printf '{"name":"getPostDetail","arguments":{"noteId":"%s","xsecToken":"%s"}}' "$NOTE_ID" "$XSEC_TOKEN")")
    detail_text=$(echo "$detail_resp" | extract_text)
    echo "$detail_text" > "$OUTPUT_DIR/03_post_detail.json"

    USER_ID=$(echo "$detail_text" | jq -r '.. | .userId? // empty' | head -1)
    log "  userId (extracted): $USER_ID"
fi

if [ -z "$USER_ID" ] || [ "$USER_ID" = "null" ]; then
    err "  无法获取 userId"
    exit 1
fi

# ==============================================================================
# Step 6: Get user profile and all notes
# ==============================================================================
log "Step 6: 获取获取获取用户 $USER_ID 的笔记列表..."
profile_resp=$(mcp_call "tools/call" "$(printf '{"name":"getUserProfile","arguments":{"userId":"%s","xsecToken":"%s"}}' "$USER_ID" "$XSEC_TOKEN")")
profile_text=$(echo "$profile_resp" | extract_text)
echo "$profile_text" > "$OUTPUT_DIR/04_user_profile.json"

# ==============================================================================
# Step 7: Filter for exam/paper related notes (Robust Python block)
# ==============================================================================
log "Step 7: 过滤试卷/题目相关笔记..."

python3 << 'PYEOF' "$OUTPUT_DIR/04_user_profile.json" "$OUTPUT_DIR/05_filtered_notes.json"
import json, sys, os

input_file = sys.argv[1]
output_file = sys.argv[2]

KEYWORDS = [
    '试卷', '试题', '题目', '考试', '考题', '真题', '模拟题', '练习题',
    '测试', '测验', '卷子', '答案', '解析', '题型', '选择题', '填空题',
    '应用题', '计算题', '数学题', '语文题', '英语题', '期末', '期中',
    '月考', '小升初', 'XSC', 'xsc', '升学', '入学考试',
    '奥数', '竞赛', '杯赛', '刷题', '做题', '解题',
]

def safe_get_notes(data):
    if isinstance(data, list): return data
    if not isinstance(data, dict): return []
    
    # Try common keys
    for key in ['notes', 'noteList', 'items', 'data']:
        val = data.get(key)
        if isinstance(val, list): return val
        if isinstance(val, dict):
            for sub_key in ['notes', 'noteList', 'items']:
                sub_val = val.get(sub_key)
                if isinstance(sub_val, list): return sub_val
    
    # Deep search
    if 'user' in data and isinstance(data['user'], dict):
        return safe_get_notes(data['user'])
    if 'userData' in data and isinstance(data['userData'], dict):
        return safe_get_notes(data['userData'])
        
    return []

try:
    if not os.path.exists(input_file):
        print(f"错误: 输入文件 {input_file} 不存在")
        sys.exit(1)
        
    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read().strip()
        if not content:
            print("警告: 输入文件为空")
            data = {}
        else:
            data = json.loads(content)

    notes = safe_get_notes(data)
    total = len(notes)

    filtered = []
    for note in notes:
        if not isinstance(note, dict): continue
        
        # Extract fields robustly
        title = str(note.get('title') or note.get('displayTitle') or '')
        desc = str(note.get('desc') or note.get('description') or '')
        
        target = (title + ' ' + desc).lower()
        matched = [kw for kw in KEYWORDS if kw.lower() in target]
        
        if matched:
            filtered.append({
                'title': title,
                'desc': desc[:200],
                'noteId': note.get('noteId') or note.get('id') or '',
                'matched_keywords': matched,
                'likes': str(note.get('likes') or note.get('likedCount') or '0')
            })

    result = {
        "total_notes": total,
        "filtered_count": len(filtered),
        "filtered_notes": filtered
    }
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"\n" + "="*60)
    print(f"用户总笔记数: {total}")
    print(f"试卷/题目相关: {len(filtered)}")
    print("="*60)
    
    for i, n in enumerate(filtered, 1):
        print(f"\n[{i}] {n['title']}")
        if n['desc']: print(f"    描述: {n['desc'][:100]}...")
        print(f"    关键词: {', '.join(n['matched_keywords'])}")

except Exception as e:
    print(f"处理脚本出错: {str(e)}")
    import traceback
    traceback.print_exc()
PYEOF

echo ""
log "数据已保存到: $OUTPUT_DIR/"
ls -lh "$OUTPUT_DIR/"
