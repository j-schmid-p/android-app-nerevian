package com.example.nerevian.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.nerevian.R
import com.example.nerevian.client.TrackerActivity
import com.example.nerevian.data.Offer

class OfferAdapter(
    private var offers: List<Offer>,
    private val isAgent: Boolean,
    private val onSeeOfferClick: (Offer) -> Unit = {}
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {

    class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderId: TextView = view.findViewById(R.id.tv_order_id)
        val statusTag: TextView = view.findViewById(R.id.tv_status_tag)
        val expandableContent: LinearLayout = view.findViewById(R.id.order_expandable_content)
        val header: View = view.findViewById(R.id.order_header)
        
        val infoGrid: View = view.findViewById(R.id.info_grid)
        val routeInfo: View = view.findViewById(R.id.route_info)
        val actionButtonsClient: View = view.findViewById(R.id.action_buttons_client)
        val pendingSection: View = view.findViewById(R.id.pending_section)
        
        val labelLeft: TextView = view.findViewById(R.id.label_left)
        val valueLeft: TextView = view.findViewById(R.id.value_left)
        val labelRight: TextView = view.findViewById(R.id.label_right)
        val valueRight: TextView = view.findViewById(R.id.value_right)
        val tvOrigin: TextView = view.findViewById(R.id.tv_origin)
        val tvDestination: TextView = view.findViewById(R.id.tv_destination)
        
        val btnTrack: TextView = view.findViewById(R.id.btn_track)
        val btnSeeInfo: TextView = view.findViewById(R.id.btn_see_info)
        val btnSeeOffer: TextView = view.findViewById(R.id.btn_see_offer)
        val agentTrackingSection: View = view.findViewById(R.id.agent_tracking_section)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        val context = holder.itemView.context
        
        holder.orderId.text = context.getString(R.string.order_number_placeholder, offer.id.toString())
        
        if (isAgent) {
            holder.statusTag.visibility = View.GONE
        } else {
            holder.statusTag.visibility = View.VISIBLE
            val status = offer.status.uppercase().trim()
            holder.statusTag.text = status
            
            when (status) {
                "ACCEPTED" -> holder.statusTag.setBackgroundResource(R.drawable.status_accepted_green)
                "SHIPPED", "IN TRANSIT" -> holder.statusTag.setBackgroundResource(R.drawable.status_accepted_green)
                "PENDING" -> holder.statusTag.setBackgroundResource(R.drawable.status_pending_yellow)
                "REJECTED" -> holder.statusTag.setBackgroundResource(R.drawable.status_rejected_gray)
                "FINISHED" -> holder.statusTag.setBackgroundResource(R.drawable.status_finished_white)
                else -> holder.statusTag.setBackgroundResource(R.drawable.status_rejected_gray)
            }
        }

        holder.infoGrid.visibility = View.GONE
        holder.routeInfo.visibility = View.GONE
        holder.actionButtonsClient.visibility = View.GONE
        holder.pendingSection.visibility = View.GONE
        holder.labelRight.visibility = View.VISIBLE
        holder.valueRight.visibility = View.VISIBLE

        holder.expandableContent.visibility = if (offer.isExpanded) View.VISIBLE else View.GONE

        holder.header.setOnClickListener {
            offer.isExpanded = !offer.isExpanded
            notifyItemChanged(position)
        }

        if (isAgent) {
            holder.infoGrid.visibility = View.VISIBLE
            holder.routeInfo.visibility = View.VISIBLE
            
            holder.labelLeft.text = "Customer:"
            holder.valueLeft.text = offer.clientName ?: "N/A"
            holder.labelRight.text = "Incoterm:"
            holder.valueRight.text = offer.incoterm ?: "N/A"
            holder.tvOrigin.text = offer.origin ?: "N/A"
            holder.tvDestination.text = offer.destination ?: "N/A"
            
            holder.agentTrackingSection.visibility = View.GONE
        } else {
            val status = offer.status.uppercase().trim()
            val isActive =
                status == "ACCEPTED" || status == "FINISHED" || status == "SHIPPED" || status == "IN TRANSIT"

            if (isActive) {
                holder.infoGrid.visibility = View.VISIBLE
                holder.routeInfo.visibility = View.VISIBLE
                holder.actionButtonsClient.visibility = View.VISIBLE

                holder.labelLeft.text = "Incoterm:"
                holder.valueLeft.text = offer.incoterm ?: "N/A"
                holder.labelRight.text = "Cargo type:"
                holder.valueRight.text = offer.cargoType ?: "N/A"
                holder.tvOrigin.text = offer.origin ?: "N/A"
                holder.tvDestination.text = offer.destination ?: "N/A"

                holder.btnSeeInfo.setOnClickListener {
                    android.util.Log.d("OfferAdapter", "SEE INFO clicked for order ${offer.id}")
                    if (offer.rawJson != null) {
                        try {
                            val intent = Intent(
                                context,
                                com.example.nerevian.client.OrderInfoActivity::class.java
                            )
                            intent.putExtra("offer_json", offer.rawJson)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "OfferAdapter",
                                "Error starting OrderInfoActivity",
                                e
                            )
                            Toast.makeText(
                                context,
                                "Error: Could not open details",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Error: No data available for this order",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                holder.btnTrack.setOnClickListener {
                    val intent = Intent(context, TrackerActivity::class.java)
                    intent.putExtra("offer_id", offer.id)
                    context.startActivity(intent)
                }
            } else if (status == "PENDING") {
                holder.pendingSection.visibility = View.VISIBLE
                holder.btnSeeOffer.setOnClickListener {
                    onSeeOfferClick(offer)
                }
            } else if (status == "REJECTED") {
                holder.infoGrid.visibility = View.VISIBLE
                holder.labelLeft.text = "Reason:"
                holder.valueLeft.text = offer.rejectionReason ?: "No reason provided"
                holder.labelRight.visibility = View.GONE
                holder.valueRight.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = offers.size

    fun updateData(newOffers: List<Offer>) {
        offers = newOffers
        notifyDataSetChanged()
    }
}
