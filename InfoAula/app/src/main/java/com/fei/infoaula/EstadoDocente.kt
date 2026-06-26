package com.fei.infoaula

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EstadoDocente : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var layoutPanelMaestro: LinearLayout
    private lateinit var etUbicacionMaestro: EditText
    private lateinit var rvEstadoProfesores: RecyclerView

    private var disponibilidadSeleccionada = "Disponible"
    private var esMaestro = false
    private val listaProfesores = ArrayList<ModelProfesor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_estado_docente)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        layoutPanelMaestro = findViewById(R.id.layoutPanelMaestro)
        etUbicacionMaestro = findViewById(R.id.etUbicacionMaestro)
        rvEstadoProfesores = findViewById(R.id.rvEstadoProfesores)
        rvEstadoProfesores.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.tbEstadoDocente)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar?.let {
                val params = it.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = systemBars.top
                it.layoutParams = params
            }
            insets
        }

        val btnDispDisponible = findViewById<Button>(R.id.btnDispDisponible)
        val btnDispOcupado = findViewById<Button>(R.id.btnDispOcupado)
        val btnDispNoMolestar = findViewById<Button>(R.id.btnDispNoMolestar)
        val btnGuardarEstado = findViewById<Button>(R.id.btnGuardarEstado)

        btnDispDisponible.setOnClickListener { disponibilidadSeleccionada = "Disponible"; Toast.makeText(this, "Seleccionaste: Disponible", Toast.LENGTH_SHORT).show() }
        btnDispOcupado.setOnClickListener { disponibilidadSeleccionada = "Ocupado"; Toast.makeText(this, "Seleccionaste: Ocupado", Toast.LENGTH_SHORT).show() }
        btnDispNoMolestar.setOnClickListener { disponibilidadSeleccionada = "No molestar"; Toast.makeText(this, "Seleccionaste: No molestar", Toast.LENGTH_SHORT).show() }

        btnGuardarEstado.setOnClickListener { guardarUbicacionDocente() }

        verificarTipoUsuario()
    }

    private fun verificarTipoUsuario() {
        val correo = auth.currentUser?.email ?: ""
        esMaestro = !correo.contains("estudiantes", ignoreCase = true)

        if (esMaestro) {
            layoutPanelMaestro.visibility = View.VISIBLE
        } else {
            layoutPanelMaestro.visibility = View.GONE
        }
        cargarUbicacionesProfesores()
    }

    private fun guardarUbicacionDocente() {
        val uid = auth.currentUser?.uid ?: return
        val ubicacion = etUbicacionMaestro.text.toString().trim()

        if (ubicacion.isEmpty()) {
            Toast.makeText(this, "Escribe dónde te encuentras actualmente", Toast.LENGTH_SHORT).show()
            return
        }

        val refMaestro = database.getReference("usuarios").child("docentes").child(uid)

        val actualizaciones = HashMap<String, Any>()
        actualizaciones["ubicacionActual"] = ubicacion
        actualizaciones["disponibilidad"] = disponibilidadSeleccionada

        refMaestro.updateChildren(actualizaciones).addOnSuccessListener {
            Toast.makeText(this, "Estado publicado con éxito", Toast.LENGTH_SHORT).show()
            etUbicacionMaestro.text.clear()
        }
    }

    private fun cargarUbicacionesProfesores() {
        val refDocentes = database.getReference("usuarios").child("docentes")

        refDocentes.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaProfesores.clear()
                for (docenteSnapshot in snapshot.children) {
                    val nombre = docenteSnapshot.child("nombreProfesor").value as? String ?: "Profesor"
                    val ubicacion = docenteSnapshot.child("ubicacionActual").value as? String ?: "No especificada"
                    val disponibilidad = docenteSnapshot.child("disponibilidad").value as? String ?: "Desconocido"

                    listaProfesores.add(ModelProfesor(nombre, ubicacion, disponibilidad))
                }
                rvEstadoProfesores.adapter = AdaptadorProfesores(listaProfesores)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    data class ModelProfesor(val nombre: String, val ubicacion: String, val disponibilidad: String)

    class AdaptadorProfesores(private val items: List<ModelProfesor>) : RecyclerView.Adapter<AdaptadorProfesores.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.tvItemNombreProfe)
            val tvUbicacion: TextView = view.findViewById(R.id.tvItemUbicacionProfe)
            val tvEstatusTexto: TextView = view.findViewById(R.id.tvItemEstatusTexto)
            val viewColor: View = view.findViewById(R.id.viewIndicadorColor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profesor_estado, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val profe = items[position]
            holder.tvNombre.text = profe.nombre
            holder.tvUbicacion.text = "📍 Actualmente en: ${profe.ubicacion}"
            holder.tvEstatusTexto.text = profe.disponibilidad

            val colorHex = when (profe.disponibilidad.lowercase().trim()) {
                "disponible" -> "#4CAF50"
                "ocupado" -> "#FF9800"
                "no molestar" -> "#F44336"
                else -> "#9E9E9E"
            }

            val bgCirculo = holder.viewColor.background as? GradientDrawable ?: GradientDrawable()
            bgCirculo.setColor(Color.parseColor(colorHex))
            holder.viewColor.background = bgCirculo

            holder.tvEstatusTexto.setTextColor(Color.parseColor(colorHex))
        }
    }
}