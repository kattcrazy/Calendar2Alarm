package kattcrazy.calendar2alarm

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun selectionButtonColors(selected: Boolean): ButtonColors =
    if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
