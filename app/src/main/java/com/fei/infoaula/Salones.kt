package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Salones : AppCompatActivity() {
    private lateinit var lvDatos: ListView
    private lateinit var listaSalones: ArrayList<Salon>
    private lateinit var adapter: SalonesAdapter
    private lateinit var database: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_salones)
        //Conectar la Firebase
        database = FirebaseDatabase.getInstance("https://infoaula-14aed-default-rtdb.firebaseio.com/").getReference("salones")
        //Inicializar componentes
        lvDatos = findViewById(R.id.lvDatos)
        listaSalones = ArrayList()
        adapter = SalonesAdapter(this, listaSalones)
        lvDatos.adapter = adapter
        //Subir datos del firebase
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaSalones.clear()
                for (salonSnapshot in snapshot.children) {
                    val salon = salonSnapshot.getValue(Salon::class.java)
                    if (salon != null) {
                        listaSalones.add(salon)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                //En caso de falla de lectura
            }
        })
        //agregar el listener a la lista
        lvDatos.setOnItemClickListener { parent, view, position, id ->
            val salonSeleccionado = listaSalones[position]
            val intent = Intent(this, DetalleSalonActivity::class.java)
            intent.putExtra("id_salon", salonSeleccionado.nombre)
            println("ID enviado: ${salonSeleccionado.id_salon}")
            startActivity(intent)
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }
}