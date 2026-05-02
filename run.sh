#!/bin/zsh

set -e

rm -rf out logs
mkdir -p out logs

ATC_MAIN="org.demos.atc.AtcNode"
AIRCRAFT_MAIN="org.demos.aircraft.AircraftNode"
CP="out"

echo "Compiling Java files..."
find src/main/java -name "*.java" > sources.txt
javac -d out @sources.txt

PIDS=()

cleanup() {
    echo ""
    echo "Stopping demo..."
    for pid in "${PIDS[@]}"; do
        kill "$pid" 2>/dev/null || true
    done
}

trap cleanup EXIT INT TERM

echo "Starting ATC..."
java -cp "$CP" "$ATC_MAIN" > logs/atc.log 2>&1 &
PIDS+=($!)

sleep 2

start_aircraft() {
    local tail="$1"
    local consumption="$2"
    local fuel="$3"
    local command="$4"
    local delay="$5"

    echo "Starting aircraft $tail: command=$command delay=${delay}s"

    {
        echo "$tail"
        echo "$consumption"
        echo "$fuel"
        sleep "$delay"
        echo "$command"
        sleep 180
    } | java -cp "$CP" "$AIRCRAFT_MAIN" > "logs/${tail}.log" 2>&1 &

    PIDS+=($!)
}

start_aircraft "A01" "1"  "1000" "L"  1
start_aircraft "A02" "1"  "1000" "T"  2
start_aircraft "A03" "1"  "1000" "L"  3
start_aircraft "A04" "1"  "1000" "T"  4
start_aircraft "A05" "1"  "1000" "L!" 5

start_aircraft "A06" "1"  "1000" "L"  6
start_aircraft "A07" "1"  "1000" "T"  7
start_aircraft "A08" "1"  "1000" "L!" 8
start_aircraft "A09" "1"  "1000" "L"  9
start_aircraft "A10" "1"  "1000" "T"  10

start_aircraft "A11" "5"  "300"  "L"  11
start_aircraft "A12" "10" "300"  "L"  12
start_aircraft "A13" "15" "300"  "L"  13
start_aircraft "A14" "20" "300"  "L"  14
start_aircraft "A15" "25" "300"  "L"  15

start_aircraft "A16" "1"  "1000" "T"  16
start_aircraft "A17" "1"  "1000" "L!" 17
start_aircraft "A18" "1"  "1000" "L"  18
start_aircraft "A19" "1"  "1000" "T"  19
start_aircraft "A20" "1"  "1000" "L!" 20

echo ""
echo "Demo running with 20 aircraft."
echo "ATC log: logs/atc.log"
echo "Aircraft logs: logs/A01.log ... logs/A20.log"
echo ""
echo "Press Ctrl+C to stop."
echo ""

tail -f logs/atc.log