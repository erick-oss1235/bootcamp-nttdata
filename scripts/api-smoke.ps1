$ErrorActionPreference = 'Stop'

function Send-Json($method, $url, $body) {
    $payload = $body | ConvertTo-Json -Depth 10
    Invoke-RestMethod -Method $method -Uri $url -ContentType 'application/json' -Body $payload
}

$customerUrl = 'http://localhost:8081/api/v1/customers'
$accountUrl = 'http://localhost:8082/api/v1/accounts'
$creditUrl = 'http://localhost:8083/api/v1/credits'
$walletUrl = 'http://localhost:8085/api/v1/wallets'
$cardUrl = 'http://localhost:8086/api/v1/debit-cards'

$document = [guid]::NewGuid().ToString('N')
$customer = Send-Json POST $customerUrl @{
    documentNumber = $document
    fullName = 'Cliente Prueba Integracion'
    email = 'integracion@example.com'
    type = 'PERSONAL'
    profile = 'STANDARD'
}
if (-not $customer.id) { throw 'No se recibió el identificador del cliente.' }
Write-Host "Cliente creado: $($customer.id)"

$account = Send-Json POST $accountUrl @{
    customerId = $customer.id
    type = 'SAVINGS'
    monthlyMovementLimit = 10
    maintenanceFee = 0
    openingDeposit = 0
    freeMonthlyTransactions = 3
    transactionFee = 2.5
}
if (-not $account.id) { throw 'No se recibió el identificador de la cuenta.' }
Write-Host "Cuenta creada: $($account.id)"

$deposit = Send-Json POST "$accountUrl/$($account.id)/deposits" @{amount = 500}
if ([decimal]$deposit.balance -ne 500) { throw 'Falló la comprobación del depósito.' }

$withdrawal = Send-Json POST "$accountUrl/$($account.id)/withdrawals" @{amount = 150}
if ([decimal]$withdrawal.balance -ne 350) { throw 'Falló la comprobación del retiro.' }

$credit = Send-Json POST $creditUrl @{
    customerId = $customer.id
    type = 'PERSONAL_CARD'
    creditLimit = 5000
}
if (-not $credit.id) { throw 'No se recibió el identificador del crédito.' }

$charge = Send-Json POST "$creditUrl/$($credit.id)/charges" @{amount = 800}
if ([decimal]$charge.outstanding -ne 800) { throw 'Falló la comprobación del consumo de la tarjeta.' }

$payment = Send-Json POST "$creditUrl/$($credit.id)/payments" @{amount = 300}
if ([decimal]$payment.outstanding -ne 500) { throw 'Falló la comprobación del pago de crédito.' }

$randomPart = Get-Random -Minimum 100000000 -Maximum 999999999
$wallet = Send-Json POST $walletUrl @{
    documentType = 'DNI'
    documentNumber = $document
    phone = [string]$randomPart
    imei = '123456789000001'
    email = 'integracion@example.com'
}
if (-not $wallet.id) { throw 'No se recibió el identificador del monedero.' }

$debit = Send-Json POST $cardUrl @{
    customerId = $customer.id
    primaryAccountId = $account.id
    lastFour = '4321'
}
if (-not $debit.id) { throw 'No se recibió el identificador de la tarjeta de débito.' }

$token = Send-Json POST 'http://localhost:8084/api/v1/auth/login' @{
    username = 'admin'
    password = 'Demo123!'
}
if (-not $token.accessToken) { throw 'No se recibió el token JWT.' }

$gatewayCustomer = Invoke-RestMethod -Method GET -Uri "http://localhost:8080/api/v1/customers/$($customer.id)" -Headers @{
    Authorization = "Bearer $($token.accessToken)"
}
if ($gatewayCustomer.id -ne $customer.id) { throw 'Falló la validación del token JWT en el Gateway.' }

Write-Host 'Pruebas HTTP correctas: clientes, cuentas, depósitos, retiros, créditos, monederos, tarjetas y JWT.'
