package se.kjellstrand.lsystemcamera.viewmodel

import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.model.LSystem

class LSystemViewModel : ViewModel() {

    private val _lSystem: MutableLiveData<LSystem> = MutableLiveData()
    private val _minWidth: MutableLiveData<Float> = MutableLiveData(1F)
    private val _maxWidth: MutableLiveData<Float> = MutableLiveData(1F)
    private val _minWidthMod: MutableLiveData<Float> = MutableLiveData(0F)
    private val _maxWidthMod: MutableLiveData<Float> = MutableLiveData(0F)
    private val _iterations: MutableLiveData<Int> = MutableLiveData(2)

    fun setLSystem(lSystem: LSystem) {
        _lSystem.value = lSystem
    }

    fun getLSystem(): LSystem? {
        return _lSystem.value
    }

    fun getIterations() = _iterations.value ?: 2

    fun setIterations(iterations: Int) {
        _iterations.value = iterations
    }

    fun calculateAndSetMaxIterations(system: LSystem, imageView: ImageView) {
        var iterations = 1
        var minWidth = 1F
        while (minWidth >= 1) {
            val (_minWidth, _) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                1f,
                ++iterations,
                system
            )
            minWidth = _minWidth * imageView.width
        }
        this._iterations.value = iterations
    }

    fun setMinWidth(min: Float) {
        _minWidth.value = min
    }

    fun getMinWidth(): Float {
        return _minWidth.value ?: 1f
    }

    fun setMaxWidth(max: Float) {
        _maxWidth.value = max
    }

    fun getMaxWidth(): Float {
        return _maxWidth.value ?: 1f
    }

    fun setMinWidthMod(min: Float) {
        _minWidthMod.value = min
    }

    fun getMinWidthMod(): Float {
        return _minWidthMod.value ?: 1f
    }

    fun setMaxWidthMod(max: Float) {
        _maxWidthMod.value = max
    }

    fun getMaxWidthMod(): Float {
        return _maxWidthMod.value ?: 1f
    }

    fun observeLSystem(owner: LifecycleOwner, observer: Observer<LSystem>) {
        _lSystem.observe(owner, observer)
    }
}
