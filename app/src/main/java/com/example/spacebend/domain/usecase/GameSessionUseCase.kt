package com.example.spacebend.domain.usecase
import com.example.spacebend.data.repository.GameRepository
import com.example.spacebend.domain.model.*
class GameSessionUseCase(private val repository: GameRepository) {
 fun settings()=repository.settings()
 fun records()=repository.records()
 fun strings(locale: GameLocale)=repository.strings(locale)
 fun saveSettings(settings: GameSettings)=repository.saveSettings(settings)
 fun record(id: Int,stars: Int,seconds: Double): Map<Int,Record> {
  val old=repository.records()[id]
  repository.saveRecord(id,old?.merge(stars,seconds)?:Record(stars,seconds))
  return repository.records()
 }
}
