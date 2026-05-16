# Test Chair Functionality
Write-Host "🔍 Testing Chair Functionality..." -ForegroundColor Green

# Test 1: Check if backend is running
Write-Host "`n1. Testing backend connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/chair/test/database" -Method GET
    Write-Host "✅ Backend is running" -ForegroundColor Green
    Write-Host "Total users: $($response.totalUsers)" -ForegroundColor White
    Write-Host "Total resolutions: $($response.totalResolutions)" -ForegroundColor White
} catch {
    Write-Host "❌ Backend is not running or not accessible" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Test chair resolutions for user 19
Write-Host "`n2. Testing chair resolutions for user 19..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/chair/resolutions/19" -Method GET
    Write-Host "✅ Chair resolutions endpoint working for user 19" -ForegroundColor Green
    Write-Host "Found $($response.Count) resolutions" -ForegroundColor White
    foreach ($resolution in $response) {
        Write-Host "  - Resolution ID: $($resolution.id), Title: $($resolution.title)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ Chair resolutions endpoint failed for user 19" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Test chair reports for user 19
Write-Host "`n3. Testing chair reports for user 19..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/chair/reports/19" -Method GET
    Write-Host "✅ Chair reports endpoint working for user 19" -ForegroundColor Green
    Write-Host "Found $($response.Count) reports" -ForegroundColor White
    foreach ($report in $response) {
        Write-Host "  - Report ID: $($report.id), Status: $($report.status), Performance: $($report.performancePercentage)%" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ Chair reports endpoint failed for user 19" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test report submission (simulate)
Write-Host "`n4. Testing report submission simulation..." -ForegroundColor Yellow
try {
    $reportData = @{
        resolution = @{ id = 1 }
        subcommittee = @{ id = 1 }
        progressDetails = "Test progress details for API testing"
        hindrances = "No hindrances"
        performancePercentage = 85
    }
    
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/chair/reports?chairId=19" -Method POST -Body ($reportData | ConvertTo-Json) -ContentType "application/json"
    Write-Host "✅ Report submission test successful" -ForegroundColor Green
    Write-Host "Report ID: $($response.id)" -ForegroundColor White
} catch {
    Write-Host "❌ Report submission test failed" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n📋 Summary:" -ForegroundColor Green
Write-Host "If all tests pass, the chair functionality should be working correctly." -ForegroundColor Yellow
Write-Host "If reports are empty, check the database setup." -ForegroundColor Yellow
