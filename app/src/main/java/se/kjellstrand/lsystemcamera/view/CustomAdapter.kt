package se.kjellstrand.lsystemcamera.view

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import se.kjellstrand.lsystemcamera.R


class CustomAdapter(context: Activity, resouceId: Int, textviewId: Int, list: List<RowItem?>) :
    ArrayAdapter<RowItem?>(context, resouceId, textviewId, list) {

    var layoutInflater: LayoutInflater = context.layoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowItem: RowItem? = getItem(position)
        val rowView: View = layoutInflater.inflate(R.layout.single_line_text_item, null, true)
        val name = rowView.findViewById<View>(R.id.lsName) as AppCompatTextView
        name.text = rowItem?.name
        val image = rowView.findViewById<View>(R.id.lsImage) as AppCompatImageView
        image.setImageResource(getIconForName(rowItem?.name))
        return rowView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View? {
        return getView(position, convertView, parent)
    }

    private fun getIconForName(name: String?): Int {
        return when (name) {
            "Dragon" -> R.drawable.dragon
            "Fudgeflake" -> R.drawable.fudge_flake
            "Gosper" -> R.drawable.gosper
            "Hilbert" -> R.drawable.hilbert
            "Moore" -> R.drawable.moore
            "Peano" -> R.drawable.peano
            "SierpinskiCurve" -> R.drawable.sierpinski_curve
            "SierpinskiSquare" -> R.drawable.sierpinski_square
            "SierpinskiTriangle" -> R.drawable.sierpinski_triangle
            "TwinDragon" -> R.drawable.twin_dragon
            else -> R.drawable.unknown_system
        }
    }
}