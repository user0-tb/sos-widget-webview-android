package kz.gov.mia.sos.widget.webview

import android.Manifest
import android.webkit.PermissionRequest

object PermissionRequestMapper {

    fun fromAndroidToWebClient(permissions: MutableMap<String, Boolean>): List<String> {
        val resources = mutableListOf<String>()
        if (Manifest.permission.RECORD_AUDIO in permissions.keys
            || Manifest.permission.MODIFY_AUDIO_SETTINGS in permissions.keys
        ) {
            resources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        }
        if (Manifest.permission.CAMERA in permissions.keys) {
            resources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        }
        return resources
    }

    fun fromWebClientToAndroid(resources: Array<String>): List<String> {
        val permissions = mutableListOf<String>()
        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
            permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }
        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) {
            permissions.add(Manifest.permission.CAMERA)
        }
        return permissions
    }

}