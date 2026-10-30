package com.duolink.app.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun CameraScannerView(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val reader = MultiFormatReader()
                    var isDetected = false

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isDetected) {
                            val qrResult = processImageProxy(reader, imageProxy)
                            if (qrResult != null) {
                                isDetected = true
                                onQrCodeScanned(qrResult)
                            }
                        }
                        imageProxy.close()
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Use case binding failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val scanBoxSize = 260.dp.toPx()
            val left = (canvasWidth - scanBoxSize) / 2
            val top = (canvasHeight - scanBoxSize) / 2

            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(scanBoxSize, scanBoxSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = DuoLinkRedPrimary,
                topLeft = Offset(left, top),
                size = Size(scanBoxSize, scanBoxSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private fun processImageProxy(reader: MultiFormatReader, image: ImageProxy): String? {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = image.width
    val height = image.height

    return try {
        val source = PlanarYUVLuminanceSource(
            bytes, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(bitmap)
        result.text
    } catch (e: Exception) {
        null
    } finally {
        reader.reset()
    }
}
