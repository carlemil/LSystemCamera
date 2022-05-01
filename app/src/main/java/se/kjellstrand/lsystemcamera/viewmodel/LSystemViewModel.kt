package se.kjellstrand.lsystemcamera.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import se.kjellstrand.lsystem.model.LSystem

class LSystemViewModel : ViewModel() {

    private val _lSystem: MutableLiveData<LSystem> = MutableLiveData()
    private val _minWidth: MutableLiveData<Double> = MutableLiveData(1.0)
    private val _maxWidth: MutableLiveData<Double> = MutableLiveData(1.0)
    private val _contrastMod: MutableLiveData<Double> = MutableLiveData(1.0)
    private val _brightnessMod: MutableLiveData<Double> = MutableLiveData(0.0)
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

    fun setMinWidth(min: Double) {
        if (_minWidth.value != min) {
            _minWidth.value = min
        }
    }

    fun getMinWidth(): Double {
        return _minWidth.value ?: 1.0
    }

    fun setMaxWidth(max: Double) {
        if (_maxWidth.value != max) {
            _maxWidth.value = max
        }
    }

    fun getMaxWidth(): Double {
        return _maxWidth.value ?: 1.0
    }

    fun setContrastMod(min: Double) {
        if (_contrastMod.value != min) {
            _contrastMod.value = min
        }
    }

    fun getContrastMod(): Double {
        return _contrastMod.value ?: 1.0
    }

    fun setBrightnessMod(max: Double) {
        if (_brightnessMod.value != max) {
            _brightnessMod.value = max
        }
    }

    fun getBrightnessMod(): Double {
        return _brightnessMod.value ?: 1.0
    }
}
