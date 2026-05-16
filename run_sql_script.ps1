# PowerShell script to run SQL migration
# This script adds the commissioner_general column to the country_committee_member table

$connectionString = "Host=postgres.railway.internal;Database=railway;Username=postgres;Password=ULYdTAOfswiXngTPBcwTyzXYJumPfiIb"

# Read the SQL script
$sqlScript = Get-Content -Path "add_commissioner_general_column.sql" -Raw

try {
    # Create a connection to PostgreSQL using Npgsql
    Add-Type -Path "C:\Program Files\PostgreSQL\15\bin\Npgsql.dll"
    
    $connection = New-Object Npgsql.NpgsqlConnection($connectionString)
    $connection.Open()
    
    $command = New-Object Npgsql.NpgsqlCommand($sqlScript, $connection)
    $result = $command.ExecuteNonQuery()
    
    Write-Host "SQL script executed successfully. Rows affected: $result"
    
    $connection.Close()
} catch {
    Write-Host "Error executing SQL script: $($_.Exception.Message)"
    
    # Alternative approach using psql if available
    Write-Host "Trying alternative approach with psql..."
    try {
        $env:PGPASSWORD = "ULYdTAOfswiXngTPBcwTyzXYJumPfiIb"
        psql -h postgres.railway.internal -U postgres -d railway -f add_commissioner_general_column.sql
        Write-Host "SQL script executed successfully using psql"
    } catch {
        Write-Host "Both approaches failed. Please run the SQL script manually:"
        Write-Host "psql -h postgres.railway.internal -U postgres -d railway -f add_commissioner_general_column.sql"
    }
}
