package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

import com.fei.infoaula.R

class Horario : AppCompatActivity() {

    private lateinit var rvHorarios: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabAgregarHorario: FloatingActionButton
    private val listaHorarios = ArrayList<ClaseAgenda>()

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var esAlumno: Boolean = true
    private var usuarioNombre: String = "Anónimo"
    private var usuarioIdentificador: String = "S/N"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_horario)

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
                R.id.nav_inicio -> { startActivity(Intent(this, Inicio::class.java)); finish(); true }
                R.id.nav_buscar -> { startActivity(Intent(this, Buscar::class.java)); finish(); true }
                R.id.nav_horarios -> true
                R.id.nav_acerca -> { startActivity(Intent(this, Acerca_de::class.java)); finish(); true }
                else -> false
            }
        }

        fabAgregarHorario.setOnClickListener { mostrarDialogoAgregarHorario() }

        obtenerDatosPerfilYRol()
        cargarHorariosDeFirebase()
    }

    private fun obtenerDatosPerfilYRol() {
        val emailActivo = auth.currentUser?.email ?: ""
        if (emailActivo.isEmpty()) return

        esAlumno = emailActivo.contains("estudiantes", ignoreCase = true)
        val subNodoRol = if (esAlumno) "alumnos" else "docentes"

        fabAgregarHorario.visibility = if (esAlumno) View.GONE else View.VISIBLE

        // Buscamos los datos completos del usuario logueado para tener su Nombre Real
        database.getReference("usuarios").child(subNodoRol)
            .orderByChild("correo").equalTo(emailActivo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            usuarioNombre = userSnapshot.child("nombreAlumno").value as? String
                                ?: userSnapshot.child("nombreProfesor").value as? String ?: "Usuario"
                            usuarioIdentificador = userSnapshot.child("matricula").value as? String
                                ?: userSnapshot.child("numeroPersonal").value as? String ?: "S/N"
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // --- MARCAR ASISTENCIA - Alumno ---
    private fun mostrarDialogoMarcarAsistencia(idMateria: String, nombreMateria: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reportar Estado: $nombreMateria")
        builder.setMessage("Selecciona tu condición para la clase de hoy:")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val btnAsistir = Button(this).apply { text = "Asistiré"; setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); setTextColor(android.graphics.Color.WHITE) }
        val btnRetraso = Button(this).apply { text = "Retrasado"; setBackgroundColor(android.graphics.Color.parseColor("#FFC107")); setTextColor(android.graphics.Color.BLACK) }
        val btnNoIr = Button(this).apply { text = "No podré ir"; setBackgroundColor(android.graphics.Color.parseColor("#F44336")); setTextColor(android.graphics.Color.WHITE) }

        // espacio entre botones
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 10, 0, 10)
        }
        layout.addView(btnAsistir, params)
        layout.addView(btnRetraso, params)
        layout.addView(btnNoIr, params)
        builder.setView(layout)

        val dialog = builder.create()

        val registrarEstado = { estado: String ->
            val asistenciaRef = database.getReference("oferta_clases")
                .child("oferta_clases").child(idMateria).child("asistencias_dia").child(usuarioIdentificador)

            val datosAsistencia = HashMap<String, Any>()
            datosAsistencia["alumno"] = usuarioNombre
            datosAsistencia["estado"] = estado

            asistenciaRef.setValue(datosAsistencia).addOnSuccessListener {
                Toast.makeText(this@Horario, "Estado '$estado' enviado al profesor", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        btnAsistir.setOnClickListener { registrarEstado("Asistirá") }
        btnRetraso.setOnClickListener { registrarEstado("Retrasado") }
        btnNoIr.setOnClickListener { registrarEstado("No asistirá") }

        builder.setNegativeButton("Cerrar") { d, _ -> d.dismiss() }
        dialog.show()
    }

    // --- CONTROL DE ASISTENCIA ---
    private fun mostrarDialogoVerAsistenciaProfesor(idMateria: String, nombreMateria: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Control de Asistencia: $nombreMateria")

        val txtLista = TextView(this).apply {
            textSize = 15f
                    setPadding(50, 30, 50, 30)
            text = "Cargando reportes de alumnos..."
            setTextColor(android.graphics.Color.BLACK)
        }
        builder.setView(txtLista)
        builder.setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.show()

        database.getReference("oferta_clases").child("oferta_clases")
            .child(idMateria).child("asistencias_dia")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val sb = StringBuilder()
                        for (alumnoSnapshot in snapshot.children) {
                            val nombre = alumnoSnapshot.child("alumno").value as? String ?: "Alumno"
                            val matricula = alumnoSnapshot.key ?: "S/M"
                            val estado = alumnoSnapshot.child("estado").value as? String ?: "Desconocido"

                            val emoji = when (estado) {
                                "Asistirá" -> "✅"
                                "Retrasado" -> "⏰"
                                else -> "❌"
                            }
                            sb.append("$emoji $nombre ($matricula)\n   ↳ Estado: $estado\n\n")
                        }
                        txtLista.text = sb.toString()
                    } else {
                        txtLista.text = "📭 Ningún alumno ha reportado asistencia el día de hoy."
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // --- FORMULARIO ALTA DOCENTE ---
    private fun mostrarDialogoAgregarHorario() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Agregar Nueva Clase")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val etMateria = EditText(this).apply { hint = "Nombre de la Materia (Ej: Networking)" }
        val etProfesor = EditText(this).apply { hint = "Profesor Titular" }
        val etHora = EditText(this).apply { hint = "Horario (Ej: 09:00 - 11:00)" }
        val etSalon = EditText(this).apply { hint = "Salón (Ej: Aula_f102)" }

        layout.addView(etMateria); layout.addView(etProfesor); layout.addView(etHora); layout.addView(etSalon)
        builder.setView(layout)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val materia = etMateria.text.toString().trim()
            val profesor = etProfesor.text.toString().trim()
            val hora = etHora.text.toString().trim()
            val salon = etSalon.text.toString().trim()

            if (materia.isNotEmpty() && profesor.isNotEmpty() && hora.isNotEmpty() && salon.isNotEmpty()) {
                val dbRef = database.getReference("oferta_clases").child("oferta_clases")
                val idNodoMateria = materia.replace(" ", "_")

                val datosMateria = HashMap<String, Any>()
                datosMateria["materia"] = materia
                datosMateria["profesor_titular"] = profesor

                val datosHorario = HashMap<String, Any>()
                datosHorario["horario"] = hora
                datosHorario["salon_asignado"] = salon
                datosHorario["bloque"] = "Mañana"
                datosHorario["duracion"] = "2 horas"
                datosHorario["estatus"] = "si hay clases"

                datosMateria["opciones_horario"] = hashMapOf("turno_matutino" to datosHorario)

                dbRef.child(idNodoMateria).setValue(datosMateria).addOnSuccessListener {
                    Toast.makeText(this@Horario, "Clase registrada exitosamente", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }
        builder.setNegativeButton("Cancelar") { d, _ -> d.cancel() }
        builder.show()
    }

    private fun cargarHorariosDeFirebase() {
        val dbRef = database.getReference("oferta_clases").child("oferta_clases")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaHorarios.clear()
                if (snapshot.exists()) {
                    for (materiaSnapshot in snapshot.children) {
                        val idMateria = materiaSnapshot.key ?: ""
                        val nombre = materiaSnapshot.child("materia").value as? String ?: "Materia Desconocida"
                        val profesor = materiaSnapshot.child("profesor_titular").value as? String ?: "Sin profesor"

                        val matutinoSnapshot = materiaSnapshot.child("opciones_horario").child("turno_matutino")
                        var bloqueHora = "Sin horario"
                        var aula = "S/A"

                        if (matutinoSnapshot.exists()) {
                            bloqueHora = matutinoSnapshot.child("horario").value as? String ?: "Sin horario"
                            aula = matutinoSnapshot.child("salon_assigned").value as? String
                                ?: matutinoSnapshot.child("salon_asignado").value as? String ?: "S/A"
                        }

                        listaHorarios.add(ClaseAgenda(idMateria, nombre, profesor, bloqueHora, aula))
                    }
                }
                // Pasamos las funciones de la Activity al adaptador mediante expresiones Lambda
                rvHorarios.adapter = AdaptadorHorarios(listaHorarios) { clase ->
                    if (esAlumno) {
                        mostrarDialogoMarcarAsistencia(clase.id, clase.nombre)
                    } else {
                        mostrarDialogoVerAsistenciaProfesor(clase.id, clase.nombre)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    data class ClaseAgenda(val id: String, val nombre: String, val profesor: String, val horario: String, val salon: String)

    class AdaptadorHorarios(
        private val items: List<ClaseAgenda>,
        private val clickListener: (ClaseAgenda) -> Unit
    ) : RecyclerView.Adapter<AdaptadorHorarios.ViewHolder>() {

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

            // Asignar el evento click a la tarjeta
            holder.itemView.setOnClickListener { clickListener(clase) }
        }
    }
}