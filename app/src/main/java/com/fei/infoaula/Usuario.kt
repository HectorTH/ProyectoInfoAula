package com.fei.infoaula

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Importación explícita del recurso R de tu proyecto
import com.fei.infoaula.R

class Usuario : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Componentes de la interfaz vinculados al XML
    private lateinit var tvNombre: TextView
    private lateinit var tvMatricula: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvCarrera: TextView
    private lateinit var tvSemestre: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuario)

        // Inicialización de instancias de Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Vinculación de los TextViews declarados en tu diseño
        tvNombre = findViewById(R.id.tvPerfilNombre)
        tvMatricula = findViewById(R.id.tvPerfilMatricula)
        tvCorreo = findViewById(R.id.tvPerfilCorreo)
        tvCarrera = findViewById(R.id.tvPerfilCarrera)
        tvSemestre = findViewById(R.id.tvPerfilSemestre)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.tbUsuario)

        // Ajuste seguro de WindowInsets para que la barra no choque con el reloj o batería
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

        obtenerInformacionDeSesion()
    }

    private fun obtenerInformacionDeSesion() {
        val emailActivo = auth.currentUser?.email ?: ""

        if (emailActivo.isEmpty()) {
            Toast.makeText(this, "Sesión inválida o expirada", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación automática de rol institucional UV basado en el dominio del correo
        val esAlumno = emailActivo.contains("estudiantes", ignoreCase = true)
        val subNodoRol = if (esAlumno) "alumnos" else "docentes"

        val dbRef = database.getReference("usuarios").child(subNodoRol)

        // Buscador exacto cruzando el campo indexado "correo"
        dbRef.orderByChild("correo").equalTo(emailActivo)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {

                            // Mapeo adaptativo según las propiedades de tu base de datos
                            val nombre = userSnapshot.child("nombreAlumno").value as? String
                                ?: userSnapshot.child("nombreProfesor").value as? String
                                ?: "No registrado"

                            val identificador = userSnapshot.child("matricula").value as? String
                                ?: userSnapshot.child("numeroPersonal").value as? String
                                ?: "S/N"

                            // Renderizado de datos adaptativo según el ROL del usuario
                            tvNombre.text = "Nombre: $nombre"
                            tvCorreo.text = "Correo: $emailActivo"

                            if (esAlumno) {
                                // Mapeo exclusivo para Estudiantes
                                val carrera = userSnapshot.child("carrera").value as? String ?: "Sin carrera"
                                val semestre = userSnapshot.child("semestre").value as? String ?: "0"

                                tvMatricula.text = "Matrícula: $identificador"
                                tvCarrera.text = "Carrera: $carrera"
                                tvSemestre.text = "Semestre: ${semestre}°"
                            } else {
                                // CORREGIDO: Mapeo exclusivo para Docentes usando las llaves reales de Firebase
                                val materia = userSnapshot.child("materiaPrincipal").value as? String ?: "Sin materia asignada"
                                val cubiculo = userSnapshot.child("cubiculo").value as? String ?: "Sin cubículo"
                                val horario = userSnapshot.child("horarioAtencion").value as? String ?: "No asignado"

                                tvMatricula.text = "No. Personal: $identificador"
                                // Reutilizamos el TextView de carrera para mostrar su especialidad docente
                                tvCarrera.text = "Materia Principal: $materia"
                                // Reutilizamos el TextView de semestre para mostrar su ubicación y horario de atención
                                tvSemestre.text = "Ubicación: $cubiculo | Horario: $horario"
                            }
                        }
                    } else {
                        Toast.makeText(this@Usuario, "El correo no figura en el nodo usuarios/$subNodoRol", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Usuario, "Error al conectar: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}