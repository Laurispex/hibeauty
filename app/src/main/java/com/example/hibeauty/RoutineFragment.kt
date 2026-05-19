package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentRoutineBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoutineFragment : Fragment() {

    private var _binding: FragmentRoutineBinding? = null

    private val binding get() = _binding!!

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val routineSteps =
        mutableListOf<RoutineStep>()

    private val routineAdapter =
        RoutineAdapter { step ->

            toggleStep(step)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentRoutineBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        // VALIDAR LOGIN REAL

        val user =
            FirebaseAuth.getInstance()
                .currentUser

        if (
            user == null ||
            user.isAnonymous
        ) {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragment_container,
                    AuthRequiredFragment()
                )
                .commit()

            return
        }

        // CONTENIDO NORMAL

        setupRecycler()

        setupButtons()

        loadRoutine()
    }

    // BUTTONS

    private fun setupButtons() {

        // BACK

        binding.btnBackRoutine
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }

        // ADD STEP

        binding.btnAddRoutine
            .setOnClickListener {

                showAddRoutineDialog()
            }
    }

    // RECYCLER

    private fun setupRecycler() {

        binding.routineRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.routineRecyclerView.adapter =
            routineAdapter
    }

    // LOAD ROUTINE

    private fun loadRoutine() {

        val currentUser =
            FirebaseAuth.getInstance()
                .currentUser
                ?: return

        db.collection("users")
            .document(currentUser.uid)
            .collection("routine")
            .get()

            .addOnSuccessListener { result ->

                routineSteps.clear()

                if (result.isEmpty) {

                    createDefaultRoutine()

                } else {

                    val loaded =
                        result.documents.map {

                            RoutineStep(

                                id =
                                    it.getString("id")
                                        ?: "",

                                title =
                                    it.getString("title")
                                        ?: "",

                                duration =
                                    it.getString("duration")
                                        ?: "",

                                icon =
                                    it.getString("icon")
                                        ?: "",

                                completed =
                                    it.getBoolean(
                                        "completed"
                                    ) ?: false
                            )
                        }

                    routineSteps.addAll(loaded)

                    routineAdapter.submitList(
                        routineSteps.toList()
                    )

                    updateProgress()
                }
            }
    }

    // DEFAULT ROUTINE

    private fun createDefaultRoutine() {

        val defaultSteps = listOf(

            RoutineStep(
                id = "1",
                title = "Limpiador Facial",
                duration = "2 min",
                icon = "🫧"
            ),

            RoutineStep(
                id = "2",
                title = "Tónico Hidratante",
                duration = "1 min",
                icon = "💧"
            ),

            RoutineStep(
                id = "3",
                title = "Serum Vitamina C",
                duration = "2 min",
                icon = ""
            ),

            RoutineStep(
                id = "4",
                title = "Crema Hidratante",
                duration = "2 min",
                icon = "🔥"
            ),

            RoutineStep(
                id = "5",
                title = "Protector Solar",
                duration = "2 min",
                icon = "☀️"
            )
        )

        routineSteps.addAll(defaultSteps)

        routineAdapter.submitList(
            routineSteps.toList()
        )

        saveRoutine()

        updateProgress()
    }

    // TOGGLE STEP

    private fun toggleStep(
        step: RoutineStep
    ) {

        step.completed =
            !step.completed

        routineAdapter.submitList(
            routineSteps.toList()
        )

        updateProgress()

        saveRoutine()
    }

    // UPDATE PROGRESS

    private fun updateProgress() {

        val completed =
            routineSteps.count {
                it.completed
            }

        val total =
            routineSteps.size

        binding.progressTitle.text =
            "$completed de $total completados"

        val progress =
            if (total == 0)
                0
            else
                (
                        (
                                completed.toFloat()
                                        / total.toFloat()
                                ) * 100
                        ).toInt()

        binding.routineProgressBar.progress =
            progress

        binding.progressMessage.text =

            when {

                progress == 100 ->
                    "¡Rutina completada con éxito!"

                progress >= 70 ->
                    "¡Excelente progreso!"

                progress >= 40 ->
                    "¡Vas por muy buen camino!"

                else ->
                    "Comienza tu rutina diaria"
            }
    }

    // ADD STEP

    private fun showAddRoutineDialog() {

        val newStep = RoutineStep(

            id =
                System.currentTimeMillis()
                    .toString(),

            title = "Nueva rutina",

            duration = "2 min",

            icon = ""
        )

        routineSteps.add(newStep)

        routineAdapter.submitList(
            routineSteps.toList()
        )

        updateProgress()

        saveRoutine()
    }

    // SAVE ROUTINE

    private fun saveRoutine() {

        val currentUser =
            FirebaseAuth.getInstance()
                .currentUser
                ?: return

        val routineRef =
            db.collection("users")
                .document(currentUser.uid)
                .collection("routine")

        routineSteps.forEach { step ->

            routineRef
                .document(step.id)
                .set(step)
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}