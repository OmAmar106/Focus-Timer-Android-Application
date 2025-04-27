package com.example.focustimerandroidapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.PropertyName
import java.util.concurrent.TimeUnit

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyAdapter = HistoryAdapter(historyList)
        historyRecyclerView.adapter = historyAdapter

        fetchHistoryFromFirestore()

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchHistoryFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("time")

        collectionRef.orderBy("actual time", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val historyItem = document.toObject(HistoryItem::class.java)
                    historyList.add(historyItem)
                }
                historyAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching history: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    class HistoryAdapter(private val historyList: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val historyItem = historyList[position]
            holder.bind(historyItem)
        }

        override fun getItemCount(): Int {
            return historyList.size
        }

        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val timeTextView: TextView = itemView.findViewById(R.id.historyTime)
            private val descriptionTextView: TextView = itemView.findViewById(R.id.historyDescription)
            private val Actualtime: TextView = itemView.findViewById(R.id.actualTime)
            fun bind(historyItem: HistoryItem) {
                val totalSeconds = historyItem.time / 1000

                val hours = TimeUnit.SECONDS.toHours(totalSeconds)
                val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
                val seconds = totalSeconds % 60

                timeTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                descriptionTextView.text = historyItem.action
                if(historyItem.action=="Snooze") {
                    descriptionTextView.setTextColor(Color.parseColor("#FFFF00"))
                }
                else if(historyItem.action=="Start"){
                    descriptionTextView.setTextColor(Color.parseColor("#32CD32"))
                }
                else{
                    descriptionTextView.setTextColor(Color.parseColor("#FF1D1D"))
                }
                Actualtime.text = historyItem.actualtime

            }
        }
    }

    data class HistoryItem(
        val time: Long = 0L,
        @get:PropertyName("actual time")
        @set:PropertyName("actual time")
        var actualtime: String = "",
        val action: String = ""
    )
}
