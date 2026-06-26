package com.fei.infoaula

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class DetalleSalonActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_salon)

        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtEdificio = findViewById<TextView>(R.id.txtEdificio)
        val txtEstado = findViewById<TextView>(R.id.txtEstado)
        val txtApartadoPor = findViewById<TextView>(R.id.txtApartadoPor)
        val txtUltimaActualizacion = findViewById<TextView>(R.id.txtUltimaActualizacion)

        //Obtención del ID
        val idSalon = intent.getStringExtra("id_salon")

        if (idSalon != null) {
            database = FirebaseDatabase.getInstance("https://infoaula-14aed-default-rtdb.firebaseio.com/")
                .getReference("usuarios/docentes").child(idSalon!!)

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
