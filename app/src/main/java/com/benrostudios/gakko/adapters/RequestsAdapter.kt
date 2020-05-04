package com.benrostudios.gakko.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.benrostudios.gakko.R
import com.benrostudios.gakko.data.models.ClassroomJoinRequest
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.request_list_item.view.*

class RequestsAdapter(private val requestList: List<ClassroomJoinRequest>, private var itemClick: ClickListener ): RecyclerView.Adapter<RequestsAdapter.requestViewHolder>(){

    interface ClickListener{
        fun acceptTrigger(posistion: Int)
        fun declineTrigger(posistion: Int)
    }
    private lateinit var mContext: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): requestViewHolder {
        var view: View = LayoutInflater.from(parent.context).inflate(R.layout.request_list_item,parent,false)
        mContext = parent.context
        return requestViewHolder(view)
    }

    override fun getItemCount(): Int = requestList.size

    override fun onBindViewHolder(holder: requestViewHolder, position: Int) {
        holder.requestName.text = requestList[position].name
        holder.requestClass.text = requestList[position].classroomName
        holder.acceptButton.setOnClickListener {
            itemClick.acceptTrigger(position)
        }
        holder.declineButton.setOnClickListener{
            itemClick.declineTrigger(position)
        }
        Glide.with(mContext)
            .load(requestList[position].profileImageLink)
            .placeholder(R.drawable.ic_defualt_profile_pic)
            .into(holder.requestImage)
    }


    class requestViewHolder(v: View): RecyclerView.ViewHolder(v){
        val requestName = v.request_name
        val requestClass = v.request_class
        val requestImage = v.request_image
        val acceptButton = v.request_accept
        val declineButton = v.request_decline
    }
}