package com.saathi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.saathi.R

/** Existing app header, shared with the assistant rather than defining a second identity. */
@Composable
internal fun SaathiBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_saathi_mark), contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary), modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Text("Saathi", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}
