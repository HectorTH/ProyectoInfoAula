package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Inicio : AppCompatActivity() {

    // Instancia de autenticación y componentes visuales que se llenarán dinámicamente
    private lateinit var auth: FirebaseAuth
    private lateinit var tvInfo: TextView
    private lateinit var lvDatos: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 1. Vincular los componentes del XML con el código Kotlin
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.tbMenuH)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.btnNavegar2)

        // Inicializar los componentes donde se inyectarán los datos de Firebase
        tvInfo = findViewById(R.id.tvInfo)
        lvDatos = findViewById(R.id.lvDatos)

        // SOLUCIÓN RADICAL: Forzar color Negro (#000000) en barra inferior
        val colorNegroSolido = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        bottomNav.itemIconTintList = colorNegroSolido
        bottomNav.itemTextColor = colorNegroSolido

        // Muestra visualmente el icono de hamburguesa en la esquina de la Toolbar
        toolbar.setNavigationIcon(R.drawable.ic_menu)

        // Ajustar los márgenes dinámicos (Edge-to-Edge) de forma segura
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Ajustar el margen superior de la Toolbar de forma segura
            (toolbar.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.topMargin = systemBars.top
                toolbar.layoutParams = params
            }

            // Ajustar el margen inferior de la barra de navegación de forma segura
            (bottomNav.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.bottomMargin = systemBars.bottom
                bottomNav.layoutParams = params
            }

            insets
        }

        // LÓGICA DE FIREBASE: Consultar el rol del usuario actual para personalizar la UI
        obtenerDatosSegunRol()

        // 2. Programar el clic en el menú hamburguesa para abrir la barra lateral
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 3. Programar el comportamiento de la barra lateral (NavigationView) usando accesos seguros
        navigationView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inicio -> {
                    // Ya nos encontramos en la pantalla de Inicio
                }
                R.id.nav_salones -> {
                    val intent = Intent(this, Salones::class.java)
                    startActivity(intent)
                }
                R.id.nav_salir -> {
                    auth.signOut() // Cierra la sesión de manera segura en Firebase
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 4. Programar las acciones de la barra inferior (BottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_buscar -> {
                    Toast.makeText(this, "Buscar aulas", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_salones -> {
                    val intent = Intent(this, Salones::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_acerca -> {
                    Toast.makeText(this, "InfoAula - Proyecto UV", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Revisa en Realtime Database el nodo 'usuarios/UID' para determinar
     * si se trata de un estudiante o un docente.
     */
    private fun obtenerDatosSegunRol() {
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val rol = snapshot.child("rol").value as? String
                    val nombreUsuario = snapshot.child("nombre").value as? String ?: "Usuario"

                    when (rol) {
                        "docente" -> {
                            // Si es maestro, muestra su nombre y busca la lista de alumnos
                            tvInfo.text = "Profesor: $nombreUsuario\nLista de Alumnos registrados:"
                            cargarListaAlumnos()
                        }
                        "estudiante" -> {
                            // Si es estudiante, muestra su nombre y busca la lista de maestros
                            tvInfo.text = "Bienvenido Estudiante: $nombreUsuario\nPlantilla Académica (Docentes):"
                            cargarListaDocentes()
                        }
                        else -> {
                            tvInfo.text = "Bienvenido a InfoAula\nÚltimos datos:"
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Inicio, "Error de conexión: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Trae de la base de datos a todas las personas registradas con el rol 'estudiante'
     */
    private fun cargarListaAlumnos() {
        val listaAlumnos = ArrayList<String>()
        val dbRef = FirebaseDatabase.getInstance().getReference("usuarios")

        dbRef.orderByChild("rol").equalTo("estudiante")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaAlumnos.clear()
                    for (userSnapshot in snapshot.children) {
                        val nombre = userSnapshot.child("nombre").value as? String
                        val matricula = userSnapshot.child("matricula").value as? String
                        if (nombre != null && matricula != null) {
                            listaAlumnos.add("$nombre ($matricula)")
                        }
                    }
                    // Pintar las cadenas de texto dentro del ListView utilizando el formato por defecto de Android
                    val adapter = ArrayAdapter(this@Inicio, android.R.layout.simple_list_item_1, listaAlumnos)
                    lvDatos.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    /**
     * Trae de la base de datos a todas las personas registradas con el rol 'docente'
     */
    private fun cargarListaDocentes() {
        val listaDocentes = ArrayList<String>()
        val dbRef = FirebaseDatabase.getInstance().getReference("usuarios")

        dbRef.orderByChild("rol").equalTo("docente")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaDocentes.clear()
                    for (userSnapshot in snapshot.children) {
                        val nombre = userSnapshot.child("nombre").value as? String
                        val cubico = userSnapshot.child("cubiculo").value as? String ?: "No definido"
                        if (nombre != null) {
                            listaDocentes.add("Prof. $nombre — Cubículo: $cubico")
                        }
                    }
                    val adapter = ArrayAdapter(this@Inicio, android.R.layout.simple_list_item_1, listaDocentes)
                    lvDatos.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}