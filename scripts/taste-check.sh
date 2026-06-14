#!/bin/bash
# 品味不变式自动检查 — 多技术栈兼容版
# 用法: bash scripts/taste-check.sh [src目录]
# 返回: 0=通过, 1=有违规
# 自动检测技术栈（src/下的文件后缀），支持 java/python/javascript/go

set -e
SRC_DIR="${1:-src}"
TASTE_FILE=".coding-taste.yaml"
HAS_ERRORS=0

# 注意：以下硬编码阈值与 .coding-taste.yaml style 段保持同步
# 修改 .coding-taste.yaml 时请同步修改本脚本中所有对应数字
# 后续可改为从 yaml 读取（当前因 awk 单引号限制保持硬编码）

echo "=== 品味不变式检查 ==="
echo ""

if [ ! -f "$TASTE_FILE" ]; then
  echo " 未定义 .coding-taste.yaml，跳过自动检查"
  echo "   如需启用，请创建 .coding-taste.yaml（参考 Hermes-标准化研发流程.md Step 1 的说明）"
  exit 0
fi

# ---------- 技术栈自动检测 ----------
detect_tech_stack() {
  local java_count python_count js_count go_count

  java_count=$(find "$SRC_DIR" -name "*.java" 2>/dev/null | wc -l)
  python_count=$(find "$SRC_DIR" -name "*.py" 2>/dev/null | wc -l)
  js_count=$(find "$SRC_DIR" -name "*.js" -o -name "*.ts" -o -name "*.jsx" -o -name "*.tsx" 2>/dev/null | wc -l)
  go_count=$(find "$SRC_DIR" -name "*.go" 2>/dev/null | wc -l)

  echo "文件统计: Java=${java_count}  Python=${python_count}  JS/TS=${js_count}  Go=${go_count}"

  # 取出现最多的栈
  if [ "$java_count" -ge "$python_count" ] && [ "$java_count" -ge "$js_count" ] && [ "$java_count" -ge "$go_count" ] && [ "$java_count" -gt 0 ]; then
    echo "检测到技术栈: Java"
    return 0
  elif [ "$python_count" -ge "$js_count" ] && [ "$python_count" -ge "$go_count" ] && [ "$python_count" -gt 0 ]; then
    echo "检测到技术栈: Python"
    return 1
  elif [ "$js_count" -ge "$go_count" ] && [ "$js_count" -gt 0 ]; then
    echo "检测到技术栈: JavaScript/TypeScript"
    return 2
  elif [ "$go_count" -gt 0 ]; then
    echo "检测到技术栈: Go"
    return 3
  else
    echo "未识别到已知代码文件（.java/.py/.js/.ts/.go），默认 Java 检查"
    return 0
  fi
}

# 用 || true 避免 set -e 阻止正常返回
STACK_EXIT=0
detect_tech_stack || STACK_EXIT=$?

echo ""
echo "--- 通用检查 ---"

# TODO 检查
found_todo=0
for ext in java py js ts jsx tsx go; do
  if find "$SRC_DIR" -name "*.$ext" -exec grep -l "TODO" {} + 2>/dev/null | head -5 | grep -q .; then
    found_todo=1
  fi
done
if [ "$found_todo" = "1" ]; then
  echo " TODO 注释（建议在主分支提交前清理）"
fi

# 硬编码密码检查（跨栈通用）
if grep -rnP '(?i)(password|secret|api[_-]?key)\s*[=:]\s*['"'"'"][^'"'"'"]+['"'"'"]' \
  --include="*.properties" --include="*.yml" --include="*.yaml" --include="*.env" --include="*.json" \
  "$SRC_DIR" 2>/dev/null | grep -v 'example\|template\|sample\|demo' | head -5 | grep -q .; then
  echo " 疑似密码硬编码（必须用环境变量）"
  HAS_ERRORS=1
fi

# XSS 检查（跨栈通用）
for ext in html htm js jsx ts tsx vue php; do
  if grep -rnP '(?i)\.innerHTML\s*=' "$SRC_DIR" --include="*.$ext" 2>/dev/null | grep -v 'textContent\|//.*innerHTML\|/\*.*innerHTML' | head -5 | grep -q .; then
    echo " 发现 .innerHTML 赋值（有 XSS 风险，建议用 textContent 或模板引擎）"
    HAS_ERRORS=1
    break
  fi
