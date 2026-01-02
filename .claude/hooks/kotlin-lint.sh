#!/usr/bin/env bash
# Claude Code hook: Run ktlint format, lint, and detekt after Kotlin file edits

# Read JSON input from stdin
input=$(cat)

# Extract file path from tool input
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

# Skip if no file path or not a Kotlin file
if [[ -z "$file_path" ]] || [[ ! "$file_path" =~ \.(kt|kts)$ ]]; then
    exit 0
fi

# Skip if file doesn't exist (was deleted)
if [[ ! -f "$file_path" ]]; then
    exit 0
fi

cd "$CLAUDE_PROJECT_DIR" || exit 1

echo "Running Kotlin code quality checks on: $file_path"

# 1. Format with ktlint
echo "[1/3] Running ktlint format..."
ktlint --format "$file_path" 2>&1

# 2. Lint with ktlint
echo "[2/3] Running ktlint check..."
ktlint "$file_path" 2>&1

# 3. Run detekt on the specific file
echo "[3/3] Running detekt..."
detekt --config detekt.yml --input "$file_path" 2>&1

echo "Kotlin code quality checks completed."
exit 0
