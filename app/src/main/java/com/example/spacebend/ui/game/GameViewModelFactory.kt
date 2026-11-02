package com.example.spacebend.ui.game
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.spacebend.domain.usecase.GameSessionUseCase
import com.example.spacebend.platform.storage.AndroidGameRepository
class GameViewModelFactory(private val context: Context): ViewModelProvider.Factory {
 @Suppress("UNCHECKED_CAST")
 override fun <T: ViewModel> create(modelClass: Class<T>): T {
  require(modelClass.isAssignableFrom(GameViewModel::class.java))
  return GameViewModel(GameSessionUseCase(AndroidGameRepository(context))) as T
 }
}
