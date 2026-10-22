import {readFileSync} from 'node:fs';
import {createRequire} from 'node:module';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const require=createRequire(path.join(root,'frontend','package.json'));
const {parse}=require('yaml');
const methods=['get','post','put','patch','delete'];
const cache=new Map();
function document(file){if(!cache.has(file))cache.set(file,parse(readFileSync(file,'utf8')));return cache.get(file);}
function resolve(node,file){
  if(!node?.$ref)return {node,file};
  const [relative,pointer]=node.$ref.split('#');const destination=path.resolve(path.dirname(file),relative||path.basename(file));
  const value=(pointer||'').split('/').slice(1).reduce((value,key)=>value[key.replaceAll('~1','/').replaceAll('~0','~')],document(destination));
  return resolve(value,destination);
}
function surface(file){
  const doc=document(file);const result=new Map();
  for(const [route,item] of Object.entries(doc.paths)){
    const resolved=resolve(item,file);const value=resolved.node;
    for(const method of methods)if(value[method]){
      const operation=value[method];const base=(operation.servers??value.servers??doc.servers)[0].url;
      const key=`${method.toUpperCase()} ${base}${route}`;
      assert(!result.has(key),`Duplicate operation ${key}`);
      result.set(key,{operation,file:resolved.file,route,method});
    }
  }
  return result;
}
export function inspectCompatibility(){
  const common=surface(path.join(root,'docs/technical/contracts/openapi.yaml'));
  const target=surface(path.join(root,'specs/005-complete-evaluation-workflows/contracts/openapi.yaml'));
  const shared=[];const incompatible=[];
  for(const [key,value] of target)if(common.has(key)){
    shared.push(key);
    if(JSON.stringify(common.get(key).operation)!==JSON.stringify(value.operation))incompatible.push(key);
  }
  return {commonOperations:common.size,targetOperations:target.size,sharedOperations:shared,incompatible};
}
if(process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href===import.meta.url){
  const result=inspectCompatibility();console.log(JSON.stringify(result,null,2));
  assert.equal(result.incompatible.length,0,'Conflicting schemas at the same method/version/path');
}
