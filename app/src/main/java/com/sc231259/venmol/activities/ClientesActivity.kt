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

data class Clientes(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = ""
)

class ClientesActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewClientes: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var headerSubtitle: TextView
    private lateinit var fabAdd: FloatingActionButton

    private val clientes = mutableListOf<Cliente>()
    private lateinit var clientesAdapter: ClientesAdapter

    override fun getLayoutResourceId(): Int = R.layout.activity_clientes
    override fun getCurrentMenuItemId(): Int = R.id.nav_clientes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        setupRecyclerView()
        loadClientes()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        recyclerViewClientes = findViewById(R.id.recyclerViewClientes)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        headerSubtitle = findViewById(R.id.headerSubtitle)
        fabAdd = findViewById(R.id.fabAdd)

        fabAdd.setOnClickListener { showAddClienteDialog() }
    }

    private fun setupRecyclerView() {
        clientesAdapter = ClientesAdapter(
            clientes,
            onEditClick = { cliente -> showEditClienteDialog(cliente) },
            onDeleteClick = { cliente -> showDeleteConfirmation(cliente) },
            onCallClick = { telefono -> makePhoneCall(telefono) },
            onEmailClick = { email -> sendEmail(email) }
        )
        recyclerViewClientes.layoutManager = LinearLayoutManager(this)
        recyclerViewClientes.adapter = clientesAdapter
    }

    private fun loadClientes() {
        showLoading(true)

        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                clientes.clear()

                for (document in result) {
                    val cliente = Cliente(
                        id = document.id,
                        nombre = document.getString("nombre") ?: "",
                        telefono = document.getString("telefono") ?: "",
                        email = document.getString("email") ?: ""
                    )
                    clientes.add(cliente)
                }

                updateUI()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error al cargar clientes: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            }
    }

    private fun updateUI() {
        if (clientes.isEmpty()) {
            showEmptyState()
        } else {
            showClientesList()
            headerSubtitle.text = "${clientes.size} clientes registrados"
            clientesAdapter.notifyDataSetChanged()
        }
    }

    private fun showAddClienteDialog() {
        showClienteDialog(null)
    }

    private fun showEditClienteDialog(cliente: Cliente) {
        showClienteDialog(cliente)
    }

    private fun showClienteDialog(cliente: Cliente?) {
        val isEdit = cliente != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_cliente, null)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnAction = dialogView.findViewById<Button>(R.id.btnAgregar)

        val nombreInput = dialogView.findViewById<TextInputEditText>(R.id.nombreInput)
        val telefonoInput = dialogView.findViewById<TextInputEditText>(R.id.telefonoInput)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.emailInput)

        if (isEdit) {
            dialogTitle.text = "Editar Cliente"
            btnAction.text = "Actualizar"

            cliente?.let {
                nombreInput.setText(it.nombre)
                telefonoInput.setText(it.telefono)
                emailInput.setText(it.email)
            }
        } else {
            dialogTitle.text = "Agregar Cliente"
            btnAction.text = "Agregar"
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false) // Lo manejamos con los botones
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnAction.setOnClickListener {
            val nombre = nombreInput.text.toString().trim()
            val telefono = telefonoInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            // 'direccion' eliminado

            if (validateInputs(nombre, telefono)) {
                if (isEdit) {
                    updateCliente(cliente!!.id, nombre, telefono, email)
                } else {
                    addCliente(nombre, telefono, email)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
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

    private fun addCliente(nombre: String, telefono: String, email: String) {
        val cliente = mapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "email" to email
        )

        db.collection("clientes")
            .add(cliente)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Cliente agregado correctamente!", Toast.LENGTH_SHORT).show()
                loadClientes()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar cliente: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateCliente(id: String, nombre: String, telefono: String, email: String) {
        val cliente = mapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "email" to email
        )

        db.collection("clientes").document(id)
            .update(cliente)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Cliente actualizado correctamente!", Toast.LENGTH_SHORT).show()
                loadClientes()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar cliente: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmation(cliente: Cliente) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a \"${cliente.nombre}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCliente(cliente)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCliente(cliente: Cliente) {
        db.collection("clientes").document(cliente.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Cliente eliminado correctamente", Toast.LENGTH_SHORT).show()
                loadClientes()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar cliente: ${e.message}", Toast.LENGTH_LONG).show()
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
        recyclerViewClientes.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        recyclerViewClientes.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        headerSubtitle.text = "0 clientes registrados"
    }

    private fun showClientesList() {
        progressBar.visibility = View.GONE
        recyclerViewClientes.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }
}

class ClientesAdapter(
    private val clientes: List<Cliente>,
    private val onEditClick: (Cliente) -> Unit,
    private val onDeleteClick: (Cliente) -> Unit,
    private val onCallClick: (String) -> Unit,
    private val onEmailClick: (String) -> Unit
) : RecyclerView.Adapter<ClientesAdapter.ClienteViewHolder>() {

    class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val avatarText: TextView = itemView.findViewById(R.id.avatarText)
        val nombreText: TextView = itemView.findViewById(R.id.nombreText)
        val telefonoText: TextView = itemView.findViewById(R.id.telefonoText)
        val emailText: TextView = itemView.findViewById(R.id.emailText)
        val callButton: ImageButton = itemView.findViewById(R.id.callButton)
        val emailButton: ImageButton = itemView.findViewById(R.id.emailButton)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente, parent, false) // Asegúrate de que item_cliente no use 'direccion'
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = clientes[position]

        holder.avatarText.text = cliente.nombre.firstOrNull()?.toString()?.uppercase() ?: "C"
        holder.nombreText.text = cliente.nombre
        holder.telefonoText.text = cliente.telefono
        holder.emailText.text = if (cliente.email.isNotEmpty()) cliente.email else "Sin email"

        holder.callButton.visibility = if (cliente.telefono.isNotEmpty()) View.VISIBLE else View.GONE
        holder.emailButton.visibility = if (cliente.email.isNotEmpty()) View.VISIBLE else View.GONE

        holder.callButton.setOnClickListener { onCallClick(cliente.telefono) }
        holder.emailButton.setOnClickListener { onEmailClick(cliente.email) }
        holder.editButton.setOnClickListener { onEditClick(cliente) }
        holder.deleteButton.setOnClickListener { onDeleteClick(cliente) }
    }

    override fun getItemCount() = clientes.size
}