# -*- coding: utf-8 -*-
from pathlib import Path
import math,json,struct,base64
O=Path(__file__).resolve().parents[2]/'assets/style-a';(O/'models').mkdir(exist_ok=True)
MATS=[{'name':'Titanium','pbrMetallicRoughness':{'baseColorFactor':[.48,.56,.62,1],'metallicFactor':.85,'roughnessFactor':.29}}, {'name':'Graphite','pbrMetallicRoughness':{'baseColorFactor':[.045,.065,.08,1],'metallicFactor':.4,'roughnessFactor':.54}}, {'name':'Ceramic','pbrMetallicRoughness':{'baseColorFactor':[.79,.81,.8,1],'metallicFactor':.08,'roughnessFactor':.42}}, {'name':'IceEmission','pbrMetallicRoughness':{'baseColorFactor':[.22,.67,.85,1],'metallicFactor':.3,'roughnessFactor':.2},'emissiveFactor':[.2,.65,.9]}, {'name':'Amber','pbrMetallicRoughness':{'baseColorFactor':[.83,.45,.14,1],'metallicFactor':.3,'roughnessFactor':.35},'emissiveFactor':[.18,.075,.005]}]
class Model:
 def __init__(self,name):self.name=name;self.parts=[]
 def mesh(self,name,v,n,ind,mat):self.parts.append((name,v,n,ind,mat))
 def box(self,name,p,s,mat=0,ry=0):
  import itertools
  half=[q/2 for q in s];bevel=min(min(s)*.16,.006);v=[];n=[];idx=[];co,si=math.cos(ry),math.sin(ry)
  def face(points,normal):
   st=len(v)//3
   for x,y,z in points:v.extend((p[0]+co*x+si*z,p[1]+y,p[2]-si*x+co*z));n.extend((co*normal[0]+si*normal[2],normal[1],-si*normal[0]+co*normal[2]))
   for j in range(1,len(points)-1):idx.extend([st,st+j,st+j+1])
  for axis in range(3):
   other=[i for i in range(3) if i!=axis]
   for sign in [-1,1]:
    points=[]
    for a,b in [(-1,-1),(-1,1),(1,1),(1,-1)]:
     q=[0,0,0];q[axis]=sign*half[axis];q[other[0]]=a*(half[other[0]]-bevel);q[other[1]]=b*(half[other[1]]-bevel);points.append(q)
    nn=[0,0,0];nn[axis]=sign;face(points,nn)
  for axis in range(3):
   other=[i for i in range(3) if i!=axis]
   for a,b in itertools.product([-1,1],repeat=2):
    points=[]
    for along,which in [(-1,0),(1,0),(1,1),(-1,1)]:
     q=[0,0,0];q[axis]=along*(half[axis]-bevel);q[other[0]]=a*(half[other[0]]-(bevel if which else 0));q[other[1]]=b*(half[other[1]]-(0 if which else bevel));points.append(q)
    nn=[0,0,0];nn[other[0]]=a/2**.5;nn[other[1]]=b/2**.5;face(points,nn)
  for signs in itertools.product([-1,1],repeat=3):
   points=[]
   for axis in range(3):points.append([signs[i]*(half[i]-(0 if i==axis else bevel)) for i in range(3)])
   face(points,[x/3**.5 for x in signs])
  self.mesh(name,v,n,idx,mat)
 def cyl(self,name,p,r,h,mat=0,axis='y',r2=None,n=24):
  if r2 is None:r2=r
  def trans(X,Y,Z):
   return (X,Y,Z) if axis=='y' else ((Y,X,Z) if axis=='x' else (X,Z,Y))
  v=[];norm=[];ind=[]
  for j in range(n):
   a=j*math.tau/n;b=(j+1)*math.tau/n;st=len(v)//3
   for angle,Y,R in [(a,-h/2,r),(b,-h/2,r),(b,h/2,r2),(a,h/2,r2)]:
    co,si=math.cos(angle),math.sin(angle);pos=trans(R*co,Y,R*si);nv=trans(co,(r-r2)/h,si);l=math.sqrt(sum(x*x for x in nv));v.extend([p[k]+pos[k] for k in range(3)]);norm.extend([x/l for x in nv])
   ind.extend([st,st+1,st+2,st,st+2,st+3])
   for Y,R,ny in [(-h/2,r,-1),(h/2,r2,1)]:
    st=len(v)//3
    for X,Z in [(0,0),(R*math.cos(a),R*math.sin(a)),(R*math.cos(b),R*math.sin(b))]:
     pos=trans(X,Y,Z);v.extend([p[k]+pos[k] for k in range(3)]);norm.extend(trans(0,ny,0))
    ind.extend([st,st+2,st+1] if ny>0 else [st,st+1,st+2])
  self.mesh(name,v,norm,ind,mat)
 def ring(self,name,p,r,t,mat=0,axis='y',n=48,k=8):
  v=[];norm=[];ind=[]
  def tr(x,y,z):return (x,y,z) if axis=='y' else ((y,x,z) if axis=='x' else (x,z,y))
  for j in range(n+1):
   a=j*math.tau/n
   for l in range(k+1):
    b=l*math.tau/k;nx,ny,nz=math.cos(a)*math.cos(b),math.sin(b),math.sin(a)*math.cos(b);P=tr((r+t*math.cos(b))*math.cos(a),t*math.sin(b),(r+t*math.cos(b))*math.sin(a));N=tr(nx,ny,nz);v.extend([p[q]+P[q] for q in range(3)]);norm.extend(N)
  for j in range(n):
   for l in range(k):
    a=j*(k+1)+l;b=a+k+1;ind.extend([a,a+1,b,a+1,b+1,b])
  self.mesh(name,v,norm,ind,mat)
 def export(self):
  binary=bytearray();views=[];acc=[];meshes=[];nodes=[]
  def buf(data,component,typ,count,limits=None):
   while len(binary)%4:binary.append(0)
   offset=len(binary);binary.extend(data);views.append({'buffer':0,'byteOffset':offset,'byteLength':len(data)})
   a={'bufferView':len(views)-1,'componentType':component,'count':count,'type':typ}
   if limits:a.update(limits)
   acc.append(a);return len(acc)-1
  allv=[]
  for name,v,n,idx,mat in self.parts:
   # GLB faces are counterclockwise when viewed from their outward normal.
   # Correct each generated face rather than relying on double-sided rendering.
   for j in range(0,len(idx),3):
    a,b,c=[idx[j+k]*3 for k in range(3)];u=[v[b+k]-v[a+k] for k in range(3)];w=[v[c+k]-v[a+k] for k in range(3)]
    cross=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]];nn=[n[a+k]+n[b+k]+n[c+k] for k in range(3)]
    if sum(x*y for x,y in zip(cross,nn))<0:idx[j+1],idx[j+2]=idx[j+2],idx[j+1]
   allv+=v;limits={'min':[min(v[k::3]) for k in range(3)],'max':[max(v[k::3]) for k in range(3)]}
   p=buf(struct.pack('<%sf'%len(v),*v),5126,'VEC3',len(v)//3,limits);nn=buf(struct.pack('<%sf'%len(n),*n),5126,'VEC3',len(n)//3);ii=buf(struct.pack('<%sI'%len(idx),*idx),5125,'SCALAR',len(idx))
   meshes.append({'name':name,'primitives':[{'attributes':{'POSITION':p,'NORMAL':nn},'indices':ii,'material':mat}]});nodes.append({'name':name,'mesh':len(meshes)-1})
  gltf={'asset':{'version':'2.0','generator':'Space Bend original procedural aerospace asset authoring'},'scene':0,'scenes':[{'name':self.name,'nodes':list(range(len(nodes)))}],'nodes':nodes,'meshes':meshes,'materials':MATS,'buffers':[{'byteLength':len(binary)}],'bufferViews':views,'accessors':acc,'extras':{'coordinateSystem':'Y up, meters','forward':'-Z','status':'asset_review_pending','source':'Original local procedural geometry; not a Spatial Editor bundle'}}
  j=json.dumps(gltf,separators=(',',':')).encode();j+=b' '*((-len(j))%4);binary+=b'\0'*((-len(binary))%4)
  glb=struct.pack('<III',0x46546c67,2,12+8+len(j)+8+len(binary))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(binary),0x004e4942)+binary
  (O/f'models/{self.name}.glb').write_bytes(glb)
  return {'id':self.name,'file':f'models/{self.name}.glb','parts':len(nodes),'triangles':sum(len(x[3])//3 for x in self.parts),'bounds':{'min':[min(allv[k::3]) for k in range(3)],'max':[max(allv[k::3]) for k in range(3)]},'reviewData':{'gltf':gltf,'bin':base64.b64encode(binary).decode()}}
models=[]
# Compact tracked rover: distinct tread assemblies, optics, sensor mast and ceramic armour.
m=Model('exploration_robot');m.box('chassis',(0,.15,0),(.29,.14,.38),0);m.box('ceramic_top',(0,.235,-.015),(.25,.04,.29),2)
for x in [-.19,.19]:
 m.box('track_belt',(x,.105,0),(.09,.13,.42),1)
 for z in [-.15,-.075,0,.075,.15]:m.cyl('roadwheel',(x,.11,z),.048,.095,0,'x',n=16)
 for z in [i*.035-.19 for i in range(12)]:
  m.box('tread',(x,.177,z),(.105,.014,.018),0);m.box('tread',(x,.034,z),(.105,.014,.018),1)
m.box('camera_head',(0,.28,-.17),(.23,.085,.09),0)
for x in [-.065,.065]:m.cyl('optic',(x,.28,-.221),.027,.018,1,'z');m.cyl('optic_glass',(x,.28,-.233),.019,.009,3,'z')
m.cyl('mast',(.085,.325,.055),.009,.17,0);m.box('sensor',(.085,.41,.055),(.055,.035,.045),1)
for x in [-.11,.11]:m.box('caution',(x,.258,.045),(.013,.008,.1),4)
models.append(m.export())
# Containment core with three orthogonal magnetic rings and protected emitting cell.
m=Model('energy_core');m.cyl('containment_cell',(0,0,0),.055,.17,3,n=32)
for axis in ['x','y','z']:m.ring('magnetic_ring',(0,0,0),.115,.014,0,axis)
for y in [-.09,.09]:m.cyl('ceramic_end',(0,y,0),.071,.026,2,n=12);m.cyl('end_contact',(0,y*1.2,0),.04,.014,1,n=16)
for a in range(4):
 ang=a*math.pi/2;m.box('containment_rib',(.083*math.cos(ang),0,.083*math.sin(ang)),(.018,.15,.018),1,ang)
models.append(m.export())
# Short wing spacecraft built from tapered fuselage, delta-like panel assembly and dual thrusters.
m=Model('spacecraft');m.cyl('fuselage',(0,0,.02),.067,.3,0,'z',n=12);m.cyl('nose',(0,0,-.2),.004,.15,2,'z',r2=.067,n=12);m.box('canopy',(0,.055,-.065),(.065,.025,.1),1)
for sign in [-1,1]:
 m.box('wing_root',(sign*.10,-.025,.055),(.13,.018,.21),0,sign*-.30);m.box('wing_tip',(sign*.185,-.025,.09),(.10,.014,.13),1,sign*-.45)
 m.cyl('engine',(sign*.092,-.01,.195),.034,.13,1,'z',n=20);m.ring('engine_lip',(sign*.092,-.01,.263),.029,.007,0,'z');m.cyl('engine_emitter',(sign*.092,-.01,.264),.019,.008,3,'z')
 m.box('wing_warning',(sign*.18,-.014,.085),(.009,.005,.07),4)
m.box('tail_fin',(0,.07,.12),(.014,.11,.1),0)
models.append(m.export())
# Micro probe: four folding-looking articulated arms, spherical-ish octagonal instrument housing.
m=Model('micro_probe');m.cyl('instrument_body',(0,0,0),.045,.13,2,n=8);m.cyl('optic_housing',(0,0,-.066),.028,.035,1,'z',n=20);m.cyl('probe_eye',(0,0,-.087),.019,.008,3,'z',n=24)
for j in range(4):
 a=j*math.pi/2;px,pz=math.cos(a),math.sin(a)
 m.box('folding_arm',(px*.075,.015,pz*.075),(.11,.009,.012),0,-a);m.cyl('hinge',(px*.123,.015,pz*.123),.014,.014,1);m.box('sensor_leaf',(px*.14,.015,pz*.14),(.045,.006,.035),1,-a);m.box('sensor_light',(px*.14,.020,pz*.14),(.025,.003,.017),3,-a)
m.cyl('antenna',(0,.115,0),.003,.1,0);m.cyl('antenna_tip',(0,.167,0),.008,.014,4)
models.append(m.export())
# Heavy cargo capsule: thick ribs, reinforced rails, inset graphite access panel, handle and latches.
m=Model('heavy_capsule');m.box('pressure_hull',(0,0,0),(.36,.29,.44),0);m.box('top_panel',(0,.155,0),(.29,.02,.32),2)
for x in [-.185,.185]:
 for z in [-.19,.19]:m.box('corner_protection',(x,0,z),(.055,.34,.075),1)
for z in [-.17,-.085,0,.085,.17]:
 m.box('top_rib',(0,.174,z),(.39,.028,.026),0)
 for x in [-.2,.2]:m.box('side_rib',(x,0,z),(.022,.30,.026),0)
m.box('access_hatch',(0,0,-.229),(.25,.19,.018),1);m.box('hatch_center',(0,0,-.242),(.18,.13,.009),0)
for x in [-.12,.12]:m.box('locking_latch',(x,0,-.252),(.025,.075,.02),4)
m.box('handle_top',(0,.224,0),(.13,.02,.026),0)
for x in [-.065,.065]:m.box('handle_support',(x,.198,0),(.018,.05,.026),0)
models.append(m.export())
# Beacon with deployable base feet, telescopic antenna, directional emitter and split rings.
m=Model('beacon');m.cyl('base',(0,.025,0),.085,.05,1,n=12);m.cyl('base_cap',(0,.057,0),.066,.018,0,n=12)
for j in range(3):
 a=j*math.tau/3;m.box('deployed_foot',(.09*math.cos(a),.008,.09*math.sin(a)),(.13,.016,.035),0,-a)
for y,r,h in [(.11,.025,.1),(.195,.018,.075),(.26,.011,.06)]:m.cyl('telescopic_mast',(0,y,0),r,h,0,n=16)
m.cyl('beacon_emitter',(0,.29,0),.034,.055,3,n=20)
for y in [.26,.32]:m.ring('emitter_guard',(0,y,0),.045,.006,0)
m.cyl('antenna',(0,.36,0),.004,.08,0);m.cyl('warning_tip',(0,.405,0),.009,.009,4)
models.append(m.export())
review=[x.pop('reviewData') for x in models]
(O/'models/catalog.json').write_text(json.dumps({'status':'pending_visual_approval','source':'Original procedural geometry; official Editor workflow blocked by bundled Python TLS certificate failure.','units':'meters','upAxis':'Y','forward':'-Z','objects':models},indent=2))
(O/'models/review-data.json').write_text(json.dumps(review,separators=(',',':')))
print([(x['id'],x['parts'],x['triangles']) for x in models])
