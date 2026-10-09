package fr.jakez.todo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Stockage local des tâches (sur le téléphone, dans les préférences de l'app). */
object Store {

    data class Task(
        val id: Long,
        val text: String,
        val horizon: Int,
        val done: Boolean,
        val pinned: Boolean = false,
        val doneAt: Long = 0L,
    )

    const val TODAY = 0
    const val WEEK = 1
    const val LATER = 2
    val HORIZONS = arrayOf("Aujourd'hui", "Semaine", "Plus tard")

    private fun prefs(c: Context) = c.getSharedPreferences("todo", Context.MODE_PRIVATE)

    // ---------- lecture / écriture ----------

    private fun parse(raw: String): List<Task> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Task(
                id = o.getLong("id"),
                text = o.getString("text"),
                horizon = o.optInt("h", TODAY).coerceIn(TODAY, LATER),
                done = o.optBoolean("done", false),
                pinned = o.optBoolean("pin", false),
                doneAt = o.optLong("doneAt", 0L),
            )
        }
    }

    private fun toJson(list: List<Task>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("text", it.text).put("h", it.horizon)
                    .put("done", it.done).put("pin", it.pinned).put("doneAt", it.doneAt)
            )
        }
        return arr.toString()
    }

    fun load(c: Context): List<Task> =
        try { parse(prefs(c).getString("tasks", "[]") ?: "[]") } catch (e: Exception) { emptyList() }

    private fun save(c: Context, list: List<Task>) {
        prefs(c).edit().putString("tasks", toJson(list)).apply()
    }

    private fun edit(c: Context, id: Long, f: (Task) -> Task) =
        save(c, load(c).map { if (it.id == id) f(it) else it })

    // ---------- affichage ----------

    /** À faire d'abord (épinglées en tête, puis ordre d'ajout), puis les faites. */
    fun forTab(c: Context, h: Int): List<Task> =
        load(c).filter { it.horizon == h }
            .sortedWith(compareBy<Task>({ it.done }, { !it.pinned }, { it.id }))

    fun count(c: Context, h: Int): Int = load(c).count { it.horizon == h && !it.done }

    fun get(c: Context, id: Long): Task? = load(c).firstOrNull { it.id == id }

    fun tab(c: Context) = prefs(c).getInt("tab", TODAY)
    fun setTab(c: Context, h: Int) = prefs(c).edit().putInt("tab", h.coerceIn(TODAY, LATER)).apply()

    // ---------- actions ----------

    fun add(c: Context, text: String, h: Int, pinned: Boolean = false) {
        val t = text.trim()
        if (t.isEmpty()) return
        // max() garantit un id unique même en ajout très rapide
        val id = maxOf(System.currentTimeMillis(), (load(c).maxOfOrNull { it.id } ?: 0L) + 1)
        save(c, load(c) + Task(id, t, h.coerceIn(TODAY, LATER), false, pinned))
    }

    fun update(c: Context, id: Long, text: String, h: Int, pinned: Boolean) {
        val t = text.trim()
        if (t.isEmpty()) return
        edit(c, id) { it.copy(text = t, horizon = h.coerceIn(TODAY, LATER), pinned = pinned) }
    }

    fun delete(c: Context, id: Long) = save(c, load(c).filterNot { it.id == id })

    fun toggle(c: Context, id: Long) = edit(c, id) {
        if (it.done) it.copy(done = false, doneAt = 0L)
        else it.copy(done = true, doneAt = System.currentTimeMillis())
    }

    /** -1 = vers Aujourd'hui, +1 = vers Plus tard. */
    fun move(c: Context, id: Long, delta: Int) =
        edit(c, id) { it.copy(horizon = (it.horizon + delta).coerceIn(TODAY, LATER)) }

    fun clearDone(c: Context, h: Int) = save(c, load(c).filterNot { it.horizon == h && it.done })

    /** Nettoyage automatique : les tâches cochées avant aujourd'hui disparaissent. */
    fun purgeOld(c: Context) {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val list = load(c)
        val kept = list.filterNot { it.done && it.doneAt < midnight }
        if (kept.size != list.size) save(c, kept)
    }

    // ---------- sauvegarde ----------

    fun exportJson(c: Context): String = JSONObject()
        .put("app", "todo-widget").put("version", 1)
        .put("tasks", JSONArray(toJson(load(c)))).toString(2)

    /** Remplace toutes les tâches par celles du fichier. Renvoie le nombre importé, ou -1 si fichier invalide. */
    fun importJson(c: Context, raw: String): Int = try {
        val trimmed = raw.trim()
        val tasksRaw = if (trimmed.startsWith("[")) trimmed
            else JSONObject(trimmed).getJSONArray("tasks").toString()
        val list = parse(tasksRaw)
        save(c, list)
        list.size
    } catch (e: Exception) { -1 }
}
