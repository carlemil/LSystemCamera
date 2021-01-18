package se.kjellstrand.lsystemcamera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import se.kjellstrand.lsystem.model.LSystem

class LSystemViewModel : ViewModel() {
    private val defaultSystemName = "Fudgeflake"
    val lSystem: LSystem? = LSystem.getByName(defaultSystemName)
    val iterations: LiveData<Int> = MutableLiveData(5)
    val name: LiveData<String> = MutableLiveData(defaultSystemName)
    val minWidthMod: LiveData<Float> = MutableLiveData(1F)
    val maxWidthMod: LiveData<Float> = MutableLiveData(1F)

}
