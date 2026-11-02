# -*- coding: utf-8 -*-
import runpy,json,struct,math
from pathlib import Path
m=runpy.run_path(str(Path(__file__).with_name('build_models.py')));O=m['O'];Model=m['Model']
p=Model('wormhole-reference')
p.ring('gravitational_rim',(0,0,0),.25,.0026,3,'z',n=96)
p.ring('outer_field',(0,0,0),.261,.0008,3,'z',n=96)
p.cyl('suction_particle',(0,0,0),.0018,.003,3,'z',n=6)
# Warp both rim meshes into a non-circular traced contour.
for index in [0,1]:
 name,v,n,ind,mat=p.parts[index]
 for j in range(0,len(v),3):
  a=math.atan2(v[j+1],v[j]);f=1+.07*math.sin(3*a+.5)+.045*math.cos(5*a);v[j]*=f;v[j+1]*=f*1.12
p.export();path=O/'models/wormhole-reference.glb';raw=path.read_bytes();jl=struct.unpack_from('<I',raw,12)[0];g=json.loads(raw[20:20+jl]);bl=struct.unpack_from('<I',raw,20+jl)[0];binary=bytearray(raw[28+jl:28+jl+bl])
g['nodes']=[{'name':'PortalRoot','children':[1,2]+list(range(3,35))},{'name':'GravitationalRim','mesh':0},{'name':'OuterField','mesh':1}]
for j in range(32):g['nodes'].append({'name':f'SuctionParticle_{j:02}','mesh':2})
g['scenes'][0]['nodes']=[0]
def accessor(vals,typ):
 while len(binary)%4:binary.append(0)
 off=len(binary);binary.extend(struct.pack('<%sf'%len(vals),*vals));g['bufferViews'].append({'buffer':0,'byteOffset':off,'byteLength':len(vals)*4});num=1 if typ=='SCALAR' else 3
 a={'bufferView':len(g['bufferViews'])-1,'componentType':5126,'count':len(vals)//num,'type':typ}
 if typ=='SCALAR':a.update({'min':[min(vals)],'max':[max(vals)]})
 g['accessors'].append(a);return len(g['accessors'])-1
def anim(name,times,channels):
 inputIndex=accessor(times,'SCALAR');a={'name':name,'samplers':[],'channels':[]}
 for node,prop,vals in channels:
  out=accessor(vals,'VEC3');i=len(a['samplers']);a['samplers'].append({'input':inputIndex,'output':out,'interpolation':'LINEAR'});a['channels'].append({'sampler':i,'target':{'node':node,'path':prop}})
 return a
g['animations']=[anim('Portal_Close',[0,.12,.35,.5],[(0,'scale',[.001]*3+[.8]*3+[1.03]*3+[1]*3)]),anim('Portal_Collapse',[0,.18,.55,.8],[(0,'scale',[1]*3+[1.03]*3+[.22]*3+[.001]*3)])]
times=[j*.08 for j in range(21)];channels=[]
for j in range(32):
 vals=[];scales=[]
 for t in times:
  u=t/1.6;a=j*math.tau/32+u*1.2;r=.36*(1-u);vals.extend([r*math.cos(a),r*math.sin(a)*1.12,.07*(1-u)]);s=math.sin(math.pi*u)*(.7+(j%3)*.2);scales.extend([s]*3)
 channels.extend([(j+3,'translation',vals),(j+3,'scale',scales)])
g['animations'].append(anim('Portal_Suction',times,channels));g['buffers'][0]['byteLength']=len(binary);g['extras'].update({'animationClips':['Portal_Close','Portal_Suction','Portal_Collapse'],'limitations':'Fixed reference contour. Runtime drawn contour and background refraction require SDK renderer work after approval.'})
j=json.dumps(g,separators=(',',':')).encode();j+=b' '*((-len(j))%4);binary+=b'\0'*((-len(binary))%4)
path.write_bytes(struct.pack('<III',0x46546c67,2,28+len(j)+len(binary))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(binary),0x004e4942)+binary)
path.rename(O/'vfx/wormhole-reference.glb');print('Portal GLB with 3 actual animation clips written')
