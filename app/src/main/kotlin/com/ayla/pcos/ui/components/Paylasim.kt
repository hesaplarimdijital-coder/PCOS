package com.ayla.pcos.ui.components

import android.content.Context
import android.content.Intent

/** Düz metni sistem paylaşım menüsüyle gönderir; uygulama metni hiçbir yere kendisi iletmez. */
fun shareText(context: Context, subject: String, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, text)
    val chooser = Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}
