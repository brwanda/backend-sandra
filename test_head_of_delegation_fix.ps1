Write-Host "Testing Head of Delegation Subcommittee Assignment Fix..." -ForegroundColor Green

# Wait for backend to be ready
Write-Host "`n1. Waiting for backend to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Test 1: Check if Head of Delegation subcommittee exists and has correct ID
Write-Host "`n2. Checking Head of Delegation subcommittee..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/sub-committees" -Method GET
    $headOfDelegation = $response | Where-Object { $_.name -eq "Head Of Delegation" }
    if ($headOfDelegation) {
        Write-Host "✅ Head of Delegation subcommittee found with ID: $($headOfDelegation.id)" -ForegroundColor Green
        $headOfDelegationId = $headOfDelegation.id
    } else {
        Write-Host "❌ Head of Delegation subcommittee not found" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Error checking subcommittees: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Check current users with Head of Delegation subcommittee
Write-Host "`n3. Checking users assigned to Head of Delegation..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
    $headOfDelegationUsers = $response | Where-Object { $_.subcommitteeId -eq $headOfDelegationId }
    Write-Host "Found $($headOfDelegationUsers.Count) users assigned to Head of Delegation" -ForegroundColor Cyan
    foreach ($user in $headOfDelegationUsers) {
        Write-Host "  - $($user.name) (Role: $($user.role), Email: $($user.email))" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Error checking users: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Check users with null subcommittee_id
Write-Host "`n4. Checking users with NULL subcommittee_id..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
    $usersWithNullSubcommittee = $response | Where-Object { $_.subcommitteeId -eq $null }
    Write-Host "Found $($usersWithNullSubcommittee.Count) users with NULL subcommittee_id" -ForegroundColor Yellow
    foreach ($user in $usersWithNullSubcommittee) {
        Write-Host "  - $($user.name) (Role: $($user.role), Email: $($user.email))" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Error checking users: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Test creating a new member in Head of Delegation
Write-Host "`n5. Testing member creation in Head of Delegation..." -ForegroundColor Yellow
try {
    $testMember = @{
        name = "Test Head of Delegation Member"
        email = "test.hod@test.com"
        phone = "1234567890"
        positionInYourRA = "Test Position"
        country = @{ id = 1 }
        subCommittee = @{ id = $headOfDelegationId }
        appointedDate = "2024-01-01"
        chair = $true
        viceChair = $false
        committeeSecretary = $false
        committeeMember = $false
        delegationSecretary = $false
    }
    
    $body = $testMember | ConvertTo-Json
    Write-Host "Sending test member data: $body" -ForegroundColor Cyan
    
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/committee-members" -Method POST -Body $body -ContentType "application/json"
    Write-Host "✅ Test member created successfully with ID: $($response.id)" -ForegroundColor Green
    
    # Check if the user was created with correct subcommittee
    Start-Sleep -Seconds 2
    $userResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method GET
    $testUser = $userResponse | Where-Object { $_.email -eq "test.hod@test.com" }
    if ($testUser) {
        Write-Host "✅ Test user created with subcommittee_id: $($testUser.subcommitteeId)" -ForegroundColor Green
        if ($testUser.subcommitteeId -eq $headOfDelegationId) {
            Write-Host "✅ Subcommittee assignment is correct!" -ForegroundColor Green
        } else {
            Write-Host "❌ Subcommittee assignment is incorrect!" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ Test user not found in users table" -ForegroundColor Red
    }
    
} catch {
    Write-Host "❌ Error creating test member: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nTest completed!" -ForegroundColor Green
