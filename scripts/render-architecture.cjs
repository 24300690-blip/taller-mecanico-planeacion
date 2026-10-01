/** Regenera Archify y coloca las ayudas antes del gráfico, sin duplicarlas. */
const fs=require('fs'),cp=require('child_process'),path=require('path');
const root=path.resolve(__dirname,'..'),cli=process.env.ARCHIFY_CLI;
if(!cli)throw new Error('Define ARCHIFY_CLI con la ruta de archify.mjs instalado.');
const spec=path.join(root,'docs/arquitectura/taller-mecanico.architecture.json');
const out=path.join(root,'docs/arquitectura/taller-mecanico.html');
const raw=cp.execFileSync(process.execPath,[cli,'deliver','architecture',spec,out,'--quality','standard','--repo-root',root,'--json'],{encoding:'utf8'});
const receipt=JSON.parse(raw);if(!receipt.ok)throw new Error(receipt.error);
let html=fs.readFileSync(out,'utf8');
const cards=[...html.matchAll(/<div class="card">[\s\S]*?<\/ul>\s*<\/div>/g)].map(x=>x[0]);
const intro=cards.filter(x=>x.includes('<h3>Cómo leer este diagrama</h3>')||x.includes('<h3>Glosario breve</h3>'));
if(intro.length!==2)throw new Error('No se encontraron las dos ayudas de lectura.');
intro.forEach(card=>{html=html.replace(card,'')});
html=html.replace('<!-- Main Diagram -->',`<section class="reading-guide" aria-label="Ayuda de lectura">${intro.join('\n')}</section>\n<!-- Main Diagram -->`);
html=html.replace('</head>',`<style>
html[data-theme][data-preset]{--frontend-fill:#eee8fa!important;--frontend-stroke:#9c86ba!important;--backend-fill:#e6eefb!important;--backend-stroke:#829fc6!important;--database-fill:#eaf3d3!important;--database-stroke:#98b56a!important;--cloud-fill:#fbe9dc!important;--cloud-stroke:#cb9f80!important;--external-fill:#fdf3cf!important;--external-stroke:#c4b16a!important}
svg [data-node-id] .t-primary,svg [data-node-id] .t-muted{fill:#111a2e!important}
.reading-guide{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin:24px 0}.reading-guide .card:first-child li{list-style:none}.reading-guide .card:first-child li::before{display:none}.reading-guide .card:first-child ul{padding-left:0}@media(max-width:700px){.reading-guide{grid-template-columns:1fr}}@media print{.toolbar{display:none}}
</style></head>`).replace('<html lang="en"','<html lang="es"');
// El índice interactivo nativo conserva los nodos sin repetir la tarjeta estática.
html=html.replace(/<div class="card">\s*<div class="card-header">\s*<div class="card-dot violet"><\/div>\s*<h3>Índice de nodos<\/h3>[\s\S]*?<\/ul>\s*<\/div>/,'');
fs.writeFileSync(out,html);
fs.writeFileSync(path.join(root,'docs/arquitectura/fase3-generacion.json'),JSON.stringify({generator:'Archify',validation:receipt.validation,postprocess:'Mover las dos tarjetas de ayuda encima del diagrama, conservando su texto de la fuente JSON.',sha256:require('crypto').createHash('sha256').update(html).digest('hex'),nota:'El recibo deliver corresponde al HTML anterior al movimiento editorial; evidencia final en fase3-verificacion.json.'},null,2));
console.log('Archify: '+receipt.validation.checksPassed+'/'+receipt.validation.checkCount+' validaciones; ayudas movidas al inicio.');
