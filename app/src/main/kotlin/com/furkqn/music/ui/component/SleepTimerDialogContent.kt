/**
 * Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.furkqn.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.furkqn.music.R
import com.furkqn.music.constants.SleepTimerDefaultKey
import com.furkqn.music.utils.safeDataStoreEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SleepTimerDialogContent(
    sleepTimerValue: Float,
    onSleepTimerValueChange: (Float) -> Unit,
    isAtDefault: Boolean,
    sleepTimerDefaultSetTemplate: String,
    coroutineScope: CoroutineScope,
    onEndOfSong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                pluralStringResource(
                    R.plurals.minute,
                    sleepTimerValue.roundToInt(),
                    sleepTimerValue.roundToInt(),
                ),
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(16.dp))

        Slider(
            value = sleepTimerValue,
            onValueChange = onSleepTimerValueChange,
            valueRange = 5f..120f,
            steps = (120 - 5) / 5 - 1,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val setDefaultClick = {
                coroutineScope.launch {
                    context.safeDataStoreEdit { settings ->
                        settings[SleepTimerDefaultKey] = sleepTimerValue
                    }
                }
                Toast.makeText(
                    context,
                    String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                    Toast.LENGTH_SHORT,
                ).show()
            }

            if (isAtDefault) {
                Button(
                    onClick = setDefaultClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.set_as_default),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = setDefaultClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.set_as_default),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            OutlinedButton(
                onClick = onEndOfSong,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.end_of_song),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
