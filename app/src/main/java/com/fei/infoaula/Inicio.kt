package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Importación explícita del recurso R de tu proyecto
import com.fei.infoaula.R

class Inicio : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvInfo: TextView
    private lateinit var rvDatos: RecyclerView
    private lateinit var rvAvisos: RecyclerView
    private lateinit var btnCrearAviso: Button
    private var esMaestro: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        auth = FirebaseAuth.getInstance()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.tbMenuH)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.btnNavegar2)

        navigationView.itemIconTintList = null

        tvInfo = findViewById(R.id.tvInfo)
        rvDatos = findViewById(R.id.rvDatos)
        rvDatos.layoutManager = LinearLayoutManager(this)

        rvAvisos = findViewById(R.id.rvAvisos)
        rvAvisos.layoutManager = LinearLayoutManager(this)
        btnCrearAviso = findViewById(R.id.btnCrearAviso)

        val colorNegroSolido = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        bottomNav.itemIconTintList = colorNegroSolido
        bottomNav.itemTextColor = colorNegroSolido
        toolbar.setNavigationIcon(R.drawable.ic_menu)

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (toolbar.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params -> params.topMargin = systemBars.top; toolbar.layoutParams = params }
            (bottomNav.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params -> params.bottomMargin = systemBars.bottom; bottomNav.layoutParams = params }
            insets
        }

        val correoUsuario = auth.currentUser?.email ?: ""
        esMaestro = !correoUsuario.contains("estudiantes", ignoreCase = true)

        if (esMaestro) {
            tvInfo.text = "Control e Incidencias Estudiantiles:"
            btnCrearAviso.visibility = View.VISIBLE
            cargarListaAlumnosParaMaestro()
        } else {
            tvInfo.text = "Plantilla Académica (Docentes):"
            cargarListaDocentes()
        }

        btnCrearAviso.setOnClickListener {
            mostrarDialogoPublicarAviso()
        }

        cargarAvisosIncidencias()

        toolbar.setNavigationOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navigationView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_materias -> startActivity(Intent(this, Materias::class.java))
                R.id.nav_usuarios -> startActivity(Intent(this, Usuario::class.java))
                R.id.nav_salones -> startActivity(Intent(this, Salones::class.java))
                R.id.nav_salir -> { auth.signOut(); startActivity(Intent(this, MainActivity::class.java)); finish() }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_buscar -> { startActivity(Intent(this, Buscar::class.java)); true }
                R.id.nav_horarios -> { startActivity(Intent(this, Horario::class.java)); true }
                R.id.nav_acerca -> { startActivity(Intent(this, Acerca_de::class.java)); true }
                else -> false
            }
        }
    }

    private fun mostrarDialogoPublicarAviso() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Publicar Cambio / Retraso / Cancelación")

        // SOLUCIONADO: Cambiado activity_dialogo_crear_aviso por tu archivo real 'dialogo_crear_aviso'
        val layout = LayoutInflater.from(this).inflate(R.layout.activity_dialogo_crear_aviso, null)
        val etMateria = layout.findViewById<EditText>(R.id.etAvisoMateria)
        val etHorario = layout.findViewById<EditText>(R.id.etAvisoHorario)
        val etMensaje = layout.findViewById<EditText>(R.id.etAvisoMensaje)

        builder.setView(layout)
        builder.setPositiveButton("Enviar Reporte") { _, _ ->
            val materia = etMateria.text.toString().trim()
            val horario = etHorario.text.toString().trim()
            val mensaje = etMensaje.text.toString().trim()

            if (materia.isNotEmpty() && horario.isNotEmpty() && mensaje.isNotEmpty()) {
                val dbRef = FirebaseDatabase.getInstance().getReference("avisosIncidencias")
                val idAviso = dbRef.push().key ?: ""

                // SOLUCIONADO: Se asocia directamente el texto ingresado a la propiedad 'aviso'
                val nuevoAviso = AvisoIncidencia(idAviso, "Docente Activo", materia, horario, aviso = mensaje)
                dbRef.child(idAviso).setValue(nuevoAviso).addOnSuccessListener {
                    Toast.makeText(this, "Incidencia notificada con éxito", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.create().show()
    }

    private fun cargarAvisosIncidencias() {
        val listaAvisos = ArrayList<AvisoIncidencia>()
        val dbRef = FirebaseDatabase.getInstance().getReference("avisosIncidencias")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaAvisos.clear()
                if (snapshot.exists()) {
                    for (avisoSnapshot in snapshot.children) {
                        val aviso = avisoSnapshot.getValue(AvisoIncidencia::class.java)
                        if (aviso != null) listaAvisos.add(aviso)
                    }
                }
                val adaptador = AdaptadorAvisos(listaAvisos)
                rvAvisos.adapter = adaptador
                adaptador.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    class AdaptadorAvisos(private val items: List<AvisoIncidencia>) : RecyclerView.Adapter<AdaptadorAvisos.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val txtMateria: TextView = view.findViewById(R.id.tvAvisoTituloMateria)
            val txtHorario: TextView = view.findViewById(R.id.tvAvisoHorarioClase)
            val txtMensaje: TextView = view.findViewById(R.id.tvAvisoCuerpoMensaje)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // SOLUCIONADO: Cambiado activity_aviso_tarjeta por tu archivo real 'item_aviso_tarjeta'
            val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_aviso_tarjeta, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val avisoItem = items[position]
            holder.txtMateria.text = "⚠️ ${avisoItem.materia}"
            holder.txtHorario.text = "Horario: ${avisoItem.horario}"
            holder.txtMensaje.text = "Estado: ${avisoItem.aviso}"
        }
    }

    private fun cargarListaDocentes() {
        val listaDocentes = ArrayList<FichaDocente>()
        try {
            val database = FirebaseDatabase.getInstance()
            val dbRef = database.getReference("usuarios").child("docentes")
            dbRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaDocentes.clear()
                    if (snapshot.exists()) {
                        for (docenteSnapshot in snapshot.children) {
                            val nombre = docenteSnapshot.child("nombreProfesor").value as? String ?: "Desconocido"
                            val materia = docenteSnapshot.child("materiaPrincipal").value as? String ?: "Sin materia"
                            val cubiculo = docenteSnapshot.child("cubiculo").value as? String ?: "Sin cubículo"
                            val correo = docenteSnapshot.child("correo").value as? String ?: "Sin correo"
                            val numPersonal = docenteSnapshot.child("numeroPersonal").value as? String ?: "S/N"
                            val horarioAtencion = docenteSnapshot.child("horarioAtencion").value as? String ?: "No asignado"
                            listaDocentes.add(FichaDocente(nombre, materia, cubiculo, correo, numPersonal, horarioAtencion))
                        }
                    }
                    val adaptador = AdaptadorDocentes(listaDocentes)
                    rvDatos.adapter = adaptador
                    adaptador.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun cargarListaAlumnosParaMaestro() {
        val listaAlumnos = ArrayList<FichaAlumno>()
        try {
            val database = FirebaseDatabase.getInstance()
            val dbRef = database.getReference("usuarios").child("alumnos")
            dbRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaAlumnos.clear()
                    if (snapshot.exists()) {
                        for (alumnoSnapshot in snapshot.children) {
                            val nombre = alumnoSnapshot.child("nombreAlumno").value as? String ?: "Desconocido"
                            val matricula = alumnoSnapshot.child("matricula").value as? String ?: "S/M"
                            val carrera = alumnoSnapshot.child("carrera").value as? String ?: "Sin carrera"
                            val semestre = alumnoSnapshot.child("semestre").value as? String ?: "0"
                            val correo = alumnoSnapshot.child("correo").value as? String ?: "Sin correo"
                            listaAlumnos.add(FichaAlumno(nombre, matricula, carrera, semestre, correo))
                        }
                    }
                    val adaptador = AdaptadorAlumnos(listaAlumnos)
                    rvDatos.adapter = adaptador
                    adaptador.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) { e.printStackTrace() }
    }

    class AdaptadorDocentes(private val items: List<FichaDocente>) : RecyclerView.Adapter<AdaptadorDocentes.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titulo: TextView = view.findViewById(R.id.tvTituloTarjeta)
            val sub1: TextView = view.findViewById(R.id.tvSubtitulo1)
            val sub2: TextView = view.findViewById(R.id.tvSubtitulo2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tarjeta, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val docente = items[position]
            holder.titulo.text = docente.nombre
            holder.sub1.text = "Materia: ${docente.materia}"
            holder.sub2.text = "Ubicación: ${docente.cubiculo}"
            holder.sub2.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            holder.itemView.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context).setTitle("Información del Académico")
                    .setMessage("• Nombre: ${docente.nombre}\n• No. Personal: ${docente.numPersonal}\n• Correo: ${docente.correo}\n• Cubículo: ${docente.cubiculo}\n• Horario de Atención: ${docente.horarioAtencion}")
                    .setPositiveButton("Cerrar", null).create().show()
            }
        }
    }

    class AdaptadorAlumnos(private val items: List<FichaAlumno>) : RecyclerView.Adapter<AdaptadorAlumnos.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titulo: TextView = view.findViewById(R.id.tvTituloTarjeta)
            val sub1: TextView = view.findViewById(R.id.tvSubtitulo1)
            val sub2: TextView = view.findViewById(R.id.tvSubtitulo2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tarjeta, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val alumno = items[position]
            holder.titulo.text = alumno.nombre
            holder.sub1.text = "Matrícula: ${alumno.matricula}"
            holder.sub2.text = "Carrera: ${alumno.carrera}"
            holder.sub2.setTextColor(android.graphics.Color.GRAY)
            holder.itemView.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context).setTitle("Detalles del Estudiante")
                    .setMessage("• Nombre: ${alumno.nombre}\n• Matrícula: ${alumno.matricula}\n• Correo: ${alumno.correo}\n• Carrera: ${alumno.carrera}\n• Semestre: ${alumno.semestre}°")
                    .setPositiveButton("Cerrar", null).create().show()
            }
        }
    }
}

// SOLUCIONADO: Clases de datos completamente unificadas con inicializadores y mapeos exactos
data class FichaDocente(val nombre: String = "", val materia: String = "", val cubiculo: String = "", val correo: String = "", val numPersonal: String = "", val horarioAtencion: String = "")
data class FichaAlumno(val nombre: String = "", val matricula: String = "", val carrera: String = "", val semestre: String = "", val correo: String = "")
data class AvisoIncidencia(val id: String = "", val profesor: String = "", val materia: String = "", val horario: String = "", val aviso: String = "")