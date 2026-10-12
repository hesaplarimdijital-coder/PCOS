package com.ayla.pcos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.ayla.pcos.MainActivity
import com.ayla.pcos.R
import com.ayla.pcos.reminders.NotificationIds
import com.ayla.pcos.reminders.ReminderActionReceiver
import com.ayla.pcos.reminders.ReminderScheduler
import java.time.format.DateTimeFormatter

/**
 * Widget görünümleri (RemoteViews; ADR-0007). İki düzen var: 2×2 (dar) ve 4×2 (geniş). Android 12+ sistem
 * uygun olanı boyuta göre seçer; daha eskide seçeneklerdeki en küçük genişliğe bakılır.
 */
object WidgetGorunumu {
    private val hourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Geniş düzenin en küçük genişliği (dp); 4×2 dikeyde ~276 dp, 2×2 yatayda ~269 dp. */
    private const val WIDE_MIN_WIDTH_DP = 250

    fun build(context: Context, state: WidgetState, options: Bundle?): RemoteViews =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RemoteViews(
                mapOf(
                    SizeF(110f, 100f) to views(context, state, wide = false),
                    SizeF(WIDE_MIN_WIDTH_DP.toFloat(), 100f) to views(context, state, wide = true),
                ),
            )
        } else {
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            views(context, state, wide = minWidth >= WIDE_MIN_WIDTH_DP)
        }

    fun views(context: Context, state: WidgetState, wide: Boolean): RemoteViews {
        val rv = RemoteViews(context.packageName, if (wide) R.layout.widget_ayla_genis else R.layout.widget_ayla)
        rv.setOnClickPendingIntent(R.id.widget_root, openApp(context, MainActivity.OPEN_TODAY))
        when (state) {
            WidgetState.Neutral -> {
                rv.setTextViewText(R.id.widget_ust_etiket, context.getString(R.string.widget_overline_neutral))
                rv.setTextViewText(R.id.widget_baslik, context.getString(R.string.widget_headline_neutral))
                rv.setTextViewText(R.id.widget_doz, context.getString(R.string.widget_neutral_text))
                rv.setViewVisibility(R.id.widget_doz_etiket, View.GONE)
                rv.setViewVisibility(R.id.widget_aldim, View.GONE)
                rv.setViewVisibility(R.id.widget_su, View.GONE)
            }
            is WidgetState.Ready -> bindReady(context, rv, state, wide)
        }
        return rv
    }

    private fun bindReady(context: Context, rv: RemoteViews, state: WidgetState.Ready, wide: Boolean) {
        val (overline, headline) = when (val h = state.headline) {
            is WidgetHeadline.CycleDay ->
                context.getString(R.string.widget_overline_cycle) to context.getString(R.string.widget_cycle_day, h.day)
            is WidgetHeadline.PillDay ->
                context.getString(if (h.placebo) R.string.widget_overline_pill_placebo else R.string.widget_overline_pill) to
                    context.getString(R.string.widget_pill_day, h.pill)
            WidgetHeadline.NoRecord ->
                context.getString(R.string.widget_overline_neutral) to context.getString(R.string.widget_headline_neutral)
        }
        rv.setTextViewText(R.id.widget_ust_etiket, overline)
        rv.setTextViewText(R.id.widget_baslik, headline)

        val dose = state.nextDose
        if (dose != null) {
            val time = dose.time.format(hourMinute)
            rv.setViewVisibility(R.id.widget_doz_etiket, if (wide) View.VISIBLE else View.GONE)
            rv.setTextViewText(
                R.id.widget_doz_etiket,
                context.getString(if (dose.overdue) R.string.widget_label_overdue else R.string.widget_label_next),
            )
            rv.setTextViewText(
                R.id.widget_doz,
                context.getString(if (dose.overdue) R.string.widget_dose_overdue else R.string.widget_dose_next, time, dose.name),
            )
            rv.setViewVisibility(R.id.widget_aldim, View.VISIBLE)
            rv.setContentDescription(R.id.widget_aldim, context.getString(R.string.widget_taken_a11y, time, dose.name))
            rv.setOnClickPendingIntent(R.id.widget_aldim, takenIntent(context, dose))
            rv.setOnClickPendingIntent(R.id.widget_doz, openApp(context, MainActivity.OPEN_MEDS))
        } else {
            rv.setViewVisibility(R.id.widget_doz_etiket, View.GONE)
            rv.setTextViewText(
                R.id.widget_doz,
                context.getString(if (state.hasDosesToday) R.string.widget_doses_done else R.string.widget_no_doses),
            )
            rv.setViewVisibility(R.id.widget_aldim, View.GONE)
        }

        if (wide) {
            rv.setViewVisibility(R.id.widget_su, View.VISIBLE)
            rv.setTextViewText(R.id.widget_su, context.getString(R.string.widget_water, state.waterCups, state.waterGoal))
        } else {
            rv.setViewVisibility(R.id.widget_su, View.GONE)
        }
    }

    /** Uygulamayı açar; hedef yalnızca beyaz listedeki sekmelerden biridir. */
    private fun openApp(context: Context, target: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, target)
            .setData("ayla://widget/open/$target".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * "Aldım": bildirimdeki eylemle aynı alıcı. Her doz için ayrı veri URI'si verilir ki iki dozun niyeti
     * aynı PendingIntent'e çökmesin; işaretleme idempotenttir, çift dokunuş stoğu iki kez düşürmez.
     */
    private fun takenIntent(context: Context, dose: NextDose): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_TAKEN)
            .setData("ayla://widget/dose/${dose.medicationId}/${dose.date}/${dose.time}".toUri())
            .putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, dose.medicationId)
            .putExtra(ReminderScheduler.EXTRA_DATE, dose.date.toString())
            .putExtra(ReminderScheduler.EXTRA_TIME, dose.time.toString())
            .putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, NotificationIds.dose(dose.medicationId, dose.time.toString()))
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
