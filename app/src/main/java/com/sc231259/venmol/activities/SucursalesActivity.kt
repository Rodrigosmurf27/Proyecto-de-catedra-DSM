package com.sc231259.venmol.activities

import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.sc231259.venmol.BaseActivity
import com.sc231259.venmol.R

class SucursalesActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun getLayoutResourceId(): Int = R.layout.activity_sucursales
    override fun getCurrentMenuItemId(): Int = R.id.cardSucursales

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // ===== Sucursales =====
        val sucursalVenmol = LatLng(13.7001597, -89.2342477)
        val sucursalPaseoVenecia = LatLng(13.7155883, -89.1440536)

        mMap.addMarker(
            MarkerOptions()
                .position(sucursalVenmol)
                .title("Sucursal Venmol")
                .snippet("Calle Gabriela Mistral, San Salvador")
        )

        mMap.addMarker(
            MarkerOptions()
                .position(sucursalPaseoVenecia)
                .title("Sucursal Paseo Venecia")
                .snippet("Paseo Venecia, Soyapango")
        )

        // Centrar el mapa para mostrar ambas sucursales
        val bounds = LatLngBounds.Builder()
            .include(sucursalVenmol)
            .include(sucursalPaseoVenecia)
            .build()

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
    }
}
