# Email Functionality Test Script
# This script helps test the email system and debug invitation issues

Write-Host "🔧 EaraConnect Email Functionality Test" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Test 1: Check if backend server is running
Write-Host "`n1. Testing backend server connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/email-test/config" -Method GET -TimeoutSec 10
    Write-Host "✅ Backend server is running" -ForegroundColor Green
    Write-Host "   Email config test result: $($response.success)" -ForegroundColor White
    if ($response.success) {
        Write-Host "   ✅ Email configuration is working" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Email configuration failed: $($response.error)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Backend server is not running or not accessible" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nPlease start the backend server first:" -ForegroundColor Yellow
    Write-Host "   cd EARACONNECT-BACKEND-master" -ForegroundColor White
    Write-Host "   ./mvnw spring-boot:run" -ForegroundColor White
    exit 1
}

# Test 2: Test email sending
Write-Host "`n2. Testing email sending..." -ForegroundColor Yellow
$testEmail = Read-Host "Enter email address to test (or press Enter to skip)"

if ($testEmail -and $testEmail.Trim()) {
    try {
        $body = @{
            email = $testEmail.Trim()
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "http://localhost:8081/api/email-test/send-test" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 30
        
        if ($response.success) {
            Write-Host "✅ Test email sent successfully to $testEmail" -ForegroundColor Green
            Write-Host "   Message: $($response.message)" -ForegroundColor White
        } else {
            Write-Host "❌ Test email failed" -ForegroundColor Red
            Write-Host "   Error: $($response.error)" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Failed to send test email" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⏭️  Skipping email test" -ForegroundColor Yellow
}

# Test 3: Test meeting invitation email
Write-Host "`n3. Testing meeting invitation email..." -ForegroundColor Yellow
$invitationEmail = Read-Host "Enter email address to test invitation (or press Enter to skip)"

if ($invitationEmail -and $invitationEmail.Trim()) {
    try {
        $body = @{
            email = $invitationEmail.Trim()
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "http://localhost:8081/api/email-test/test-invitation" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 30
        
        if ($response.success) {
            Write-Host "✅ Meeting invitation test email sent successfully to $invitationEmail" -ForegroundColor Green
            Write-Host "   Message: $($response.message)" -ForegroundColor White
        } else {
            Write-Host "❌ Meeting invitation test email failed" -ForegroundColor Red
            Write-Host "   Error: $($response.error)" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Failed to send meeting invitation test email" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⏭️  Skipping invitation test" -ForegroundColor Yellow
}

# Test 4: Check available meetings
Write-Host "`n4. Checking available meetings..." -ForegroundColor Yellow
try {
    $meetings = Invoke-RestMethod -Uri "http://localhost:8081/api/meetings" -Method GET -TimeoutSec 10
    
    $schedulableMeetings = $meetings | Where-Object { 
        $_.status -in @('SCHEDULED', 'DRAFT') -and 
        [DateTime]::Parse($_.meetingDate) -gt [DateTime]::Now 
    }
    
    Write-Host "✅ Found $($schedulableMeetings.Count) schedulable meetings" -ForegroundColor Green
    
    if ($schedulableMeetings.Count -gt 0) {
        Write-Host "   Available meetings:" -ForegroundColor White
        $schedulableMeetings | ForEach-Object {
            Write-Host "   - ID: $($_.id), Title: $($_.title), Date: $($_.meetingDate)" -ForegroundColor White
        }
    } else {
        Write-Host "   ⚠️  No schedulable meetings found" -ForegroundColor Yellow
        Write-Host "   Create a meeting first to test invitations" -ForegroundColor White
    }
} catch {
    Write-Host "❌ Failed to fetch meetings" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Check committees and subcommittees
Write-Host "`n5. Checking committees and subcommittees..." -ForegroundColor Yellow
try {
    $committees = Invoke-RestMethod -Uri "http://localhost:8081/api/committees" -Method GET -TimeoutSec 10
    $subcommittees = Invoke-RestMethod -Uri "http://localhost:8081/api/subcommittees" -Method GET -TimeoutSec 10
    
    Write-Host "✅ Found $($committees.Count) committees and $($subcommittees.Count) subcommittees" -ForegroundColor Green
    
    if ($committees.Count -gt 0) {
        Write-Host "   Committees:" -ForegroundColor White
        $committees | ForEach-Object {
            Write-Host "   - ID: $($_.id), Name: $($_.name)" -ForegroundColor White
        }
    }
    
    if ($subcommittees.Count -gt 0) {
        Write-Host "   Subcommittees:" -ForegroundColor White
        $subcommittees | ForEach-Object {
            Write-Host "   - ID: $($_.id), Name: $($_.name)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "❌ Failed to fetch committees/subcommittees" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "1. If email tests failed, check the backend logs for SMTP errors" -ForegroundColor White
Write-Host "2. Verify Gmail app password is correct in application.properties" -ForegroundColor White
Write-Host "3. Test the frontend invitation system at: http://localhost:3000/email-test" -ForegroundColor White
Write-Host "4. Check browser console for detailed error messages" -ForegroundColor White

Write-Host "`n✅ Email functionality test completed!" -ForegroundColor Green
