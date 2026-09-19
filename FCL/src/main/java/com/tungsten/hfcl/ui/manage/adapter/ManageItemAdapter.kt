package com.tungsten.hfcl.ui.manage.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.mio.ui.adapter.ViewHolder
import com.tungsten.hfcl.R
import com.tungsten.hfcl.databinding.ItemManageBinding
import com.tungsten.hfcl.ui.manage.item.ManageItem
import com.tungsten.hfcllibrary.component.theme.ThemeEngine

class ManageItemAdapter(val context: Context, private val itemList: List<ManageItem>) :
    RecyclerView.Adapter<ViewHolder>() {

    interface OnClickListener {
        fun onClick(view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_manage,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = itemList[position]
        ItemManageBinding.bind(holder.itemView).apply {
            item.setOnClickListener {
                data.action.invoke(item)
            }
            item.setText(data.text)
            val end = item.compoundDrawablesRelative[2]
            val start = AppCompatResources.getDrawable(
                context,
                data.drawableStart
            )
            start?.setBounds(end.bounds.left, end.bounds.top, end.bounds.right, end.bounds.bottom)
            start?.setTint(ThemeEngine.getInstance().getTheme().autoTint)
            item.setCompoundDrawablesRelative(
                start,
                null,
                item.compoundDrawablesRelative[2],
                null
            )
        }
    }
}