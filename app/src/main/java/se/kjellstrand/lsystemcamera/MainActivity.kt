package se.kjellstrand.lsystemcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel

class MainActivity : ComponentActivity() {

    private val vm: LSystemViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LSystemTheme { MainScreen(vm) } }
    }
}
