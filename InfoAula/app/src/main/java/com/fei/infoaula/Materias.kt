package com.fei.infoaula

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class ObjetoMateria(
    val idRutaFirebase: String = "",
    val nombreConBloque: String = "",
    val profesor: String = "",
    val salon: String = "",
    val estatus: String = "",
    val horario: String = ""
)

class Materias : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rvMaterias: RecyclerView
    private var esMaestro: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_materias)

        auth = FirebaseAuth.getInstance()
        rvMaterias = findViewById(R.id.rvMaterias)
        rvMaterias.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.tbMaterias)

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

        val correoUsuario = auth.currentUser?.email ?: ""
        esMaestro = !correoUsuario.contains("estudiantes", ignoreCase = true)

        cargarListaMaterias()
    }

    private fun cargarListaMaterias() {
        val listaMaterias = ArrayList<ObjetoMateria>()
        val database = FirebaseDatabase.getInstance()
        val dbRef = database.getReference("oferta_clases").child("oferta_clases")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaMaterias.clear()

                if (!snapshot.exists()) {
                    Toast.makeText(this@Materias, "El nodo 'oferta_clases' no existe.", Toast.LENGTH_LONG).show()
                    return
                }

                for (materiaSnapshot in snapshot.children) {
                    val nombreMateria = materiaSnapshot.key ?: "Materia"
                    val profesor = materiaSnapshot.child("profesor_titular").value as? String ?: "Desconocido"
                    val opcionesHorario = materiaSnapshot.child("opciones_horario")

                    for (turnoSnapshot in opcionesHorario.children) {
                        val llaveMateria = materiaSnapshot.key ?: ""
                        val llaveTurno = turnoSnapshot.key ?: ""

                        val rutaExacta = "$llaveMateria/opciones_horario/$llaveTurno"

                        val bloque = turnoSnapshot.child("bloque").value as? String ?: ""
                        val horario = turnoSnapshot.child("horario").value as? String ?: ""
                        val salon = turnoSnapshot.child("salon_asignado").value as? String ?: "S/S"
                        val estatus = turnoSnapshot.child("estatus").value as? String ?: "si hay clases"

                        listaMaterias.add(ObjetoMateria(
                            idRutaFirebase = rutaExacta,
                            nombreConBloque = "$nombreMateria ($bloque)",
                            profesor = profesor,
                            salon = salon,
                            estatus = estatus,
                            horario = horario
                        ))
                    }
                }

                rvMaterias.adapter = AdaptadorMaterias(listaMaterias, esMaestro)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Materias, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class AdaptadorMaterias(
        private val items: List<ObjetoMateria>,
        private val esMaestro: Boolean
    ) : RecyclerView.Adapter<AdaptadorMaterias.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtNombre: TextView = view.findViewById(R.id.tvNombreMateria)
            val txtEstatus: TextView = view.findViewById(R.id.tvEstatusMateria)
            val txtDocente: TextView = view.findViewById(R.id.tvDocenteMateria)
            val txtInfo: TextView = view.findViewById(R.id.tvHorarioSalon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_materia, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val clase = items[position]
            holder.txtNombre.text = clase.nombreConBloque
            holder.txtDocente.text = "Profesor: ${clase.profesor}"
            holder.txtInfo.text = "Horario: ${clase.horario} | Salón: ${clase.salon}"
            holder.txtEstatus.text = clase.estatus

            when (clase.estatus.lowercase().trim()) {
                "si hay clases", "habrá clase" -> {
                    holder.txtEstatus.setTextColor(Color.parseColor("#4CAF50"))
                    holder.txtEstatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "va retrasado" -> {
                    holder.txtEstatus.setTextColor(Color.parseColor("#FF9800"))
                    holder.txtEstatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                "no hay clases", "clase suspendida" -> {
                    holder.txtEstatus.setTextColor(Color.parseColor("#F44336"))
                    holder.txtEstatus.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                else -> {
                    holder.txtEstatus.setTextColor(Color.GRAY)
                    holder.txtEstatus.setBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            }

            holder.itemView.setOnClickListener {
                if (esMaestro) {
                    val opciones = arrayOf("si hay clases", "va retrasado", "no hay clases")
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Actualizar Estatus del Turno")
                        .setItems(opciones) { _, posicionSeleccionada ->
                            val nuevoEstatus = opciones[posicionSeleccionada]

                            // CORREGIDO: Añadido .child("oferta_clases") intermedio para respetar la jerarquía exacta
                            FirebaseDatabase.getInstance().getReference("oferta_clases")
                                .child("oferta_clases")
                                .child(clase.idRutaFirebase)
                                .child("estatus")
                                .setValue(nuevoEstatus)
                                .addOnSuccessListener {
                                    Toast.makeText(holder.itemView.context, "Estatus actualizado con éxito", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .setNegativeButton("Cancelar", null)
                        .create()
                        .show()
                } else {
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Detalle de Horario")
                        .setMessage("Materia: ${clase.nombreConBloque}\nHorario: ${clase.horario}\nEstatus: ${clase.estatus}")
                        .setPositiveButton("Cerrar", null)
                        .create()
                        .show()
                }
            }
        }
    }
}