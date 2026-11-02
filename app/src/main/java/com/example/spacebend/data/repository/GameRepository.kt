package com.example.spacebend.data.repository
import com.example.spacebend.domain.model.*
interface GameRepository {
    fun settings(): GameSettings
    fun saveSettings(settings: GameSettings)
    fun records(): Map<Int,Record>
    fun saveRecord(id: Int,record: Record)
    fun strings(locale: GameLocale): Map<String,String>
}
