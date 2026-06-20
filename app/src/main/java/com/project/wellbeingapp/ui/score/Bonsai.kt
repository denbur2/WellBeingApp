package com.project.wellbeingapp.ui.score

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lädt & koloriert die vorgerenderten ASCII-Bonsai-Stufen aus den Assets. */
object Bonsai {

    private const val DIR = "trees"

    /** Liest eine Wachstumsstufe (0..20) aus assets/trees/stage_XX.txt. */
    suspend fun loadStage(context: Context, stage: Int): String =
        readAsset(context, "stage_%02d.txt".format(stage))

    /** Liest das Error-Bild (negativer Score) aus assets/trees/error.txt. */
    suspend fun loadError(context: Context): String =
        readAsset(context, "error.txt")

    private suspend fun readAsset(context: Context, name: String): String =
        withContext(Dispatchers.IO) {
            context.assets.open("$DIR/$name").bufferedReader().use { it.readText() }
        }

    /**
     * Koloriert den ASCII-Baum: `&` = Blätter (grün), Stamm/Topf-Zeichen = Holz (braun),
     * alles andere unverändert.
     */
    fun colorize(ascii: String, leaf: Color, wood: Color): AnnotatedString =
        buildAnnotatedString {
            for (c in ascii) {
                val color = when (c) {
                    '&' -> leaf
                    '/', '\\', '|', '~', '_', '.', '-', ':', '(', ')' -> wood
                    else -> null
                }
                if (color != null) {
                    withStyle(SpanStyle(color = color)) { append(c) }
                } else {
                    append(c)
                }
            }
        }
}

/**
 * Lädt den passenden ASCII-Text (Wachstumsstufe oder Error) abhängig vom Score
 * asynchron aus den Assets und gibt ihn als State zurück (leer während des Ladens).
 */
@Composable
fun rememberBonsaiAscii(context: Context, displayScore: Int, stage: Int): String {
    val state = produceState(initialValue = "", key1 = displayScore < 0, key2 = stage) {
        value = if (displayScore < 0) Bonsai.loadError(context) else Bonsai.loadStage(context, stage)
    }
    return state.value
}
