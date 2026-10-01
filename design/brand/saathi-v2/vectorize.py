"""Rebuild the supplied generated mark as smooth, flat vector contours. No new visual source."""
from pathlib import Path
from collections import defaultdict
from PIL import Image
import math
import numpy as np
ROOT=Path(__file__).resolve().parents[3]
im=Image.open(ROOT/'design/brand/saathi-v1/saathi-symbol-green.png').convert('RGBA')
w,h=im.size
pixels=im.load()
solid={(x,y) for y in range(h) for x in range(w) if pixels[x,y][3]>=128}
edges={}
for x,y in solid:
 for neighbor,a,b in [((x,y-1),(x,y),(x+1,y)),((x+1,y),(x+1,y),(x+1,y+1)),((x,y+1),(x+1,y+1),(x,y+1)),((x-1,y),(x,y+1),(x,y))]:
  if neighbor not in solid: edges[a]=b
loops=[]
while edges:
 start=next(iter(edges));p=start;loop=[]
 while p in edges:
  loop.append(p);p=edges.pop(p)
  if p==start: break
 if len(loop)>100: loops.append(loop)
def area(poly): return abs(sum(x*b-y*a for (x,y),(a,b) in zip(poly,poly[1:]+poly[:1]))/2)
loops=sorted(loops,key=area,reverse=True)[:2]
def rdp(points,eps):
 if len(points)<3:return points
 a,b=points[0],points[-1];dx=b[0]-a[0];dy=b[1]-a[1];den=math.hypot(dx,dy)
 distances=[abs(dy*(x-a[0])-dx*(y-a[1]))/den if den else math.hypot(x-a[0],y-a[1]) for x,y in points]
 i=max(range(len(points)),key=lambda j:distances[j])
 return rdp(points[:i+1],eps)[:-1]+rdp(points[i:],eps) if distances[i]>eps else [a,b]
xs=[x for loop in loops for x,y in loop];ys=[y for loop in loops for x,y in loop]
scale=84/max(max(xs)-min(xs),max(ys)-min(ys));cx=(min(xs)+max(xs))/2;cy=(min(ys)+max(ys))/2
def unit(v):
 n=np.linalg.norm(v)
 return v/n if n else v

def fit(points,left,right,error=5.0):
 if len(points)==2:
  d=np.linalg.norm(points[-1]-points[0])/3
  return [(points[0],points[0]+left*d,points[-1]+right*d,points[-1])]
 distances=np.linalg.norm(np.diff(points,axis=0),axis=1)
 u=np.r_[0,np.cumsum(distances)];u/=u[-1]
 b0=(1-u)**3;b1=3*u*(1-u)**2;b2=3*u*u*(1-u);b3=u**3
 A=np.stack([b1[:,None]*left,b2[:,None]*right],axis=2).reshape(-1,2)
 residual=points-(b0+b1)[:,None]*points[0]-(b2+b3)[:,None]*points[-1]
 alpha=np.linalg.lstsq(A,residual.reshape(-1),rcond=None)[0]
 if min(alpha)<.001: alpha[:]=np.linalg.norm(points[-1]-points[0])/3
 control=(points[0],points[0]+left*alpha[0],points[-1]+right*alpha[1],points[-1])
 curve=sum(b[:,None]*p for b,p in zip([b0,b1,b2,b3],control))
 errors=np.sum((curve-points)**2,axis=1);split=int(np.argmax(errors))
 if errors[split]<=error*error:return [control]
 split=max(1,min(len(points)-2,split));t=unit(points[split-1]-points[split+1])
 return fit(points[:split+1],left,t,error)+fit(points[split:],-t,right,error)

paths=[]
for loop in loops:
 half_raw=len(loop)//2
 simplified=rdp(loop[:half_raw+1],2.0)[:-1]+rdp(loop[half_raw:]+[loop[0]],2.0)[:-1]
 p=np.array(simplified,dtype=float)
 # Split the closed contour into two curves with shared endpoint tangents.
 half=len(p)//2
 tangent=unit(p[3]-p[-3]);middle=unit(p[half+3]-p[half-3])
 curves=fit(p[:half+1],tangent,-middle)+fit(np.concatenate([p[half:],p[:1]]),middle,-tangent)
 def point(v): return ((v[0]-cx)*scale+50,(v[1]-cy)*scale+50)
 first=point(curves[0][0]);d=f'M{first[0]:.1f},{first[1]:.1f}'
 for curve in curves:
  a,b,c=[point(v) for v in curve[1:]]
  d+=f'C{a[0]:.1f},{a[1]:.1f} {b[0]:.1f},{b[1]:.1f} {c[0]:.1f},{c[1]:.1f}'
 paths.append(d+'Z')
 print('Contour',len(loop),'pixels ->',len(curves),'fitted cubic segments;',len(d),'characters')
out=Path(__file__).parent
for name,color in [('green','#087900'),('mint','#A4EE99'),('white','#FFFFFF')]:
 (out/f'saathi-symbol-{name}.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">'+''.join(f'<path fill="{color}" d="{d}"/>' for d in paths)+'</svg>\n')
res=ROOT/'app/src/main/res'
def vector(width,viewport,color,group=''):
 return f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{width}dp" android:height="{width}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">'+group+''.join(f'<path android:fillColor="{color}" android:pathData="{d}"/>' for d in paths)+('</group>' if group else '')+'</vector>\n'
(res/'drawable/ic_saathi_mark.xml').write_text(vector(40,100,'#087900'))
(res/'drawable/ic_saathi_launcher_foreground.xml').write_text(vector(108,108,'#A4EE99','<group android:scaleX="0.70" android:scaleY="0.70" android:translateX="19" android:translateY="19">'))
(res/'drawable/ic_saathi_splash.xml').write_text(vector(192,150,'#A4EE99','<group android:name="breathing" android:pivotX="75" android:pivotY="75"><group android:translateX="25" android:translateY="25">').replace('</group></vector>','</group></group></vector>'))
