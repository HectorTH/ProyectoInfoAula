package com.fei.infoaula

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

import com.fei.infoaula.R

class Buscar : AppCompatActivity() {

    private lateinit var etBuscar: EditText
    private lateinit var lvResultados: ListView
    private lateinit var bottomNav: BottomNavigationView
    private val listaCompletaUniverso = ArrayList<ElementoBusqueda>()
    private val listaFiltrada = ArrayList<ElementoBusqueda>()
    private lateinit var adaptadorPersonalizado: AdaptadorBusqueda

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buscar)

        etBuscar = findViewById(R.id.etBuscar)
        lvResultados = findViewById(R.id.lvResultados)
        bottomNav = findViewById(R.id.btnNavegarBuscar)

        adaptadorPersonalizado = AdaptadorBusqueda(this, listaFiltrada)
        lvResultados.adapter = adaptadorPersonalizado

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (bottomNav.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let { params ->
                params.bottomMargin = systemBars.bottom
                bottomNav.layoutParams = params
            }
            insets
        }

        val colorNegroSolido = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        bottomNav.itemIconTintList = colorNegroSolido
        bottomNav.itemTextColor = colorNegroSolido
        bottomNav.selectedItemId = R.id.nav_buscar

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> { startActivity(Intent(this, Inicio::class.java)); finish(); true }
                R.id.nav_buscar -> true
                R.id.nav_salones -> { startActivity(Intent(this, Salones::class.java)); finish(); true }
                R.id.nav_acerca -> { startActivity(Intent(this, Acerca_de::class.java)); finish(); true }
                else -> false
            }
        }

        descargarTodoElUniversoFirebase()

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarBusqueda(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun descargarTodoElUniversoFirebase() {
        val database = FirebaseDatabase.getInstance()
        val dbRefUsuarios = database.getReference("usuarios")

        dbRefUsuarios.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaCompletaUniverso.clear()

                if (snapshot.exists()) {
                    // 1. Docentes
                    val docentesSnapshot = snapshot.child("docentes")
                    if (docentesSnapshot.exists()) {
                        for (docente in docentesSnapshot.children) {
                            val nombre = docente.child("nombreProfesor").value as? String
                            val materia = docente.child("materiaPrincipal").value as? String ?: "Sin materia asignada"
                            val cubiculo = docente.child("cubiculo").value as? String ?: "S/N"
                            val correo = docente.child("correo").value as? String ?: "Sin correo"
                            val numPersonal = docente.child("numeroPersonal").value as? String ?: "S/N"
                            val horario = docente.child("horarioAtencion").value as? String ?: "No asignado"

                            if (nombre != null) {
                                listaCompletaUniverso.add(
                                    ElementoBusqueda(
                                        tipo = "DOCENTE",
                                        nombre = nombre,
                                        linea1 = " Materia: $materia",
                                        linea2 = " Cubículo/Aula: $cubiculo  |  Horario: $horario",
                                        linea3 = " Correo: $correo  |  ID: $numPersonal"
                                    )
                                )
                            }
                        }
                    }

                    // 2. Alumnos
                    val alumnosSnapshot = snapshot.child("alumnos")
                    if (alumnosSnapshot.exists()) {
                        for (alumno in alumnosSnapshot.children) {
                            val nombre = alumno.child("nombreAlumno").value as? String
                            val matricula = alumno.child("matricula").value as? String ?: "S/M"
                            val carrera = alumno.child("carrera").value as? String ?: "Sin carrera"
                            val semestre = alumno.child("semestre").value as? String ?: "0"
                            val correo = alumno.child("correo").value as? String ?: "Sin correo"

                            if (nombre != null) {
                                listaCompletaUniverso.add(
                                    ElementoBusqueda(
                                        tipo = "ESTUDIANTE",
                                        nombre = nombre,
                                        linea1 = "Carrera: $carrera",
                                        linea2 = " Matrícula: $matricula  |   Semestre: ${semestre}°",
                                        linea3 = " Correo: $correo"
                                    )
                                )
                            }
                        }
                    }
                }
                filtrarBusqueda(etBuscar.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Buscar, "Error en sincronización: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filtrarBusqueda(texto: String) {
        val query = texto.lowercase(Locale.getDefault()).trim()
        listaFiltrada.clear()

        if (query.isEmpty()) {
            listaFiltrada.addAll(listaCompletaUniverso)
        } else {
            for (item in listaCompletaUniverso) {
                if (item.nombre.lowercase(Locale.getDefault()).contains(query) ||
                    item.linea1.lowercase(Locale.getDefault()).contains(query) ||
                    item.linea2.lowercase(Locale.getDefault()).contains(query) ||
                    item.linea3.lowercase(Locale.getDefault()).contains(query) ||
                    item.tipo.lowercase(Locale.getDefault()).contains(query)) {
                    listaFiltrada.add(item)
                }
            }
        }
        adaptadorPersonalizado.notifyDataSetChanged()
    }

    data class ElementoBusqueda(
        val tipo: String,
        val nombre: String,
        val linea1: String,
        val linea2: String,
        val linea3: String
    )

    class AdaptadorBusqueda(private val context: Context, private val items: List<ElementoBusqueda>) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.activity_resultado_busqueda, parent, false)

            val item = items[position]

            val tvTipo = view.findViewById<TextView>(R.id.tvTipo)
            val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
            val tvDetalle1 = view.findViewById<TextView>(R.id.tvDetalle1)
            val tvDetalle2 = view.findViewById<TextView>(R.id.tvDetalle2)
            val tvDetalle3 = view.findViewById<TextView>(R.id.tvDetalle3)

            tvTipo.text = item.tipo
            tvNombre.text = item.nombre
            tvDetalle1.text = item.linea1
            tvDetalle2.text = item.linea2
            tvDetalle3.text = item.linea3

            if (item.tipo == "DOCENTE") {
                tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#E8EFEA"))
                tvTipo.setTextColor(android.graphics.Color.parseColor("#557A5E"))
            } else {
                tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
                tvTipo.setTextColor(android.graphics.Color.parseColor("#1565C0"))
            }

            return view
        }
    }
}