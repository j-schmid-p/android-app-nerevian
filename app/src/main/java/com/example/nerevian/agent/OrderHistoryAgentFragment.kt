package com.example.nerevian.agent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nerevian.R
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OrderHistoryAgentFragment : Fragment() {

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
                    for (i in 0 until ordersArray.length()) {
                        addOrderView(ordersArray.getJSONObject(i))
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addOrderView(order: JSONObject) {
        val orderView = LayoutInflater.from(requireContext()).inflate(R.layout.item_order, orderListContainer, false)
        
        val orderId = order.optString("id", "N/A")
        orderView.findViewById<TextView>(R.id.tv_order_id).text = "Order #$orderId"
        
        // As an Agent, the status tag is hidden
        orderView.findViewById<TextView>(R.id.tv_status_tag).visibility = View.GONE

        val expandableContent = orderView.findViewById<LinearLayout>(R.id.order_expandable_content)
        val header = orderView.findViewById<View>(R.id.order_header)

        header.setOnClickListener {
            expandableContent.visibility = if (expandableContent.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        // Agent expansion: Show general info (Incoterm, Cargo, Route)
        orderView.findViewById<View>(R.id.info_grid).visibility = View.VISIBLE
        orderView.findViewById<View>(R.id.route_info).visibility = View.VISIBLE
        
        // For agents, we might want to show the customer name in the grid
        orderView.findViewById<TextView>(R.id.label_left).text = "Customer:"
        orderView.findViewById<TextView>(R.id.value_left).text = order.optString("customer_name", "N/A")
        
        orderView.findViewById<TextView>(R.id.label_right).text = "Incoterm:"
        orderView.findViewById<TextView>(R.id.value_right).text = order.optString("incoterm", "N/A")
        
        orderView.findViewById<TextView>(R.id.tv_origin).text = order.optString("origin", "N/A")
        orderView.findViewById<TextView>(R.id.tv_destination).text = order.optString("destination", "N/A")

        orderListContainer.addView(orderView)
    }
}
