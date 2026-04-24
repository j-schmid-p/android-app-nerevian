package com.example.nerevian.agent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nerevian.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nerevian.adapters.OfferAdapter
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderHistoryAgentFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OfferAdapter
    private val apiService = ApiService()
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_order_history_agent, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        
        recyclerView = view.findViewById(R.id.rv_orders)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = OfferAdapter(emptyList(), isAgent = true)
        recyclerView.adapter = adapter

        fetchOrders()
    }

    private fun fetchOrders() {
        val token = session.token ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val offersList = apiService.getOffersList(token)
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    if (offersList != null) {
                        if (offersList.isEmpty()) {
                            Toast.makeText(requireContext(), "No orders found", Toast.LENGTH_SHORT).show()
                        }
                        adapter.updateData(offersList)
                    } else {
                        Toast.makeText(requireContext(), "Failed to load orders", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchOrders()
    }
}