#!/bin/bash

BASE_DIR="$HOME/DailyReceipt"
PYTHON_DIR="$BASE_DIR/LLMReceiptPrintAPI"
JAVA_DIR="$BASE_DIR/Executor"

echo "--- Starting DailyReceipt Deployment ---"

# Kill old processes
echo "Step 1: Clearing ports and old builds..."
fuser -k 8001/tcp 2>/dev/null
pkill -f "gradlew" 2>/dev/null

# Python Start 
echo "Step 2: Starting Python API..."
cd "$PYTHON_DIR" || exit 1

# Looking for main.py in the project root
if [ -f "main.py" ]; then
    nohup ./.venv/bin/python3 -u main.py > python_nohup.out 2>&1 &
    PYTHON_PID=$!
else
    echo "ERROR: main.py not found in $PYTHON_DIR. Is it in a different folder?"
    exit 1
fi

# Java Build & Run
echo "Step 3: Starting Java Executor..."
cd "$JAVA_DIR" || exit 1

# If the code says "not found", let's make sure we are running from the right spot
if ./gradlew clean assemble > /dev/null 2>&1; then
    # We run from the Executor root so its a valid path
    nohup ./gradlew run --no-daemon > java_nohup.out 2>&1 &
    JAVA_PID=$!
else
    echo "ERROR: Java Build Failed."
    kill $PYTHON_PID 2>/dev/null
    exit 1
fi

# Final Verification
echo "Step 4: Verifying stability..."
sleep 5

PY_STATUS=$(ps -p $PYTHON_PID > /dev/null && echo "RUNNING" || echo "CRASHED")
JV_STATUS=$(ps -p $JAVA_PID > /dev/null && echo "RUNNING" || echo "CRASHED")

if [ "$PY_STATUS" == "RUNNING" ] && [ "$JV_STATUS" == "RUNNING" ]; then
    cat <<EOF > "$BASE_DIR/stop_receipt_printer.sh"
#!/bin/bash
kill $PYTHON_PID $JAVA_PID 2>/dev/null
echo "Stopped DailyReceipt."
EOF
    chmod +x "$BASE_DIR/stop_receipt_printer.sh"
    echo "--- SUCCESS ---"
    echo "Python (PID $PYTHON_PID) is UP."
    echo "Java   (PID $JAVA_PID) is UP."
else
    echo "--- FAILURE ---"
    echo "Python Status: $PY_STATUS"
    echo "Java Status:   $JV_STATUS"
    echo "Check logs: $PYTHON_DIR/python_nohup.out and $JAVA_DIR/java_nohup.out"
    kill $PYTHON_PID $JAVA_PID 2>/dev/null
fi
