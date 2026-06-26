package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Importación explícita del recurso R de tu proyecto
import com.fei.infoaula.R

class Horario : AppCompatActivity() {

    private lateinit var rvHorarios: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabAgregarHorario: FloatingActionButton
    private val listaHorarios = ArrayList<ClaseAgenda>()

    // Instancias de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_horario)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        rvHorarios = findViewById(R.id.rvHorarios)
        bottomNav = findViewById(R.id.btnNavegarHorario)
        fabAgregarHorario = findViewById(R.id.fabAgregarHorario)

        rvHorarios.layoutManager = LinearLayoutManager(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.tbHorario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar?.let {
                (it.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.topMargin = systemBars.top
                    it.layoutParams = params
                }
            }
            (bottomNav.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.bottomMargin = systemBars.bottom
                bottomNav.layoutParams = params
            }
            insets
        }

        val colorNegroSolido = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        bottomNav.itemIconTintList = colorNegroSolido
        bottomNav.itemTextColor = colorNegroSolido

        bottomNav.selectedItemId = R.id.nav_horarios

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, Inicio::class.java))
                    finish()
                    true
                }
                R.id.nav_buscar -> {
                    startActivity(Intent(this, Buscar::class.java))
                    finish()
                    true
                }
                R.id.nav_horarios -> true
                R.id.nav_acerca -> {
                    startActivity(Intent(this, Acerca_de::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // Listener del botón para agregar nuevo horario
        fabAgregarHorario.setOnClickListener {
            mostrarDialogoAgregarHorario()
        }

        verificarRolYMostrarBoton()
        cargarHorariosDeFirebase()
    }

    private fun verificarRolYMostrarBoton() {
        val emailActivo = auth.currentUser?.email ?: ""

        // Validación basada en el dominio institucional
        if (emailActivo.isNotEmpty()) {
            val esAlumno = emailActivo.contains("estudiantes", ignoreCase = true)
            if (!esAlumno) {
                // Si NO contiene "estudiantes", es un maestro/administrativo y puede agregar
                fabAgregarHorario.visibility = View.VISIBLE
            } else {
                fabAgregarHorario.visibility = View.GONE
            }
        }
    }

    private fun mostrarDialogoAgregarHorario() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar Nueva Clase")

        // Formulario creado dinámicamente en un contenedor lineal
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)

        val etMateria = EditText(this).apply { hint = "Nombre de la Materia (Ej: Networking)" }
        val etProfesor = EditText(this).apply { hint = "Profesor Titular" }
        val etHora = EditText(this).apply { hint = "Horario (Ej: 09:00 - 11:00)" }
        val etSalon = EditText(this).apply { hint = "Salón (Ej: Aula_f102)" }

        layout.addView(etMateria)
        layout.addView(etProfesor)
        layout.addView(etHora)
        layout.addView(etSalon)
        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val materia = etMateria.text.toString().trim()
            val profesor = etProfesor.text.toString().trim()
            val hora = etHora.text.toString().trim()
            val salon = etSalon.text.toString().trim()

            if (materia.isNotEmpty() && profesor.isNotEmpty() && hora.isNotEmpty() && salon.isNotEmpty()) {
                guardarMateriaEnFirebase(materia, profesor, hora, salon)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun guardarMateriaEnFirebase(materia: String, profesor: String, hora: String, salon: String) {
        val dbRef = database.getReference("oferta_clases").child("oferta_clases")

        // Creamos un ID usando el nombre de la materia (reemplazando espacios por guiones bajos)
        val idNodoMateria = materia.replace(" ", "_")

        // Armamos la estructura idéntica de tu Firebase
        val datosMateria = HashMap<String, Any>()
        datosMateria["materia"] = materia
        datosMateria["profesor_titular"] = profesor

        val datosHorario = HashMap<String, Any>()
        datosHorario["horario"] = hora
        datosHorario["salon_asignado"] = salon
        datosHorario["bloque"] = "Mañana" // Valor por defecto estructurado
        datosHorario["duracion"] = "2 horas"
        datosHorario["estatus"] = "si hay clases"

        val opcionesHorario = HashMap<String, Any>()
        opcionesHorario["turno_matutino"] = datosHorario

        datosMateria["opciones_horario"] = opcionesHorario

        // Inserción en tiempo real
        dbRef.child(idNodoMateria).setValue(datosMateria)
            .addOnSuccessListener {
                Toast.makeText(this, "Clase registrada exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error al guardar: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarHorariosDeFirebase() {
        val dbRef = database.getReference("oferta_clases").child("oferta_clases")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaHorarios.clear()
                if (snapshot.exists()) {
                    for (materiaSnapshot in snapshot.children) {
                        val nombre = materiaSnapshot.child("materia").value as? String ?: "Materia Desconocida"
                        val profesor = materiaSnapshot.child("profesor_titular").value as? String ?: "Sin profesor"

                        val matutinoSnapshot = materiaSnapshot.child("opciones_horario").child("turno_matutino")

                        var bloqueHora = "Sin horario"
                        var aula = "S/A"

                        if (matutinoSnapshot.exists()) {
                            bloqueHora = matutinoSnapshot.child("horario").value as? String ?: "Sin horario"
                            aula = matutinoSnapshot.child("salon_assigned").value as? String
                                ?: matutinoSnapshot.child("salon_asignado").value as? String
                                        ?: "S/A"
                        }

                        listaHorarios.add(ClaseAgenda(nombre, profesor, bloqueHora, aula))
                    }
                }
                rvHorarios.adapter = AdaptadorHorarios(listaHorarios)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    data class ClaseAgenda(val nombre: String, val profesor: String, val horario: String, val salon: String)

    class AdaptadorHorarios(private val items: List<ClaseAgenda>) : RecyclerView.Adapter<AdaptadorHorarios.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.tvHorarioMateria)
            val tvProfesor: TextView = view.findViewById(R.id.tvHorarioProfesor)
            val tvHorario: TextView = view.findViewById(R.id.tvHorarioBloque)
            val tvSalon: TextView = view.findViewById(R.id.tvHorarioAula)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_horario_clase, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val clase = items[position]
            holder.tvNombre.text = clase.nombre
            holder.tvProfesor.text = "👨‍🏫 ${clase.profesor}"
            holder.tvHorario.text = "⏰ ${clase.horario}"
            holder.tvSalon.text = "📍 Salón: ${clase.salon}"
        }
    }
}