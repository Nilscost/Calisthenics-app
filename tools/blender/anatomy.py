"""Optional continuous torso for the motion pilot, built from our own geometry."""
import math
import bpy
from mathutils import Vector

# (distance along the P->S->C spine, transverse radius, front/back radius).
SECTIONS=((-0.10,.12,.095),(0,.155,.10),(.09,.145,.095),(.20,.135,.09),
          (.29,.15,.10),(.39,.175,.112),(.45,.185,.12),(.48,.185,.11),
          (.51,.16,.095),(.57,.055,.055))
SIDES=32


def vertices(frame):
    p,s,c=(Vector(frame[n]) for n in ('pelvis','spine','chest'))
    u1,u2=(Vector(frame[n]).normalized() for n in ('pelvis_up','chest_up'))
    l1,l2=(Vector(frame[n]).normalized() for n in ('pelvis_left','chest_left'))
    result=[]
    for length,width,depth in SECTIONS:
        if length<=.24:centre=p+u1*length;up,left=u1,l1
        else:centre=s+u2*(length-.24);up,left=u2,l2
        forward=left.cross(up).normalized()
        for i in range(SIDES):
            t=2*math.pi*i/SIDES
            result.append(centre+left*(width*math.cos(t))+forward*(depth*math.sin(t)))
    return result


class Torso:
    def __init__(self, frame, material):
        verts=vertices(frame);faces=[]
        for ring in range(len(SECTIONS)-1):
            for i in range(SIDES):
                a=ring*SIDES+i;b=ring*SIDES+(i+1)%SIDES
                faces.append((a,b,b+SIDES,a+SIDES))
        faces.extend((tuple(reversed(range(SIDES))),tuple(range(len(verts)-SIDES,len(verts)))))
        mesh=bpy.data.meshes.new('continuous_torso');mesh.from_pydata(verts,[],faces);mesh.update()
        self.obj=bpy.data.objects.new('continuous_torso',mesh)
        bpy.context.scene.collection.objects.link(self.obj);mesh.materials.append(material)
        for poly in mesh.polygons:poly.use_smooth=True
        modifier=self.obj.modifiers.new('smooth_torso','SUBSURF');modifier.levels=2;modifier.render_levels=2

    def pose(self, frame):
        for v,co in zip(self.obj.data.vertices,vertices(frame)):v.co=co
        self.obj.data.update()
