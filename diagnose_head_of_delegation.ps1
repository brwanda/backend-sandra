Write-Host "Diagnosing Head of Delegation Subcommittee Assignment Issue..." -ForegroundColor Green

# Test 1: Check if Head of Delegation subcommittee exists
Write-Host "`n1. Checking Head of Delegation subcommittee..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/sub-committees" -Method GET
    $headOfDelegation = $response | Where-Object { $_.name -eq "Head Of Delegation" }
    if ($headOfDelegation) {
        Write-Host "✅ Head of Delegation subcommittee found with ID: $($headOfDelegation.id)" -ForegroundColor Green
    } else {
        Write-Host "❌ Head of Delegation subcommittee not found" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error checking subcommittees: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Check users with null subcommittee_id
Write-Host "`n2. Checking users with null subcommittee_id..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
    $usersWithNullSubcommittee = $response | Where-Object { $_.subcommitteeId -eq $null }
    Write-Host "Found $($usersWithNullSubcommittee.Count) users with null subcommittee_id" -ForegroundColor Cyan
    foreach ($user in $usersWithNullSubcommittee) {
        Write-Host "  - $($user.name) (Role: $($user.role), Email: $($user.email))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking users: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Check csub_committee_members table
Write-Host "`n3. Checking csub_committee_members for Head of Delegation..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/committee-members" -Method GET
    $headOfDelegationMembers = $response | Where-Object { $_.positionInEar -and $_.positionInEar.name -eq "Head Of Delegation" }
    Write-Host "Found $($headOfDelegationMembers.Count) members in Head of Delegation" -ForegroundColor Cyan
    foreach ($member in $headOfDelegationMembers) {
        Write-Host "  - $($member.name) (Chair: $($member.isChair), ViceChair: $($member.isViceChair), DelegationSecretary: $($member.isDelegationSecretary))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error checking committee members: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nDiagnosis complete!" -ForegroundColor Green
