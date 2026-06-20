package com.project.wellbeingapp.ui.score

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.wellbeingapp.score.ScoreData
import com.project.wellbeingapp.score.ScoreRules
import com.project.wellbeingapp.ui.theme.TerminalBrown
import com.project.wellbeingapp.ui.theme.TerminalDim
import com.project.wellbeingapp.ui.theme.TerminalError
import com.project.wellbeingapp.ui.theme.TerminalGreen

/**
 * Score-Screen im Terminal-Stil: Kennzahlen oben, darunter als Easteregg der
 * wachsende ASCII-Bonsai (bzw. ein Error-Bild bei negativem Score).
 */
@Composable
fun ScoreScreen(
    state: ScoreUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val data = state.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WELLBEING // SCORE",
            color = TerminalGreen,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))

        if (state.isLoading || data == null) {
            Text("> lade daten ...", color = TerminalDim)
            return@Column
        }

        ScoreStats(data)

        // Schiebt den Baum ans untere Bildschirmende.
        Spacer(Modifier.weight(1f))

        val stage = ScoreRules.treeStage(data.score)
        val ascii = rememberBonsaiAscii(context, data.score, stage)
        
        BonsaiArt(ascii = ascii, isError = data.score < 0)
        Spacer(Modifier.height(8.dp))

        if (data.score < 0) {
            Text(text = "[ STUFE -- / ERROR ]", color = TerminalError)
        } else {
            Text(
                text = "[ STUFE $stage / ${ScoreRules.MAX_TREE_STAGE} ]",
                color = TerminalDim
            )
        }
    }
}

@Composable
private fun ScoreStats(data: ScoreData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Stat(label = "SCORE", value = data.score.toString())
        Stat(label = "LEVEL", value = data.level.toString())
        Stat(label = "MINUTEN", value = data.screenTimeMinutes.toString())
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TerminalGreen, style = MaterialTheme.typography.titleLarge)
        Text(label, color = TerminalDim, style = MaterialTheme.typography.labelSmall)
    }
}


@Composable
private fun BonsaiArt(ascii: String, isError: Boolean) {
    if (ascii.isEmpty()) {
        Text("> rendere baum ...", color = TerminalDim)
        return
    }
    val text =
        if (isError) androidx.compose.ui.text.AnnotatedString(ascii)
        else Bonsai.colorize(ascii, leaf = TerminalGreen, wood = TerminalBrown)

    Text(
        text = text,
        color = if (isError) TerminalError else TerminalGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 11.sp,
        modifier = Modifier.horizontalScroll(rememberScrollState())
    )
}
