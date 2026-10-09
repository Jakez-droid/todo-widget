package fr.jakez.todo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

/**
 * Une seule fenêtre pour tout :
 *  - ajout (depuis le +) : Entrée ajoute et garde la fenêtre ouverte pour la suivante ;
 *  - modification (tap sur le texte d'une tâche) : texte, horizon, épingle, suppression ;
 *  - depuis l'icône de l'app : ajout + sauvegarde / restauration.
 */
class AddActivity : Activity() {

    private val radioIds = intArrayOf(R.id.h0, R.id.h1, R.id.h2)
    private val reqExport = 1
    private val reqImport = 2

    private lateinit var input: EditText
    private lateinit var group: RadioGroup
    private lateinit var pin: CheckBox
    private var editId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        input = findViewById(R.id.input)
        group = findViewById(R.id.horizon)
        pin = findViewById(R.id.pin)
        val counter = findViewById<TextView>(R.id.counter)

        editId = intent.getLongExtra(TodoWidget.EXTRA_ID, -1L)
        val task = if (editId >= 0) Store.get(this, editId) else null
        if (editId >= 0 && task == null) { finish(); return }
        val fromWidget = intent.getBooleanExtra(TodoWidget.EXTRA_FROM_WIDGET, false)

        if (task != null) {
            // Mode modification
            input.setText(task.text)
            input.setSelection(task.text.length)
            group.check(radioIds[task.horizon])
            pin.isChecked = task.pinned
            findViewById<View>(R.id.btn_delete).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    Store.delete(this@AddActivity, editId)
                    TodoWidget.refresh(this@AddActivity)
                    finish()
                }
            }
        } else {
            // Mode ajout
            val start = intent.getIntExtra(TodoWidget.EXTRA_TAB, Store.tab(this)).coerceIn(0, 2)
            group.check(radioIds[start])
            if (!fromWidget) {
                findViewById<View>(R.id.backup_row).visibility = View.VISIBLE
                findViewById<View>(R.id.btn_export).setOnClickListener { export() }
                findViewById<View>(R.id.btn_import).setOnClickListener { import() }
            }
        }

        var added = 0
        fun horizon() = radioIds.indexOf(group.checkedRadioButtonId).coerceAtLeast(0)

        /** close = true : on ferme après ; false : ajout en rafale. */
        fun submit(close: Boolean) {
            val text = input.text.toString()
            if (task != null) {
                if (text.isNotBlank()) Store.update(this, editId, text, horizon(), pin.isChecked)
                TodoWidget.refresh(this)
                finish()
                return
            }
            if (text.isNotBlank()) {
                Store.add(this, text, horizon(), pin.isChecked)
                Store.setTab(this, horizon()) // le widget affiche l'onglet où la tâche arrive
                TodoWidget.refresh(this)
                added++
                counter.text = if (added == 1) "1 tâche ajoutée" else "$added tâches ajoutées"
                counter.visibility = View.VISIBLE
                input.setText("")
                pin.isChecked = false
            }
            if (close || text.isBlank()) finish()
        }

        input.setOnEditorActionListener { _: TextView, actionId: Int, event: KeyEvent? ->
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event?.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_DONE || enter) { submit(close = false); true } else false
        }
        findViewById<TextView>(R.id.btn_ok).setOnClickListener { submit(close = true) }
        input.requestFocus()
    }

    // ---------- sauvegarde / restauration ----------

    private fun export() {
        @Suppress("DEPRECATION")
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, "todo-sauvegarde.json"),
            reqExport
        )
    }

    private fun import() {
        AlertDialog.Builder(this)
            .setMessage("Restaurer remplace toutes tes tâches actuelles par celles du fichier. Continuer ?")
            .setPositiveButton("Choisir le fichier") { _, _ ->
                @Suppress("DEPRECATION")
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*"),
                    reqImport
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    @Deprecated("API Activity classique, suffisante ici")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) return
        try {
            when (requestCode) {
                reqExport -> {
                    contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(Store.exportJson(this).toByteArray())
                    }
                    toast("Sauvegarde enregistrée")
                }
                reqImport -> {
                    val raw = contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
                    val n = Store.importJson(this, raw)
                    if (n < 0) toast("Fichier non reconnu") else {
                        TodoWidget.refresh(this)
                        toast("$n tâches restaurées")
                    }
                }
            }
        } catch (e: Exception) {
            toast("Erreur : ${e.message}")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
