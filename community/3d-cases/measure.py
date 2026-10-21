"""Recompute case geometry from the adjacent, unchanged STL files."""
from pathlib import Path
import hashlib
import json
import numpy as np
import trimesh
from shapely.geometry import MultiPoint

root = Path(__file__).resolve().parent
for source in sorted((root / 'models').glob('*.stl')):
    mesh = trimesh.load_mesh(source)
    rectangle = np.array(MultiPoint(mesh.vertices[:, :2]).minimum_rotated_rectangle.exterior.coords)[:4]
    edges = np.roll(rectangle, -1, axis=0) - rectangle
    axis = edges[np.argmin(np.linalg.norm(edges, axis=1))]
    axis /= np.linalg.norm(axis)
    if axis[0] < 0:
        axis = -axis
    rotation = np.array([[axis[0], axis[1], 0], [-axis[1], axis[0], 0], [0, 0, 1]])
    mesh.vertices = mesh.vertices @ rotation.T
    mesh.vertices -= mesh.bounds[0]
    section = mesh.section(plane_normal=[0, 0, 1], plane_origin=[0, 0, 2.01])
    inner = min(section.discrete, key=lambda c: np.ptp(c[:, 0]) * np.ptp(c[:, 1]))
    hits, _, _ = mesh.ray.intersects_location([[36.75, 100, -1]], [[0, 0, 1]])
    side_hits, _, _ = mesh.ray.intersects_location([[-1, 77, 5]], [[1, 0, 0]])
    data = {
        'file': source.name,
        'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
        'unit_assumption': 'mm; STL does not encode units',
        'xy_rotation_to_remove_deg': float(np.degrees(np.arctan2(axis[1], axis[0]))),
        'outer_lwh': mesh.extents[[1, 0, 2]].tolist(),
        'inner_lw_at_z_2_01': np.ptp(inner, axis=0)[[1, 0]].tolist(),
        'back_thickness_at_x_36_75_y_100': float(np.ptp(hits[:, 2])),
        'side_thickness_at_y_77_z_5': float(np.ptp(side_hits[:, 0])),
        'volume': float(mesh.volume),
        'surface_area': float(mesh.area),
        'triangles': len(mesh.faces),
        'watertight': bool(mesh.is_watertight),
        'winding_consistent': bool(mesh.is_winding_consistent),
        'components': len(mesh.split()),
        'euler_number': int(mesh.euler_number),
        'contours_at_z_0_5': len(mesh.section(plane_normal=[0, 0, 1], plane_origin=[0, 0, 0.5]).discrete),
    }
    print(json.dumps(data, ensure_ascii=False, indent=2))
