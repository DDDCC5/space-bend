package com.example.spacebend.platform.storage
import android.content.Context
import com.example.spacebend.data.repository.GameRepository
import com.example.spacebend.domain.model.*
import org.json.JSONObject
class AndroidGameRepository(context: Context): GameRepository {
 private val app=context.applicationContext
 private val prefs=app.getSharedPreferences("space-bend-v1",Context.MODE_PRIVATE)
 private val tables=mutableMapOf<GameLocale,Map<String,String>>()
 override fun settings()=GameSettings(
  locale=GameLocale.entries.firstOrNull{it.textFolder==prefs.getString("locale","zh-Hans")}?:GameLocale.SIMPLIFIED,
  music=prefs.getFloat("music",.45f),effects=prefs.getFloat("effects",.7f),voice=prefs.getFloat("voice",.8f),
  haptics=prefs.getBoolean("haptics",true),tutorialDone=prefs.getBoolean("tutorial",false))
 override fun saveSettings(settings: GameSettings) {
  prefs.edit().putString("locale",settings.locale.textFolder).putFloat("music",settings.music).putFloat("effects",settings.effects).putFloat("voice",settings.voice).putBoolean("haptics",settings.haptics).putBoolean("tutorial",settings.tutorialDone).apply()
 }
 override fun records(): Map<Int,Record> = (1..12).mapNotNull{n->
  val stars=prefs.getInt("stars.$n",0);if(stars==0)null else n to Record(stars,Double.fromBits(prefs.getLong("best.$n",Double.POSITIVE_INFINITY.toBits())))
 }.toMap()
 override fun saveRecord(id: Int,record: Record) {prefs.edit().putInt("stars.$id",record.stars).putLong("best.$id",record.bestSeconds.toBits()).apply()}
 override fun strings(locale: GameLocale): Map<String,String> = tables.getOrPut(locale) {
  val json=JSONObject(app.assets.open("locales/${locale.textFolder}.json").bufferedReader().use{it.readText()})
  json.keys().asSequence().associateWith{json.getString(it)}
 }
}
