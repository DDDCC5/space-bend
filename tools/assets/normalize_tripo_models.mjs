// Offline asset preparation. Original downloads remain intact under ignored generated/.
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {NodeIO,getBounds} from './tripo-runtime/node_modules/@gltf-transform/core/dist/index.js';
import {ALL_EXTENSIONS} from './tripo-runtime/node_modules/@gltf-transform/extensions/dist/index.js';
import sharp from './tripo-runtime/node_modules/sharp/dist/index.mjs';
import {tangents,unweld,weld,prune} from './tripo-runtime/node_modules/@gltf-transform/functions/dist/index.js';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const require=createRequire(path.join(root,'tools/assets/tripo-runtime/package.json'));
const {generateTangents}=require('mikktspace');
const plan=JSON.parse(await fs.readFile(path.join(root,'docs/assets/tripo-20261127/generation-plan.json'),'utf8'));
const requested=process.argv.slice(2);
if(!requested.length)throw Error('Pass explicit asset IDs');
const kotlin=await fs.readFile(path.join(root,'app/src/main/java/com/example/spacebend/domain/model/GameModels.kt'),'utf8');
const radii=Object.fromEntries([...kotlin.matchAll(/\w+\("([^"]+)","[^"]+",[\d.]+,[\d.]+,([\d.]+)\)/g)].map(m=>[m[1],Number(m[2])]));
const out=path.join(root,'assets/tripo/normalized');await fs.mkdir(out,{recursive:true});
const io=new NodeIO().registerExtensions(ALL_EXTENSIONS);
const reviews=JSON.parse(await fs.readFile(path.join(root,'docs/assets/tripo-20261127/visual-review.json'),'utf8'));
for(const id of requested){
 const spec=plan.assets.find(a=>a.id===id);if(!spec)throw Error('Unknown asset '+id);
 const receipt=JSON.parse(await fs.readFile(path.join(root,'assets/tripo/runs',id+'.stdout.json'),'utf8'));
 if(receipt.status!=='success')throw Error(id+': task incomplete');
 const source=path.join(root,receipt.model_file);const document=await io.read(source);const modelRoot=document.getRoot();
 if(modelRoot.listScenes().length!==1||modelRoot.listSkins().length||modelRoot.listAnimations().length)throw Error(id+': unexpected non-static model');
 const scene=modelRoot.listScenes()[0];const sourceBounds=getBounds(scene);
 const yawDegrees=reviews[id]?.yawDegrees??0;const theta=yawDegrees*Math.PI/180;
 const orientation=document.createNode('SpaceBend_Orientation_'+id).setRotation([0,Math.sin(theta/2),0,Math.cos(theta/2)]);
 for(const child of [...scene.listChildren()])orientation.addChild(child);
 scene.addChild(orientation);
 const original=getBounds(scene);
 const center=original.min.map((x,k)=>(x+original.max[k])/2);
 const halfDiagonal=Math.hypot(...original.max.map((x,k)=>(x-original.min[k])/2));
 if(!Number.isFinite(halfDiagonal)||halfDiagonal<1e-6||!radii[id])throw Error(id+': invalid bounds or collision radius');
 // Leave a small margin inside the unchanged, conservative gameplay sphere.
 const scale=radii[id]*.98/halfDiagonal;
 const owner=document.createNode('SpaceBend_Normalized_'+id).setScale([scale,scale,scale]).setTranslation(center.map(x=>-x*scale));
 for(const child of [...scene.listChildren()])owner.addChild(child);
 scene.addChild(owner);
 const textures=[];
 for(const texture of modelRoot.listTextures()){
  const image=texture.getImage();if(!image)throw Error(id+': missing embedded image');
  const input=Buffer.from(image);const before=await sharp(input).metadata();
  let pipeline=sharp(input).resize({width:1024,height:1024,fit:'inside',withoutEnlargement:true});
  const png=texture.getMimeType()==='image/png';const result=await (png?pipeline.png({compressionLevel:9}):pipeline.jpeg({quality:90})).toBuffer();
  texture.setImage(result).setMimeType(png?'image/png':'image/jpeg');
  const after=await sharp(result).metadata();textures.push({before:[before.width,before.height],after:[after.width,after.height],mime:texture.getMimeType()});
 }
 const bounds=getBounds(scene);let triangles=0;
 for(const mesh of modelRoot.listMeshes())for(const primitive of mesh.listPrimitives()){
  if(primitive.getMode()!==4)throw Error(id+': non-triangle primitive');
  triangles+=(primitive.getIndices()?.getCount()??primitive.getAttribute('POSITION').getCount())/3;
 }
 const normalizedRadius=Math.hypot(...bounds.max.map((x,k)=>(x-bounds.min[k])/2));
 if(normalizedRadius>radii[id]+1e-6)throw Error(id+': model exceeds collider');
 if(triangles>spec.faceCountTarget*1.1)throw Error(id+': unexpectedly over mesh budget');
 await document.transform(unweld(),tangents({generateTangents}),weld(),prune());
 let repairedDegenerateTangents=0;
 for(const mesh of modelRoot.listMeshes())for(const primitive of mesh.listPrimitives()){
  const tangent=primitive.getAttribute('TANGENT');const normal=primitive.getAttribute('NORMAL');
  if(!tangent||!normal)throw Error(id+': normal-mapped primitive lacks tangent or normal');
  const t=tangent.getArray();const n=normal.getArray();
  for(let i=0;i<t.length;i+=4){
   const length=Math.hypot(t[i],t[i+1],t[i+2]);
   if(!Number.isFinite(length)||length<1e-8){
    const j=i/4*3;const nx=n[j],ny=n[j+1],nz=n[j+2];
    // A zero-area UV corner has no defined UV derivative. Supply an orthogonal unit basis.
    const axis=Math.abs(nx)<.8?[1,0,0]:[0,1,0];
    const x=axis[1]*nz-axis[2]*ny,y=axis[2]*nx-axis[0]*nz,z=axis[0]*ny-axis[1]*nx;
    const l=Math.hypot(x,y,z);if(l<1e-8)throw Error(id+': invalid normal at degenerate UV corner');
    t[i]=x/l;t[i+1]=y/l;t[i+2]=z/l;t[i+3]=t[i+3]===-1?-1:1;repairedDegenerateTangents++;
   }
  }
  tangent.setArray(t);
 }
 const target=path.join(out,id+'.glb');await io.write(target,document);const bytes=await fs.readFile(target);
 const record={id,taskId:receipt.task_id,sourceModel:receipt.model_file,creditsConsumed:receipt.credits_consumed,
  sourceBytes:(await fs.stat(source)).size,normalizedBytes:bytes.length,triangles,materials:modelRoot.listMaterials().length,
  textures,tangentSpace:'MikkTSpace, generated offline',repairedDegenerateTangents,originalBounds:sourceBounds,orientedBounds:original,bounds,uniformScale:scale,collisionRadius:radii[id],normalizedRadius,
  sha256:crypto.createHash('sha256').update(bytes).digest('hex'),yawDegrees,
  orientation:reviews[id]?.sourceFrontAxis?`Y-up; source front ${reviews[id].sourceFrontAxis}; rotated ${yawDegrees} degrees about Y`:'Y-up; semantic front pending visual review',
  modelVersion:plan.model,preparedFile:path.relative(root,target)};
 await fs.writeFile(path.join(out,id+'.json'),JSON.stringify(record,null,2)+'\n');console.log(JSON.stringify({id,triangles,bytes:bytes.length,normalizedRadius}));
}
