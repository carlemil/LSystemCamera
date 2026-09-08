package se.kjellstrand.lsystemcamera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.update
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import kotlin.math.roundToInt

private val systems = LSystem.systems.filter { it.name != "KochSnowFlake" }.sortedBy { it.name }
private val params = listOf(R.string.contrastSliderText, R.string.brightnessSliderText, R.string.iterationsSliderText)

@Composable
fun MainScreen(vm: LSystemViewModel, hasCamera: Boolean, onShare: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val frame by vm.frame.collectAsState()
    Scaffold { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Surface(
                Modifier.fillMaxWidth().aspectRatio(1f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                if (hasCamera) {
                    frame?.let { Image(it.asImageBitmap(), contentDescription = null, Modifier.fillMaxSize()) }
                } else {
                    CameraPermission()
                }
            }
            val frozen by vm.frozen.collectAsState()
            // Two states: live shows the shutter, held shows the resume button in the same spot.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { vm.frontCamera.update { !it } }, enabled = hasCamera) {
                        Icon(painterResource(R.drawable.ic_switch_camera), contentDescription = stringResource(R.string.switch_camera))
                    }
                }
                if (frozen) {
                    Resume { vm.frozen.value = false }
                } else {
                    Shutter(enabled = frame != null) { vm.frozen.value = true }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onShare, enabled = frame != null) {
                        Icon(painterResource(android.R.drawable.ic_menu_share), contentDescription = stringResource(R.string.share))
                    }
                }
            }
            SystemStrip(ui.system, vm::select)

            // One slider at a time: pick the parameter, then adjust it.
            var param by rememberSaveable { mutableIntStateOf(0) }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                params.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = param == i,
                        onClick = { param = i },
                        shape = SegmentedButtonDefaults.itemShape(i, params.size)
                    ) { Text(stringResource(label)) }
                }
            }
            val system = ui.system
            when (param) {
                0 -> ValueSlider(ui.contrast, 0f..1f, "%.2f".format(ui.contrast)) { v ->
                    vm.ui.update { it.copy(contrast = v) }
                }
                1 -> ValueSlider(ui.brightness, -2f..2f, "%+.1f".format(ui.brightness)) { v ->
                    vm.ui.update { it.copy(brightness = v) }
                }
                else -> ValueSlider(
                    ui.iterations.toFloat(),
                    system.minIterations.toFloat()..system.maxIterations.toFloat(),
                    ui.iterations.toString(),
                    steps = system.maxIterations - system.minIterations - 1
                ) { v ->
                    vm.ui.update { it.copy(iterations = v.roundToInt()) }
                }
            }
        }
    }
}

/** Camera-style ring: holds the current frame so Share exports exactly what is on screen. */
@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val label = stringResource(R.string.capture)
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .border(3.dp, color, CircleShape)
            .padding(7.dp)
            .background(color, CircleShape)
            .semantics { contentDescription = label }
    )
}

/** Same footprint as the shutter, so the row does not shift between states. */
@Composable
private fun Resume(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .border(3.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(android.R.drawable.ic_media_play),
            contentDescription = stringResource(R.string.resume),
            Modifier.size(32.dp),
            tint = color
        )
    }
}

@Composable
private fun CameraPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Column(
        Modifier.fillMaxSize().padding(24.dp),
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

@Composable
private fun SystemStrip(selected: LSystem, onSelect: (LSystem) -> Unit) {
    val state = rememberLazyListState(systems.indexOfFirst { it.name == selected.name }.coerceAtLeast(0))
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(systems, key = { it.name }) { system ->
            val chosen = system.name == selected.name
            Column(
                Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (chosen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .clickable { onSelect(system) }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(painterResource(iconFor(system.name)), contentDescription = null, Modifier.size(40.dp))
                Text(
                    displayName(system.name),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ValueSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    steps: Int = 0,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slider(value = value, onValueChange = onChange, Modifier.weight(1f), valueRange = range, steps = steps)
        Text(
            valueText,
            Modifier.width(44.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End
        )
    }
}

/** "SierpinskiTriangle" -> "Sierpinski Triangle". */
private fun displayName(name: String) = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

/** Icons are rendered by CurvePreviewTest (app/src/test) and copied into res/drawable-nodpi. */
private fun iconFor(name: String) = when (name) {
    "Cross" -> R.drawable.cross
    "Dragon" -> R.drawable.dragon
    "FassFour" -> R.drawable.fass_four
    "FassThree" -> R.drawable.fass_three
    "Fudgeflake" -> R.drawable.fudge_flake
    "Gosper" -> R.drawable.gosper
    "Hilbert" -> R.drawable.hilbert
    "KrishnaAnklets" -> R.drawable.krishna_anklets
    "Moore" -> R.drawable.moore
    "Peano" -> R.drawable.peano
    "Pentaplexity" -> R.drawable.pentaplexity
    "QuadraticGosper" -> R.drawable.quadratic_gosper
    "SierpinskiCurve" -> R.drawable.sierpinski_curve
    "SierpinskiSquare" -> R.drawable.sierpinski_square
    "SierpinskiTriangle" -> R.drawable.sierpinski_triangle
    "Terdragon" -> R.drawable.terdragon
    "Tiles" -> R.drawable.tiles
    "TwinDragon" -> R.drawable.twin_dragon
    else -> R.drawable.unknown_system
}
