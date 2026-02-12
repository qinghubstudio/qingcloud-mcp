#!/bin/bash
# ==============================================================================
# search_xsc.sh - 搜索"重庆XSC试卷"并获取第一个用户的试卷/题目相关笔记
#
# 使用 MCP Streamable HTTP 协议与 qingcloud-mcp 服务器交互
# 流程: 登录检查 -> 搜索 -> 提取第一条 -> 获取帖子详情 -> 获取用户笔记 -> 过滤
# ==============================================================================

set -euo pipefail

MCP_URL="${MCP_URL:-http://localhost:8082/mcp}"
KEYWORD="${1:-重庆XSC试卷}"
OUTPUT_DIR="/tmp/xsc_search_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$OUTPUT_DIR"

# Colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

log()  { echo -e "${GREEN}[$(date +%H:%M:%S)]${NC} $*"; }
warn() { echo -e "${YELLOW}[$(date +%H:%M:%S)] ⚠${NC} $*"; }
err()  { echo -e "${RED}[$(date +%H:%M:%S)] ✗${NC} $*" >&2; }

# ------------------------------------------------------------------
# MCP Streamable HTTP helper
# Returns the JSON-RPC result body (extracts from SSE data: lines)
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

    curl -s --max-time 180 -D "$tmp_h" -X POST "$MCP_URL" "${headers[@]}" -d "$body" > "$tmp_b" 2>/dev/null

    # Extract session ID from headers on first call
    if [ -z "$SESSION_ID" ]; then
        SESSION_ID=$(grep -i "^Mcp-Session-Id:" "$tmp_h" 2>/dev/null | head -1 | sed 's/^[^:]*: *//' | tr -d '\r\n' || true)
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

# Extract text content from MCP tool call result
extract_text() {
    python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    for c in d.get('result',{}).get('content',[]):
        if c.get('type') == 'text':
            print(c['text'])
            break
except Exception as e:
    print('')
"
}

# ==============================================================================
# Step 1: Initialize MCP session
# ==============================================================================
log "Step 1: 初始化 MCP 会话..."
init_response=$(mcp_call "initialize" '{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"xsc-search","version":"1.0"}}')
# Extract Session ID from the header file created by mcp_call (which matches CALL_ID=1 inside the subshell)
SESSION_ID=$(grep -i "^Mcp-Session-Id:" "/tmp/xsc_h_1.txt" 2>/dev/null | head -1 | sed 's/^[^:]*: *//' | tr -d '\r\n' || true)

if echo "$init_response" | python3 -c "import sys,json; d=json.load(sys.stdin); assert 'result' in d" 2>/dev/null; then
    log "  ✓ 会话已建立 (Session: ${SESSION_ID})"
    
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

IS_LOGGED_IN=$(echo "$login_text" | python3 -c "import sys,json; print(json.load(sys.stdin).get('isLoggedIn',False))" 2>/dev/null || echo "False")

if [ "$IS_LOGGED_IN" = "True" ]; then
    log "  ✓ 已登录"
