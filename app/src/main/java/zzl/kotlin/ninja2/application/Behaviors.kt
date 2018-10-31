package zzl.kotlin.ninja2.application

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.find
import zzl.kotlin.ninja2.R

/**
 * 地址栏嵌套滑动的Behavior，该Behavior只能作用于带有
 * {@link app:layout_behavior="@string/bottom_sheet_behavior"}的节点，
 * 并且直接父布局为[android.support.design.widget.CoordinatorLayout]
 * Created by zhongzilu on 2018/9/27
 */
class InputBoxBehavior(var context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<View>(context, attrs) {

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        if (mBottomSheetBehavior == null) {
            mBottomSheetBehavior = BottomSheetBehavior.from(coordinatorLayout.find<View>(R.id.bottomSheetLayout))
        }

        return true
    }


    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        if (dyConsumed > 0) {// 手指上滑
            mBottomSheetBehavior?.let {
                if (it.isCollapsed()) {
                    it.hidden() //隐藏
                }
            }
        } else if (dyConsumed < 0) {// 手指下滑
            mBottomSheetBehavior?.let {
                if (it.isHidden()) {
                    it.collapsed() //显示
                }
            }
        }
    }
}