done

# ---------- Java 检查 ----------
java_checks() {
  echo ""
  echo "--- Java 检查 ---"

  # UpperCamelCase 类名
  while IFS= read -r f; do
    basename=$(basename "$f" .java)
    if echo "$basename" | grep -q '^[a-z]'; then
      echo " 类名应 UpperCamelCase: $f"
      HAS_ERRORS=1
    fi
  done < <(find "$SRC_DIR" -name "*.java" 2>/dev/null)

  # controller -> repo 跨越
  if grep -qr 'controller.*import.*[Rr]epositor\|@Autowired.*[Rr]epositor' "$SRC_DIR" 2>/dev/null; then
    echo " Controller 直接依赖 Repository（违反分层约束）"
    HAS_ERRORS=1
  fi

  # 空 catch
  if grep -rnP 'catch\s*\([^)]*\)\s*\{\s*\}' "$SRC_DIR" --include="*.java" 2>/dev/null; then
    echo " 发现空 catch 块（不允许无处理逻辑的 catch）"
    HAS_ERRORS=1
  fi

  # System.out.println
  if grep -rn "System\.out\.println" "$SRC_DIR" --include="*.java" 2>/dev/null; then
    echo " 发现 System.out.println（必须用 Logger）"
    HAS_ERRORS=1
  fi

  # SQL 拼接
  if grep -rnP 'String\s+\w*[Ss]ql\s*=\s*"SELECT.*\+|\+.*"WHERE' --include="*.java" "$SRC_DIR" 2>/dev/null; then
    echo " 疑似 SQL 拼接（必须用参数化查询 / MyBatis / JPA）"
    HAS_ERRORS=1
  fi

  # 方法超过 30 行
  find "$SRC_DIR" -name "*.java" 2>/dev/null | while read f; do
    awk '
      /(public|private|protected).*\(.*\)\s*\{/ { method_line=$0; method_start=NR; in_method=1; line_count=0; next }
      in_method {
        line_count++
        if ($0 ~ /^\s*\}/) {
          if (line_count > 30) {
            printf " 方法超过 30 行（%d行）: %s:%d %s\n", line_count, FILENAME, method_start, substr(method_line,1,60)
          }
          in_method=0
        }
      }
    ' "$f" 2>/dev/null
  done

  # 类超过 300 行
  find "$SRC_DIR" -name "*.java" 2>/dev/null | while read f; do
    lines=$(wc -l < "$f")
    if [ "$lines" -gt 300 ]; then
      echo " 类超过 300 行（${lines}行）: $f（建议拆分）"
    fi
  done
}

# ---------- Python 检查 ----------
python_checks() {
  echo ""
  echo "--- Python 检查 ---"

  # print 语句
  if grep -rn "print(" "$SRC_DIR" --include="*.py" 2>/dev/null | grep -v '#.*print\|"""\|.*__name__.*main\|\.debug\|\.info\|\.warning\|\.error' | head -20 | grep -q .; then
    echo " 发现 print()（生产代码必须用 logging 模块）"
    HAS_ERRORS=1
  fi

  # 裸 except
  if grep -rnP '^(\s*)except\s*:\s*$' "$SRC_DIR" --include="*.py" 2>/dev/null; then
    echo " 发现裸 except:（必须指定异常类型，如 except ValueError:）"
    HAS_ERRORS=1
  fi

  # f-string SQL 拼接（危险）
  if grep -rnP '(cursor|execute|execute_values)\(f["\x27]' "$SRC_DIR" --include="*.py" 2>/dev/null; then
    echo " 发现 f-string SQL 拼接（有 SQL 注入风险，必须用参数化查询）"
    HAS_ERRORS=1
  fi

  # 函数超过 30 行
  find "$SRC_DIR" -name "*.py" 2>/dev/null | while read f; do
    awk '
      /^def |^    def |^        def / { fn_name=$0; fn_start=NR; in_fn=1; line_count=0; next }
      in_fn {
        if ($0 ~ /^[^ ]/ && NR > fn_start+1) {
          if (line_count > 30) {
            printf " 函数超过 30 行（%d行）: %s:%d %s\n", line_count, FILENAME, fn_start, substr(fn_name,1,60)
          }
          in_fn=0
        } else {
          line_count++
        }
      }
    ' "$f" 2>/dev/null
  done

  # snake_case 方法名检查（类方法应 snake_case）
  find "$SRC_DIR" -name "*.py" 2>/dev/null | while read f; do
    if grep -nP '^\s+def [A-Z]' "$f" 2>/dev/null; then
      echo " Python 方法应为 snake_case（大写开头疑似类命名错误）: $f"
    fi
  done
}

