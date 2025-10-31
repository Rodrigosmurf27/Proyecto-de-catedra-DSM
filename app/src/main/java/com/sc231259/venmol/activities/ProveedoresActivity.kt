package com.sc231259.venmol.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sc231259.venmol.BaseActivity
import com.sc231259.venmol.R

data class Proveedor(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = ""
)

class ProveedoresActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewProveedores: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var headerSubtitle: TextView
    private lateinit var fabAdd: FloatingActionButton

    private val proveedores = mutableListOf<Proveedor>()
    private lateinit var proveedoresAdapter: ProveedoresAdapter

    override fun getLayoutResourceId(): Int = R.layout.activity_proveedores
    override fun getCurrentMenuItemId(): Int = R.id.nav_proveedores

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        setupRecyclerView()
        loadProveedores()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        recyclerViewProveedores = findViewById(R.id.recyclerViewProveedores)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        headerSubtitle = findViewById(R.id.headerSubtitle)
        fabAdd = findViewById(R.id.fabAdd)

        fabAdd.setOnClickListener { showAddProveedorDialog() }
    }

    private fun setupRecyclerView() {
        proveedoresAdapter = ProveedoresAdapter(
            proveedores,
            onEditClick = { proveedor -> showEditProveedorDialog(proveedor) },
            onDeleteClick = { proveedor -> showDeleteConfirmation(proveedor) },
            onCallClick = { telefono -> makePhoneCall(telefono) },
            onEmailClick = { email -> sendEmail(email) }
        )
        recyclerViewProveedores.layoutManager = LinearLayoutManager(this)
        recyclerViewProveedores.adapter = proveedoresAdapter
    }

    private fun loadProveedores() {
        showLoading(true)

        db.collection("proveedores")
            .get()
            .addOnSuccessListener { result ->
                proveedores.clear()

                for (document in result) {
                    val proveedor = Proveedor(
                        id = document.id,
                        nombre = document.getString("nombre") ?: "",
                        telefono = document.getString("telefono") ?: "",
                        email = document.getString("email") ?: "",
                        direccion = document.getString("direccion") ?: ""
                    )
                    proveedores.add(proveedor)
                }

                updateUI()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error al cargar proveedores: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            }
    }

    private fun updateUI() {
        if (proveedores.isEmpty()) {
            showEmptyState()
        } else {
            showProveedoresList()
            headerSubtitle.text = "${proveedores.size} proveedores registrados"
            proveedoresAdapter.notifyDataSetChanged()
        }
    }

    private fun showAddProveedorDialog() {
        showProveedorDialog(null)
    }

    private fun showEditProveedorDialog(proveedor: Proveedor) {
        showProveedorDialog(proveedor)
    }

    private fun showProveedorDialog(proveedor: Proveedor?) {
        val isEdit = proveedor != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_proveedor, null)

        val nombreInput = dialogView.findViewById<TextInputEditText>(R.id.nombreInput)
        val telefonoInput = dialogView.findViewById<TextInputEditText>(R.id.telefonoInput)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.emailInput)
        val direccionInput = dialogView.findViewById<TextInputEditText>(R.id.direccionInput)

        proveedor?.let {
            nombreInput.setText(it.nombre)
            telefonoInput.setText(it.telefono)
            emailInput.setText(it.email)
            direccionInput.setText(it.direccion)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) "Editar Proveedor" else "Agregar Proveedor")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Actualizar" else "Agregar") { _, _ ->
                val nombre = nombreInput.text.toString().trim()
                val telefono = telefonoInput.text.toString().trim()
                val email = emailInput.text.toString().trim()
                val direccion = direccionInput.text.toString().trim()

                if (validateInputs(nombre, telefono)) {
                    if (isEdit) {
                        updateProveedor(proveedor!!.id, nombre, telefono, email, direccion)
                    } else {
                        addProveedor(nombre, telefono, email, direccion)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validateInputs(nombre: String, telefono: String): Boolean {
        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre es requerido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (telefono.isEmpty()) {
            Toast.makeText(this, "El teléfono es requerido", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun addProveedor(nombre: String, telefono: String, email: String, direccion: String) {
        val proveedor = mapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "email" to email,
            "direccion" to direccion
        )

        db.collection("proveedores")
            .add(proveedor)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Proveedor agregado correctamente!", Toast.LENGTH_SHORT).show()
                loadProveedores()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar proveedor: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProveedor(id: String, nombre: String, telefono: String, email: String, direccion: String) {
        val proveedor = mapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "email" to email,
            "direccion" to direccion
        )

        db.collection("proveedores").document(id)
            .update(proveedor)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Proveedor actualizado correctamente!", Toast.LENGTH_SHORT).show()
                loadProveedores()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar proveedor: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmation(proveedor: Proveedor) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a \"${proveedor.nombre}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteProveedor(proveedor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProveedor(proveedor: Proveedor) {
        db.collection("proveedores").document(proveedor.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Proveedor eliminado correctamente", Toast.LENGTH_SHORT).show()
                loadProveedores()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar proveedor: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun makePhoneCall(telefono: String) {
        if (telefono.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$telefono")
            startActivity(intent)
        }
    }

    private fun sendEmail(email: String) {
        if (email.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:$email")
            startActivity(intent)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerViewProveedores.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        recyclerViewProveedores.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        headerSubtitle.text = "0 proveedores registrados"
    }

    private fun showProveedoresList() {
        progressBar.visibility = View.GONE
        recyclerViewProveedores.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }
}

class ProveedoresAdapter(
    private val proveedores: List<Proveedor>,
    private val onEditClick: (Proveedor) -> Unit,
    private val onDeleteClick: (Proveedor) -> Unit,
    private val onCallClick: (String) -> Unit,
    private val onEmailClick: (String) -> Unit
) : RecyclerView.Adapter<ProveedoresAdapter.ProveedorViewHolder>() {

    class ProveedorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val avatarText: TextView = itemView.findViewById(R.id.avatarText)
        val nombreText: TextView = itemView.findViewById(R.id.nombreText)
        val telefonoText: TextView = itemView.findViewById(R.id.telefonoText)
        val emailText: TextView = itemView.findViewById(R.id.emailText)
        val direccionText: TextView = itemView.findViewById(R.id.direccionText)
        val callButton: ImageButton = itemView.findViewById(R.id.callButton)
        val emailButton: ImageButton = itemView.findViewById(R.id.emailButton)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return ProveedorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
        val proveedor = proveedores[position]

        holder.avatarText.text = proveedor.nombre.firstOrNull()?.toString()?.uppercase() ?: "P"
        holder.nombreText.text = proveedor.nombre
        holder.telefonoText.text = proveedor.telefono
        holder.emailText.text = if (proveedor.email.isNotEmpty()) proveedor.email else "Sin email"
        holder.direccionText.text = if (proveedor.direccion.isNotEmpty()) proveedor.direccion else "Sin dirección"

        holder.callButton.visibility = if (proveedor.telefono.isNotEmpty()) View.VISIBLE else View.GONE
        holder.emailButton.visibility = if (proveedor.email.isNotEmpty()) View.VISIBLE else View.GONE

        holder.callButton.setOnClickListener { onCallClick(proveedor.telefono) }
        holder.emailButton.setOnClickListener { onEmailClick(proveedor.email) }
        holder.editButton.setOnClickListener { onEditClick(proveedor) }
        holder.deleteButton.setOnClickListener { onDeleteClick(proveedor) }
    }

    override fun getItemCount() = proveedores.size
}