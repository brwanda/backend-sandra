# =====================================================================
# PowerShell Script to Run HOD Migration
# =====================================================================
# This script runs the HOD migration SQL file against your PostgreSQL database
# =====================================================================

# Database configuration (from application.properties)
$DB_HOST = "postgres.railway.internal"
$DB_PORT = "5432"
$DB_NAME = "railway"
$DB_USER = "postgres"
$DB_PASSWORD = "ULYdTAOfswiXngTPBcwTyzXYJumPfiIb"

# SQL file to execute
$SQL_FILE = "migrate_hod_to_committee_table_safe.sql"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "HOD MIGRATION SCRIPT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Database: $DB_NAME" -ForegroundColor Yellow
Write-Host "SQL File: $SQL_FILE" -ForegroundColor Yellow
Write-Host ""

# Check if PostgreSQL is installed
$psqlPath = $null

# Common PostgreSQL installation paths
$possiblePaths = @(
    "C:\Program Files\PostgreSQL\*\bin\psql.exe",
    "C:\Program Files (x86)\PostgreSQL\*\bin\psql.exe",
    "C:\PostgreSQL\*\bin\psql.exe"
)

foreach ($path in $possiblePaths) {
    $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $psqlPath = $found.FullName
        break
    }
}

if (-not $psqlPath) {
    Write-Host "ERROR: PostgreSQL psql.exe not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install PostgreSQL or add it to your PATH." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Alternative: Run the SQL file manually using pgAdmin or your preferred PostgreSQL client." -ForegroundColor Yellow
    Write-Host "SQL File Location: $PSScriptRoot\$SQL_FILE" -ForegroundColor Cyan
    Write-Host ""
    pause
    exit 1
}

Write-Host "Found PostgreSQL at: $psqlPath" -ForegroundColor Green
Write-Host ""

# Confirm before proceeding
Write-Host "WARNING: This will modify your database!" -ForegroundColor Yellow
Write-Host "The migration will:" -ForegroundColor Yellow
Write-Host "  1. Create 'Head Of Delegation' in the committee table" -ForegroundColor White
Write-Host "  2. Move HOD members from csub_committee_members to country_committee_member" -ForegroundColor White
Write-Host "  3. Update users table" -ForegroundColor White
Write-Host "  4. Remove old HOD data from sub_committee table" -ForegroundColor White
Write-Host ""
$confirmation = Read-Host "Do you want to proceed? (yes/no)"

if ($confirmation -ne "yes") {
    Write-Host "Migration cancelled." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Running migration..." -ForegroundColor Cyan
Write-Host ""

# Set password environment variable
$env:PGPASSWORD = $DB_PASSWORD

# Run the SQL file
try {
    & $psqlPath -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$PSScriptRoot\$SQL_FILE"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "MIGRATION COMPLETED SUCCESSFULLY!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Cyan
        Write-Host "  1. Restart your Spring Boot backend (if running)" -ForegroundColor White
        Write-Host "  2. Refresh your React frontend" -ForegroundColor White
        Write-Host "  3. Try creating a new member - HOD should now appear in the dropdown!" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host ""
        Write-Host "ERROR: Migration failed with exit code $LASTEXITCODE" -ForegroundColor Red
        Write-Host "Please check the error messages above." -ForegroundColor Yellow
        Write-Host ""
    }
} catch {
    Write-Host ""
    Write-Host "ERROR: Failed to run migration" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
} finally {
    # Clear password from environment
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host ""
pause
