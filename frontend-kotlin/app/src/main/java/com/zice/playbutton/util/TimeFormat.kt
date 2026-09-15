package com.zice.playbutton.util

/** Duraciones en formato "m:ss", como en el reproductor de la app anterior. */
fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
