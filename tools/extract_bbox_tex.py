import struct, glob, os, json
from PIL import Image
PY_ROOT="/tmp/cb/assets/120302"
import subprocess
WT="/tmp/cb/WTExtractor/pywttools/wtextract.py"; PYBIN="/tmp/cb/wtvenv/bin/python"

def decode_wsgo(path):
    out="/tmp/cb/analysis/_bb.bin"
    subprocess.run([PYBIN,WT,path,out,"-q"],stderr=subprocess.DEVNULL)
    return open(out,"rb").read()

def find_bbox(d):
    vip=d.find(b"ViPm")
    if vip<0: return None
    # scan from after ViPm for 8 consecutive float32 forming min.xyz,0,max.xyz,0
    import math
    for o in range(vip+8, min(len(d)-32, vip+400)):
        try: vals=struct.unpack_from("<8f", d, o)
        except: continue
        mn=vals[0:3]; z1=vals[3]; mx=vals[4:7]; z2=vals[7]
        if abs(z1)>1e-3 or abs(z2)>1e-3: continue
        if not all(math.isfinite(v) for v in vals): continue
        if all(mn[i]<mx[i] for i in range(3)) and all(abs(v)<10000 for v in mn+mx) and any(mx[i]-mn[i]>0.01 for i in range(3)):
            return mn, mx, o
    return None

# map model dir -> its main .wsgo (actor if present, else first)
results={}
for mdir in sorted(glob.glob(PY_ROOT+"/MEDIA/OBJECTS/*")):
    name=os.path.basename(mdir)
    wsgos=glob.glob(mdir+"/resources/*.wsgo")
    if not wsgos: continue
    actor=[w for w in wsgos if "actor" in w.lower()]
    # for multi-part (cannon), union all part bboxes
    mn=[1e9]*3; mx=[-1e9]*3; got=False
    for w in wsgos:
        d=decode_wsgo(w)
        bb=find_bbox(d)
        if bb:
            got=True
            for i in range(3):
                mn[i]=min(mn[i],bb[0][i]); mx[i]=max(mx[i],bb[1][i])
    if not got: continue
    dims=[round(mx[i]-mn[i],3) for i in range(3)]
    # textures: decoded jpg/png in converted/models/<name>/textures
    texdir=f"/tmp/cb/converted/models/{name}/textures"
    texs=[]
    if os.path.isdir(texdir):
        for t in os.listdir(texdir):
            p=os.path.join(texdir,t)
            try:
                im=Image.open(p); alpha = im.mode in ("RGBA","LA") or (im.mode=="P" and "transparency" in im.info)
                texs.append((t, im.size, alpha))
            except: pass
    results[name]={"dims":dims,"min":[round(x,3) for x in mn],"max":[round(x,3) for x in mx],"textures":texs}

for n,r in results.items():
    print(f"{n:12s} dims(WxHxD)={r['dims']}  textures={[(t[0],t[2]) for t in r['textures']]}")
json.dump(results, open("/tmp/cb/converted/models/bbox_tex.json","w"), indent=1)
print("\nwrote bbox_tex.json")
