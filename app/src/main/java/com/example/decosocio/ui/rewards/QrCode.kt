package com.example.decosocio.ui.rewards

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun qrBitmap(content: String, sizePx: Int = 512): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height) { index ->
        if (matrix.get(index % width, index / width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Always black on white (also in dark mode) so shop scanners can read it. */
@Composable
fun QrCode(content: String, contentDescription: String, size: Dp = 240.dp, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { qrBitmap(content) }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        filterQuality = FilterQuality.None,
        modifier = modifier.background(Color.White).padding(8.dp).size(size),
    )
}
