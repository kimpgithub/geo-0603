package com.example.geo_0603

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.geo_0603.ui.theme.Geo0603Theme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.nio.charset.Charset

class MainActivity : ComponentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var guPolygons: List<GuPolygon>
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)  // Initialize mapView here

        setContent {
            Geo0603Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapViewContainer(savedInstanceState)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val seoul = LatLng(37.5665, 126.9780)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10f))
        loadGuBoundaries()
        mMap.setOnMapClickListener { latLng ->
            guPolygons.forEach { it.resetHighlight() } // 모든 폴리곤의 강조를 초기화
            val selectedGu = guPolygons.find { it.contains(latLng) }
            selectedGu?.highlight(mMap)
        }
    }

    private fun loadGuBoundaries() {
        val inputStream = resources.openRawResource(R.raw.seoul_boundaries)
        val geoJsonString = inputStream.readBytes().toString(Charset.defaultCharset())
        val geoJson = JSONObject(geoJsonString)
        val features = geoJson.getJSONArray("features")

        guPolygons = (0 until features.length()).mapNotNull { i ->
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val type = geometry.getString("type")

            if (type == "Polygon") {
                val polygonOptions = PolygonOptions()
                val coordinates = geometry.getJSONArray("coordinates")

                for (j in 0 until coordinates.length()) {
                    val ring = coordinates.getJSONArray(j)
                    for (k in 0 until ring.length()) {
                        val coordinate = ring.getJSONArray(k)
                        val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0))
                        polygonOptions.add(latLng)
                    }
                }

                polygonOptions.strokeColor(Color.BLACK)
                polygonOptions.fillColor(Color.argb(50, 0, 0, 0))
                val polygon = mMap.addPolygon(polygonOptions)
                GuPolygon(polygon, polygonOptions.points)
            } else null
        }
    }

    data class GuPolygon(val polygon: Polygon, val points: List<LatLng>) {
        fun contains(latLng: LatLng): Boolean = PolyUtil.containsLocation(latLng, points, true)

        fun highlight(map: GoogleMap) {
            polygon.strokeColor = Color.RED
            polygon.fillColor = Color.argb(50, 150, 50, 50)
        }

        fun resetHighlight() {
            polygon.strokeColor = Color.BLACK
            polygon.fillColor = Color.argb(50, 0, 0, 0)
        }
    }

    @Composable
    fun MapViewContainer(savedInstanceState: Bundle?) {
        val context = LocalContext.current
        AndroidView(factory = {
            mapView.apply {
                onCreate(savedInstanceState)
                onResume()
                getMapAsync(this@MainActivity)
            }
        })
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        Geo0603Theme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MapViewContainer(null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) {
            mapView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) {
            mapView.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) {
            mapView.onDestroy()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::mapView.isInitialized) {
            mapView.onLowMemory()
        }
    }
}
