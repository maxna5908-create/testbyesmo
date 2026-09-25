package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Minimal destinations for this splash-only sample. No authentication is simulated. */
@Composable
fun LaunchDestination(initiallyComplete: Boolean, onOnboardingComplete: () -> Unit) {
    var complete by rememberSaveable { mutableStateOf(initiallyComplete) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val titles = intArrayOf(R.string.intro_title_1, R.string.intro_title_2, R.string.intro_title_3)
    val descriptions = intArrayOf(R.string.intro_body_1, R.string.intro_body_2, R.string.intro_body_3)
    Box(Modifier.fillMaxSize().background(Color(0xFF292D32)).safeDrawingPadding().padding(28.dp)) {
        Text("byesmo", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Column(Modifier.align(Alignment.Center).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (complete) {
                Text(stringResource(R.string.welcome_title), color = Color.White,
                    style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.welcome_body), color = Color(0xFFD1D3D6),
                    style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("${page + 1} / 3", color = Color(0xFFFF2B3D))
                Text(stringResource(titles[page]), color = Color.White,
                    style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(descriptions[page]), color = Color(0xFFD1D3D6),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (!complete) {
            Button(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                contentPadding = PaddingValues(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2B3D), contentColor = Color.White),
                onClick = {
                    if (page < 2) page++ else {
                        onOnboardingComplete()
                        complete = true
                    }
                },
            ) { Text(stringResource(if (page < 2) R.string.intro_next else R.string.intro_start)) }
        }
    }
}
