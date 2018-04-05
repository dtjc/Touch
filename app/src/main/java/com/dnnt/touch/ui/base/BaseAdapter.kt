package com.dnnt.touch.ui.base

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.dnnt.touch.BR
import com.dnnt.touch.ui.main.contact.ItemEvenHandler

/**
 * Created by dnnt on 18-3-21.
 */
abstract class BaseAdapter<T>(list: MutableList<T> = mutableListOf()) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<T>>() {

    private var mList = list


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(LayoutInflater.from(parent.context),getLayoutId(),parent,false)
        return BaseViewHolder(binding)
    }

    override fun getItemCount() = mList.size

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        holder.bind(mList[position],getItemEvenHandler())
    }

    class BaseViewHolder<T>(private val mBinding: ViewDataBinding) : RecyclerView.ViewHolder(mBinding.root){
        fun bind(item: T, handler: ItemEvenHandler<T>){
//            要求item layout中类型为T的variable的name必须为item
            mBinding.setVariable(BR.item,item)
            mBinding.setVariable(BR.evenHandler,handler)
            mBinding.executePendingBindings()
        }
    }

    abstract fun getItemEvenHandler(): ItemEvenHandler<T>

    abstract fun getLayoutId(): Int

    fun setList(list: MutableList<T>?){
        mList = list ?: mutableListOf()
        notifyDataSetChanged()
    }

}