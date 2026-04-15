package com.example.nerevian.client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.example.nerevian.R
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OrderHistoryClientFragment : Fragment() {

    private lateinit var orderListContainer: LinearLayout
    private val apiService = ApiService()
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_order_history_agent, container, false)
        orderListContainer = view.findViewById(R.id.order_list_container)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        fetchOrders()
    }

    fun refreshData() {
        fetchOrders()
    }

    private fun fetchOrders() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = apiService.getOrders(session.token ?: "")
            withContext(Dispatchers.Main) {
                if (response != null && response.has("orders")) {
                    val ordersArray = response.getJSONArray("orders")
                    orderListContainer.removeAllViews()
                    if (ordersArray.length() == 0) {
                        Toast.makeText(requireContext(), "No orders found", Toast.LENGTH_SHORT).show()
                    }
                    for (i in 0 until ordersArray.length()) {
                        addOrderView(ordersArray.getJSONObject(i))
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load orders from server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addOrderView(order: JSONObject) {
        val orderView = LayoutInflater.from(requireContext()).inflate(R.layout.item_order, orderListContainer, false)
        
        val orderId = order.optString("id", "N/A")
        val status = order.optString("status", "UNKNOWN").uppercase()

        orderView.findViewById<TextView>(R.id.tv_order_id).text = getString(R.string.order_number_placeholder, orderId)
        
        val statusTag = orderView.findViewById<TextView>(R.id.tv_status_tag)
        statusTag.text = status
        statusTag.visibility = View.VISIBLE // Client sees the status tag

        // Backgrounds based on status
        when (status) {
            "ACCEPTED" -> statusTag.setBackgroundResource(R.drawable.status_accepted_green)
            "PENDING" -> statusTag.setBackgroundResource(R.drawable.status_pending_yellow)
            "REJECTED" -> statusTag.setBackgroundResource(R.drawable.status_rejected_gray)
            "FINISHED" -> statusTag.setBackgroundResource(R.drawable.status_finished_white)
            else -> statusTag.setBackgroundResource(R.drawable.status_rejected_gray)
        }

        val expandableContent = orderView.findViewById<LinearLayout>(R.id.order_expandable_content)
        val header = orderView.findViewById<View>(R.id.order_header)

        header.setOnClickListener {
            expandableContent.visibility = if (expandableContent.isGone) View.VISIBLE else View.GONE
        }

        // Configure expandable content based on status for CLIENT
        when (status) {
            "ACCEPTED" -> {
                orderView.findViewById<View>(R.id.info_grid).visibility = View.VISIBLE
                orderView.findViewById<View>(R.id.route_info).visibility = View.VISIBLE
                orderView.findViewById<View>(R.id.action_buttons_client).visibility = View.VISIBLE
                
                orderView.findViewById<TextView>(R.id.value_left).text = order.optString("incoterm", "N/A")
                orderView.findViewById<TextView>(R.id.value_right).text = order.optString("cargo_type", "N/A")
                orderView.findViewById<TextView>(R.id.tv_origin).text = order.optString("origin", "N/A")
                orderView.findViewById<TextView>(R.id.tv_destination).text = order.optString("destination", "N/A")

                orderView.findViewById<TextView>(R.id.btn_track).setOnClickListener {
                    startActivity(Intent(requireContext(), TrackerActivity::class.java))
                }
            }
            "PENDING" -> {
                orderView.findViewById<View>(R.id.pending_section).visibility = View.VISIBLE
                orderView.findViewById<TextView>(R.id.btn_see_offer).setOnClickListener {
                    // Navigate to see offer logic
                    Toast.makeText(requireContext(), "Opening offer for order #$orderId", Toast.LENGTH_SHORT).show()
                }
            }
        }

        orderListContainer.addView(orderView)
    }
}
