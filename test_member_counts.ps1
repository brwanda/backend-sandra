# Test script for member count endpoints
Write-Host "Testing Member Count Endpoints..." -ForegroundColor Green

# Wait for server to start
Write-Host "Waiting for server to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Test 1: Get all committee members
Write-Host "`n1. Testing /api/country-committee-members/all" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/country-committee-members/all" -Method GET
    $members = $response.Content | ConvertFrom-Json
    Write-Host "✅ Found $($members.Count) total members" -ForegroundColor Green
    if ($members.Count -gt 0) {
        Write-Host "   First member: $($members[0].name) - $($members[0].email)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Get committees with member counts
Write-Host "`n2. Testing /api/country-committee-members/committees/with-counts" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/country-committee-members/committees/with-counts" -Method GET
    $committees = $response.Content | ConvertFrom-Json
    Write-Host "✅ Found $($committees.Count) committees:" -ForegroundColor Green
    foreach ($committee in $committees) {
        Write-Host "   $($committee.name): $($committee.memberCount) members" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Get subcommittees with member counts
Write-Host "`n3. Testing /api/country-committee-members/subcommittees/with-counts" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/country-committee-members/subcommittees/with-counts" -Method GET
    $subcommittees = $response.Content | ConvertFrom-Json
    Write-Host "✅ Found $($subcommittees.Count) subcommittees:" -ForegroundColor Green
    foreach ($subcommittee in $subcommittees) {
        Write-Host "   $($subcommittee.name): $($subcommittee.memberCount) members" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test specific subcommittee members
Write-Host "`n4. Testing /api/country-committee-members/sub-committee/1" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/country-committee-members/sub-committee/1" -Method GET
    $members = $response.Content | ConvertFrom-Json
    Write-Host "✅ Subcommittee 1 has $($members.Count) members" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nTest completed!" -ForegroundColor Green
