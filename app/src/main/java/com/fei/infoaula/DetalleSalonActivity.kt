package com.fei.infoaula

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class DetalleSalonActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_salon)
        //Llamar a los datos para la ListView
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtEdificio = findViewById<TextView>(R.id.txtEdificio)
        val txtEstado = findViewById<TextView>(R.id.txtEstado)
        val txtApartadoPor = findViewById<TextView>(R.id.txtApartadoPor)
        val txtUltimaActualizacion = findViewById<TextView>(R.id.txtUltimaActualizacion)
        //Obtener los datos para el Intent
        val nombre = intent.getStringExtra("nombre")
        val edificio = intent.getStringExtra("edificio")
        val estado = intent.getStringExtra("estado")
        val apartadoPor = intent.getStringExtra("apartado_por")
        val ultimaActualizacion = intent.getStringExtra("ultima_actualizacion")
        //Mostrar los datos
        txtNombre.text = nombre
        txtEdificio.text = "Edificio: $edificio"
        txtEstado.text = "Estado: $estado"
        txtApartadoPor.text = "Apartado por: $apartadoPor"
        txtUltimaActualizacion.text = "Última actualización: $ultimaActualizacion"

        //Obtención del nombre para rellenar
        val nombreSalon = intent.getStringExtra("nombre_salon")

        if (nombreSalon != null) {
            database = FirebaseDatabase.getInstance("https://infoaula-14aed-default-rtdb.firebaseio.com/")
                .getReference("salones").child(nombreSalon)

            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        txtNombre.text = snapshot.child("nombre").value.toString()
                        txtEdificio.text = "Edificio: ${snapshot.child("edificio").value}"
                        txtEstado.text = "Estado: ${snapshot.child("estado").value}"
                        txtApartadoPor.text = "Apartado por: ${snapshot.child("apartado_por").value}"
                        txtUltimaActualizacion.text = "Última actualización: ${snapshot.child("ultima_actualizacion").value}"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    //En caso de que falle la lectura
                    Toast.makeText(this@DetalleSalonActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
