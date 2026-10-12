package com.ayla.pcos.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Düz metni sistem paylaşım menüsüyle gönderir; uygulama metni hiçbir yere kendisi iletmez. */
fun shareText(context: Context, subject: String, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, text)
    val chooser = Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}

/**
 * Bir PDF dosyasını (ör. doktor raporu) sistem paylaşım menüsüyle gönderir. Dosya uygulamanın kendi
 * FileProvider'ından (`<applicationId>.photos`, yalnızca `cacheDir/raporlar`) geçici okuma izniyle sunulur;
 * uygulama dosyayı hiçbir yere kendisi iletmez.
 */
fun sharePdf(context: Context, file: File, subject: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + PHOTO_AUTHORITY_SUFFIX, file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(chooser) }
}

/** Manifestteki FileProvider yetkisinin eki; PhotoStore ile aynı sağlayıcı. */
private const val PHOTO_AUTHORITY_SUFFIX = ".photos"
