package se.kjellstrand.lsystemcamera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import se.kjellstrand.lsystem.model.LSystem

class LSystemViewModel : ViewModel() {
    private val defaultSystemName = "Fudgeflake"
    private val lSystem: LSystem = LSystem.getByName(defaultSystemName)
    private val iterations: LiveData<Int> = MutableLiveData(5)
    private val name: LiveData<String> = MutableLiveData(defaultSystemName)
    private val minWidthMod: LiveData<Float> = MutableLiveData(1F)
    private val maxWidthMod: LiveData<Float> = MutableLiveData(1F)

    fun getIterations() = iterations.value ?: 2
    fun getLSystem() = lSystem
}
