package app.zero.inlet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.zero.inlet.archive.ArchiveScreen
import app.zero.inlet.ui.theme.InletTest0Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InletTest0Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArchiveScreen()
                }
            }
        }
    }
}