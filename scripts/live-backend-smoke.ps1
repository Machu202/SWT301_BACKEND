[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [Parameter(Mandatory=$true)][string]$CustomerUsername,
    [Parameter(Mandatory=$true)][string]$CustomerPassword,
    [Parameter(Mandatory=$true)][string]$AdminUsername,
    [Parameter(Mandatory=$true)][string]$AdminPassword,
    [string]$VoucherCode = ''
)

$ErrorActionPreference = 'Stop'
$results = New-Object System.Collections.Generic.List[object]

function Invoke-TestRequest {
    param([string]$Name, [string]$Method, [string]$Path, [hashtable]$Headers = @{}, $Body = $null)
    try {
        $params = @{
            Uri = "$BaseUrl$Path"
            Method = $Method
            Headers = $Headers
            TimeoutSec = 20
            ErrorAction = 'Stop'
        }
        if ($null -ne $Body) {
            $params.ContentType = 'application/json'
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
        }
        $response = Invoke-RestMethod @params
        $results.Add([pscustomobject]@{ Test=$Name; Result='PASS'; Detail='HTTP request completed' })
        return $response
    } catch {
        $results.Add([pscustomobject]@{ Test=$Name; Result='FAIL'; Detail=$_.Exception.Message })
        return $null
    }
}

$publicProducts = Invoke-TestRequest 'Public products' GET '/api/products'
Invoke-TestRequest 'Public product page' GET '/api/products/page?page=0&size=12'
Invoke-TestRequest 'Independent categories' GET '/api/reference/categories'

$customerLogin = Invoke-TestRequest 'Customer login' POST '/api/auth/login' @{} @{
    username=$CustomerUsername; password=$CustomerPassword
}
$adminLogin = Invoke-TestRequest 'Admin login' POST '/api/auth/login' @{} @{
    username=$AdminUsername; password=$AdminPassword
}

if ($customerLogin -and $customerLogin.token) {
    $customerHeaders = @{ Authorization = "Bearer $($customerLogin.token)" }
    Invoke-TestRequest 'Customer profile' GET '/api/profile' $customerHeaders
    $cart = Invoke-TestRequest 'Customer cart' GET '/api/carts' $customerHeaders
    Invoke-TestRequest 'Customer addresses' GET '/api/addresses' $customerHeaders
    Invoke-TestRequest 'Customer orders' GET '/api/orders' $customerHeaders
    Invoke-TestRequest 'Customer order page' GET '/api/orders/page?page=0&size=10' $customerHeaders
    Invoke-TestRequest 'Payment methods' GET '/api/reference/payment-methods' $customerHeaders
    if ($cart -and $cart.items -and $cart.items.Count -gt 0) {
        Invoke-TestRequest 'Checkout preview' GET '/api/orders/preview' $customerHeaders
        if ($VoucherCode) {
            Invoke-TestRequest 'Voucher validation' GET "/api/vouchers/check?code=$([uri]::EscapeDataString($VoucherCode))&orderSubtotal=$($cart.totalCartPrice)" $customerHeaders
        }
    } else {
        $results.Add([pscustomobject]@{ Test='Checkout preview'; Result='SKIP'; Detail='Cart is empty' })
    }
}

if ($adminLogin -and $adminLogin.token) {
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.token)" }
    Invoke-TestRequest 'Admin product page' GET '/api/admin/products?page=0&size=12' $adminHeaders
    Invoke-TestRequest 'Admin orders' GET '/api/admin/orders' $adminHeaders
    Invoke-TestRequest 'Admin order page' GET '/api/admin/orders/page?page=0&size=10' $adminHeaders
    Invoke-TestRequest 'Admin metrics' GET '/api/admin/orders/metrics' $adminHeaders
    Invoke-TestRequest 'Order statuses' GET '/api/reference/order-statuses' $adminHeaders
    Invoke-TestRequest 'Admin payment methods' GET '/api/reference/payment-methods' $adminHeaders
}

$results | Format-Table -AutoSize
$failed = @($results | Where-Object Result -eq 'FAIL').Count
if ($failed -gt 0) { exit 1 }
