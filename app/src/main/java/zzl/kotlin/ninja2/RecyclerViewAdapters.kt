package zzl.kotlin.ninja2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import zzl.kotlin.ninja2.application.findViewOften

/**
 * 项目中所有用到的RecyclerView的Adapter集中到一个文件中
 * Created by zhongzilu on 18-9-19.
 */

typealias ItemClickListener = (view: View, position: Int) -> Unit

/**
 * RecyclerViewAdapter封装抽象类
 */
abstract class BaseAdapter<T>(var context: Context, var mList: ArrayList<T>) : RecyclerView.Adapter<BaseViewHolder>() {
    override fun getItemCount() = mList.size

    var mClickListener: ItemClickListener? = null
    fun setOnClickListener(todo: ItemClickListener) {
        mClickListener = todo
    }
}

/**
 * ViewHolder封装抽象类
 */
open class BaseViewHolder(itemView: View, private val listener: ItemClickListener?)
    : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    override fun onClick(v: View) {
        listener?.invoke(v, adapterPosition)
    }

    init {
        itemView.setOnClickListener(this)
    }

    fun <T : View> findView(viewId: Int): T {
        return itemView.findViewOften(viewId)
    }
}

/**
 * 主页Pins适配器
 */
class PinsAdapter(context: Context, mPins: ArrayList<Pin>) : BaseAdapter<Pin>(context, mPins) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pins, parent, false)
        return BaseViewHolder(view, mClickListener)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.findView<TextView>(R.id.pin).text = mList[position].title
    }

    /**
     * 将数据添加到列表中的指定位置
     * @param position 添加到的指定位置
     * @param pin 待添加的数据对象
     */
    fun addItem(position: Int, pin: Pin) {
//        L.i("-->", "position: $position")
//        L.i("-->", "size: ${mList.size}")
        if (position >= mList.size) {
            mList.add(pin)
            notifyItemInserted(position)
            notifyItemRangeChanged(0, mList.size)
            return
        }

        mList.add(position, pin)
        notifyItemInserted(position)
        notifyItemChanged(position)
    }

    /**
     * 将指定位置的数据从集合中移除
     * @param position 待删除的指定位置
     */
    fun removeItem(position: Int) {
        mList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemChanged(position)
    }

    /**
     * 批量添加数据
     * @param pins 待添加的数据集合
     * @param clear 添加之前是否要清除原有数据，默认为false
     */
    fun addAll(pins: List<Pin>, clear: Boolean = false) {
        if (clear) mList.clear()
        mList.addAll(pins)
        notifyItemRangeChanged(0, mList.size)
    }

}

/**
 * 历史访问记录列表适配器
 */
class RecordsAdapter(context: Context, mRecords: ArrayList<Record>) : BaseAdapter<Record>(context, mRecords) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
        return BaseViewHolder(view, mClickListener)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val bean = mList[position]
        holder.findView<TextView>(R.id.title).text = bean.title
        holder.findView<TextView>(R.id.url).text = bean.url
    }

}