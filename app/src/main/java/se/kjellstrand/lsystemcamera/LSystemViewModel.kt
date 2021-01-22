package se.kjellstrand.lsystemcamera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import se.kjellstrand.lsystem.model.LSystem

class LSystemViewModel : ViewModel() {
   // val defaultSystemName = "Fudgeflake"

//    private val minWidthMod: LiveData<Float> = MutableLiveData(1F)
//    private val maxWidthMod: LiveData<Float> = MutableLiveData(1F)

    val iterations: MutableLiveData<Int> = MutableLiveData(5)
    val lSystem : MutableLiveData<LSystem> = MutableLiveData()

    fun getIterations() = iterations.value ?: 2
}
