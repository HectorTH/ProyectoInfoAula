package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    // Declaración de los componentes de la interfaz
    private lateinit var etUsuario: EditText
    private lateinit var pwContrasena: EditText
    private lateinit var btnIniciar: Button

    // Variable para interactuar con Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // SOLUCIÓN: Buscamos el contenedor raíz por defecto de Android de manera segura
        findViewById<android.view.View>(android.R.id.content)?.let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 1. Vincular los componentes del código con los IDs del XML
        etUsuario = findViewById(R.id.etUsuario)
        pwContrasena = findViewById(R.id.pwContrasena)
        btnIniciar = findViewById(R.id.btnIniciar)

        // 2. Configurar la acción del botón al hacer clic
        btnIniciar.setOnClickListener {
            loginDeUsuario()
        }
    }

    /**
     * Método que maneja la lógica de inicio de sesión con Firebase y la
     * diferenciación entre Estudiante y Docente.
     */
    private fun loginDeUsuario() {
        val usuario = etUsuario.text.toString().trim()
        val contrasena = pwContrasena.text.toString().trim()

        // Validación de campos vacíos antes de llamar a Firebase
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Realizar la autenticación real en la nube de Firebase
        auth.signInWithEmailAndPassword(usuario, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // ¡Login exitoso en Firebase! Pasamos a evaluar el formato
                    evaluarRolYEntrar(usuario)
                } else {
                    // Si las credenciales están mal, muestra el error de Firebase
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Evalúa el formato del usuario para decidir si es Estudiante o Docente
     * y abre la pantalla principal de la aplicación.
     */
    private fun evaluarRolYEntrar(usuario: String) {
        val usuarioMinuscula = usuario.lowercase()

        when {
            // Caso Estudiante: Matrículas de la UV (ej. zs210115@uv.mx o s220145)
            usuarioMinuscula.startsWith("z") || usuarioMinuscula.startsWith("s") -> {
                Toast.makeText(this, "Bienvenido Estudiante: $usuario", Toast.LENGTH_LONG).show()

                // Despliega la pantalla de Inicio (la que tiene el menú inferior y la hamburguesa)
                val intent = Intent(this, Inicio::class.java)
                startActivity(intent)
                finish()
            }

            // Caso Docente: Identificador con "d" o número de personal antes del @ con longitud 5
            usuarioMinuscula.startsWith("d") || usuarioMinuscula.split("@")[0].length == 5 -> {
                Toast.makeText(this, "Bienvenido Docente: $usuario", Toast.LENGTH_LONG).show()

                val intent = Intent(this, Inicio::class.java)
                startActivity(intent)
                finish()
            }

            // Caso de respaldo seguro para garantizar el cambio de pantalla
            else -> {
                Toast.makeText(this, "Acceso concedido", Toast.LENGTH_LONG).show()
                val intent = Intent(this, Inicio::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}