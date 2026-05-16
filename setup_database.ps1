# PowerShell script to set up the database with test data
# This script will help you run the SQL setup and test the chair functionality

Write-Host "🔧 Setting up database with test data..." -ForegroundColor Green

# Check if PostgreSQL is running
Write-Host "Checking PostgreSQL connection..." -ForegroundColor Yellow
try {
    $testConnection = psql -h localhost -U postgres -d eara_connect -c "SELECT 1;" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ PostgreSQL is running" -ForegroundColor Green
    } else {
        Write-Host "❌ PostgreSQL is not running or connection failed" -ForegroundColor Red
        Write-Host "Please start PostgreSQL and ensure the database 'eara_connect' exists" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ PostgreSQL command not found. Please ensure PostgreSQL is installed and in PATH" -ForegroundColor Red
    exit 1
}

# Run the setup script
Write-Host "Running setup_test_data.sql..." -ForegroundColor Yellow
try {
    psql -h localhost -U postgres -d eara_connect -f setup_test_data.sql
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Database setup completed successfully!" -ForegroundColor Green
    } else {
        Write-Host "❌ Database setup failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Error running setup script: $_" -ForegroundColor Red
    exit 1
}

# Test the database setup
Write-Host "Testing database setup..." -ForegroundColor Yellow
try {
    $testQuery = @"
SELECT 'User 19 exists:' as test, COUNT(*) as result FROM users WHERE id = 19
UNION ALL
SELECT 'User 19 is CHAIR:' as test, COUNT(*) as result FROM users WHERE id = 19 AND role = 'CHAIR'
UNION ALL
SELECT 'User 19 has subcommittee:' as test, COUNT(*) as result FROM users WHERE id = 19 AND subcommittee_id IS NOT NULL
UNION ALL
SELECT 'Resolutions for subcommittee 1:' as test, COUNT(*) as result FROM resolution_assignments WHERE subcommittee_id = 1;
"@
    
    $results = psql -h localhost -U postgres -d eara_connect -c $testQuery
    Write-Host "Database test results:" -ForegroundColor Cyan
    Write-Host $results
} catch {
    Write-Host "❌ Error testing database: $_" -ForegroundColor Red
}

Write-Host "`n🎯 Next steps:" -ForegroundColor Green
Write-Host "1. Start the backend server: cd eara_connect_new_backend-main-2; mvn spring-boot:run" -ForegroundColor Yellow
Write-Host "2. Start the frontend server: cd eara_connect_new_frontend-main; npm start" -ForegroundColor Yellow
Write-Host "3. Login with: chair19@tech.eara.org / password" -ForegroundColor Yellow
Write-Host "4. Test the chair dashboard functionality" -ForegroundColor Yellow

Write-Host "`n📋 Test credentials:" -ForegroundColor Cyan
Write-Host "Email: chair19@tech.eara.org" -ForegroundColor White
Write-Host "Password: password" -ForegroundColor White
Write-Host "User ID: 19" -ForegroundColor White
