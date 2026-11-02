package com.example.spacebend.platform.spatial
import com.example.spacebend.domain.model.*
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import kotlin.math.*
internal fun Vector3.game()=V3(x.toDouble(),y.toDouble(),z.toDouble())
internal fun V3.sdk()=Vector3(x.toFloat(),y.toFloat(),z.toFloat())
internal fun rotate(q: Quat,v: V3): V3 {
 val a=V3(q.x.toDouble(),q.y.toDouble(),q.z.toDouble());val t=a.cross(v)*2.0
 return v+t*q.w.toDouble()+a.cross(t)
}
internal fun facing(direction: V3): Quat {
 val from=V3(0.0,0.0,-1.0);val to=direction.normalized();val dot=from.dot(to)
 if(to.length()<1e-6)return Quat()
 if(dot < -.999999)return Quat(0f,1f,0f,0f)
 val cross=from.cross(to);val w=1+dot;val n=sqrt(cross.dot(cross)+w*w)
 return Quat((cross.x/n).toFloat(),(cross.y/n).toFloat(),(cross.z/n).toFloat(),(w/n).toFloat())
}
/** Quaternion from an orthonormal right-handed basis (local X, Y, Z as columns). */
internal fun basisRotation(u: V3,v: V3,n: V3): Quat {
 val trace=u.x+v.y+n.z
 val x: Double;val y: Double;val z: Double;val w: Double
 if(trace>0){val s=sqrt(trace+1.0)*2;w=.25*s;x=(v.z-n.y)/s;y=(n.x-u.z)/s;z=(u.y-v.x)/s}
 else if(u.x>v.y&&u.x>n.z){val s=sqrt(1+u.x-v.y-n.z)*2;w=(v.z-n.y)/s;x=.25*s;y=(v.x+u.y)/s;z=(n.x+u.z)/s}
 else if(v.y>n.z){val s=sqrt(1+v.y-u.x-n.z)*2;w=(n.x-u.z)/s;x=(v.x+u.y)/s;y=.25*s;z=(n.y+v.z)/s}
 else {val s=sqrt(1+n.z-u.x-v.y)*2;w=(u.y-v.x)/s;x=(n.x+u.z)/s;y=(n.y+v.z)/s;z=.25*s}
 return Quat(x.toFloat(),y.toFloat(),z.toFloat(),w.toFloat())
}
