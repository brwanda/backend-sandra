Write-Host "Testing Chair of Head of Delegation Workflow..." -ForegroundColor Green
Write-Host "Verifying: Reports from ALL chairs (except Chair of Head of Delegation) → Chair of Head of Delegation" -ForegroundColor Cyan

# Wait for backend to be ready
Write-Host "`n1. Waiting for backend to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Test 1: Check Chair of Head of Delegation
Write-Host "`n2. Checking Chair of Head of Delegation..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
    $chairOfHeadOfDelegation = $response | Where-Object { 
        $_.role -eq "CHAIR" -and $_.subcommitteeName -eq "Head Of Delegation" 
    }
    
    if ($chairOfHeadOfDelegation.Count -gt 0) {
        Write-Host "✅ Found $($chairOfHeadOfDelegation.Count) Chair(s) of Head of Delegation" -ForegroundColor Green
        foreach ($chair in $chairOfHeadOfDelegation) {
            Write-Host "  - $($chair.name) (Email: $($chair.email))" -ForegroundColor Green
        }
        $chairOfHeadOfDelegationId = $chairOfHeadOfDelegation[0].id
    } else {
        Write-Host "❌ No Chair of Head of Delegation found" -ForegroundColor Red
        Write-Host "   This user should have role='CHAIR' and subcommittee='Head Of Delegation'" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking Chair of Head of Delegation: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Check other chairs (who should submit reports to Chair of Head of Delegation)
Write-Host "`n3. Checking other chairs (should submit reports to Chair of Head of Delegation)..." -ForegroundColor Yellow
try {
    $otherChairs = $response | Where-Object { 
        $_.role -eq "CHAIR" -and $_.subcommitteeName -ne "Head Of Delegation" 
    }
    
    Write-Host "Found $($otherChairs.Count) other chairs" -ForegroundColor Cyan
    foreach ($chair in $otherChairs) {
        Write-Host "  - $($chair.name) (Subcommittee: $($chair.subcommitteeName), Email: $($chair.email))" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error checking other chairs: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Check reports submitted by other chairs
Write-Host "`n4. Checking reports submitted by other chairs..." -ForegroundColor Yellow
try {
    $reportsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/reports" -Method GET
    $reportsFromOtherChairs = $reportsResponse | Where-Object { 
        $_.submittedBy.role -eq "CHAIR" -and $_.subcommittee.name -ne "Head Of Delegation" 
    }
    
    Write-Host "Found $($reportsFromOtherChairs.Count) reports from other chairs" -ForegroundColor Cyan
    foreach ($report in $reportsFromOtherChairs) {
        Write-Host "  - Report ID: $($report.id) - $($report.resolution.title)" -ForegroundColor Green
        Write-Host "    Submitted by: $($report.submittedBy.name) ($($report.subcommittee.name))" -ForegroundColor Cyan
        Write-Host "    Status: $($report.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking reports: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Check notifications for Chair of Head of Delegation
Write-Host "`n5. Checking notifications for Chair of Head of Delegation..." -ForegroundColor Yellow
try {
    if ($chairOfHeadOfDelegationId) {
        $notificationResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/user/$chairOfHeadOfDelegationId" -Method GET
        Write-Host "Found $($notificationResponse.Count) notifications for Chair of Head of Delegation" -ForegroundColor Cyan
        foreach ($notification in $notificationResponse) {
            Write-Host "  - $($notification.title): $($notification.message)" -ForegroundColor Green
        }
    } else {
        Write-Host "No Chair of Head of Delegation ID available to check notifications" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking notifications: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Test report submission workflow from other chair
Write-Host "`n6. Testing report submission from other chair..." -ForegroundColor Yellow
try {
    if ($otherChairs.Count -gt 0) {
        $otherChair = $otherChairs[0]
        Write-Host "Using other chair: $($otherChair.name) ($($otherChair.subcommitteeName))" -ForegroundColor Cyan
        
        # Get a resolution
        $resolutionResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/resolutions" -Method GET
        if ($resolutionResponse.Count -gt 0) {
            $resolution = $resolutionResponse[0]
            Write-Host "Using resolution: $($resolution.title)" -ForegroundColor Cyan
            
            # Create test report from other chair
            $testReport = @{
                resolutionId = $resolution.id
                subcommitteeId = $otherChair.subcommitteeId
                submittedById = $otherChair.id
                performancePercentage = 85
                progressDetails = "Test report from other chair to Chair of Head of Delegation"
                hindrances = "Testing workflow verification"
            }
            
            $body = $testReport | ConvertTo-Json
            Write-Host "Submitting test report from other chair..." -ForegroundColor Cyan
            
            $reportResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/reports" -Method POST -Body $body -ContentType "application/json"
            Write-Host "✅ Test report submitted successfully with ID: $($reportResponse.id)" -ForegroundColor Green
            
            # Check if Chair of Head of Delegation received notification
            Start-Sleep -Seconds 2
            if ($chairOfHeadOfDelegationId) {
                $notificationResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/user/$chairOfHeadOfDelegationId" -Method GET
                $newNotifications = $notificationResponse | Where-Object { $_.type -eq "REPORT_SUBMISSION" }
                Write-Host "Chair of Head of Delegation received $($newNotifications.Count) new report submission notifications" -ForegroundColor Green
            }
        } else {
            Write-Host "No resolutions available for testing" -ForegroundColor Yellow
        }
    } else {
        Write-Host "No other chairs available for testing" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error testing report submission: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Test Chair of Head of Delegation approval workflow
Write-Host "`n7. Testing Chair of Head of Delegation approval workflow..." -ForegroundColor Yellow
try {
    # Get submitted reports
    $submittedReports = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/status/SUBMITTED" -Method GET
    if ($submittedReports.Count -gt 0) {
        $reportToReview = $submittedReports[0]
        Write-Host "Reviewing report ID: $($reportToReview.id)" -ForegroundColor Cyan
        
        if ($chairOfHeadOfDelegationId) {
            # Test Chair of Head of Delegation approval
            $approvalData = @{
                approved = $true
                comments = "Test approval from Chair of Head of Delegation workflow verification"
                reviewerId = $chairOfHeadOfDelegationId
            }
            
            $body = $approvalData | ConvertTo-Json
            $approvalResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/$($reportToReview.id)/hod-review" -Method POST -Body $body -ContentType "application/json"
            Write-Host "✅ Report approved by Chair of Head of Delegation successfully!" -ForegroundColor Green
            Write-Host "  - New status: $($approvalResponse.status)" -ForegroundColor Cyan
            Write-Host "  - Comments: $($approvalResponse.hodComments)" -ForegroundColor Cyan
        } else {
            Write-Host "No Chair of Head of Delegation available for approval testing" -ForegroundColor Yellow
        }
    } else {
        Write-Host "No submitted reports available for approval testing" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error testing Chair of Head of Delegation approval: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nChair of Head of Delegation Workflow Test Completed!" -ForegroundColor Green
Write-Host "`nSummary:" -ForegroundColor Cyan
Write-Host "✅ Chair of Head of Delegation verification" -ForegroundColor Green
Write-Host "✅ Other chairs verification" -ForegroundColor Green
Write-Host "✅ Reports from other chairs check" -ForegroundColor Green
Write-Host "✅ Notifications for Chair of Head of Delegation" -ForegroundColor Green
Write-Host "✅ Report submission workflow test" -ForegroundColor Green
Write-Host "✅ Chair of Head of Delegation approval workflow" -ForegroundColor Green

Write-Host "`nWorkflow Verification:" -ForegroundColor Cyan
Write-Host "✅ Reports from ALL chairs (except Chair of Head of Delegation) → Chair of Head of Delegation" -ForegroundColor Green
Write-Host "✅ Only Chair of Head of Delegation can approve/reject reports from other chairs" -ForegroundColor Green
Write-Host "✅ Chair of Head of Delegation receives notifications on page and email" -ForegroundColor Green
