package com.dhbw.triplog.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dhbw.triplog.R
import com.dhbw.triplog.other.FilterItem

class AlertFilterAdapter(val context: Context) : RecyclerView.Adapter<AlertFilterAdapter.MyViewHolder>() {

    var filterList : List<FilterItem> = mutableListOf()

    private var selectedItem: Int = -1
    var callback: RecyclerviewCallbacks<FilterItem>? = null

    fun addAlertFilter(filers: List<FilterItem>) {
        filterList = filers.toMutableList()
        notifyDataSetChanged()
    }

    fun selectedItem(position: Int){
        selectedItem = position
        notifyItemChanged(position)
    }

    override fun onBindViewHolder(holder: MyViewHolder, p1: Int) {
        val item = filterList[p1]
        holder.tvName.text = item.name
        holder.alert_filter_icon.background = ContextCompat.getDrawable(context, item.icon)

        if(p1 == selectedItem) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.alert_filter_icon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.dhbw_red)
            }
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.dhbw_red))
            holder.alert_filter_selected.visibility = View.VISIBLE
        } else {
            holder.alert_filter_selected.visibility = View.INVISIBLE
        }
    }

    fun setOnClick(click: RecyclerviewCallbacks<FilterItem>){
        callback = click
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MyViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.vehicle_layout,p0,false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    inner class MyViewHolder(item: View) : RecyclerView.ViewHolder(item) {

        var tvName: TextView = itemView.findViewById(R.id.alert_filter_name)
        var alert_filter_icon: ImageView = itemView.findViewById(R.id.alert_filter_icon)
        var alert_filter_selected: ImageView = itemView.findViewById(R.id.alert_filter_selected)
        var filterLayout: ConstraintLayout = itemView.findViewById(R.id.alert_filter_item_layout)
        init {
            setClickListener(filterLayout)
        }

        private fun setClickListener(view: View) {
            view.setOnClickListener {
                callback?.onItemClick(it, adapterPosition, filterList[adapterPosition])
            }
        }
    }
}

interface RecyclerviewCallbacks<T> {

    fun onItemClick(view: View, position: Int, item: T)

}