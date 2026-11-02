import json,struct,sys
from pathlib import Path
bad=0;total=0
for p in Path('app/src/main/assets/models').glob('*.glb'):
 b=p.read_bytes();n=struct.unpack_from('<I',b,12)[0];g=json.loads(b[20:20+n]);data=b[28+n:];errors=0
 def values(aidx):
  a=g['accessors'][aidx];v=g['bufferViews'][a['bufferView']];off=v.get('byteOffset',0)+a.get('byteOffset',0);components={'VEC3':3,'SCALAR':1}[a['type']];count=a['count']*components;fmt={5126:'f',5125:'I',5123:'H'}[a['componentType']];return struct.unpack_from('<'+str(count)+fmt,data,off)
 for mesh in g['meshes']:
  for prim in mesh['primitives']:
   v=values(prim['attributes']['POSITION']);norm=values(prim['attributes']['NORMAL']);ind=values(prim['indices'])
   for j in range(0,len(ind),3):
    a,c,d=[ind[j+k]*3 for k in range(3)];u=[v[c+k]-v[a+k] for k in range(3)];w=[v[d+k]-v[a+k] for k in range(3)];cross=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]];nn=[norm[a+k]+norm[c+k]+norm[d+k] for k in range(3)]
    if sum(x*y for x,y in zip(cross,nn)) < -1e-13:errors+=1
    total+=1
 print(p.stem,'inverted',errors);bad+=errors
print('Total triangles',total,'inverted',bad)
sys.exit(1 if bad else 0)
