package com.example.maxitv

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/** Une seule activité : elle affiche soit la connexion, soit l'accueil. Aucun identifiant ne passe par un Intent. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ok = Session.ready || Session.begin(this)
        if (savedInstanceState == null || !ok) show(if (ok) HomeFragment() else LoginFragment())
    }

    fun showHome() = show(if (Session.begin(this)) HomeFragment() else LoginFragment())

    fun showLogin() = show(LoginFragment())

    private fun show(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.main_container, f).commitAllowingStateLoss()
    }
}
