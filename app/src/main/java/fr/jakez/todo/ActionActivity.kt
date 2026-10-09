package fr.jakez.todo

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Relais invisible pour les taps sur une tâche du widget.
 * Il passe par une « activité » (et pas un simple signal en arrière-plan)
 * pour avoir le droit d'ouvrir la fenêtre de modification quand on tape le texte.
 * Il ne s'affiche jamais (thème NoDisplay) et se ferme aussitôt.
 */
class ActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(TodoWidget.EXTRA_ID, -1L)
        if (id >= 0) {
            when (intent.getStringExtra(TodoWidget.EXTRA_OP)) {
                TodoWidget.OP_TOGGLE -> Store.toggle(this, id)
                TodoWidget.OP_LEFT -> Store.move(this, id, -1)
                TodoWidget.OP_RIGHT -> Store.move(this, id, +1)
                TodoWidget.OP_EDIT -> startActivity(
                    Intent(this, AddActivity::class.java).putExtra(TodoWidget.EXTRA_ID, id)
                )
            }
            TodoWidget.refresh(this)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