else
    warn "  未登录，正在触发登录..."
    echo "  请配合完成登录操作 (最大等待 120 秒)..."
    
    LOGIN_BODY='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"login","arguments":{}},"id":2}'

    log "  Session ID used: '$SESSION_ID'"

    echo "  Waiting for login completion (max 120s)..."
    login_resp=$(curl -s -X POST "$MCP_URL" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json, text/event-stream" \
      -H "Mcp-Session-Id: $SESSION_ID" \
      -d "$LOGIN_BODY" \
      --max-time 180)
    
    # Dump raw response for debugging
    echo "$login_resp" > "$OUTPUT_DIR/debug_login_response.txt"
    log "  Raw login response saved to $OUTPUT_DIR/debug_login_response.txt"
    echo "--- RAW RESPONSE START ---"
    cat "$OUTPUT_DIR/debug_login_response.txt"
    echo "--- RAW RESPONSE END ---"

    # Parse SSE response to extract JSON (remove 'data: ' prefix)
    login_json=$(echo "$login_resp" | grep "^data: " | sed 's/^data: //' | head -1)
    
    # Check result from login response
    login_text=$(echo "$login_json" | extract_text)
    
    # Verify login again to be sure
    check_again_resp=$(mcp_call "tools/call" '{"name":"checkLoginStatus","arguments":{}}')
    check_again_text=$(echo "$check_again_resp" | extract_text)
    IS_LOGGED_IN=$(echo "$check_again_text" | python3 -c "import sys,json; print(json.load(sys.stdin).get('isLoggedIn',False))" 2>/dev/null || echo "False")

    if [ "$IS_LOGGED_IN" = "True" ]; then
        log "  ✓ 登录成功"
    else
        err "  登录失败或超时"
        err "  登录响应: $login_text"
        err "  状态检查响应: $check_again_text"
        exit 1
    fi
fi

# ==============================================================================
# Step 3: Search notes
# ==============================================================================
log "Step 3: 搜索关键词 '$KEYWORD'..."
# Debug schema
log "  Debug: Fetching tool schema..."
mcp_call "tools/list" "{}" > "$OUTPUT_DIR/debug_tools_list.json"
cat "$OUTPUT_DIR/debug_tools_list.json" | grep "searchNotes" -A 20
log "  Debug: Schema fetched."

SEARCH_PARAMS="{\"name\":\"searchNotes\",\"arguments\":{\"keyword\":\"$KEYWORD\"}}"
log "  Params: $SEARCH_PARAMS"
search_resp=$(mcp_call "tools/call" "$SEARCH_PARAMS")
search_text=$(echo "$search_resp" | extract_text)
echo "$search_text" > "$OUTPUT_DIR/02_search_results.json"

# Parse search results
ITEM_COUNT=$(echo "$search_text" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    items = d.get('items', d.get('results', d.get('data', [])))
    if isinstance(items, dict): items = items.get('items', [])
    if isinstance(items, list): print(len(items))
    else: print(0)
except: print(0)
" 2>/dev/null || echo "0")

log "  搜索到 $ITEM_COUNT 条结果"

if [ "$ITEM_COUNT" = "0" ]; then
    err "  未搜索到结果"
    log "  原始响应: $search_text"
    exit 1
fi

# ==============================================================================
# Step 4: Extract first result info
# ==============================================================================
log "Step 4: 提取第一条结果..."
read -r NOTE_ID XSEC_TOKEN USER_ID TITLE <<< $(echo "$search_text" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read())
items = d.get('items', d.get('results', d.get('data', [])))
if isinstance(items, dict): items = items.get('items', [])
if items:
    item = items[0]
    nid = item.get('noteId','')
    xsec = item.get('xsecToken','')
    uid = item.get('userId','')
    title = item.get('title','N/A').replace(' ','_')[:50]
    print(f'{nid} {xsec} {uid} {title}')
else:
    print('   ')
" 2>/dev/null || echo "   ")

log "  标题: $TITLE"
log "  noteId: $NOTE_ID"
log "  xsecToken: ${XSEC_TOKEN:0:20}..."

# ==============================================================================
# Step 5: Get post detail to find userId
# ==============================================================================
if [ -z "$USER_ID" ] && [ -n "$NOTE_ID" ] && [ -n "$XSEC_TOKEN" ]; then
    log "Step 5: 获取帖子详情 (提取 userId)..."
    detail_resp=$(mcp_call "tools/call" "{\"name\":\"getPostDetail\",\"arguments\":{\"noteId\":\"$NOTE_ID\",\"xsecToken\":\"$XSEC_TOKEN\"}}")
    detail_text=$(echo "$detail_resp" | extract_text)
    echo "$detail_text" > "$OUTPUT_DIR/03_post_detail.json"

    USER_ID=$(echo "$detail_text" | python3 -c "
import sys, json
d = json.loads(sys.stdin.read())
# Search for userId in various paths
for path in [
    lambda x: x.get('userId',''),
    lambda x: x.get('user',{}).get('userId',''),
    lambda x: x.get('user',{}).get('uid',''),
    lambda x: x.get('noteData',{}).get('user',{}).get('userId',''),
    lambda x: x.get('note',{}).get('user',{}).get('userId',''),
    lambda x: x.get('data',{}).get('user',{}).get('userId',''),
]:
    try:
        uid = path(d)
        if uid:
            print(uid)
            exit()
    except: pass
print('')
" 2>/dev/null || echo "")
    log "  userId: $USER_ID"
elif [ -n "$USER_ID" ]; then
    log "Step 5: userId 已从搜索结果获取: $USER_ID"
fi

if [ -z "$USER_ID" ]; then
    err "  无法获取 userId"
    exit 1
fi

# ==============================================================================
# Step 6: Get user profile and all notes
# ==============================================================================
log "Step 6: 获取用户 $USER_ID 的笔记列表..."
profile_resp=$(mcp_call "tools/call" "{\"name\":\"getUserProfile\",\"arguments\":{\"userId\":\"$USER_ID\",\"xsecToken\":\"$XSEC_TOKEN\"}}")
profile_text=$(echo "$profile_resp" | extract_text)
echo "$profile_text" > "$OUTPUT_DIR/04_user_profile.json"

# ==============================================================================
# Step 7: Filter for exam/paper related notes
# ==============================================================================
log "Step 7: 过滤试卷/题目相关笔记..."

python3 << 'PYEOF' "$OUTPUT_DIR/04_user_profile.json" "$OUTPUT_DIR/05_filtered_notes.json"
import json, sys

input_file = sys.argv[1]
output_file = sys.argv[2]

KEYWORDS = [
    '试卷', '试题', '题目', '考试', '考题', '真题', '模拟题', '练习题',
    '测试', '测验', '卷子', '答案', '解析', '题型', '选择题', '填空题',
    '应用题', '计算题', '数学题', '语文题', '英语题', '期末', '期中',
    '月考', '小升初', 'XSC', 'xsc', '升学', '入学考试',
    '奥数', '竞赛', '杯赛', '刷题', '做题', '解题',
]

try:
    with open(input_file) as f:
        text = f.read().strip()
    if not text:
        print("没有获取到用户笔记数据")
        json.dump({"filtered": [], "total_notes": 0}, open(output_file, 'w'), ensure_ascii=False, indent=2)
        sys.exit(0)

    data = json.loads(text)

    # Find notes in various structures
    notes = []
    if isinstance(data, dict):
        for key in ['notes', 'noteList', 'items', 'data']:
            v = data.get(key, None)
            if isinstance(v, list) and v:
                notes = v
                break
        if not notes:
            for key in ['userData', 'user', 'profile']:
                sub = data.get(key, {})
                if isinstance(sub, dict):
                    for nk in ['notes', 'noteList']:
                        v = sub.get(nk, None)
                        if isinstance(v, list) and v:
                            notes = v
                            break
    elif isinstance(data, list):
        notes = data

    total = len(notes)

    # Filter
    filtered = []
    for note in notes:
        if not isinstance(note, dict):
            continue
        title = str(note.get('title', note.get('displayTitle', note.get('display_title', ''))))
        desc = str(note.get('desc', note.get('description', '')))
        nc = note.get('noteCard', {})
        if isinstance(nc, dict):
            title = title or nc.get('displayTitle', nc.get('title', ''))
            desc = desc or nc.get('desc', '')
        search_text = (title + ' ' + desc).lower()
        matched = [kw for kw in KEYWORDS if kw.lower() in search_text]
        if matched:
            filtered.append({
                'title': title, 'desc': desc[:200],
                'noteId': note.get('noteId', note.get('id', note.get('note_id', ''))),
                'matched_keywords': matched,
                'likes': note.get('likes', note.get('likedCount', '')),
            })

    result = {"total_notes": total, "filtered_count": len(filtered), "filtered_notes": filtered}
    with open(output_file, 'w') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"\n{'='*60}")
    print(f"用户总笔记数: {total}")
    print(f"试卷/题目相关: {len(filtered)}")
    print(f"{'='*60}")
    for i, n in enumerate(filtered, 1):
        print(f"\n[{i}] {n['title']}")
        if n['desc']: print(f"    描述: {n['desc'][:100]}...")
        print(f"    关键词: {', '.join(n['matched_keywords'])}")
        if n['noteId']: print(f"    noteId: {n['noteId']}")

    if not filtered and notes:
        print("\n⚠ 未找到试卷相关笔记。全部笔记标题：")
        for i, n in enumerate(notes[:20], 1):
            t = n.get('title', n.get('displayTitle', '')) if isinstance(n, dict) else str(n)
            nc2 = n.get('noteCard', {}) if isinstance(n, dict) else {}
            if not t and isinstance(nc2, dict): t = nc2.get('displayTitle', '')
            print(f"  [{i}] {t}")

except Exception as e:
    print(f"解析错误: {e}")
    import traceback; traceback.print_exc()
    json.dump({"error": str(e)}, open(output_file, 'w'), ensure_ascii=False, indent=2)
PYEOF

echo ""
log "数据已保存到: $OUTPUT_DIR/"
ls -la "$OUTPUT_DIR/"
