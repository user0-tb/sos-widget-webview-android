package kz.gov.mia.sos.widget.webview.multimedia.selection

object MimeType {
    const val IMAGE = "image/*"

    const val VIDEO = "video/*"

    const val AUDIO = "audio/*"

    val DOCUMENT = arrayOf(
        "text/*",
        "application/pdf",
        "application/excel",
        "application/vnd.ms-excel",
        "application/x-excel",
        "application/x-msexcel",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    )
}
