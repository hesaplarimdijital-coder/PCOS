package com.ayla.pcos.ekran

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R

/** Uygulama simgesinin farklı başlatıcı maskeleri, temalı simge, bildirim simgesi ve açılış ekranı. */
@Composable
internal fun SimgeOnizleme() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Başlatıcı maskeleri", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AdaptifSimge(CircleShape, 96.dp)
            AdaptifSimge(RoundedCornerShape(30), 96.dp)
            AdaptifSimge(RoundedCornerShape(12), 96.dp)
            AdaptifSimge(CircleShape, 48.dp)
        }
        Text("Temalı simge (Android 13+)", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TemaliSimge(96.dp, Color(0xFFE8DEF8), Color(0xFF4A4458))
            TemaliSimge(96.dp, Color(0xFF3B2F43), Color(0xFFE8DEF8))
        }
        Text("Bildirim simgesi", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.background(Color(0xFF1B1B1F), RoundedCornerShape(12.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_notification), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Icon(painterResource(R.drawable.ic_notification), contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
        }
        Text("Açılış ekranı", style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier.fillMaxWidth().height(260.dp).background(Color(0xFFB02E5A), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_splash), contentDescription = null, modifier = Modifier.size(200.dp))
        }
    }
}

@Composable
private fun AdaptifSimge(shape: Shape, size: Dp) {
    val layer = size * (108f / 72f)
    Box(modifier = Modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.requiredSize(layer))
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(layer))
    }
}

@Composable
private fun TemaliSimge(size: Dp, background: Color, tint: Color) {
    val layer = size * (108f / 72f)
    Box(modifier = Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.requiredSize(layer),
        )
    }
}
