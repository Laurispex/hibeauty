package com.example.hibeauty.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.RoutineStep
import com.example.hibeauty.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RoutineViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _steps = MutableStateFlow<List<RoutineStep>>(emptyList())
    val steps: StateFlow<List<RoutineStep>> = _steps

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _progressLabel = MutableStateFlow("Comienza tu rutina diaria")
    val progressLabel: StateFlow<String> = _progressLabel

    private val _progressTitle = MutableStateFlow("0 de 0 completados")
    val progressTitle: StateFlow<String> = _progressTitle

    private val db = FirebaseFirestore.getInstance()

    fun load() {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        viewModelScope.launch {
            val result = runCatching {
                db.collection("users").document(uid).collection("routine").get().await()
            }
            result.onSuccess { snapshot ->
                if (snapshot.isEmpty) {
                    saveAndEmitDefault(uid)
                } else {
                    val loaded = snapshot.documents.map { doc ->
                        RoutineStep(
                            id = doc.getString("id") ?: doc.id,
                            title = doc.getString("title") ?: "",
                            duration = doc.getString("duration") ?: "",
                            isCompleted = doc.getBoolean("completed") ?: false,
                            order = (doc.getLong("order") ?: 0L).toInt()
                        )
                    }.sortedBy { it.order }
                    _steps.value = loaded
                    updateProgress(loaded)
                }
            }
        }
    }

    fun toggleStep(step: RoutineStep) {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        val updated = _steps.value.map {
            if (it.id == step.id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _steps.value = updated
        updateProgress(updated)
        viewModelScope.launch {
            val toggled = updated.first { it.id == step.id }
            runCatching {
                db.collection("users").document(uid)
                    .collection("routine").document(step.id)
                    .update("completed", toggled.isCompleted).await()
            }
        }
    }

    fun addStep() {
        val uid = userRepo.currentFirebaseUser?.uid ?: return
        val newStep = RoutineStep(
            id = System.currentTimeMillis().toString(),
            title = "Nueva rutina",
            duration = "2 min",
            isCompleted = false,
            order = _steps.value.size
        )
        val updated = _steps.value + newStep
        _steps.value = updated
        updateProgress(updated)
        viewModelScope.launch {
            runCatching {
                db.collection("users").document(uid)
                    .collection("routine").document(newStep.id)
                    .set(mapOf(
                        "id" to newStep.id,
                        "title" to newStep.title,
                        "duration" to newStep.duration,
                        "completed" to false,
                        "order" to newStep.order
                    )).await()
            }
        }
    }

    private fun saveAndEmitDefault(uid: String) {
        val defaults = listOf(
            RoutineStep("1", "Limpiador Facial",  "2 min", false, 0),
            RoutineStep("2", "Tónico Hidratante", "1 min", false, 1),
            RoutineStep("3", "Serum Vitamina C",  "2 min", false, 2),
            RoutineStep("4", "Crema Hidratante",  "2 min", false, 3),
            RoutineStep("5", "Protector Solar",   "2 min", false, 4)
        )
        _steps.value = defaults
        updateProgress(defaults)
        viewModelScope.launch {
            val ref = db.collection("users").document(uid).collection("routine")
            defaults.forEach { step ->
                runCatching {
                    ref.document(step.id).set(
                        mapOf("id" to step.id, "title" to step.title,
                            "duration" to step.duration, "completed" to false, "order" to step.order)
                    ).await()
                }
            }
        }
    }

    private fun updateProgress(steps: List<RoutineStep>) {
        val done = steps.count { it.isCompleted }
        val total = steps.size
        val pct = if (total == 0) 0 else ((done.toFloat() / total) * 100).toInt()
        _progress.value = pct
        _progressTitle.value = "$done de $total completados"
        _progressLabel.value = when {
            pct == 100  -> "¡Rutina completada con éxito! 🎉"
            pct >= 70   -> "¡Excelente progreso! 💪"
            pct >= 40   -> "¡Vas por muy buen camino!"
            else        -> "Comienza tu rutina diaria"
        }
    }
}
