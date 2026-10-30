package com.duolink.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.duolink.app.data.webrtc.WebRtcManager
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun WebRtcRemoteVideoView(
    webRtcManager: WebRtcManager,
    remoteTrack: VideoTrack?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    init(webRtcManager.rootEglBase.eglBaseContext, null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                    remoteTrack?.addSink(this)
                }
            },
            update = { renderer ->
                remoteTrack?.addSink(renderer)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun WebRtcLocalPreviewView(
    webRtcManager: WebRtcManager,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).apply {
                    webRtcManager.attachLocalVideoRenderer(this)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setZOrderMediaOverlay(true)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

