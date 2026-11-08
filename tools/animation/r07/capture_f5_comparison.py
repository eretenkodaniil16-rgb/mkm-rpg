#!/usr/bin/env python3
"""Reproducible 60-FPS *real Minecraft* R07 A/B/C video frame extractor.

This program does not generate synthetic gameplay. It only reads user-supplied
60FPS recordings via ffprobe/ffmpeg, samples the same original 14-tick action
phase from each clip and produces a reviewer manifest. Pillow is optional.
Example:
  python tools/animation/r07/capture_f5_comparison.py \
    --c3 C3.mp4 --six SIX.mp4 --r07 R07.mp4 \
    --start-c3 12.2 --start-six 12.2 --start-r07 12.2 \
    --out build/reports/mnae-f5

Time origin: exactly the first game tick of the R combo stage-1 animation.
The game server owns impact at tick5; frame 16 of the 43-frame source occurs
at 0.25 s. A freeze/pause, mismatched camera or hotbar invalidates comparison.
"""
import argparse
import json
import math
import shutil
import subprocess
from pathlib import Path

FPS=60
ANIMATION_FRAMES=43
KEY_FRAMES=(1,5,9,13,16,22,29,36,43)
LABELS={1:"guard",5:"anticipation",9:"windup",13:"pre-impact",
        16:"impact",22:"follow-through",29:"recovery",36:"settle",43:"return"}

def frame_offsets():
    return {f:(f-1)/FPS for f in KEY_FRAMES}

def frame_to_server_tick(frame):
    return (frame-1)*14/(ANIMATION_FRAMES-1)

def ffprobe(path):
    cmd=["ffprobe","-v","error","-select_streams","v:0",
         "-show_entries","stream=avg_frame_rate,nb_frames,width,height",
         "-of","json",str(path)]
    obj=json.loads(subprocess.check_output(cmd,text=True))
    if not obj.get("streams"):raise ValueError(f"No video stream: {path}")
    stream=obj["streams"][0]
    num,den=[int(x) for x in stream["avg_frame_rate"].split("/")]
    if den==0 or abs(num/den-60)>1:
        raise ValueError(f"Record Minecraft in F5 at 60 FPS; got {num}/{den} from {path}")
    return {"fps":num/den,"width":stream.get("width"),
            "height":stream.get("height"),"frames":stream.get("nb_frames")}

def ffmpeg_frame(path,at,output):
    proc=subprocess.run(["ffmpeg","-hide_banner","-nostdin","-v","error","-y",
                          "-ss",f"{at:.6f}","-i",str(path),
                          "-frames:v","1","-pix_fmt","rgb24",str(output)],
                         text=True,capture_output=True)
    if proc.returncode!=0 or not output.exists() or output.stat().st_size<100:
        raise RuntimeError(f"Frame extraction failed: {path} at {at}s: {proc.stderr}")

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("c3","six","r07"):
        p.add_argument("--"+name,type=Path,required=True)
        p.add_argument("--start-"+name,type=float,required=True)
    p.add_argument("--out",type=Path,default=Path("build/reports/mnae-f5"))
    args=p.parse_args()
    if not shutil.which("ffprobe") or not shutil.which("ffmpeg"):
        p.error("Install ffmpeg and ffprobe first; no images will be fabricated")
    start={name:getattr(args,"start_"+name) for name in ("c3","six","r07")}
    inputs={name:getattr(args,name) for name in start}
    for name,path in inputs.items():
        if not path.is_file() or not math.isfinite(start[name]) or start[name]<0:
            p.error(f"Missing recording or negative/nonfinite action onset for {name}")
    args.out.mkdir(parents=True,exist_ok=True)
    evidence={"source":"actual user-supplied Minecraft F5 MP4 only",
              "not_a_gameplay_quality_verdict":True,"server_ticks":14,
              "impact": {"frame":16,"server_tick":5,"seconds_after_start":.25},
              "modes":{},"frames":[]}
    for name,path in inputs.items():
        properties=ffprobe(path)
        evidence["modes"][name]={"source_file":str(path),
                                 "start_seconds":start[name],**properties}
        dest=args.out/name;dest.mkdir(exist_ok=True)
        for frame,delta in frame_offsets().items():
            out=dest/f"{frame:03}_{LABELS[frame]}.png"
            ffmpeg_frame(path,start[name]+delta,out)
            evidence["frames"].append({"mode":name,"frame":frame,
                "server_tick":frame_to_server_tick(frame),
                "time_seconds":start[name]+delta,"image":str(out)})
    (args.out/"evidence.json").write_text(json.dumps(evidence,indent=2)+"\n")
    try:
        from PIL import Image,ImageDraw
    except ImportError:
        print("Saved 27 real frames + metadata. Install Pillow for optional contact sheet.")
    else:
        thumbnails=[]
        for mode in ("c3","six","r07"):
            row=[]
            for frame in KEY_FRAMES:
                image=next((args.out/mode).glob(f"{frame:03}_*.png"))
                with Image.open(image) as src:
                    src.thumbnail((250,250))
                    pane=Image.new("RGB",(270,290),(27,34,45))
                    pane.paste(src,((270-src.width)//2,20))
                    ImageDraw.Draw(pane).text((10,268),
                        f"{mode} | {LABELS[frame]} | tick {frame_to_server_tick(frame):.2f}",
                        fill=(255,255,255))
                    row.append(pane)
            thumbnails.append(row)
        result=Image.new("RGB",(270*len(KEY_FRAMES),290*3))
        for i,row in enumerate(thumbnails):
            for j,im in enumerate(row):result.paste(im,(270*j,290*i))
        result.save(args.out/"three_mode_real_gameplay_contact_sheet.png")
    print(f"Produced 27 actual F5 frames + manifest in {args.out}")

if __name__=="__main__":
    main()
