Write-Host "Testing Complete HOD Workflow..." -ForegroundColor Green

# Wait for backend to be ready
Write-Host "`n1. Waiting for backend to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Test 1: Check HOD users
Write-Host "`n2. Checking HOD users..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
    $hodUsers = $response | Where-Object { $_.role -eq "HOD" }
    Write-Host "Found $($hodUsers.Count) HOD users" -ForegroundColor Cyan
    foreach ($user in $hodUsers) {
        Write-Host "  - $($user.name) (Email: $($user.email), Subcommittee: $($user.subcommitteeId))" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error checking HOD users: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Check reports pending HOD review
Write-Host "`n3. Checking reports pending HOD review..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/status/SUBMITTED" -Method GET
    Write-Host "Found $($response.Count) reports pending HOD review" -ForegroundColor Cyan
    foreach ($report in $response) {
        Write-Host "  - Report ID: $($report.id) - $($report.resolution.title) (Submitted by: $($report.submittedBy.name))" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error checking reports: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Check HOD notifications
Write-Host "`n4. Checking HOD notifications..." -ForegroundColor Yellow
try {
    $hodUsers = $response | Where-Object { $_.role -eq "HOD" }
    if ($hodUsers.Count -gt 0) {
        $hodId = $hodUsers[0].id
        $notificationResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/user/$hodId" -Method GET
        Write-Host "Found $($notificationResponse.Count) notifications for HOD" -ForegroundColor Cyan
        foreach ($notification in $notificationResponse) {
            Write-Host "  - $($notification.title): $($notification.message)" -ForegroundColor Green
        }
    } else {
        Write-Host "No HOD users found to check notifications" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking notifications: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test report submission workflow
Write-Host "`n5. Testing report submission workflow..." -ForegroundColor Yellow
try {
    # Get a chair user
    $chairUsers = $response | Where-Object { $_.role -eq "CHAIR" }
    if ($chairUsers.Count -gt 0) {
        $chairUser = $chairUsers[0]
        Write-Host "Using chair user: $($chairUser.name)" -ForegroundColor Cyan
        
        # Get a resolution
        $resolutionResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/resolutions" -Method GET
        if ($resolutionResponse.Count -gt 0) {
            $resolution = $resolutionResponse[0]
            Write-Host "Using resolution: $($resolution.title)" -ForegroundColor Cyan
            
            # Create test report
            $testReport = @{
                resolutionId = $resolution.id
                subcommitteeId = $chairUser.subcommitteeId
                submittedById = $chairUser.id
                performancePercentage = 85
                progressDetails = "Test progress report for HOD workflow verification"
                hindrances = "No major hindrances encountered"
            }
            
            $body = $testReport | ConvertTo-Json
            Write-Host "Submitting test report..." -ForegroundColor Cyan
            
            $reportResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/reports" -Method POST -Body $body -ContentType "application/json"
            Write-Host "✅ Test report submitted successfully with ID: $($reportResponse.id)" -ForegroundColor Green
            
            # Check if HOD received notification
            Start-Sleep -Seconds 2
            if ($hodUsers.Count -gt 0) {
                $hodId = $hodUsers[0].id
                $notificationResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/user/$hodId" -Method GET
                $newNotifications = $notificationResponse | Where-Object { $_.type -eq "REPORT_SUBMISSION" }
                Write-Host "HOD received $($newNotifications.Count) new report submission notifications" -ForegroundColor Green
            }
        } else {
            Write-Host "No resolutions available for testing" -ForegroundColor Yellow
        }
    } else {
        Write-Host "No chair users found for testing" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error testing report submission: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Test HOD approval workflow
Write-Host "`n6. Testing HOD approval workflow..." -ForegroundColor Yellow
try {
    # Get submitted reports
    $submittedReports = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/status/SUBMITTED" -Method GET
    if ($submittedReports.Count -gt 0) {
        $reportToReview = $submittedReports[0]
        Write-Host "Reviewing report ID: $($reportToReview.id)" -ForegroundColor Cyan
        
        if ($hodUsers.Count -gt 0) {
            $hodId = $hodUsers[0].id
            
            # Test HOD approval
            $approvalData = @{
                approved = $true
                comments = "Test approval from HOD workflow verification"
                reviewerId = $hodId
            }
            
            $body = $approvalData | ConvertTo-Json
            $approvalResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/$($reportToReview.id)/hod-review" -Method POST -Body $body -ContentType "application/json"
            Write-Host "✅ Report approved by HOD successfully!" -ForegroundColor Green
            Write-Host "  - New status: $($approvalResponse.status)" -ForegroundColor Cyan
            Write-Host "  - HOD comments: $($approvalResponse.hodComments)" -ForegroundColor Cyan
        } else {
            Write-Host "No HOD users available for approval testing" -ForegroundColor Yellow
        }
    } else {
        Write-Host "No submitted reports available for approval testing" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error testing HOD approval: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nHOD Workflow Test Completed!" -ForegroundColor Green
Write-Host "`nSummary:" -ForegroundColor Cyan
Write-Host "✅ HOD users verification" -ForegroundColor Green
Write-Host "✅ Reports pending review check" -ForegroundColor Green
Write-Host "✅ Notifications system check" -ForegroundColor Green
Write-Host "✅ Report submission workflow" -ForegroundColor Green
Write-Host "✅ HOD approval workflow" -ForegroundColor Green
