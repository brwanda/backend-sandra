# PowerShell script to diagnose chair functionality issues
# This script will test the API endpoints and help identify the problem

Write-Host "🔍 Diagnosing Chair Functionality Issues..." -ForegroundColor Green

# Test 1: Check if backend is running
Write-Host "`n1. Testing backend connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/chair/test/database" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Backend is running" -ForegroundColor Green
        $data = $response.Content | ConvertFrom-Json
        Write-Host "Database summary:" -ForegroundColor Cyan
        Write-Host "Total users: $($data.totalUsers)" -ForegroundColor White
        Write-Host "Total resolutions: $($data.totalResolutions)" -ForegroundColor White
    } else {
        Write-Host "❌ Backend returned status: $($response.StatusCode)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Backend is not running or not accessible" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Test chair resolutions endpoint for user 19
Write-Host "`n2. Testing chair resolutions for user 19..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/chair/resolutions/19" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Chair resolutions endpoint working for user 19" -ForegroundColor Green
        $resolutions = $response.Content | ConvertFrom-Json
        Write-Host "Found $($resolutions.Count) resolutions" -ForegroundColor White
    } else {
        Write-Host "❌ Chair resolutions endpoint failed for user 19" -ForegroundColor Red
        Write-Host "Status: $($response.StatusCode)" -ForegroundColor Red
        Write-Host "Response: $($response.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error testing chair resolutions endpoint" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Test chair reports endpoint for user 19
Write-Host "`n3. Testing chair reports for user 19..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/chair/reports/19" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Chair reports endpoint working for user 19" -ForegroundColor Green
        $reports = $response.Content | ConvertFrom-Json
        Write-Host "Found $($reports.Count) reports" -ForegroundColor White
    } else {
        Write-Host "❌ Chair reports endpoint failed for user 19" -ForegroundColor Red
        Write-Host "Status: $($response.StatusCode)" -ForegroundColor Red
        Write-Host "Response: $($response.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error testing chair reports endpoint" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test with user 1 (fallback)
Write-Host "`n4. Testing chair resolutions for user 1 (fallback)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/chair/resolutions/1" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Chair resolutions endpoint working for user 1" -ForegroundColor Green
        $resolutions = $response.Content | ConvertFrom-Json
        Write-Host "Found $($resolutions.Count) resolutions" -ForegroundColor White
    } else {
        Write-Host "❌ Chair resolutions endpoint failed for user 1" -ForegroundColor Red
        Write-Host "Status: $($response.StatusCode)" -ForegroundColor Red
        Write-Host "Response: $($response.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error testing chair resolutions endpoint for user 1" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n📋 Summary:" -ForegroundColor Green
Write-Host "If all tests pass, the issue might be in the frontend." -ForegroundColor Yellow
Write-Host "If tests fail, the issue is in the backend or database." -ForegroundColor Yellow
Write-Host "Run setup_database.ps1 to fix database issues." -ForegroundColor Yellow
