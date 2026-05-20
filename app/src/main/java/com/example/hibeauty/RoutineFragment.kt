package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hibeauty.databinding.FragmentRoutineBinding
import com.example.hibeauty.ui.routine.RoutineViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RoutineFragment : Fragment() {

    private var _binding: FragmentRoutineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoutineViewModel by viewModels()

    private val routineAdapter = RoutineAdapter { step -> viewModel.toggleStep(step) }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Guard: must be logged in to access routines
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AuthRequiredFragment())
                .commit()
            return
        }

        setupRecycler()
        setupButtons()
        observeViewModel()
        viewModel.load()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    // ─── SETUP ─────────────────────────────────────────────────────────────────

    private fun setupRecycler() {
        binding.routineRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.routineRecyclerView.adapter = routineAdapter
    }

    private fun setupButtons() {
        binding.btnBackRoutine.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnAddRoutine.setOnClickListener { viewModel.addStep() }
    }

    // ─── OBSERVERS ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.steps.collect { steps ->
                    routineAdapter.submitList(steps)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progress.collect { pct ->
                    binding.routineProgressBar.progress = pct
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progressTitle.collect { title ->
                    binding.progressTitle.text = title
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progressLabel.collect { label ->
                    binding.progressMessage.text = label
                }
            }
        }
    }
}
