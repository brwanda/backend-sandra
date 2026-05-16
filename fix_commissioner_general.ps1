# Fix COMMISSIONER_GENERAL Issue Script
Write-Host "🔧 Fixing COMMISSIONER_GENERAL Role Issues..." -ForegroundColor Green

# Step 1: Stop any running Java processes
Write-Host "📋 Step 1: Stopping any running Java processes..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Step 2: Run database migration
Write-Host "📋 Step 2: Running database migration..." -ForegroundColor Yellow
try {
    # Try to use psql if available
    $env:PGPASSWORD = "bernard123"
    $sqlScript = Get-Content -Path "add_commissioner_general_column.sql" -Raw
    
    # Execute SQL directly using psql
    $sqlScript | psql -h localhost -U postgres -d eara_connect_db-final-EARA10
    
    Write-Host "✅ Database migration completed successfully!" -ForegroundColor Green
} catch {
    Write-Host "⚠️ Could not run database migration automatically. Please run manually:" -ForegroundColor Yellow
    Write-Host "psql -h localhost -U postgres -d eara_connect_db-final-EARA10 -f add_commissioner_general_column.sql" -ForegroundColor Cyan
}

# Step 3: Start backend server
Write-Host "📋 Step 3: Starting backend server..." -ForegroundColor Yellow
Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '$PWD'; ./mvnw spring-boot:run" -WindowStyle Normal

# Step 4: Wait for backend to start
Write-Host "📋 Step 4: Waiting for backend server to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Step 5: Test backend endpoint
Write-Host "📋 Step 5: Testing backend endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/commissioner-generals/get-all" -Method GET -TimeoutSec 10
    Write-Host "✅ Backend server is running and responding!" -ForegroundColor Green
    Write-Host "Response status: $($response.StatusCode)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Backend server is not responding. Please check the logs." -ForegroundColor Red
}

# Step 6: Start frontend server
Write-Host "📋 Step 6: Starting frontend server..." -ForegroundColor Yellow
Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '../EARACONNECT-FRONTEND-master'; npm start" -WindowStyle Normal

Write-Host "🎉 Setup complete! Please check the server windows for any errors." -ForegroundColor Green
Write-Host "Frontend should be available at: http://localhost:3000" -ForegroundColor Cyan
Write-Host "Backend should be available at: http://localhost:8081" -ForegroundColor Cyan
