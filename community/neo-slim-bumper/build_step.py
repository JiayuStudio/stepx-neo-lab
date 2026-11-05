"""STEPX Neo v0.1.4: analytic B-rep rebuild of the v0.1.3 bumper, millimeters.
Run with CadQuery 2.8.0. No mesh conversion or input STL is used for construction.
"""
from pathlib import Path
import math,json
from collections import Counter
import cadquery as cq
R=Path(__file__).resolve().parent
P={'width':74.5,'length':155.6,'height':10.42,'outer_corner_radius':10.7,'outer_edge_radius':0.4,'cavity_width':71.5,'cavity_length':152.6,'cavity_corner_radius':9.7,'back_seat_z':1.0,'front_seat_z':8.9,'front_lip_overlap':0.8,'back_rim_width':3.0,'button_tip_radius':3.5,'entry_bevel_height':.62,'entry_flare_per_side':.4}

def rr(w,h,r,plane=None):
 wire=cq.Workplane(plane or 'XY').rect(w,h).val()
 return wire.fillet2D(r,wire.Vertices())

def xywire(w,l,r,z):return rr(w,l,r,cq.Plane(origin=(0,0,z)))

def prism(w,l,r,z0,z1):return cq.Solid.extrudeLinear(xywire(w,l,r,z0),[],cq.Vector(0,0,z1-z0))

def loft(wires):return cq.Solid.makeLoft(wires,ruled=True)

def checkpoint(label,shape):
 assert shape.isValid(),label
 assert len(shape.Solids())==1,(label,len(shape.Solids()))
 print(label, 'faces',len(shape.Faces()),'volume',round(shape.Volume(),5),flush=True)

body=prism(P['width'],P['length'],P['outer_corner_radius'],0,P['height'])
rim_edges=[e for e in body.Edges() if e.BoundingBox().zlen<1e-7]
body=body.fillet(P['outer_edge_radius'],rim_edges)
checkpoint('outer rolled envelope',body)
# Continuous cavity, with a positive shoulder at 8.9 and entry ramp above 9.8.
main=prism(P['cavity_width'],P['cavity_length'],P['cavity_corner_radius'],P['back_seat_z'],P['front_seat_z'])
fw=P['cavity_width']-2*P['front_lip_overlap'];fl=P['cavity_length']-2*P['front_lip_overlap'];fr=P['cavity_corner_radius']-P['front_lip_overlap']
ez=P['height']-P['entry_bevel_height'];flare=P['entry_flare_per_side']
capture=prism(fw,fl,fr,P['front_seat_z'],ez)
entry=loft([xywire(fw,fl,fr,ez),xywire(fw+2*flare,fl+2*flare,fr+flare,P['height'])])
top=prism(fw+2*flare,fl+2*flare,fr+flare,P['height'],12)
back=prism(P['width']-2*P['back_rim_width'],P['length']-2*P['back_rim_width'],P['outer_corner_radius']-P['back_rim_width'],-1,P['back_seat_z']+.01)
void=main.fuse(capture,entry,top,back).clean()
body=body.cut(void).clean();checkpoint('capture and open back',body)
# Button opening follows the same outer-to-inner taper as the source mesh.
button_wires=[]
for width,x in [(69,-40.5),(69,-37.25),(67,-35.75),(67,-32.5)]:
 pl=cq.Plane(origin=(x,-18.3,9.1),xDir=(0,1,0),normal=(1,0,0))
 button_wires.append(rr(width,15,2,pl))
body=body.cut(loft(button_wires)).clean();checkpoint('button opening',body)
# Both short-end slots.
for y in [-77.3,77.3]:
 pl=cq.Plane(origin=(0,y+4,4.9),xDir=(1,0,0),normal=(0,-1,0))
 wire=rr(50,5.8,2.8,pl)
 body=body.cut(cq.Solid.extrudeLinear(wire,[],cq.Vector(0,-8,0))).clean()
# Opposite long-side speaker slots, at the same marked end.
for side in [-1,1]:
 wires=[]
 for width,height,rad,depth in [(10,4.6,2.1,-4),(10,4.6,2.1,-.75),(12,5.4,2.5,.75),(12,5.4,2.5,4)]:
  pl=cq.Plane(origin=(side*(36.5+depth),-61,5.2),xDir=(0,side,0),normal=(side,0,0))
  wires.append(rr(width,height,rad,pl))
 body=body.cut(loft(wires)).clean()
checkpoint('all ports',body)
# True quarter-circle tip profiles (not polygon arcs) across the tapered wall.
r=P['button_tip_radius'];cz=P['height']-r
for end in [-1,1]:
 wires=[]
 for half,x in [(34.5,-40.5),(34.5,-37.25),(33.5,-35.75),(33.5,-32.5)]:
  pl=cq.Plane(origin=(x,-18.3,0),xDir=(0,1,0),normal=(1,0,0))
  wp=(cq.Workplane(pl).moveTo(end*(half-3),cz).lineTo(end*(half-3),12)
      .lineTo(end*(half+r),12).lineTo(end*(half+r),P['height'])
      .threePointArc((end*(half+r-r/math.sqrt(2)),cz+r/math.sqrt(2)),(end*half,cz)).close())
  wires.append(wp.val())
 body=body.cut(loft(wires)).clean()
checkpoint('R3.5 button tips',body)
# Keep this smooth solid as the main CAD deliverable; STEP has no feature history.
step=R/'STEPX_Neo_Slim_Bumper_v0.1.4.step'
cq.exporters.export(body,str(step))
body.exportBrep(str(R/'STEPX_Neo_Slim_Bumper_v0.1.4.brep'))
cq.exporters.export(body,str(R/'STEPX_Neo_Slim_Bumper_v0.1.4.stl'),tolerance=.004,angularTolerance=.035)
reopened=cq.importers.importStep(str(step)).val()
checkpoint('STEP readback',reopened)
bb=reopened.BoundingBox()
report={'version':'0.1.4','basis':'analytic rebuild from v0.1.3 dimensions; not a faceted mesh STEP','parameters_mm':P,'step_valid':reopened.isValid(),'solids':len(reopened.Solids()),'shells':len(reopened.Shells()),'faces':len(reopened.Faces()),'surface_types':dict(Counter(f.geomType() for f in reopened.Faces())),'dimensions_w_l_h_mm':[bb.xlen,bb.ylen,bb.zlen],'volume_mm3':reopened.Volume(),'export_import_volume_difference_mm3':abs(body.Volume()-reopened.Volume()),'physical_fit':'not tested'}
assert report['solids']==1 and report['export_import_volume_difference_mm3']<1e-5
(R/'step-check.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
(R/'parameters.json').write_text(json.dumps(P,indent=2))
print(json.dumps(report,indent=2),flush=True)

# Optional CQ-editor viewport; ignored in the command-line build.
if "show_object" in globals():
 show_object(body,name="STEPX Neo slim bumper v0.1.4")
