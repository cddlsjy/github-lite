package com.example.githubexplorer.ui.repodetail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView

class TreeAdapter(
    private val context: Context,
    private val groups: List<TreeGroup>,
    private val onFileClick: (String) -> Unit
) : BaseExpandableListAdapter() {

    data class TreeGroup(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val children: MutableList<TreeGroup>
    )

    override fun getGroupCount(): Int = groups.size
    override fun getChildrenCount(groupPosition: Int): Int = groups[groupPosition].children.size
    override fun getGroup(groupPosition: Int): Any = groups[groupPosition]
    override fun getChild(groupPosition: Int, childPosition: Int): Any = groups[groupPosition].children[childPosition]
    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()
    override fun hasStableIds(): Boolean = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val group = groups[groupPosition]
        textView.text = if (group.isDirectory) "📁 ${group.name}" else "📄 ${group.name}"
        textView.compoundDrawablePadding = 8
        return view
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val child = getChild(groupPosition, childPosition) as TreeGroup
        val icon = if (child.isDirectory) "📁 " else "📄 "
        textView.text = "$icon${child.name}"
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        val child = groups[groupPosition].children[childPosition]
        return !child.isDirectory
    }

    fun onChildClick(parent: ExpandableListView?, v: View?, groupPosition: Int, childPosition: Int, id: Long): Boolean {
        val child = groups[groupPosition].children[childPosition]
        if (!child.isDirectory) {
            onFileClick(child.path)
        }
        return true
    }
}
