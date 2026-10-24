param(
    [string]$ApiUrl = "http://localhost:8080",
    [Parameter(Mandatory)]
    [System.Management.Automation.PSCredential]$AdminCredential
)

$ErrorActionPreference = "Stop"
$baseUrl = $ApiUrl.TrimEnd("/")
$runId = Get-Date -Format "yyyyMMddHHmmss"

function Invoke-StockFlowApi {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Path,
        [object]$Body,
        [switch]$Anonymous
    )

    $parameters = @{ Method = $Method; Uri = "$baseUrl$Path" }
    if (-not $Anonymous) { $parameters.Headers = @{ Authorization = "Bearer $script:token" } }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = $Body | ConvertTo-Json -Depth 8
    }
    Invoke-RestMethod @parameters
}

$login = Invoke-StockFlowApi -Method Post -Path "/api/auth/login" -Anonymous -Body @{
    email = $AdminCredential.UserName
    password = $AdminCredential.GetNetworkCredential().Password
}
$script:token = $login.token

$categories = @(
    Invoke-StockFlowApi Post "/api/categories" @{ name = "Demo Hardware $runId"; description = "Equipos demo"; active = $true }
    Invoke-StockFlowApi Post "/api/categories" @{ name = "Demo Perifericos $runId"; description = "Accesorios demo"; active = $true }
    Invoke-StockFlowApi Post "/api/categories" @{ name = "Demo Oficina $runId"; description = "Insumos demo"; active = $true }
)
$suppliers = @(
    Invoke-StockFlowApi Post "/api/suppliers" @{ name = "Proveedor Norte $runId"; taxId = "DEMO-N-$runId"; contactName = "Ana Demo"; email = "norte-$runId@example.test"; phone = "3000000001"; address = "Bogota"; active = $true }
    Invoke-StockFlowApi Post "/api/suppliers" @{ name = "Proveedor Sur $runId"; taxId = "DEMO-S-$runId"; contactName = "Luis Demo"; email = "sur-$runId@example.test"; phone = "3000000002"; address = "Medellin"; active = $true }
)
$customers = 1..3 | ForEach-Object {
    Invoke-StockFlowApi Post "/api/customers" @{ documentNumber = "DEMO-$runId-$_"; firstName = "Cliente"; lastName = "Demo $_"; email = "cliente$_-$runId@example.test"; phone = "300000001$_"; address = "Colombia"; active = $true }
}
$productNames = @("Teclado", "Mouse", "Monitor", "Base", "Audifonos", "Cuaderno")
$products = for ($index = 0; $index -lt $productNames.Count; $index++) {
    Invoke-StockFlowApi Post "/api/products" @{
        sku = "DEMO-$runId-$($index + 1)"
        name = "$($productNames[$index]) Demo"
        description = "Dato demostrativo StockFlow"
        salePrice = 25 + ($index * 10)
        active = $true
        categoryId = $categories[$index % $categories.Count].id
    }
}

$entry = Invoke-StockFlowApi Post "/api/stock-entries" @{
    supplierId = $suppliers[0].id
    referenceNumber = "DEMO-ENTRY-$runId"
    notes = "Carga controlada de demostracion"
    items = @(@{ productId = $products[0].id; quantity = 20; unitCost = 15 })
}
$entry = Invoke-StockFlowApi Post "/api/stock-entries/$($entry.id)/confirm"

$sale = Invoke-StockFlowApi Post "/api/sales" @{
    customerId = $customers[0].id
    items = @(@{ productId = $products[0].id; quantity = 3 })
}
$sale = Invoke-StockFlowApi Post "/api/sales/$($sale.id)/complete"

$exit = Invoke-StockFlowApi Post "/api/stock-exits" @{
    reason = "INTERNAL_USE"
    notes = "Salida demostrativa"
    items = @(@{ productId = $products[0].id; quantity = 2 })
}
$exit = Invoke-StockFlowApi Post "/api/stock-exits/$($exit.id)/confirm"

$inventory = Invoke-StockFlowApi Get "/api/inventory/product/$($products[0].id)"
$movements = Invoke-StockFlowApi Get "/api/inventory-movements?productId=$($products[0].id)&size=20&sort=id,asc"
$lowStock = Invoke-StockFlowApi Get "/api/reports/low-stock"
$inventorySummary = Invoke-StockFlowApi Get "/api/reports/inventory-summary"
$salesSummary = Invoke-StockFlowApi Get "/api/reports/sales-summary"

if ($inventory.currentStock -ne 15) { throw "Expected final stock 15, observed $($inventory.currentStock)." }

[pscustomobject]@{
    runId = $runId
    productId = $products[0].id
    initialStock = 0
    entryQuantity = 20
    saleQuantity = 3
    exitQuantity = 2
    finalStock = $inventory.currentStock
    movementCount = $movements.totalElements
    lowStockProducts = $lowStock.Count
    inventorySummary = $inventorySummary
    salesSummary = $salesSummary
} | ConvertTo-Json -Depth 6
