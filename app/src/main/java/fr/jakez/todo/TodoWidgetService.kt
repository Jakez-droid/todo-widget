package fr.jakez.todo

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val c: Context) : RemoteViewsFactory {
        private var items: List<Store.Task> = emptyList()

        override fun onCreate() {}
        override fun onDestroy() {}
        override fun onDataSetChanged() { items = Store.forTab(c, Store.tab(c)) }
        override fun getCount() = items.size
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = items.getOrNull(position)?.id ?: position.toLong()
        override fun hasStableIds() = true

        override fun getViewAt(position: Int): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.item)
            val t = items.getOrNull(position) ?: return v

            if (t.done) {
                val s = SpannableString(t.text)
                s.setSpan(StrikethroughSpan(), 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                v.setTextViewText(R.id.text_done, s)
                v.setViewVisibility(R.id.text_done, View.VISIBLE)
                v.setViewVisibility(R.id.text, View.GONE)
                v.setImageViewResource(R.id.check, R.drawable.ic_checked)
            } else {
                v.setTextViewText(R.id.text, t.text)
                v.setViewVisibility(R.id.text, View.VISIBLE)
                v.setViewVisibility(R.id.text_done, View.GONE)
                v.setImageViewResource(R.id.check, R.drawable.ic_unchecked)
            }

            // Flèches de déplacement entre horizons (masquées pour les tâches faites).
            val showLeft = !t.done && t.horizon > Store.TODAY
            val showRight = !t.done && t.horizon < Store.LATER
            v.setViewVisibility(R.id.left, if (showLeft) View.VISIBLE else View.GONE)
            v.setViewVisibility(R.id.right, if (showRight) View.VISIBLE else View.GONE)

            v.setOnClickFillInIntent(R.id.check_area, fill(TodoWidget.OP_TOGGLE, t.id))
            v.setOnClickFillInIntent(R.id.left, fill(TodoWidget.OP_LEFT, t.id))
            v.setOnClickFillInIntent(R.id.right, fill(TodoWidget.OP_RIGHT, t.id))
            return v
        }

        private fun fill(op: String, id: Long) =
            Intent().putExtra(TodoWidget.EXTRA_OP, op).putExtra(TodoWidget.EXTRA_ID, id)
    }
}
