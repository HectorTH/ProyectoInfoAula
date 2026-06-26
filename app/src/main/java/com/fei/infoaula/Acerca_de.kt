package com.fei.infoaula

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Acerca_de : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_acerca_de)

        // Buscamos el Toolbar de tu diseño xml
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.tbAcercaDe)

        // Ajustamos los insets para que el Toolbar baje lo necesario y no se tape con la barra de notificaciones
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar?.let {
                (it.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.topMargin = systemBars.top
                    it.layoutParams = params
                }
            }
            insets
        }
    }
}