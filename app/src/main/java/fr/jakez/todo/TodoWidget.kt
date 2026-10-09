package fr.jakez.todo

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class TodoWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TAB = "fr.jakez.todo.TAB"
        const val ACTION_CLEAR = "fr.jakez.todo.CLEAR"
        const val EXTRA_TAB = "tab"
        const val EXTRA_OP = "op"
        const val EXTRA_ID = "id"
        const val OP_TOGGLE = "toggle"
        const val OP_LEFT = "left"
        const val OP_RIGHT = "right"
        const val OP_EDIT = "edit"
        const val EXTRA_FROM_WIDGET = "fromWidget"

        private val TAB_IDS = intArrayOf(R.id.tab0, R.id.tab1, R.id.tab2)

        /** Redessine tous les widgets posés sur l'écran d'accueil. */
        fun refresh(c: Context) {
            Store.purgeOld(c)
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, TodoWidget::class.java))
            if (ids.isEmpty()) return
            ids.forEach { mgr.updateAppWidget(it, build(c, it)) }
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.list)
        }

        private fun broadcast(c: Context, action: String, req: Int, extra: Int? = null): PendingIntent {
            val i = Intent(c, TodoWidget::class.java).setAction(action)
            if (extra != null) i.putExtra(EXTRA_TAB, extra)
            return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun build(c: Context, widgetId: Int): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget)
            val current = Store.tab(c)

            // Onglets : l'onglet actif est « désactivé » (style surligné, non cliquable).
            for (h in 0..2) {
                val n = Store.count(c, h)
                val label = if (n > 0) "${Store.HORIZONS[h]} $n" else Store.HORIZONS[h]
                v.setTextViewText(TAB_IDS[h], label)
                v.setBoolean(TAB_IDS[h], "setEnabled", h != current)
                v.setOnClickPendingIntent(TAB_IDS[h], broadcast(c, ACTION_TAB, 100 + h, extra = h))
            }

            // Bouton + : ouvre la fenêtre d'ajout, pré-réglée sur l'onglet affiché.
            val add = Intent(c, AddActivity::class.java)
                .putExtra(EXTRA_TAB, current)
                .putExtra(EXTRA_FROM_WIDGET, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            v.setOnClickPendingIntent(
                R.id.btn_add,
                PendingIntent.getActivity(c, 300, add, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            v.setOnClickPendingIntent(R.id.btn_clear, broadcast(c, ACTION_CLEAR, 400))

            // Liste
            val svc = Intent(c, TodoWidgetService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
            @Suppress("DEPRECATION")
            v.setRemoteAdapter(R.id.list, svc)
            v.setEmptyView(R.id.list, R.id.empty)
            // Taps sur une tâche → relais invisible (voir ActionActivity).
            val action = Intent(c, ActionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            v.setPendingIntentTemplate(
                R.id.list,
                PendingIntent.getActivity(c, 200, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            )
            return v
        }
    }

    override fun onUpdate(c: Context, mgr: AppWidgetManager, ids: IntArray) = refresh(c)

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        when (intent.action) {
            ACTION_TAB -> Store.setTab(c, intent.getIntExtra(EXTRA_TAB, 0))
            ACTION_CLEAR -> Store.clearDone(c, Store.tab(c))
            else -> return
        }
        refresh(c)
    }
}
