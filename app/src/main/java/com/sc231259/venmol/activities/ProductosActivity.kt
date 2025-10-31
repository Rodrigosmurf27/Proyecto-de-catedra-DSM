package com.sc231259.venmol.activities

import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sc231259.venmol.BaseActivity
import com.sc231259.venmol.R
import com.sc231259.venmol.models.Producto
import com.sc231259.venmol.models.ProductoConId
import java.text.NumberFormat
import java.util.*


class ProductosActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    // comoponentes
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewProductos: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var headerSubtitle: TextView

    // data
    private val productos = mutableListOf<ProductoConId>()
    private lateinit var productosAdapter: ProductosAdapter

    override fun getLayoutResourceId(): Int = R.layout.activity_productos
    override fun getCurrentMenuItemId(): Int = R.id.nav_productos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        setupRecyclerView()
        loadProductos()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        recyclerViewProductos = findViewById(R.id.recyclerViewProductos)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        headerSubtitle = findViewById(R.id.headerSubtitle)
    }

    private fun setupRecyclerView() {
        // grid de 2 columnas para que se vea bien no muy pequeñ
        val spanCount = 2
        val layoutManager = GridLayoutManager(this, spanCount)

        productosAdapter = ProductosAdapter(productos) { productoConId ->
            showProductDetail(productoConId)
        }

        recyclerViewProductos.layoutManager = layoutManager
        recyclerViewProductos.adapter = productosAdapter

        // espaciado entre los items
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerViewProductos.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing, true))
    }

    private fun loadProductos() {
        showLoading(true)

        db.collection("productos")
            .get()
            .addOnSuccessListener { result ->
                productos.clear()

                for (document in result) {
                    val producto = Producto(
                        nombre = document.getString("nombre") ?: "",
                        marca = document.getString("marca") ?: "Sin marca",
                        descripcion = document.getString("descripcion") ?: "",
                        precio = document.getDouble("precio") ?: 0.0,
                        cantidad = document.getLong("cantidad")?.toInt() ?: 0,
                        imagen = document.getString("imagen") ?: ""
                    )
                    val productoConId = ProductoConId(document.id, producto)
                    productos.add(productoConId)
                }

                updateUI()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            }
    }

    private fun updateUI() {
        if (productos.isEmpty()) {
            showEmptyState()
        } else {
            showProductsList()
            headerSubtitle.text = "${productos.size} productos disponibles"
            productosAdapter.notifyDataSetChanged()
        }
    }

    private fun showProductDetail(productoConId: ProductoConId) {
        val dialog = Dialog(this, android.R.style.Theme_Light_NoTitleBar)
        dialog.setContentView(R.layout.dialog_producto_detail)

        // referencias a las vistas del dialog
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        val productImage = dialog.findViewById<ImageView>(R.id.productImage)
        val productName = dialog.findViewById<TextView>(R.id.productName)
        val productBrand = dialog.findViewById<TextView>(R.id.productBrand)
        val productPrice = dialog.findViewById<TextView>(R.id.productPrice)
        val productDescription = dialog.findViewById<TextView>(R.id.productDescription)
        val productQuantity = dialog.findViewById<TextView>(R.id.productQuantity)

        // y lo llenamos
        val producto = productoConId.producto

        productName.text = producto.nombre
        productBrand.text = producto.marca
        productPrice.text = formatCurrency(producto.precio)
        productDescription.text = if (producto.descripcion.isNotEmpty()) {
            producto.descripcion
        } else {
            "Sin descripción disponible"
        }
        productQuantity.text = producto.cantidad.toString()

        // cargar imagen con Glide
        if (producto.imagen.isNotEmpty()) {
            Glide.with(this)
                .load(producto.imagen)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_product)
                .error(R.drawable.placeholder_product)
                .into(productImage)
        } else {
            productImage.setImageResource(R.drawable.placeholder_product)
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // para que se cierre al tocar afuera
        dialog.setCanceledOnTouchOutside(true)

        dialog.show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerViewProductos.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        recyclerViewProductos.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        headerSubtitle.text = "0 productos disponibles"
    }

    private fun showProductsList() {
        progressBar.visibility = View.GONE
        recyclerViewProductos.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return formatter.format(amount)
    }
}

// adapter para el grid de productos
class ProductosAdapter(
    private val productos: List<ProductoConId>,
    private val onItemClick: (ProductoConId) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productBrand: TextView = itemView.findViewById(R.id.productBrand)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val priceOverlay: TextView = itemView.findViewById(R.id.priceOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_grid, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val productoConId = productos[position]
        val producto = productoConId.producto

        holder.productName.text = producto.nombre
        holder.productBrand.text = producto.marca

        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        val precioFormateado = formatter.format(producto.precio)
        holder.productPrice.text = precioFormateado
        holder.priceOverlay.text = precioFormateado

        // imagen con Glide
        if (producto.imagen.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(producto.imagen)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_product)
                .error(R.drawable.placeholder_product)
                .centerCrop()
                .into(holder.productImage)
        } else {
            holder.productImage.setImageResource(R.drawable.placeholder_product)
        }

        // click listener
        holder.cardView.setOnClickListener {
            onItemClick(productoConId)
        }

        // Animación
        holder.cardView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    holder.cardView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holder.cardView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
            }
            false
        }
    }

    override fun getItemCount() = productos.size
}

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}