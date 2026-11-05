from pathlib import Path
import json,numpy as np,trimesh,cadquery as cq
from OCP.IntCurvesFace import IntCurvesFace_ShapeIntersector
from OCP.gp import gp_Lin,gp_Pnt,gp_Dir
R=Path(__file__).resolve().parent
shape=cq.importers.importStep(str(R/'STEPX_Neo_Slim_Bumper_v0.1.4.step')).val()
# Derive the print mesh from the actual imported STEP, not from a separate model.
cq.exporters.export(shape,str(R/'STEPX_Neo_Slim_Bumper_v0.1.4.stl'),tolerance=.004,angularTolerance=.035)
m=trimesh.load(R/'STEPX_Neo_Slim_Bumper_v0.1.4.stl');old=trimesh.load(R/'source/reference-v0.1.3.stl')
assert m.is_watertight and m.is_winding_consistent and len(m.split())==1
m.export(R/'STEPX_Neo_Slim_Bumper_v0.1.4.3mf')
inter=IntCurvesFace_ShapeIntersector();inter.Load(shape.wrapped,1e-7)
inter.Perform(gp_Lin(gp_Pnt(35.35,40,0),gp_Dir(0,0,1)),-1,12)
zs=sorted(set(round(inter.Pnt(i).Z(),7) for i in range(1,inter.NbPnt()+1)))
assert zs==[0.0,1.0,8.9,10.42],zs
probes={}
for name,size,center in [('left_speaker',(8,7,2.8),(-36.5,-61,5.2)),('right_speaker',(8,7,2.8),(36.5,-61,5.2)),('button_approach',(6,60,9.8),(-37,-18,7.1))]:
 box=cq.Workplane('XY').box(*size).val().translate(center)
 volume=shape.intersect(box).Volume();probes[name]=volume
 assert abs(volume)<1e-7,(name,volume)
print('STEP functional probes passed',flush=True)
np.random.seed(20261106)
# Sampling estimates geometric deviation; it is not a Hausdorff bound.
def distances(source,target):
 points,_=trimesh.sample.sample_surface(source,6000)
 d=[]
 for chunk in np.array_split(points,24):
  d.extend(trimesh.proximity.closest_point(target,chunk)[1].tolist())
 return {'sample_count':len(d),'maximum_sampled_mm':float(max(d)),'p99_sampled_mm':float(np.percentile(d,99)),'median_sampled_mm':float(np.median(d))}
a=distances(old,m);print('old to STEP',a,flush=True)
b=distances(m,old);print('STEP to old',b,flush=True)
report={'step_valid':shape.isValid(),'solids':len(shape.Solids()),'shells':len(shape.Shells()),'faces':len(shape.Faces()),'step_units':'millimeter','ray_z_intersections_mm':zs,'capture_gap_mm':zs[2]-zs[1],'passage_probe_volumes_mm3':probes,'old_to_step_mesh_sampled_distance':a,'step_mesh_to_old_sampled_distance':b,'reference_volume_mm3':float(old.volume),'step_volume_mm3':shape.Volume(),'volume_change_percent':100*(shape.Volume()-old.volume)/old.volume,'step_derived_stl_watertight':bool(m.is_watertight),'step_derived_stl_winding_consistent':bool(m.is_winding_consistent),'step_derived_stl_components':len(m.split()),'sampled_distance_is_strict_maximum':False,'physical_fit':'not tested'}
assert max(a['maximum_sampled_mm'],b['maximum_sampled_mm'])<.05,report
(R/'comparison-check.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2),flush=True)
