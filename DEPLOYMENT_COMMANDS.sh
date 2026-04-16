#!/bin/bash

# Deployment Commands for Gemini API Integration
# For Prompt Wars Competition

echo "🚀 StadiumFlow - Gemini API Deployment"
echo "======================================"
echo ""

# Step 1: Get API Key
echo "STEP 1: Get Gemini API Key"
echo "Open: https://aistudio.google.com/app/apikey"
echo ""
read -p "Paste your Gemini API key here: " GEMINI_API_KEY
echo ""

# Step 2: Commit changes (if not already done)
echo "STEP 2: Commit and Push Changes"
git add .
git commit -m "feat: Add Google Gemini API integration for Prompt Wars

- Add GeminiApiService for Google Gemini API
- Update GeminiService to prioritize Gemini API over Vertex AI
- Add Gemini API dependency to build.gradle
- Update HealthController to show Gemini API status
- Maintain 93% instruction coverage, 87% branch coverage
- Ready for Prompt Wars competition with Google AI"

git push origin master
echo ""

# Step 3: Deploy to Cloud Run
echo "STEP 3: Deploy to Cloud Run with Gemini API"
echo "Run these commands in Google Cloud Shell:"
echo ""
echo "cd ~/stadiumflow"
echo "git pull origin master"
echo ""
echo "gcloud run deploy stadiumflow \\"
echo "    --source . \\"
echo "    --platform managed \\"
echo "    --region us-central1 \\"
echo "    --allow-unauthenticated \\"
echo "    --service-account=cloudrun-stadiumflow@physicaleventexperience.iam.gserviceaccount.com \\"
echo "    --set-env-vars=\"SPRING_PROFILES_ACTIVE=prod,GEMINI_API_KEY=$GEMINI_API_KEY,GCP_STORAGE_ENABLED=true\" \\"
echo "    --timeout=600 \\"
echo "    --memory=1Gi"
echo ""

# Step 4: Verification commands
echo "STEP 4: Verify Deployment"
echo ""
echo "# Get service URL"
echo "SERVICE_URL=\$(gcloud run services describe stadiumflow --region=us-central1 --format='value(status.url)')"
echo ""
echo "# Test health endpoint"
echo "curl -s \"\$SERVICE_URL/api/health/vertex-ai\" | jq '.'"
echo ""
echo "# Test AI query"
echo "curl -s -X POST \"\$SERVICE_URL/api/ai/ask\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"query\": \"What is the stadium status?\"}' | jq '.provider'"
echo ""
echo "Expected response: \"Google Gemini API\" ✅"
echo ""

# Step 5: Quick test script
cat > /tmp/test_gemini.sh << 'TESTEOF'
#!/bin/bash
SERVICE_URL=$(gcloud run services describe stadiumflow --region=us-central1 --format='value(status.url)')

echo "🧪 Testing Gemini API Integration"
echo "=================================="
echo ""

echo "1️⃣ Health Check:"
curl -s "$SERVICE_URL/api/health/vertex-ai" | jq '{geminiApiAvailable, status, provider}'
echo ""

echo "2️⃣ AI Query Test:"
RESPONSE=$(curl -s -X POST "$SERVICE_URL/api/ai/ask" \
  -H "Content-Type: application/json" \
  -d '{"query": "What is the stadium status?"}')

echo "$RESPONSE" | jq '.'
echo ""

PROVIDER=$(echo "$RESPONSE" | jq -r '.provider')

if [[ "$PROVIDER" == "Google Gemini API" ]]; then
  echo "✅ SUCCESS! Gemini API is working!"
else
  echo "⚠️  Provider: $PROVIDER (expected: Google Gemini API)"
fi
TESTEOF

chmod +x /tmp/test_gemini.sh

echo "Test script created at: /tmp/test_gemini.sh"
echo "Copy this to Cloud Shell and run it after deployment!"
echo ""
echo "======================================"
echo "✅ All commands ready!"
echo "Now go to Cloud Shell and run the deployment commands above."
echo "======================================"
