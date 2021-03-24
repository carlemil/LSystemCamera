package se.kjellstrand.lsystemcamera.view

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import se.kjellstrand.lsystemcamera.R


class CustomAdapter(context: Activity, resouceId: Int, textviewId: Int, list: List<RowItem?>) :
    ArrayAdapter<RowItem?>(context, resouceId, textviewId, list) {
    var layoutInflater: LayoutInflater = context.layoutInflater
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowItem: RowItem? = getItem(position)
        val rowview: View = layoutInflater.inflate(R.layout.single_line_text_item, null, true)
        val txtTitle = rowview.findViewById<View>(R.id.lsName) as TextView
        txtTitle.text = rowItem?.title
        return rowview
    }

}