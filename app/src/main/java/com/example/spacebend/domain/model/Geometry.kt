package com.example.spacebend.domain.model

import kotlin.math.*

data class V2(val x: Double, val y: Double) {
    operator fun plus(b: V2)=V2(x+b.x,y+b.y)
    operator fun minus(b: V2)=V2(x-b.x,y-b.y)
    operator fun times(s: Double)=V2(x*s,y*s)
    fun length()=hypot(x,y)
    fun dot(b: V2)=x*b.x+y*b.y
}
data class V3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(b: V3)=V3(x+b.x,y+b.y,z+b.z)
    operator fun minus(b: V3)=V3(x-b.x,y-b.y,z-b.z)
    operator fun unaryMinus()=this * -1.0
    operator fun times(s: Double)=V3(x*s,y*s,z*s)
    operator fun div(s: Double)=this*(1.0/s)
    fun dot(b: V3)=x*b.x+y*b.y+z*b.z
    fun cross(b: V3)=V3(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x)
    fun length()=sqrt(dot(this))
    fun normalized(): V3 { val l=length();return if(l<1e-9)ZERO else this/l }
    fun distance(b: V3)=(this-b).length()
    companion object { val ZERO=V3(0.0,0.0,0.0); val GRAVITY=V3(0.0,-9.8,0.0) }
}
data class Ray(val origin: V3,val direction: V3)
data class SurfaceHit(val surface: Surface,val point: V3,val distance: Double)
data class Surface(val id: String,val origin: V3,val u: V3,val v: V3,val boundary: List<V2>,val semantic: String="UNKNOWN",val holes: List<List<V2>> = emptyList()) {
    val normal: V3 get()=u.cross(v).normalized()
    fun contains(point: V2)=Polygon.contains(boundary,point)&&holes.none{Polygon.contains(it,point)}
    fun permits(outline: List<V2>)=Polygon.encloses(boundary,outline)&&holes.none{Polygon.overlaps(it,outline)}
    fun project(p: V3): V2 {val d=p-origin;return V2(d.dot(u),d.dot(v))}
    fun world(p: V2)=origin+u*p.x+v*p.y
    fun signedDistance(p: V3)=(p-origin).dot(normal)
    fun hit(ray: Ray): SurfaceHit? {
        val denom=ray.direction.dot(normal);if(abs(denom)<1e-8)return null
        val t=(origin-ray.origin).dot(normal)/denom
        if(t<0.0||t>12.0)return null
        val p=ray.origin+ray.direction*t
        return if(contains(project(p))) SurfaceHit(this,p,t) else null
    }
}
object Polygon {
    fun area(p: List<V2>): Double = abs(p.indices.sumOf { i->val b=p[(i+1)%p.size];p[i].x*b.y-b.x*p[i].y })*.5
    fun contains(p: List<V2>,q: V2): Boolean {
        if(p.size<3)return false
        var inside=false
        for(i in p.indices){val a=p[i];val b=p[(i+1)%p.size]
            if(distanceToSegment(q,a,b)<1e-8)return true
            if((a.y>q.y)!=(b.y>q.y)&&q.x<(b.x-a.x)*(q.y-a.y)/(b.y-a.y)+a.x)inside=!inside
        };return inside
    }
    fun distanceToSegment(p: V2,a: V2,b: V2): Double {
        val d=b-a;val l=d.dot(d);val t=if(l<1e-15)0.0 else ((p-a).dot(d)/l).coerceIn(0.0,1.0)
        return (p-(a+d*t)).length()
    }
    fun fitsCircle(p: List<V2>,q: V2,r: Double)=contains(p,q)&&p.indices.all{distanceToSegment(q,p[it],p[(it+1)%p.size])+1e-8>=r}
    fun encloses(container: List<V2>,child: List<V2>): Boolean {
        if(child.any{!contains(container,it)})return false
        fun cross(a: V2,b: V2)=a.x*b.y-a.y*b.x
        for(i in child.indices){
            val p=child[i];val d=child[(i+1)%child.size]-p
            if(!contains(container,p+d*.5))return false
            for(j in container.indices){
                val q=container[j];val e=container[(j+1)%container.size]-q;val den=cross(d,e)
                if(abs(den)>1e-10){val t=cross(q-p,e)/den;val u=cross(q-p,d)/den;if(t>1e-8&&t<1-1e-8&&u>1e-8&&u<1-1e-8)return false}
            }
        };return true
    }
    fun overlaps(a: List<V2>,b: List<V2>): Boolean {
        if(a.any{contains(b,it)}||b.any{contains(a,it)})return true
        fun cross(a: V2,b: V2)=a.x*b.y-a.y*b.x
        for(i in a.indices)for(j in b.indices){
            val p=a[i];val r=a[(i+1)%a.size]-p;val q=b[j];val d=b[(j+1)%b.size]-q;val den=cross(r,d)
            if(abs(den)>1e-10){val t=cross(q-p,d)/den;val u=cross(q-p,r)/den;if(t in 0.0..1.0&&u in 0.0..1.0)return true}
        }
        return false
    }
    fun centroid(p: List<V2>): V2 {
        var twiceArea=0.0;var x=0.0;var y=0.0
        for(i in p.indices){val a=p[i];val b=p[(i+1)%p.size];val c=a.x*b.y-b.x*a.y;twiceArea+=c;x+=(a.x+b.x)*c;y+=(a.y+b.y)*c}
        return if(abs(twiceArea)<1e-9) V2(p.sumOf{it.x}/p.size,p.sumOf{it.y}/p.size) else V2(x/(3*twiceArea),y/(3*twiceArea))
    }
    fun isSimple(p: List<V2>): Boolean {
        if(p.size<3||area(p)<.0001)return false
        fun orient(a: V2,b: V2,c: V2)=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x)
        fun intersects(a: V2,b: V2,c: V2,d: V2): Boolean {
            val o1=orient(a,b,c);val o2=orient(a,b,d);val o3=orient(c,d,a);val o4=orient(c,d,b)
            if(o1*o2 < -1e-12 && o3*o4 < -1e-12)return true
            return (abs(o1)<1e-9&&distanceToSegment(c,a,b)<1e-8)||(abs(o2)<1e-9&&distanceToSegment(d,a,b)<1e-8)||(abs(o3)<1e-9&&distanceToSegment(a,c,d)<1e-8)||(abs(o4)<1e-9&&distanceToSegment(b,c,d)<1e-8)
        }
        for(i in p.indices)for(j in i+1 until p.size){if(j==i+1||(i==0&&j==p.lastIndex))continue
            if(intersects(p[i],p[(i+1)%p.size],p[j],p[(j+1)%p.size]))return false
        };return true
    }
}

data class Orientation(val x: Double=0.0,val y: Double=0.0,val z: Double=0.0,val w: Double=1.0) {
 fun rotate(v: V3): V3 {val a=V3(x,y,z);val t=a.cross(v)*2.0;return v+t*w+a.cross(t)}
}
