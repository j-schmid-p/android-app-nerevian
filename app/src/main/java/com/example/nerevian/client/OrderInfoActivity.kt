
package com.example.nerevian.client

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.nerevian.R
import com.example.nerevian.data.Offer
import org.json.JSONObject

class OrderInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_info)

        val offerJson = intent.getStringExtra("offer_json")
        android.util.Log.d("OrderInfoActivity", "Received offerJson: $offerJson")
        if (offerJson != null) {
            val offer = Offer.fromJson(JSONObject(offerJson))
            displayOrderInfo(offer)
        } else {
            android.util.Log.e("OrderInfoActivity", "No offerJson found in intent extras")
        }

        setupNavigationBar()
    }

    private fun displayOrderInfo(offer: Offer) {
        findViewById<TextView>(R.id.tv_order_id_header).text = "#${offer.id}"

        setupField(R.id.label_status, R.id.tv_status, offer.status)
        setupField(R.id.label_origin, R.id.tv_origin, offer.origin)
        setupField(R.id.label_destination, R.id.tv_destination, offer.destination)
        setupField(R.id.label_cargo, R.id.tv_cargo, offer.cargoType)
        setupField(R.id.label_incoterm, R.id.tv_incoterm, offer.incoterm)

        val formattedDate = offer.dateCreated?.split("T")?.get(0)?.split("-")?.let {
            if (it.size == 3) "${it[2]}/${it[1]}/${it[0]}" else offer.dateCreated
        } ?: offer.dateCreated
        setupField(R.id.label_date, R.id.tv_date, formattedDate)

        setupField(R.id.label_weight, R.id.tv_weight, offer.grossWeight?.toString())
        setupField(R.id.label_volume, R.id.tv_volume, offer.volume?.toString())
        setupField(R.id.label_transport, R.id.tv_transport, offer.transportType)
        setupField(R.id.label_flow, R.id.tv_flow, offer.flowType)
        setupField(R.id.label_container, R.id.tv_container, offer.containerType)
        setupField(R.id.label_shipping_line, R.id.tv_shipping_line, offer.shippingLine)
        setupField(R.id.label_comments, R.id.tv_comments, offer.comments)
    }

    private fun setupField(labelId: Int, valueId: Int, value: String?) {
        val label = findViewById<TextView>(labelId)
        val textView = findViewById<TextView>(valueId)
        
        if (value.isNullOrEmpty() || value == "null") {
            label.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            label.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = value
        }
    }

    private fun setupNavigationBar() {
        val navView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        com.example.nerevian.utils.NavigationBar(this).setup(navView)
    }
}