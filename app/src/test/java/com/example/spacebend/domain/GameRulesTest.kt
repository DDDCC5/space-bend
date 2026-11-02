package com.example.spacebend.domain
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test

class GameRulesTest {
 @Test fun twelveMissionsHaveFourTiersAndMonotonicUnlocks() {
  assertEquals(12,Missions.all.size);assertEquals(setOf(1,2,3,4),Missions.all.map{it.tier}.toSet())
  assertTrue(Missions.unlocked(1,emptyMap()));assertFalse(Missions.unlocked(2,emptyMap()))
  assertTrue(Missions.unlocked(2,mapOf(1 to Record(1,45.0))))
 }
 @Test fun gradeRequiresOptimalCountAndNoCollisionForThreeStars() {
  val m=Missions.all.first()
  assertEquals(3,Scoring.grade(m,true,m.optimalPortals,m.optimalSeconds,0))
  assertEquals(1,Scoring.grade(m,true,m.maxPortals,m.limitSeconds+1,1))
  assertEquals(0,Scoring.grade(m,false,m.optimalPortals,1.0,0))
 }
 @Test fun chineseNarrationIsSharedButTextLocalesStayDistinct() {
  assertEquals("zh-Hans",GameLocale.TRADITIONAL.voiceFolder)
  assertEquals("zh-Hant",GameLocale.TRADITIONAL.textFolder)
  assertEquals("zh-Hans",GameLocale.SIMPLIFIED.voiceFolder)
  assertEquals("en",GameLocale.ENGLISH.voiceFolder)
 }
 @Test fun bestTimeAndStarsArePreservedIndependently() {
  val r=Record(3,20.0).merge(1,12.0);assertEquals(3,r.stars);assertEquals(12.0,r.bestSeconds,1e-6)
 }
}
