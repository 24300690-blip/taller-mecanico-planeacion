/** Pruebas API con curl; los secretos permanecen en .verification, ignorado por Git. */
const fs=require('fs'), cp=require('child_process'), path=require('path');
const root=path.resolve(__dirname,'..'), tmp=path.join(root,'.verification'); fs.mkdirSync(tmp,{recursive:true});
for (const key of ["TEST_PASSWORD_1","TEST_PASSWORD_2","TEST_PASSWORD_3","TEST_PASSWORD_4","TEST_PASSWORD_5"]) { if (!process.env[key]) throw new Error(`Define ${key} en el entorno.`); }
const env=Object.fromEntries(fs.readFileSync(path.join(root,'.env'),'utf8').split(/\r?\n/).filter(x=>/^[A-Z_]+=/.test(x)).map(x=>{const i=x.indexOf('=');return [x.slice(0,i),x.slice(i+1).replace(/^['"]|['"]$/g,'')]}));
let api=process.env.TEST_API||'http://localhost:8088'; const results=[], suffix=Date.now();
/** Ejecuta curl sin imprimir respuestas sensibles y comprueba cuerpo y estado. */
function call(name,method,url,data,token,expected=200,jar='admin',extra=[],check=()=>true){
 const body=path.join(tmp,'response.json'), input=path.join(tmp,'request.json');
 const args=['-sS','-o',body,'-w','%{http_code}','-X',method,api+url,'-b',path.join(tmp,jar+'.cookies'),'-c',path.join(tmp,jar+'.cookies')];
 if(token)args.push('-H','Authorization: Bearer '+token);
 if(data){fs.writeFileSync(input,JSON.stringify(data));args.push('-H','Content-Type: application/json','--data-binary','@'+input)}
 args.push(...extra); let code=Number(cp.execFileSync('curl.exe',args,{encoding:'utf8'})), json={};try{json=JSON.parse(fs.readFileSync(body,'utf8'))}catch{}
 const ok=[expected].flat().includes(code)&&check(json);results.push({Caso:name,HTTP:code,Esperado:expected,Resultado:ok?'OK':'FALLO',Detalle:ok?'':json.message}); return json;
}
/** Datos únicos para evitar interferencia con registros existentes. */
const account=(role='MECANICO')=>({email:`${role.toLowerCase()}-${suffix}@example.com`,fullName:'Prueba '+role,password:process.env.TEST_PASSWORD_1,role});
const login=(a,jar)=>call('Login '+a.role,'POST','/api/auth/login',{email:a.email,password:a.password},null,200,jar);
if(process.env.TEST_PUBLIC_ONLY==='true'){
 api=process.env.TEST_PUBLIC_API||'http://localhost:8092';
 for(const role of ['ADMINISTRADOR','GERENTE'])call('Registro público rechaza '+role,'POST','/api/auth/register',account(role),null,400,'public');
 call('Registro público habilitado','POST','/api/auth/register',account('RECEPCIONISTA'),null,201,'public');
 const previous=JSON.parse(fs.readFileSync(path.join(root,'docs/pruebas-api.json'),'utf8')).filter(x=>!results.some(r=>r.Caso===x.Caso));
 fs.writeFileSync(path.join(root,'docs/pruebas-api.json'),JSON.stringify([...previous,...results],null,2));console.table(results);process.exit(results.every(x=>x.Resultado==='OK')?0:1);
}
let admin=call('Login correcto','POST','/api/auth/login',{email:env.BOOTSTRAP_ADMIN_EMAIL,password:process.env.BOOTSTRAP_ADMIN_PASSWORD}).accessToken;
call('Login fallido','POST','/api/auth/login',{email:env.BOOTSTRAP_ADMIN_EMAIL,password:process.env.TEST_PASSWORD_2},null,401,'bad');
call('Refresh','POST','/api/auth/refresh',null,null,200);
call('Recuperación','POST','/api/auth/forgot-password',{email:env.BOOTSTRAP_ADMIN_EMAIL});
call('Registro público apagado','POST','/api/auth/register',account(),null,403);
call('Catálogo de roles','GET','/api/roles',null,admin);
call('Usuarios sin sesión','GET','/api/usuarios',null,null,401,'anon');
call('Crear usuario sin sesión','POST','/api/usuarios',account(),null,401,'anon');
const a=account(), created=call('Usuario creado','POST','/api/usuarios',a,admin,201);
call('Usuario duplicado','POST','/api/usuarios',a,admin,409);
call('Usuarios paginados sin hash','GET','/api/usuarios',null,admin,200,'admin',[],j=>Array.isArray(j.content)&&!JSON.stringify(j).includes('passwordHash'));
let temp=login(a,'mechanic');
call('Primer acceso restringido','GET','/api/clientes',null,temp.accessToken,403,'mechanic',[],j=>j.message==='PASSWORD_CHANGE_REQUIRED'&&temp.mustChangePassword===true);
const change={currentPassword:a.password,newPassword:process.env.TEST_PASSWORD_3,confirmPassword:process.env.TEST_PASSWORD_3};
for(const [name,patch] of [['Actual incorrecta',{currentPassword:process.env.TEST_PASSWORD_2}],['Nueva débil',{newPassword:process.env.TEST_PASSWORD_4,confirmPassword:process.env.TEST_PASSWORD_4}],['Nueva igual',{newPassword:a.password,confirmPassword:a.password}],['Confirmación distinta',{confirmPassword:process.env.TEST_PASSWORD_5}]])
 call(name,'POST','/api/auth/change-password',{...change,...patch},temp.accessToken,400,'mechanic');
fs.copyFileSync(path.join(tmp,'mechanic.cookies'),path.join(tmp,'old.cookies'));
let normal=call('Cambiar contraseña','POST','/api/auth/change-password',change,temp.accessToken,200,'mechanic',[],j=>j.mustChangePassword===false).accessToken;
call('Refresh anterior revocado','POST','/api/auth/refresh',null,null,401,'old');
call('Temporal anterior rechazada','POST','/api/auth/login',{email:a.email,password:a.password},null,401,'bad');
a.password=change.newPassword; normal=login(a,'mechanic').accessToken;
call('Mecánico sin clientes','GET','/api/clientes',null,normal,403,'mechanic');
call('Mecánico sin usuarios','POST','/api/usuarios',account('AYUDANTE'),normal,403,'mechanic');
call('Mecánico sin roles','GET','/api/roles',null,normal,403,'mechanic');
call('No desactivarse','PATCH',`/api/usuarios/${call('Identidad','GET','/api/auth/me',null,admin).id}/estado`,{activo:false},admin,400);
call('Desactivar usuario','PATCH',`/api/usuarios/${created.id}/estado`,{activo:false},admin);
call('Inactivo no inicia','POST','/api/auth/login',{email:a.email,password:a.password},null,401,'bad');
call('JWT revocado por baja','GET','/api/auth/me',null,normal,401,'mechanic');
call('Activar usuario','PATCH',`/api/usuarios/${created.id}/estado`,{activo:true},admin);
normal=login(a,'mechanic').accessToken;
const reset=call('Restablecer contraseña','POST',`/api/usuarios/${created.id}/restablecer-contrasena`,{},admin);
a.password=reset.temporaryPassword; temp=login(a,'mechanic');
call('Temporal restablecida restringida','GET','/api/clientes',null,temp.accessToken,403,'mechanic',[],j=>j.message==='PASSWORD_CHANGE_REQUIRED');
call('Cambiar temporal restablecida','POST','/api/auth/change-password',{...change,currentPassword:a.password},temp.accessToken,200,'mechanic');
const png=path.join(tmp,'foto.png');fs.writeFileSync(png,Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=','base64'));
const bad=path.join(tmp,'bad.jpg');fs.writeFileSync(bad,'no image');const large=path.join(tmp,'large.jpg');let bytes=Buffer.alloc(15*1024*1024+1);bytes[0]=255;bytes[1]=216;bytes[2]=255;fs.writeFileSync(large,bytes);
for(const [name,file,code,token] of [['Foto propia válida',png,200,admin],['Foto demasiado grande',large,413,admin],['Foto falsa',bad,400,admin],['Foto sin sesión',png,401,null]])call(name,'POST','/api/perfil/foto',null,token,code,'anon',['-F','foto=@'+file]);
call('Consultar foto propia','GET','/api/perfil/foto',null,admin);
const uniqueName='Prueba'+String(suffix).split('').map(x=>String.fromCharCode(97+Number(x))).join('');
const client={nombres:uniqueName,apellidoPaterno:'Cliente',apellidoMaterno:'Taller',fechaNacimiento:'1988-04-15',telefonoPersonal:String(suffix).slice(-10),correoPersonal:`cliente-${suffix}@example.com`,calle:'Reforma 100',colonia:'Centro',municipio:'Coyoacan',estado:'Ciudad de Mexico',codigoPostal:'04000'};
/** Crea multipart para los mismos validadores usados por Vue. */
function form(data,photo){return [...Object.entries(data).flatMap(([k,v])=>['-F',k+'='+v]),...(photo?['-F','foto=@'+photo]:[])]}
call('Cliente sin foto','POST','/api/clientes',null,admin,201,'admin',form(client));
call('Cliente duplicado','POST','/api/clientes',null,admin,409,'admin',form(client));
call('Cliente inválido','POST','/api/clientes',null,admin,400,'admin',form({...client,nombres:'123'}));
const photoClient={...client,nombres:'Foto'+uniqueName,correoPersonal:`foto-${suffix}@example.com`,telefonoPersonal:String(Number(client.telefonoPersonal)+1).padStart(10,'0')};
call('Cliente foto falsa','POST','/api/clientes',null,admin,400,'admin',form(photoClient,bad));
call('Cliente foto mayor a 15 MB vía nginx','POST','/api/clientes',null,admin,413,'admin',form(photoClient,large));
call('Cliente con foto','POST','/api/clientes',null,admin,201,'admin',form(photoClient,png));
call('Búsqueda q','GET','/api/clientes?q='+encodeURIComponent(client.correoPersonal),null,admin,200,'admin',[],j=>j.totalElements===1);
call('Clientes sin sesión','GET','/api/clientes',null,null,401,'anon');
const sql=`SELECT CONCAT('clientes=',COUNT(*),',fotos=',SUM(foto_path IS NOT NULL)) FROM cliente WHERE correo_personal IN ('${client.correoPersonal}','${photoClient.correoPersonal}'); SELECT CONCAT('usuario=',COUNT(*)) FROM users WHERE email='${a.email}'; SELECT CONCAT('foto_perfil=',COUNT(*)) FROM users WHERE email='${env.BOOTSTRAP_ADMIN_EMAIL}' AND foto_path IS NOT NULL;`;
try {const output=cp.execFileSync('docker.exe',['exec','taller-mecanico-mysql','sh','-lc','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --batch --skip-column-names taller_mecanico -e "$1"','sh',sql],{encoding:'utf8'}).trim();results.push({Caso:'MySQL: cliente, usuario, fotos',HTTP:output,Esperado:'2 clientes; 1 foto; 1 usuario; 1 perfil',Resultado:output.includes('clientes=2,fotos=1')&&output.includes('usuario=1')&&output.includes('foto_perfil=1')?'OK':'FALLO'})}catch{results.push({Caso:'MySQL',Resultado:'FALLO'})}
call('Quitar foto','DELETE','/api/perfil/foto',null,admin);
call('Foto eliminada','GET','/api/perfil/foto',null,admin,404);
if(process.env.TEST_PUBLIC_API){api=process.env.TEST_PUBLIC_API;
 for(const role of ['ADMINISTRADOR','GERENTE'])call('Registro público rechaza '+role,'POST','/api/auth/register',account(role),null,400,'public');
 call('Registro público habilitado','POST','/api/auth/register',account('RECEPCIONISTA'),null,201,'public');
}
fs.writeFileSync(path.join(root,'docs','pruebas-api.json'),JSON.stringify(results,null,2)); console.table(results); if(results.some(x=>x.Resultado==='FALLO'))process.exitCode=1;
