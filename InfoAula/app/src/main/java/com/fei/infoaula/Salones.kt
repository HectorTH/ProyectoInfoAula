package com.fei.infoaula

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class Salones : AppCompatActivity() {
    //Lista de salones
    val listaSalones = listOf(
        Salon(101, EstadoSalon.DISPONIBLE, horaDisponible = "14:00"),
        Salon(102, EstadoSalon.OCUPADO, horaOcupado = "15:30"),
        Salon(103, EstadoSalon.RESERVADO, horaReserva = "16:00"),
        Salon(104, EstadoSalon.DISPONIBLE, horaDisponible = "13:45"),
        Salon(105, EstadoSalon.OCUPADO, horaOcupado = "17:00"),
        Salon(106, EstadoSalon.RESERVADO, horaReserva = "18:00"),
        Salon(107, EstadoSalon.DISPONIBLE, horaDisponible = "12:30"),
        Salon(108, EstadoSalon.OCUPADO, horaOcupado = "14:45")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_salones)
        //Referenciar con la ListView
        val listView = findViewById<ListView>(R.id.lvDatos)
        //conectar la lista con el adapter
        val adapter = SalonesAdapter(this,listaSalones)
        listView.adapter = adapter
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
