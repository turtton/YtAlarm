package net.turtton.ytalarm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.turtton.ytalarm.DataRepository
import net.turtton.ytalarm.structure.Alarm

class AlarmViewModel(private val repository: DataRepository) : ViewModel() {
    val allAlarms: LiveData<List<Alarm>> by lazy { repository.allAlarms.asLiveData() }

    fun getFromId(id: Int): LiveData<Alarm> {
        return repository.getAlarmFromId(id).asLiveData()
    }

    fun insert(alarm: Alarm) = viewModelScope.launch {
        repository.insert(alarm)
    }

    fun update(alarm: Alarm) = viewModelScope.launch {
        repository.update(alarm)
    }

    fun delete(alarm: Alarm) = viewModelScope.launch {
        repository.delete(alarm)
    }
}

class AlarmViewModelFactory(private val repository: DataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository) as T
        } else {
            throw IllegalStateException("Unknown ViewModel class")
        }
    }
}