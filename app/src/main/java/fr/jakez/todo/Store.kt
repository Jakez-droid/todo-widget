package fr.jakez.todo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stockage local des tâches (sur le téléphone, dans les préférences de l'app). */
object Store {

    data class Task(val id: Long, val text: String, val horizon: Int, val done: Boolean)

    const val TODAY = 0
    const val WEEK = 1
    const val LATER = 2
    val HORIZONS = arrayOf("Aujourd'hui", "Semaine", "Plus tard")

    private fun prefs(c: Context) = c.getSharedPreferences("todo", Context.MODE_PRIVATE)

    fun load(c: Context): List<Task> {
        val raw = prefs(c).getString("tasks", "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Task(
                o.getLong("id"),
                o.getString("text"),
                o.optInt("h", TODAY).coerceIn(TODAY, LATER),
                o.optBoolean("done", false)
            )
        }
    }

    private fun save(c: Context, list: List<Task>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text).put("h", it.horizon).put("done", it.done))
        }
        prefs(c).edit().putString("tasks", arr.toString()).apply()
    }

    /** Tâches d'un onglet : à faire d'abord (dans l'ordre d'ajout), puis les faites. */
    fun forTab(c: Context, h: Int): List<Task> =
        load(c).filter { it.horizon == h }.sortedWith(compareBy({ it.done }, { it.id }))

    fun count(c: Context, h: Int): Int = load(c).count { it.horizon == h && !it.done }

    var Context.currentTab: Int
        get() = prefs(this).getInt("tab", TODAY)
        set(v) { prefs(this).edit().putInt("tab", v).apply() }

    fun tab(c: Context) = c.currentTab
    fun setTab(c: Context, h: Int) { c.currentTab = h.coerceIn(TODAY, LATER) }

    fun add(c: Context, text: String, h: Int) {
        val t = text.trim()
        if (t.isEmpty()) return
        save(c, load(c) + Task(System.currentTimeMillis(), t, h, false))
    }

    fun toggle(c: Context, id: Long) =
        save(c, load(c).map { if (it.id == id) it.copy(done = !it.done) else it })

    /** Déplace une tâche d'un horizon (-1 = vers Aujourd'hui, +1 = vers Plus tard). */
    fun move(c: Context, id: Long, delta: Int) =
        save(c, load(c).map {
            if (it.id == id) it.copy(horizon = (it.horizon + delta).coerceIn(TODAY, LATER)) else it
        })

    fun clearDone(c: Context, h: Int) =
        save(c, load(c).filterNot { it.horizon == h && it.done })
}
