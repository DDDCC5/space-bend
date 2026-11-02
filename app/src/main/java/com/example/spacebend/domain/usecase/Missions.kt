package com.example.spacebend.domain.usecase
import com.example.spacebend.domain.model.*
object Missions {
    val all: List<Mission> = (1..12).map { n ->
        val tier=(n-1)/3+1;val optimal=2+(n-1)/3;val pool=when(n){1->setOf(PayloadKind.CORE);2->setOf(PayloadKind.PROBE);3->setOf(PayloadKind.CORE,PayloadKind.PROBE);4->setOf(PayloadKind.ROBOT);5->setOf(PayloadKind.BEACON);6->setOf(PayloadKind.ROBOT,PayloadKind.BEACON);7->setOf(PayloadKind.CRAFT);8->setOf(PayloadKind.CORE);9->setOf(PayloadKind.CRAFT,PayloadKind.CORE);10->setOf(PayloadKind.CAPSULE);else->PayloadKind.entries.toSet()}
        Mission(n,tier,optimal+2,optimal+1,optimal,180.0+tier*60,90.0+tier*30,pool,"mission_$n")
    }
    fun unlocked(id: Int,records: Map<Int,Record>)=id==1||(records[id-1]?.stars?:0)>0
}
object Scoring {
    fun grade(m: Mission,success: Boolean,count: Int,seconds: Double,collisions: Int): Int = when {
        !success->0
        count==m.optimalPortals&&collisions==0&&seconds<=m.optimalSeconds->3
        count<=m.recommendedPortals&&seconds<=m.limitSeconds->2
        else->1
    }
}
