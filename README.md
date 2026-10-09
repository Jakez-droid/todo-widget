# To-do widget

Widget Android plein écran, 3 onglets : **Aujourd'hui / Semaine / Plus tard**.

- Tap sur le rond → tâche cochée (barrée, passe en bas). Re-tap → décochée.
- Tap sur le texte → fenêtre de modification : texte, horizon, épingle, suppression.
- Les tâches épinglées restent en haut de leur onglet.
- `‹` / `›` → déplace la tâche vers l'horizon plus proche / plus lointain.
- `+` → fenêtre d'ajout. Entrée ajoute et laisse la fenêtre ouverte pour la suivante ; ✓ ou tap à côté pour fermer.
- Les tâches cochées disparaissent toutes seules le lendemain.
- Icône de l'app → Sauvegarder / Restaurer (fichier JSON).
- Icône double-coche en bas à gauche → efface les tâches faites de l'onglet.
- Thème clair / sombre selon le réglage du téléphone. Tâches stockées sur le téléphone.

## Compilation

Chaque push sur `main` lance GitHub Actions, qui publie `Todo.apk` dans **Releases**.
Télécharger depuis le téléphone : `https://github.com/<toi>/todo-widget/releases/latest`.
