#!/usr/bin/env python3
"""CI static timing/pose QA for six C3 sword/unarmed combo strikes.

Produces a quantitative SVG clip-timeline plot, not a Minecraft screenshot.
Authoritative server damage still occurs exclusively in CombatService.
"""
import argparse
import json
import math
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/"src/main/resources/assets/mkm/animations"
WINDUP=[5,4,6]
RECOVERY=[8,7,11]
CLIPS=[("unarmed","unarmed/"+p) for p in ("jab","cross","uppercut")]
CLIPS += [("sword",f"sword/light_{i}") for i in (1,2,3)]

def slope(keys,index,axis_index):
    # Match C3.3 Java's shape-preserving harmonic interior tangents.
    if index==0 or index==len(keys)-1:return 0.0
    a,b,c=keys[index-1:index+2]
    h0=b["t"]-a["t"];h1=c["t"]-b["t"]
    d0=(b["r"][axis_index]-a["r"][axis_index])/h0
    d1=(c["r"][axis_index]-b["r"][axis_index])/h1
    if not math.isfinite(d0) or not math.isfinite(d1) or d0==0 or d1==0 or (d0>0)!=(d1>0):
        return 0.0
    w0=2*h1+h0;w1=h1+2*h0
    denom=w0/d0+w1/d1
    return (w0+w1)/denom if math.isfinite(denom) and denom else 0.0

def axis(keys,t,k):
    if t<=keys[0]["t"]:return keys[0]["r"][k]
    for n in range(1,len(keys)):
        a,b=keys[n-1],keys[n]
        if t<=b["t"]:
            h=b["t"]-a["t"]
            u=max(0.0,min(1.0,(t-a["t"])/h))
            p,q=a["r"][k],b["r"][k]
            m0=slope(keys,n-1,k);m1=slope(keys,n,k)
            u2=u*u;u3=u2*u
            value=(2*u3-3*u2+1)*p+(u3-2*u2+u)*h*m0+(-2*u3+3*u2)*q+(u3-u2)*h*m1
            return max(min(p,q),min(max(p,q),value))
    return keys[-1]["r"][k]

def check(files):
    assert len(files)==6
    report=[]
    poses={}
    for j,(profile,name,data) in enumerate(files):
        stage=j%3
        duration=WINDUP[stage]+1+RECOVERY[stage]
        hit=WINDUP[stage]/duration
        assert data["format"]==1 and data["duration_ticks"]==duration,(name,"duration")
        assert data["layer"]=="full_body", (name,"expected grounded attack layer")
        impacts=[m for m in data["markers"] if m["name"]=="impact"]
        assert len(impacts)==1 and abs(impacts[0]["t"]-hit)<=.000001,(name,"impact marker")
        assert "body" in data["tracks"] and "right_arm" in data["tracks"] and "left_arm" in data["tracks"]
        assert "left_leg" in data["tracks"] and "right_leg" in data["tracks"],name
        for part,keys in data["tracks"].items():
            assert len(keys)>=2 and keys[0]["t"]==0 and keys[-1]["t"]==1
            assert all(0<=k["t"]<=1 and len(k["r"])==3 and
                       all(math.isfinite(x) and abs(x)<=2*math.pi for x in k["r"]) for k in keys)
            assert all(keys[n]["t"]>keys[n-1]["t"] for n in range(1,len(keys)))
        pose=tuple(round(axis(data["tracks"][part],hit,k),3) for part in ("body","left_arm","right_arm") for k in range(3))
        poses[(profile,stage)]=pose
        legs=[axis(data["tracks"][part],hit,0) for part in ("left_leg","right_leg")]
        assert .30 < sum(abs(x) for x in legs) < 1.1,(name,"weight transfer leg angle")
        report.append({"clip":name,"duration_ticks":duration,"impact_tick":WINDUP[stage],
                       "leg_pitch_at_impact":[round(x,3) for x in legs],
                       "impact_normalized":round(hit,6),"body_yaw_at_impact":pose[1],
                       "left_arm_x_at_impact":pose[3],"right_arm_x_at_impact":pose[6]})
    for profile in ("unarmed","sword"):
        shapes=[poses[(profile,j)] for j in range(3)]
        assert all(max(abs(a-b) for a,b in zip(shapes[i],shapes[j]))>.25
                   for i in range(3) for j in range(i+1,3)),profile+" actions look too similar"
    assert abs(poses[("unarmed",0)][3])>abs(poses[("unarmed",0)][6]),"jab should lead with left"
    assert abs(poses[("unarmed",1)][6])>abs(poses[("unarmed",1)][3]),"cross should lead with right"
    return report

def plot(files,output):
    width,height=1260,720
    svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
         '<rect width="100%" height="100%" fill="#131b24"/>',
         '<text x="24" y="38" fill="#e8eef7" font-size="22" font-family="sans-serif">MKM C3.2 — attack motion / server impact synchronization</text>',
         '<text x="24" y="61" fill="#9daebe" font-size="13" font-family="sans-serif">Orange = left arm pitch · blue = right arm pitch · green = torso yaw. Dashed vertical = server hit tick.</text>']
    for j,(profile,name,data) in enumerate(files):
        y=95+j*101
        x0=220; span=980;baseline=y+45
        stage=j%3
        hit=WINDUP[stage]/data["duration_ticks"]
        svg.append(f'<text x="24" y="{y+38}" fill="#e9f0f7" font-family="monospace" font-size="17">{name}</text>')
        svg.append(f'<line x1="{x0}" y1="{baseline}" x2="{x0+span}" y2="{baseline}" stroke="#536274"/>')
        svg.append(f'<line x1="{x0+hit*span:.1f}" y1="{y+4}" x2="{x0+hit*span:.1f}" y2="{y+84}" stroke="#ffdb84" stroke-dasharray="5 5" stroke-width="2"/>')
        for part,component,color in [("left_arm",0,"#e6a16b"),("right_arm",0,"#78b4e9"),("body",1,"#75c7a2")]:
            keys=data["tracks"][part]
            points=[]
            for k in range(151):
                t=k/150
                value=axis(keys,t,component)
                points.append(f'{x0+t*span:.1f},{baseline+value*22:.1f}')
            svg.append(f'<polyline points="{" ".join(points)}" fill="none" stroke="{color}" stroke-width="2"/>')
        svg.append(f'<text x="{x0+span-12}" y="{y+88}" fill="#94a6b5" font-size="12" font-family="monospace" text-anchor="end">{data["duration_ticks"]} ticks | HIT {WINDUP[stage]}</text>')
    svg.append('</svg>')
    output.write_text("\n".join(svg)+"\n",encoding="utf-8")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--out",default="build/reports/animation-qa")
    opt=p.parse_args();out=Path(opt.out);out.mkdir(parents=True,exist_ok=True)
    files=[(profile,name,json.loads((ASSETS/(name+".json")).read_text(encoding="utf-8"))) for profile,name in CLIPS]
    report={"status":"PASS","scope":"static clip timing and pose differentiation; not actual Minecraft rendering",
            "clips_checked":len(files),"stages":check(files)}
    (out/"attack-report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    plot(files,out/"attack-timing.svg")
    print(json.dumps(report,indent=2))
if __name__=="__main__":main()
