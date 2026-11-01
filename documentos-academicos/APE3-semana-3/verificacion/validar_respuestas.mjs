import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const folder=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const root=path.resolve(folder,'../..');
const require=createRequire(path.join(root,'frontend/package.json'));
const {parse}=require('yaml');
const Ajv=require('ajv/dist/2020').default;
const addFormats=require('ajv-formats');
const ajv=new Ajv({strict:false,allErrors:true});addFormats(ajv);
const contract=parse(fs.readFileSync(path.join(folder,'contratos/openapi-observado.yaml'),'utf8'));
const evidence=JSON.parse(fs.readFileSync(path.join(folder,'evidencias/http-resultados.json'),'utf8'));
const result=[];
function deref(node){return node?.$ref?node.$ref.split('/').slice(1).reduce((v,k)=>v[k],contract):node;}
for(const item of evidence.cases){
 const url=item.path.split('?')[0];
 const route=Object.keys(contract.paths).find(route=>new RegExp('^'+route.replace(/\{[^}]+\}/g,'[^/]+')+'$').test(url));
 assert(route,'Undocumented path '+url);
 const operation=contract.paths[route][item.method.toLowerCase()];assert(operation,'Undocumented method');
 const response=deref(operation.responses[String(item.actual_status)]);
 assert(response,'Undocumented status '+item.actual_status+' '+item.method+' '+route);
 if(item.actual_status===204){assert(item.body_bytes===0);result.push({case:item.case,passed:true,body:'absent'});continue;}
 const type=item.headers['Content-Type']?.split(';')[0];const schema=response.content?.[type]?.schema;
 assert(schema,'Undocumented media type '+type+' '+route);
 const validator=ajv.compile({...schema,components:contract.components});
 const valid=validator(item.body);
 result.push({case:item.case,passed:valid,errors:validator.errors});
}
const report={executed_at_utc:new Date().toISOString(),schema:'openapi-observado.yaml',cases:result,passed:result.every(r=>r.passed),
 limitations:'Response-body sample validation only. Does not validate all headers, requests, security, business coverage or proposed envelope.'};
fs.writeFileSync(path.join(folder,'evidencias/esquemas-resultados.json'),JSON.stringify(report,null,2)+'\n');
assert(report.passed,JSON.stringify(result.filter(r=>!r.passed)));
console.log('Response samples validated: '+result.length);
