Write-Host "Testing Report Submission..." -ForegroundColor Green

$body = @{
    resolution = @{ id = 2 }
    subcommittee = @{ id = 1 }
    progressDetails = "Test progress details for API testing"
    hindrances = "No hindrances"
    performancePercentage = 85
} | ConvertTo-Json

Write-Host "Request Body: $body" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/chair/reports?chairId=19" -Method POST -Body $body -ContentType "application/json"
    Write-Host "Success: $($response.Content)" -ForegroundColor Green
} catch {
    Write-Host "Error Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    Write-Host "Error Message: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody" -ForegroundColor Red
    }
}
