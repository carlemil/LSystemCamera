package se.kjellstrand.lsystemcamera.viewmodel

import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.model.LSystem

class LSystemViewModel : ViewModel() {
    // val defaultSystemName = "Fudgeflake"

//    private val minWidthMod: LiveData<Float> = MutableLiveData(1F)
//    private val maxWidthMod: LiveData<Float> = MutableLiveData(1F)

    val iterations: MutableLiveData<Int> = MutableLiveData(5)
    val lSystem: MutableLiveData<LSystem> = MutableLiveData()

    fun getIterations() = iterations.value ?: 2

    fun setMaxIterations(system: LSystem, imageView: ImageView) {
        var iterations = 1
        var minWidth = 1F
        while (minWidth >= 1) {
            var (_minWidth, _) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                imageView.width,
                ++iterations,
                system
            )
            minWidth = _minWidth
        }
        this.iterations.value = iterations
    }
}
