// Build a standalone academic subset without changing product contracts.
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
import crypto from 'node:crypto';
const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../../..');
const require=createRequire(path.join(root,'frontend/package.json'));
const {parse,stringify}=require('yaml');
const source=path.join(root,'docs/technical/contracts/openapi.yaml');
const cache=new Map(), registered=new Map(), components={};
function doc(file){if(!cache.has(file))cache.set(file,parse(fs.readFileSync(file,'utf8')));return cache.get(file);}
function locate(ref,file){
  const [relative,pointer='']=ref.split('#');
  const target=path.resolve(path.dirname(file),relative||path.basename(file));
  const value=pointer.split('/').slice(1).reduce((v,k)=>v?.[k.replaceAll('~1','/').replaceAll('~0','~')],doc(target));
  if(!value)throw Error('Unresolved '+ref);
  return {target,pointer,value};
}
function bundle(node,file=source){
  if(Array.isArray(node))return node.map(v=>bundle(v,file));
  if(!node||typeof node!=='object')return node;
  if(node.$ref){
    const {target,pointer,value}=locate(node.$ref,file);
    const match=pointer.match(/^\/components\/([^/]+)\/(.+)$/);
    if(!match)return bundle(value,target);
    const id=target+'#'+pointer;
    if(!registered.has(id)){
      const [,category,name]=match;
      const key=name+'_'+crypto.createHash('sha256').update(path.relative(root,target)).digest('hex').slice(0,6);
      registered.set(id,'#/components/'+category+'/'+key);
      components[category]??={};components[category][key]={};
      components[category][key]=bundle(value,target);
    }
    return {$ref:registered.get(id)};
  }
  return Object.fromEntries(Object.entries(node).map(([k,v])=>[k,bundle(v,file)]));
}
const selection={
  '/sessions':['post','delete'], '/academic-units':['post'], '/careers':['get','post'],
  '/careers/{careerId}':['get','patch'], '/careers/{careerId}/history':['get'],
  '/careers/{careerId}/state-decisions':['post'], '/model-versions/{versionId}':['get'],
  '/processes':['get','post'], '/processes/{processId}':['get']
};
const contract={openapi:'3.1.0',info:{title:'SIGEAC APE3 - subconjunto del contrato observado',version:'0.1.0',
  description:'Backend Spring Boot existente. Artefacto academico; no implementa Django ni el sobre universal. Esquemas derivados del contrato de producto y contrastados con codigo y muestra HTTP.'},servers:[{url:'http://127.0.0.1:18093/api/v1',description:'Backend temporal aislado; iniciarlo segun VERIFICACION.md'}],
  security:[{sessionCookie:[]}],paths:{},components};
for(const [route,methods] of Object.entries(selection)){
  let item=doc(source).paths[route];if(item.$ref){const ref=locate(item.$ref,source);item=bundle(ref.value,ref.target);}else item=bundle(item);
  contract.paths[route]={};if(item.parameters)contract.paths[route].parameters=item.parameters;
  for(const method of methods){
    if(!item[method])throw Error(method+' '+route+' absent');
    contract.paths[route][method]=item[method];
  }
}
// This real controller route is absent from the selected common paths.
const modelSchema=bundle({$ref:'#/components/schemas/ConfiguredModelVersionView'});
contract.paths['/model-versions/{versionId}/configured']={get:{operationId:'ape3GetConfiguredVersion',tags:['Models'],
  summary:'Consultar representacion configurada completa',description:'Ruta real de EvaluationModelController; no cambia la version base v1.',
  parameters:[{name:'versionId',in:'path',required:true,schema:{type:'string',format:'uuid'}}],responses:{
    '200':{description:'Version configurada',headers:{ETag:{schema:{type:'string'},description:'Revision fuerte'}},content:{'application/json':{schema:modelSchema}}},
    '401':bundle({$ref:'#/components/responses/Unauthorized'}),
    '403':bundle({$ref:'#/components/responses/Forbidden'}),
    '404':bundle({$ref:'#/components/responses/NotFound'})}}};
components.securitySchemes={sessionCookie:{type:'apiKey',in:'cookie',name:'SEC_SESSION',description:'Sesion opaca revocable; no token Bearer'}};
const outcomes={
 'post /sessions':[201,400,401,429], 'delete /sessions':[204,401,403],
 'post /academic-units':[201,400,401,403,409], 'get /careers':[200,400,401,403],
 'post /careers':[201,400,401,403,409,422], 'get /careers/{careerId}':[200,400,401,404],
 'patch /careers/{careerId}':[200,400,401,403,404,409,412,422,428],
 'get /careers/{careerId}/history':[200,400,401,403,404],
 'post /careers/{careerId}/state-decisions':[201,400,401,403,404,409,412,422,428],
 'get /model-versions/{versionId}':[200,400,401,403,404,409],
 'get /model-versions/{versionId}/configured':[200,400,401,403,404],
 'get /processes':[200,401], 'post /processes':[201,400,401,403,404,409,422],
 'get /processes/{processId}':[200,400,401,404]
};
const causes={400:'Entrada, DTO, UUID o ETag invalido segun operacion',401:'Sesion ausente, invalida o expirada',
 403:'Capacidad denegada o CSRF invalido en mutaciones',404:'Recurso inexistente o no visible',
 409:'Duplicado, estado incompatible o representacion legada no aplicable segun operacion',
 412:'Revision obsoleta',422:'Regla de unidad, motivo o vigencia no satisfecha segun operacion',
 428:'Falta If-Match requerido',429:'Limite de intentos de autenticacion'};