# ---------- JavaScript/TypeScript 检查 ----------
javascript_checks() {
  echo ""
  echo "--- JavaScript/TypeScript 检查 ---"

  # console.log
  if grep -rn "console\.\(log\|warn\|error\)(" "$SRC_DIR" --include="*.js" --include="*.ts" --include="*.jsx" --include="*.tsx" 2>/dev/null \
    | grep -v '//.*console\|/\*.*console' | head -20 | grep -q .; then
    echo " 发现 console.log/warn/error（生产代码建议加 eslint no-console 规则）"
    HAS_ERRORS=1
  fi

  # any 类型（TS 专用）
  if grep -rnP ': any(?=[,\s\)\]\}])|as any' "$SRC_DIR" --include="*.ts" --include="*.tsx" 2>/dev/null | head -20 | grep -q .; then
    echo " 发现 any 类型（TypeScript 应定义具体类型）"
    HAS_ERRORS=1
  fi

  # eval
  if grep -rn "eval(" "$SRC_DIR" --include="*.js" --include="*.ts" --include="*.jsx" --include="*.tsx" 2>/dev/null; then
    echo " 发现 eval()（禁止使用，有安全风险）"
    HAS_ERRORS=1
  fi

  # 函数超过 30 行
  for ext in js ts jsx tsx; do
    find "$SRC_DIR" -name "*.$ext" 2>/dev/null | while read f; do
      awk '
        /^(export )?(async )?function |^(async )?[a-zA-Z_$][a-zA-Z0-9_$]*\s*=\s*(async\s*)?\(/ { fn_name=$0; fn_start=NR; in_fn=1; line_count=0; next }
        in_fn {
          line_count++
          if ($0 ~ /^\s*(\}|\);)/) {
            if (line_count > 30) {
              printf " 函数超过 30 行（%d行）: %s:%d %s\n", line_count, FILENAME, fn_start, substr(fn_name,1,60)
            }
            in_fn=0
          }
        }
      ' "$f" 2>/dev/null
    done
  done
}

# ---------- Go 检查 ----------
go_checks() {
  echo ""
  echo "--- Go 检查 ---"

  # fmt.Println / fmt.Print
  if grep -rn 'fmt\.\(Print\|Println\|Printf\)(' "$SRC_DIR" --include="*.go" 2>/dev/null \
    | grep -v '//.*fmt\.Print\|_test\.go' | head -10 | grep -q .; then
    echo " 发现 fmt.Print/Println/Printf（生产代码建议用结构化日志库）"
    HAS_ERRORS=1
  fi

  # 裸 error 忽略
  if grep -rnP '^\s+_,\s*err\s*:=\s*.+$' "$SRC_DIR" --include="*.go" 2>/dev/null | head -5 | grep -q .; then
    # 更精确：检查 err 赋值后没有检查
    echo " 检查 Go error 处理..."
    # Go 编译器会处理大部分，这里仅做提示
  fi

  # 函数超过 30 行
  find "$SRC_DIR" -name "*.go" 2>/dev/null | while read f; do
    awk '
      /^func / { fn_name=$0; fn_start=NR; in_fn=1; line_count=0; braces=0; next }
      in_fn {
        line_count++
        if ($0 ~ /\{/) braces++
        if ($0 ~ /\}/) braces--
        if (braces <= 0 && NR > fn_start+1) {
          if (line_count > 30) {
            printf " 函数超过 30 行（%d行）: %s:%d %s\n", line_count, FILENAME, fn_start, substr(fn_name,1,60)
          }
          in_fn=0
        }
      }
    ' "$f" 2>/dev/null
  done
}

# 根据检测到的栈执行对应检查
case "$STACK_EXIT" in
  0) java_checks ;;
  1) python_checks ;;
  2) javascript_checks ;;
  3) go_checks ;;
esac

echo ""
if [ "$HAS_ERRORS" = "0" ]; then
  echo "品味检查通过"
else
  echo "品味检查未通过，修复后重跑"
fi
exit $HAS_ERRORS
