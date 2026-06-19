package com.fei.infoaula

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Salones : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_salones)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
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
}
