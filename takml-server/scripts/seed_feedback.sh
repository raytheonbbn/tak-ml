#!/bin/bash

#set -e

URL="https://localhost"
PORT="8234"
MODEL_NAME="MobileNet Onnx (Remote)"
MODEL_VERSION="1.0"
COUNT=20

CALLSIGNS=("alpha" "bravo" "charlie" "delta" "echo" "foxtrot" "golf" "hotel" "india" "juliet")
OUTPUT_ERRORS=("FALSE_POSITIVE" "FALSE_NEGATIVE" "INCORRECT_LABEL" "MISSING_LABEL" "OTHER")

if [ $# -ne 2 ]; then
  echo "Error: Expected exactly 2 arguments." 1>&2
  exit 1
fi 

CERT="$1"
CERT_PASS="$2"

echo "$CERT:$CERT_PASS"
for ((i=1; i<=COUNT; i++)); do
  callsign=${CALLSIGNS[$RANDOM % ${#CALLSIGNS[@]}]}

  if ((RANDOM % 2)); then
    correct=true
    outputErrorType=""
  else
    correct=false
    outputErrorType=${OUTPUT_ERRORS[$RANDOM % ${#OUTPUT_ERRORS[@]}]}
    outputErrorField="-F outputErrorType=$outputErrorType"
  fi

  confidence=$((RANDOM % 5 + 1))
  rating=$((RANDOM % 5 + 1))

  response=$(curl -sk \
      --cert-type P12 \
      --cert $CERT:$CERT_PASS \
      -w "HTTPSTATUS:%{http_code}" -X POST "$URL:$PORT/model_feedback/add_feedback" \
      -F "modelName=$MODEL_NAME" \
      -F "modelVersion=$MODEL_VERSION" \
      -F "callsign=$callsign" \
      -F "inputText=Hello world" \
      -F "output=Generated output" \
      -F "isCorrect=$correct" \
      $outputErrorField \
      -F "evaluationConfidence=$confidence" \
      -F "evaluationRating=$rating" \
      -F "comment=comment")

    body=$(echo "$response" | sed -e 's/HTTPSTATUS\:.*//g' | python3 -m json.tool)
    status=$(echo "$response" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')

    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
      echo "$i - SUCCESS status: $status body: $body"
    else
      echo "$i - ERROR status: $status body: $body"
    fi
done