for(const [route,item] of Object.entries(contract.paths))for(const method of ['get','post','delete','patch']){
  const op=item[method];if(!op)continue;
  op.responses=Object.fromEntries(outcomes[method+' '+route].map(status=>[
    String(status),status<400?op.responses[String(status)]:{description:causes[status],
      content:{'application/problem+json':{schema:bundle({$ref:'#/components/schemas/Problem'})}}}
  ]));
  if(route.endsWith('/state-decisions'))delete op.responses['201'].headers?.Location;
  op.summary??=op.operationId;
  if(method!=='get'&&!(route==='/sessions'&&method==='post')){
    op.parameters??=[];
    if(!op.parameters.some(p=>{
      const value=p.$ref?p.$ref.split('/').slice(1).reduce((v,k)=>v[k],contract):p;
      return value.name==='X-CSRF-TOKEN'&&value.in==='header';
    }))op.parameters.push({name:'X-CSRF-TOKEN',in:'header',required:true,schema:{type:'string'},description:'Token CSRF de la sesion vigente'});
  }
}
contract.tags=[...new Set(Object.values(contract.paths).flatMap(item=>Object.values(item).flatMap(op=>op.tags??[])))].map(name=>({name,description:'Operaciones seleccionadas de '+name}));
contract['x-implementation']='existing-spring-boot';
const output=path.resolve(here,'../contratos');fs.mkdirSync(output,{recursive:true});
function prune(document){
  const keep=new Set();
  function visit(node){
    if(!node||typeof node!=='object')return;
    if(node.$ref?.startsWith('#/components/')){
      if(keep.has(node.$ref))return;
      keep.add(node.$ref);visit(node.$ref.split('/').slice(1).reduce((v,k)=>v[k],document));
    }
    for(const value of Object.values(node))visit(value);
  }
  visit(document.paths);
  for(const [category,entries] of Object.entries(document.components)){
    if(category==='securitySchemes')continue;
    for(const name of Object.keys(entries))if(!keep.has('#/components/'+category+'/'+name))delete entries[name];
  }
}
prune(contract);
fs.writeFileSync(path.join(output,'openapi-observado.yaml'),stringify(contract));

const objective=structuredClone(contract);
objective.info.title='SIGEAC APE3 - sobre universal PROPUESTO';
objective.info.description='NO IMPLEMENTADO. Propuesta de envoltura para revision, no sustituye el contrato observado ni publica una API v2.';
objective.servers=[{url:'/api-objetivo-no-implementada',description:'Identificador de contrato propuesto; no existe servidor en esta ruta'}];
objective['x-implementation']='not-implemented-proposal';
for(const item of Object.values(objective.paths))for(const method of ['get','post','delete','patch']){
 const op=item[method];if(!op)continue;
 for(const [code,res] of Object.entries(op.responses)){
  // Resolve response references before wrapping; schemas remain internal refs.
  let response=res;
  if(res.$ref)response=structuredClone(res.$ref.split('/').slice(1).reduce((v,k)=>v[k],objective));
  if(code==='204'||code==='304'){delete response.content;op.responses[code]=response;continue;}
  if(!response.content)continue;
  const old=Object.values(response.content)[0]?.schema??{};const success=Number(code)>=200&&Number(code)<300;
  response.content={'application/json':{schema:{type:'object',required:['success','message','data','errors','meta'],additionalProperties:false,properties:{
   success:{type:'boolean',const:success},message:{type:'string'},
   data:success?old:{type:'null'},
   errors:success?{type:'null'}:{type:'array',minItems:1,items:{type:'object',required:['code','message'],properties:{code:{type:'string'},message:{type:'string'},field:{type:['string','null']}}}},
   meta:{type:'object',required:['correlationId'],properties:{correlationId:{type:'string'}},additionalProperties:true}
  }}}};
  op.responses[code]=response;
 }
}
// Preserve existing page schema inside data to avoid inventing an implicit pagination migration.
objective['x-pagination-decision']='Pages remain data.items/page/size/totalElements; meta carries correlation. Subject to teacher/team review.';
prune(objective);
fs.writeFileSync(path.join(output,'openapi-objetivo.yaml'),stringify(objective));
console.log('Generated observed and proposed OpenAPI; operations='+Object.values(contract.paths).reduce((n,item)=>n+['get','post','delete','patch'].filter(m=>item[m]).length,0));
