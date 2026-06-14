#!/bin/bash
# Harness 熵管理：循环清理脚本
# 用法: bash scripts/entropy-cleanup.sh [项目根目录]
# 建议: 每周五 17:00 通过 cronjob 自动执行

set -e
PROJECT_DIR="${1:-.}"
REPORT_DIR="$PROJECT_DIR/docs/reports"
mkdir -p "$REPORT_DIR"
REPORT="$REPORT_DIR/entropy-$(date +%Y%m%d).md"

echo "# 熵管理报告 — $(date '+%Y-%m-%d %H:%M')" > "$REPORT"
echo "" >> "$REPORT"

cd "$PROJECT_DIR"

# 1. 安全扫描
echo "## 安全扫描" >> "$REPORT"
trivy fs --severity CRITICAL,HIGH . 2>/dev/null >> "$REPORT" || echo "⚠️ trivy 未安装" >> "$REPORT"
echo "" >> "$REPORT"

# 2. 代码模式检查
echo "## 代码模式检查" >> "$REPORT"
semgrep --config=auto . 2>/dev/null >> "$REPORT" || echo "⚠️ semgrep 未安装" >> "$REPORT"
echo "" >> "$REPORT"

# 3. 品味不变式检查
echo "## 品味检查" >> "$REPORT"
if [ -f "scripts/taste-check.sh" ]; then
  bash scripts/taste-check.sh >> "$REPORT" 2>&1 || true
else
  echo "未配置品味检查" >> "$REPORT"
fi
echo "" >> "$REPORT"

# 4. 文档同步检查
echo "## 文档同步检查" >> "$REPORT"
echo "最后修改时间戳：" >> "$REPORT"
find docs/ -name "*.md" -newer "$REPORT" -printf '%T+ %p\n' 2>/dev/null | head -20 >> "$REPORT" || echo "无 docs/ 目录" >> "$REPORT"

echo "" >> "$REPORT"
echo "---" >> "$REPORT"
echo "报告生成时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$REPORT"

echo "✅ 熵管理报告已生成: $REPORT"
