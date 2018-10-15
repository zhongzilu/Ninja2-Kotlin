package zzl.kotlin.ninja2

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import zzl.kotlin.ninja2.application.L
import zzl.kotlin.ninja2.application.findViewOften

/**
 * 项目中所有用到的RecyclerView的Adapter集中到一个文件中
 * Created by zhongzilu on 18-9-19.
 */

/**
 * RecyclerViewAdapter封装抽象类
 */
abstract class BaseAdapter<T>(var context: Context, var mList: ArrayList<T>) : RecyclerView.Adapter<BaseViewHolder>() {
    override fun getItemCount() = mList.size

    var mClickListener: ((view: View, position: Int) -> Unit)? = null
    fun setOnClickListener(todo: (view: View, position: Int) -> Unit) {
        mClickListener = todo
    }
}

/**
 * ViewHolder封装抽象类
 */
open class BaseViewHolder(itemView: View?, private val listener: ((view: View, position: Int) -> Unit)?)
    : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    override fun onClick(v: View) {
        listener?.invoke(v, adapterPosition)
    }

    init {
        itemView?.setOnClickListener(this)
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
        L.i("-->", "position: $position")

        if (position == mList.size){
            mList.add(pin)
            notifyItemInserted(position)
            notifyItemRangeChanged(0, mList.size)
            return
        }

        mList.add(position, pin)
        notifyItemInserted(position)
        notifyItemChanged(position)
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