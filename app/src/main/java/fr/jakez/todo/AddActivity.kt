package fr.jakez.todo

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView

/** Fenêtre d'ajout rapide : on tape, on valide avec la touche Entrée, ça se ferme. */
class AddActivity : Activity() {

    private val radioIds = intArrayOf(R.id.h0, R.id.h1, R.id.h2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        val input = findViewById<EditText>(R.id.input)
        val group = findViewById<RadioGroup>(R.id.horizon)
        val start = intent.getIntExtra(TodoWidget.EXTRA_TAB, Store.tab(this)).coerceIn(0, 2)
        group.check(radioIds[start])

        fun submit() {
            val h = radioIds.indexOf(group.checkedRadioButtonId).coerceAtLeast(0)
            val text = input.text.toString()
            if (text.isNotBlank()) {
                Store.add(this, text, h)
                Store.setTab(this, h) // le widget affiche l'onglet où la tâche vient d'arriver
                TodoWidget.refresh(this)
            }
            finish()
        }

        input.setOnEditorActionListener { _: TextView, actionId: Int, event: KeyEvent? ->
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event?.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_DONE || enter) { submit(); true } else false
        }
        findViewById<TextView>(R.id.btn_ok).setOnClickListener { submit() }
        input.requestFocus()
    }
}
