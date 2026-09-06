package se.kjellstrand.lsystemcamera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.update
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import kotlin.math.roundToInt

private val systems = LSystem.systems.filter { it.name != "KochSnowFlake" }.sortedBy { it.name }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: LSystemViewModel, hasCamera: Boolean, onShare: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val frame by vm.frame.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onShare, enabled = frame != null) {
                        Icon(
                            painterResource(android.R.drawable.ic_menu_share),
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                }
            )
        }
    ) { inner ->
        // Width-capped and centered: on tablets and in landscape (Android 17 ignores the
        // portrait lock there) the square image would otherwise fill the whole width.
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasCamera) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    frame?.let { Image(it.asImageBitmap(), contentDescription = null, Modifier.fillMaxSize()) }
                }
            } else {
                CameraPermission()
            }
            SystemPicker(ui.system, vm::select)
            LabeledSlider(R.string.contrastSliderText, ui.contrast, 0f..1f) { v ->
                vm.ui.update { it.copy(contrast = v) }
            }
            LabeledSlider(R.string.brightnessSliderText, ui.brightness, -2f..2f) { v ->
                vm.ui.update { it.copy(brightness = v) }
            }
            val system = ui.system
            LabeledSlider(
                R.string.iterationsSliderText,
                ui.iterations.toFloat(),
                system.minIterations.toFloat()..system.maxIterations.toFloat(),
                steps = system.maxIterations - system.minIterations - 1
            ) { v ->
                vm.ui.update { it.copy(iterations = v.roundToInt()) }
            }
        }
    }
}

@Composable
private fun CameraPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Column(
        Modifier.fillMaxWidth().aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.camera_rationale), textAlign = TextAlign.Center)
        Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
            Text(stringResource(R.string.allow_camera))
        }
        // Shown after any denial, so "don't ask again" has a way out without an Activity reference.
        TextButton(onClick = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        }) {
            Text(stringResource(R.string.open_settings))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SystemPicker(selected: LSystem, onSelect: (LSystem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            leadingIcon = { SystemIcon(selected.name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            systems.forEach { system ->
                DropdownMenuItem(
                    text = { Text(system.name) },
                    leadingIcon = { SystemIcon(system.name) },
                    onClick = {
                        expanded = false
                        onSelect(system)
                    }
                )
            }
        }
    }
}

@Composable
private fun SystemIcon(name: String) {
    Image(painterResource(iconFor(name)), contentDescription = null, Modifier.size(32.dp))
}

@Composable
private fun LabeledSlider(
    label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit
) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

private fun iconFor(name: String) = when (name) {
    "Dragon" -> R.drawable.dragon
    "Fudgeflake" -> R.drawable.fudge_flake
    "Gosper" -> R.drawable.gosper
    "Hilbert" -> R.drawable.hilbert
    "Moore" -> R.drawable.moore
    "Peano" -> R.drawable.peano
    "SierpinskiCurve" -> R.drawable.sierpinski_curve
    "SierpinskiSquare" -> R.drawable.sierpinski_square
    "SierpinskiTriangle" -> R.drawable.sierpinski_triangle
    "TwinDragon" -> R.drawable.twin_dragon
    else -> R.drawable.unknown_system
}
