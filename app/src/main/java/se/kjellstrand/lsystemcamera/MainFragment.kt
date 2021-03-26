package se.kjellstrand.lsystemcamera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.slider.Slider
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.model.LSTriple
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.view.CustomAdapter
import se.kjellstrand.lsystemcamera.view.RowItem
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import kotlin.math.pow

class MainFragment : Fragment() {

    companion object {
        private const val DEFAULT_SYSTEM = "Moore"
    }

    private val model: LSystemViewModel by activityViewModels()
    private val bannedSystemNames = listOf("KochSnowFlake")

    private lateinit var spinner: Spinner
    private lateinit var iterationsSlider: Slider
    private lateinit var contrastSlider: Slider
    private lateinit var brightnessSlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLSystemObserver()
        setupMaxIterationsObserver()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        spinner = view.findViewById(R.id.systemSelectorSpinner)
        iterationsSlider = view.findViewById(R.id.iterationsSlider)
        contrastSlider = view.findViewById(R.id.contrastSlider)
        brightnessSlider = view.findViewById(R.id.brightnessSlider)

        val systemsNames = LSystem.systems
            .map { system -> system.name }
            .sorted()
            .filter { name -> name !in bannedSystemNames }

        inflateSystemNameSpinner(systemsNames)
        inflateBrightnessAndContrastSliders()
        inflateIterationsSlider()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupLSystemObserver() {
        model.observeLSystem(this, {
            ImageAnalyzer.updateLSystem(model)
            updateLSystem()
        })
    }

    private fun updateLSystem() {
        model.getLSystem()?.let { system ->
            val (minWidth, maxWidth) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                model.getIterations(), system
            )
            model.setMinWidth(minWidth)
            model.setMaxWidth(maxWidth)

            iterationsSlider.value = 2f
            iterationsSlider.valueFrom = system.minIterations.toFloat()
            iterationsSlider.valueTo = system.maxIterations.toFloat()
            val iterations = system.maxIterations - 1
            model.setIterations(iterations)
            iterationsSlider.value = iterations.toFloat()
        }
    }

    private fun setupMaxIterationsObserver() {
        model.observeIterations(this, { maxIterations ->
            model.getLSystem()?.let { system ->
                val (minWidth, maxWidth) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                    maxIterations, system
                )
                model.setMinWidth(minWidth)
                model.setMaxWidth(maxWidth)
            }
        })
    }

    // Used when testing the min and max const values in the LSystem lib
    private fun getRecommendedMinAndMaxWidth(iteration: Int, def: LSystem): LSTriple {
        val maxWidth = (1 / 1.45.pow(iteration)) * 0.1
        val minWidth = maxWidth / 10.0
        return LSTriple(minWidth, maxWidth, 1.0)
    }

    private fun inflateSystemNameSpinner(systemsNames: List<String>) {
        spinner.adapter = activity?.let { activity ->
            CustomAdapter(
                activity,
                R.layout.single_line_text_item,
                R.id.lsName,
                systemsNames.map { name -> RowItem(name) }
            )
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                LSystem.getByName(systemsNames[position])
                    .let { system -> model.setLSystem(system) }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        val selectedIndex = systemsNames.indexOf(DEFAULT_SYSTEM)
        if (selectedIndex != -1) {
            spinner.setSelection(selectedIndex)
            model.setLSystem(LSystem.getByName(DEFAULT_SYSTEM))
        }
    }

    private fun inflateBrightnessAndContrastSliders() {
        contrastSlider.addOnChangeListener { slider, _, _ ->
            model.setContrastMod(slider.value.toDouble())
        }
        contrastSlider.setLabelFormatter { value -> value.toString() }

        brightnessSlider.addOnChangeListener { slider, _, _ ->
            model.setBrightnessMod(slider.value.toDouble())
        }
        brightnessSlider.setLabelFormatter { value -> value.toString() }
    }

    private fun inflateIterationsSlider() {
        iterationsSlider.addOnChangeListener { slider, _, _ ->
            model.setIterations(slider.value.toInt())
        }
        contrastSlider.setLabelFormatter { value -> value.toString() }
    }
}
