package com.example.gymplan.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WorkoutModel(
    @PrimaryKey(autoGenerate = false)
    val name: String,
    val workoutPlanName: String,
    val desc: String? = null,
)