package se.kjellstrand.lsystemcamera.viewmodel

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
    private val _contrastMod: MutableLiveData<Float> = MutableLiveData(1F)
    private val _brightnessMod: MutableLiveData<Float> = MutableLiveData(0F)
    private val _iterations: MutableLiveData<Int> = MutableLiveData(2)

    fun setLSystem(lSystem: LSystem) {
        _iterations.value = 2
        _lSystem.value = lSystem
    }

    fun getLSystem(): LSystem? {
        return _lSystem.value
    }

    fun observeLSystem(owner: LifecycleOwner, observer: Observer<LSystem>) {
        _lSystem.observe(owner, observer)
    }

    fun getIterations() = _iterations.value ?: 2

    fun setIterations(iterations: Int) {
        if (_iterations.value != iterations) {
            _iterations.value = iterations
        }
    }

    fun observeIterations(owner: LifecycleOwner, observer: Observer<Int>) {
        _iterations.observe(owner, observer)
    }

    fun setMinWidth(min: Float) {
        if (_minWidth.value != min) {
            _minWidth.value = min
        }
    }

    fun getMinWidth(): Float {
        return _minWidth.value ?: 1f
    }

    fun setMaxWidth(max: Float) {
        if (_maxWidth.value != max) {
            _maxWidth.value = max
        }
    }

    fun getMaxWidth(): Float {
        return _maxWidth.value ?: 1f
    }

    fun setContrastMod(min: Float) {
        if (_contrastMod.value != min) {
            _contrastMod.value = min
        }
    }

    fun getContrastMod(): Float {
        return _contrastMod.value ?: 1f
    }

    fun setBrightnessMod(max: Float) {
        if (_brightnessMod.value != max) {
            _brightnessMod.value = max
        }
    }

    fun getBrightnessMod(): Float {
        return _brightnessMod.value ?: 1f
    }
}
