package com.example.maxitv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = view.findViewById<EditText>(R.id.host)
        val user = view.findViewById<EditText>(R.id.user)
        val pass = view.findViewById<EditText>(R.id.pass)
        val status = view.findViewById<TextView>(R.id.status)
        val connect = view.findViewById<Button>(R.id.connect)
        val cancel = view.findViewById<Button>(R.id.cancel)

        if (Profiles.all(requireContext()).isNotEmpty()) {
            cancel.visibility = View.VISIBLE
            cancel.setOnClickListener { (activity as? MainActivity)?.showHome() }
        }

        fun go() {
            if (!connect.isEnabled) return
            val h = host.text.toString().trim()
            val u = user.text.toString().trim()
            val p = pass.text.toString()
            if (h.isEmpty() || u.isEmpty() || p.isEmpty()) {
                status.text = "Remplis les trois champs."
            } else {
                connect.isEnabled = false
                status.text = "Connexion…"
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        XtreamApi(h, u, p).login()
                        Profiles.add(requireContext(), h, u, p)
                        (activity as? MainActivity)?.showHome()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApiError) {
                        status.text = e.message ?: "Connexion impossible."
                        connect.isEnabled = true
                    } catch (e: Exception) {
                        status.text = "Connexion impossible."
                        connect.isEnabled = true
                    }
                }
            }
        }
        connect.setOnClickListener { go() }
        pass.setOnEditorActionListener { _, _, _ -> go(); true }
    }
}
