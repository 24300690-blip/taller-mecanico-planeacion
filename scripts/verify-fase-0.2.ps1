param([string]$Api = 'http://localhost:8081', [switch]$DiagnoseClient, [switch]$RetryClientFailures, [switch]$RetryDatabase)
$ErrorActionPreference = 'Stop'
foreach ($key in @('TEST_PASSWORD_1','TEST_PASSWORD_2','TEST_PASSWORD_3','TEST_PASSWORD_4','TEST_PASSWORD_5')) { if (![Environment]::GetEnvironmentVariable($key)) { throw "Define $key en el entorno." } }
$root = Split-Path -Parent $PSScriptRoot
$envValues = @{}
Get-Content (Join-Path $root '.env') | ForEach-Object {
  if ($_ -match '^\s*([A-Z0-9_]+)=(.*)$') { $envValues[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
$adminEmail = $envValues['BOOTSTRAP_ADMIN_EMAIL']
$adminPassword = $env:BOOTSTRAP_ADMIN_PASSWORD
if (!$adminEmail -or !$adminPassword) { throw 'Configura BOOTSTRAP_ADMIN_EMAIL y BOOTSTRAP_ADMIN_PASSWORD en .env.' }
$cookie = Join-Path $env:TEMP "fase02-$([guid]::NewGuid()).cookies"
$tmp = Join-Path $env:TEMP "fase02-$([guid]::NewGuid())"
New-Item -ItemType Directory -Path $tmp | Out-Null
$results = [System.Collections.Generic.List[object]]::new()
$suffix = [guid]::NewGuid().ToString('N').Substring(0,10)
$uniqueGivenName = -join (1..10 | ForEach-Object { [char](Get-Random -Minimum 97 -Maximum 123) })
$client = @{ nombres=$uniqueGivenName; apellidoPaterno='Cliente'; apellidoMaterno='Taller'; fechaNacimiento='1988-04-15'; telefonoPersonal="55$((Get-Random -Minimum 10000000 -Maximum 99999999))"; telefonoTrabajo=''; correoPersonal="cliente-$suffix@example.com"; correoTrabajo=''; calle='Av. Reforma 100'; colonia='Centro'; municipio='Coyoacán'; estado='Ciudad de México'; codigoPostal='04000' }

# Ejecuta un caso curl, valida el código y guarda resultado resumido.
function Call-Curl([string]$name, [string[]]$arguments, [int[]]$expected) {
  $raw = (& curl.exe -sS -w "`n%{http_code}" @arguments 2>&1 | Out-String).TrimEnd()
  $parts = $raw -split "`r?`n"
  $status = [int]$parts[-1]
  $body = if ($parts.Length -gt 1) { $parts[0..($parts.Length-2)] -join "`n" } else { '' }
  $ok = $expected -contains $status
  $detail = if ($ok) { '' } else { try { ($body | ConvertFrom-Json).message } catch { $body } }
  $results.Add([pscustomobject]@{ Caso=$name; Esperado=($expected -join '/'); HTTP=$status; Resultado=if($ok){'OK'}else{'FALLO'}; Detalle=$detail })
  return [pscustomobject]@{ Status=$status; Body=$body; Json=try{$body | ConvertFrom-Json}catch{$null} }
}
# Construye argumentos curl multipart para un alta de cliente.
function Client-Form([hashtable]$data, [string]$photoPath='') {
  $args = @('-X','POST',"$Api/api/clientes",'-H',"Authorization: Bearer $script:adminToken")
  foreach ($key in $data.Keys) { if ($data[$key] -ne '') { $args += @('-F',"$key=$($data[$key])") } }
  if ($photoPath) { $args += @('-F',"foto=@$photoPath;type=image/jpeg") }
  return $args
}

if ($DiagnoseClient) {
  $admin=Call-Curl 'Credencial de prueba para diagnóstico' @('-X','POST',"$Api/api/auth/login",'-H','Content-Type: application/json','-c',$cookie,'-d',(@{email=$adminEmail;password=$adminPassword}|ConvertTo-Json -Compress)) @(200)
  $script:adminToken=$admin.Json.accessToken
  $null=Call-Curl 'Diagnóstico de alta multipart válida' (Client-Form $client) @(201)
  $results | Format-Table -AutoSize
  Remove-Item -LiteralPath $cookie -Force -ErrorAction SilentlyContinue
  Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
  if ($results.Resultado -contains 'FALLO') { exit 1 }; exit 0
}

if ($RetryDatabase) {
  $sql="SELECT CONCAT('clientes=',COUNT(*),',completos=',SUM(nombres<>'' AND apellido_paterno<>'' AND fecha_nacimiento IS NOT NULL AND telefono_personal<>'' AND correo_personal<>'' AND calle<>'' AND colonia<>'' AND municipio<>'' AND estado<>'' AND codigo_postal<>''),',fotos=',SUM(foto_path IS NOT NULL)) FROM cliente WHERE created_by=(SELECT id FROM users WHERE email='$adminEmail') AND created_at >= NOW() - INTERVAL 1 HOUR;"
  $db=& docker.exe exec taller-mecanico-mysql sh -lc "mysql -uroot -p`"`$MYSQL_ROOT_PASSWORD`" --batch --skip-column-names taller_mecanico -e `"$sql`"" 2>&1 | Out-String
  $dbValue=($db -split "`r?`n" | Where-Object { $_ -match '^clientes=' } | Select-Object -Last 1).Trim()
  $dbOk=$dbValue -match '^clientes=2,completos=2,fotos=1$'
  $results.Add([pscustomobject]@{Caso='Reintento: MySQL confirma clientes completos y foto';Esperado='clientes=2,completos=2,fotos=1';HTTP=if($dbOk){'SQL'}else{'ERROR'};Resultado=if($dbOk){'OK'}else{'FALLO'};Detalle=if($dbOk){''}else{$dbValue}})
  $results | Format-Table -AutoSize
  if ($results.Resultado -contains 'FALLO') { exit 1 }; exit 0
}

if ($RetryClientFailures) {
  $adminLogin=Call-Curl 'Login requerido para repetir fallos de cliente' @('-X','POST',"$Api/api/auth/login",'-H','Content-Type: application/json','-c',$cookie,'-d',(@{email=$adminEmail;password=$adminPassword}|ConvertTo-Json -Compress)) @(200)
  $script:adminToken=$adminLogin.Json.accessToken
  $created=Call-Curl 'Reintento: cliente válido sin foto' (Client-Form $client) @(201)
  $dupeEmail=$client.Clone(); $dupeEmail.telefonoPersonal='5512345678'; $dupeEmail.nombres='Otro'; $dupeEmail.correoPersonal=$client.correoPersonal
  $null=Call-Curl 'Reintento: duplicado por correo' (Client-Form $dupeEmail) @(409)
  $dupePhone=$client.Clone(); $dupePhone.correoPersonal="otro-$suffix@example.com"; $dupePhone.nombres='Otro'
  $null=Call-Curl 'Reintento: duplicado por teléfono' (Client-Form $dupePhone) @(409)
  $dupeName=$client.Clone(); $dupeName.correoPersonal="otro2-$suffix@example.com"; $dupeName.telefonoPersonal='5587654321'
  $null=Call-Curl 'Reintento: duplicado por nombre y nacimiento' (Client-Form $dupeName) @(409)
  $jpeg=Join-Path $tmp 'foto.jpg'; [IO.File]::WriteAllBytes($jpeg,[byte[]](0xFF,0xD8,0xFF,0xD9))
  $fileClient=$client.Clone(); $fileClient.nombres='Foto'; $fileClient.telefonoPersonal="55$((Get-Random -Minimum 10000000 -Maximum 99999999))"; $fileClient.correoPersonal="foto-ok-$suffix@example.com"
  $null=Call-Curl 'Reintento: cliente válido con foto' (Client-Form $fileClient $jpeg) @(201)
  $sql="SELECT CONCAT('cliente=',COUNT(*),',campos=',SUM(nombres<>'' AND apellido_paterno<>'' AND fecha_nacimiento IS NOT NULL AND telefono_personal<>'' AND correo_personal<>'' AND calle<>'' AND colonia<>'' AND municipio<>'' AND estado<>'' AND codigo_postal<>'')) FROM cliente WHERE correo_personal='$($client.correoPersonal)';"
  $db=& docker.exe exec taller-mecanico-mysql sh -lc "mysql -uroot -p`"`$MYSQL_ROOT_PASSWORD`" --batch --skip-column-names taller_mecanico -e `"$sql`"" 2>&1 | Out-String
  $photoSql="SELECT COUNT(*) FROM cliente WHERE correo_personal='$($fileClient.correoPersonal)' AND foto_path IS NOT NULL;"
  $photoDb=& docker.exe exec taller-mecanico-mysql sh -lc "mysql -uroot -p`"`$MYSQL_ROOT_PASSWORD`" --batch --skip-column-names taller_mecanico -e `"$photoSql`"" 2>&1 | Out-String
  $dbValue=($db -split "`r?`n" | Where-Object { $_ -match '^cliente=' } | Select-Object -Last 1).Trim()
  $photoValue=($photoDb -split "`r?`n" | Where-Object { $_ -match '^1$' } | Select-Object -Last 1).Trim()
  $dbOk=$dbValue -eq 'cliente=1,campos=1' -and $photoValue -eq '1'
  $results.Add([pscustomobject]@{Caso='Reintento: MySQL persiste todos los campos y la foto';Esperado='cliente=1,campos=1;foto=1';HTTP=if($dbOk){'SQL'}else{'ERROR'};Resultado=if($dbOk){'OK'}else{'FALLO'};Detalle=if($dbOk){''}else{"$db $photoDb"}})
  $results | Format-Table -AutoSize
  Remove-Item -LiteralPath $cookie -Force -ErrorAction SilentlyContinue
  Remove-Item -LiteralPath $jpeg -Force -ErrorAction SilentlyContinue
  Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
  if ($results.Resultado -contains 'FALLO') { exit 1 }; exit 0
}

try {
  $adminLogin = Call-Curl 'Login administrador correcto' @('-X','POST',"$Api/api/auth/login",'-H','Content-Type: application/json','-c',$cookie,'-d',(@{email=$adminEmail;password=$adminPassword}|ConvertTo-Json -Compress)) @(200)
  $script:adminToken = $adminLogin.Json.accessToken
  $null = Call-Curl 'Login fallido' @('-X','POST',"$Api/api/auth/login",'-H','Content-Type: application/json','-d',(@{email=$adminEmail;password=$env:TEST_PASSWORD_1}|ConvertTo-Json -Compress)) @(401)
  $null = Call-Curl 'Refresh token' @('-X','POST',"$Api/api/auth/refresh",'-b',$cookie,'-c',$cookie) @(200)
  $null = Call-Curl 'Solicitud de recuperación' @('-X','POST',"$Api/api/auth/forgot-password",'-H','Content-Type: application/json','-d',(@{email=$adminEmail}|ConvertTo-Json -Compress)) @(200)
  $null = Call-Curl 'Registro público recepcionista' @('-X','POST',"$Api/api/auth/register",'-H','Content-Type: application/json','-d',(@{email="recep-$suffix@example.com";fullName='Recepcionista Prueba';password=$env:TEST_PASSWORD_2;role='RECEPCIONISTA'}|ConvertTo-Json -Compress)) @(201)
  $null = Call-Curl 'Registro público GERENTE bloqueado' @('-X','POST',"$Api/api/auth/register",'-H','Content-Type: application/json','-d',(@{email="gerente-$suffix@example.com";fullName='Gerente Prueba';password=$env:TEST_PASSWORD_3;role='GERENTE'}|ConvertTo-Json -Compress)) @(400)
  $null = Call-Curl 'Registro público ADMINISTRADOR bloqueado' @('-X','POST',"$Api/api/auth/register",'-H','Content-Type: application/json','-d',(@{email="admin-$suffix@example.com";fullName='Admin Prueba';password=$env:TEST_PASSWORD_4;role='ADMINISTRADOR'}|ConvertTo-Json -Compress)) @(400)
  $null = Call-Curl 'Catálogo de roles como admin' @('-X','GET',"$Api/api/roles",'-H',"Authorization: Bearer $adminToken") @(200)
  $created = Call-Curl 'Cliente válido sin foto' (Client-Form $client) @(201)
  if ($created.Json.id) { $null = Call-Curl 'Consulta de cliente por ID' @('-X','GET',"$Api/api/clientes/$($created.Json.id)",'-H',"Authorization: Bearer $adminToken") @(200) }
  $base = Call-Curl 'Lectura paginada de clientes' @('-X','GET',"$Api/api/clientes?page=0&size=10",'-H',"Authorization: Bearer $adminToken") @(200)
  $dupeEmail = $client.Clone(); $dupeEmail.telefonoPersonal='5512345678'; $dupeEmail.nombres='Otro'; $dupeEmail.correoPersonal=$client.correoPersonal
  $null = Call-Curl 'Duplicado por correo' (Client-Form $dupeEmail) @(409)
  $dupePhone = $client.Clone(); $dupePhone.correoPersonal="otro-$suffix@example.com"; $dupePhone.nombres='Otro'
  $null = Call-Curl 'Duplicado por teléfono' (Client-Form $dupePhone) @(409)
  $dupeName = $client.Clone(); $dupeName.correoPersonal="otro2-$suffix@example.com"; $dupeName.telefonoPersonal='5587654321'
  $null = Call-Curl 'Duplicado por nombre y nacimiento' (Client-Form $dupeName) @(409)
  $invalid = $client.Clone(); $invalid.correoPersonal="invalid-$suffix@example.com"; $invalid.nombres='Ana123'
  $null = Call-Curl 'Formato inválido' (Client-Form $invalid) @(400)
  $notImage = Join-Path $tmp 'no-es-imagen.jpg'; [IO.File]::WriteAllText($notImage,'contenido no imagen')
  $fileClient = $client.Clone(); $fileClient.correoPersonal="foto-$suffix@example.com"; $fileClient.telefonoPersonal='5598765432'; $fileClient.nombres='Foto'
  $null = Call-Curl 'Archivo que no es imagen' (Client-Form $fileClient $notImage) @(400)
  $jpeg = Join-Path $tmp 'foto.jpg'; [IO.File]::WriteAllBytes($jpeg,[byte[]](0xFF,0xD8,0xFF,0xD9))
  $fileClient.correoPersonal="foto-ok-$suffix@example.com"; $fileClient.telefonoPersonal='5576543210'
  $null = Call-Curl 'Cliente válido con fotografía' (Client-Form $fileClient $jpeg) @(201)
  $large = Join-Path $tmp 'grande.jpg'; $bytes = [byte[]]::new(15*1024*1024+1); $bytes[0]=0xFF; $bytes[1]=0xD8; $bytes[2]=0xFF; [IO.File]::WriteAllBytes($large,$bytes)
  $largeClient = $client.Clone(); $largeClient.correoPersonal="large-$suffix@example.com"; $largeClient.telefonoPersonal='5565432109'
  $null = Call-Curl 'Fotografía supera 15 MB' (Client-Form $largeClient $large) @(413,400)
  $null = Call-Curl 'Sin token' @('-X','GET',"$Api/api/clientes") @(401)
  $mechanic = Call-Curl 'Crear usuario mecánico para RBAC' @('-X','POST',"$Api/api/auth/register",'-H','Content-Type: application/json','-d',(@{email="mecanico-$suffix@example.com";fullName='Mecanico Prueba';password=$env:TEST_PASSWORD_5;role='MECANICO'}|ConvertTo-Json -Compress)) @(201)
  $mechLogin=Call-Curl 'Login mecánico' @('-X','POST',"$Api/api/auth/login",'-H','Content-Type: application/json','-d',(@{email="mecanico-$suffix@example.com";password=$env:TEST_PASSWORD_5}|ConvertTo-Json -Compress)) @(200)
  $null = Call-Curl 'Rol mecánico sin permiso' @('-X','GET',"$Api/api/clientes",'-H',"Authorization: Bearer $($mechLogin.Json.accessToken)") @(403)
  $emailForSql=$client.correoPersonal
  $sql="SELECT CONCAT('cliente=',COUNT(*),',campos=',SUM(nombres<>'' AND apellido_paterno<>'' AND fecha_nacimiento IS NOT NULL AND telefono_personal<>'' AND correo_personal<>'' AND calle<>'' AND colonia<>'' AND municipio<>'' AND estado<>'' AND codigo_postal<>'')) FROM cliente WHERE correo_personal='$emailForSql';"
  $db = & docker.exe exec taller-mecanico-mysql sh -lc "mysql -uroot -p`"`$MYSQL_ROOT_PASSWORD`" --batch --skip-column-names taller_mecanico -e `"$sql`"" 2>&1 | Out-String
  $photoMail="foto-ok-$suffix@example.com"
  $photoSql="SELECT COUNT(*) FROM cliente WHERE correo_personal='$photoMail' AND foto_path IS NOT NULL;"
  $photoDb = & docker.exe exec taller-mecanico-mysql sh -lc "mysql -uroot -p`"`$MYSQL_ROOT_PASSWORD`" --batch --skip-column-names taller_mecanico -e `"$photoSql`"" 2>&1 | Out-String
  $dbValue = ($db -split "`r?`n" | Where-Object { $_ -match '^cliente=' } | Select-Object -Last 1).Trim()
  $photoValue = ($photoDb -split "`r?`n" | Where-Object { $_ -match '^1$' } | Select-Object -Last 1).Trim()
  $dbOk = $dbValue -eq 'cliente=1,campos=1' -and $photoValue -eq '1'
  $results.Add([pscustomobject]@{Caso='MySQL: datos completos y ruta de foto persistidos';Esperado='cliente=1,campos=1;foto=1';HTTP=if($dbOk){'SQL'}else{'ERROR'};Resultado=if($dbOk){'OK'}else{'FALLO'};Detalle=if($dbOk){''}else{"$dbValue $photoValue"}})
} finally {
  Remove-Item -LiteralPath $cookie -Force -ErrorAction SilentlyContinue
  $tempRoot = [IO.Path]::GetFullPath($env:TEMP).TrimEnd('\') + '\'
  $resolvedTemp = [IO.Path]::GetFullPath($tmp)
  if ($resolvedTemp.StartsWith($tempRoot,[StringComparison]::OrdinalIgnoreCase)) { Remove-Item -LiteralPath $resolvedTemp -Recurse -Force -ErrorAction SilentlyContinue }
}
$results | Format-Table -AutoSize
if ($results.Resultado -contains 'FALLO') { exit 1 }
