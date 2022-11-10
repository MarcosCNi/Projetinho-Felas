package com.example.gymplan.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymplan.R
import com.example.gymplan.data.model.entity.WorkoutModel
import com.example.gymplan.databinding.FragmentHomeBinding
import com.example.gymplan.ui.adapters.WorkoutListAdapter
import com.example.gymplan.ui.base.BaseFragment
import com.example.gymplan.utils.gone
import com.example.gymplan.utils.show
import com.example.gymplan.utils.toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.tsuryo.swipeablerv.SwipeLeftRightCallback
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(){

    override val viewModel: HomeViewModel by viewModels()
    private val workoutAdapter by lazy { WorkoutListAdapter() }

    override fun onResume() {
        super.onResume()
        collectObservers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        checkUser()
        clickAdapter()
        collectObservers()
        setupOptionBtn()
    }

    private fun clickAdapter() {
        workoutAdapter.setOnclickListener { workoutModel ->
            val action = HomeFragmentDirections
                .actionHomeFragmentToCustomExerciseListFragment(workoutModel)
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() = with(binding) {
        rvPlanList.apply {
            adapter = workoutAdapter
            layoutManager = LinearLayoutManager(context)
        }
        rvPlanList.setListener(object : SwipeLeftRightCallback.Listener {
            override fun onSwipedLeft(position: Int) {
                val workout = workoutAdapter.getWorkoutPosition(position)
                setupRenameWorkoutDialog(workout)
                toast("Edit")
            }
            override fun onSwipedRight(position: Int) {
                val workout = workoutAdapter.getWorkoutPosition(position)
                viewModel.deleteWorkout(workout).also {
                    collectObservers()
                    toast(getString(R.string.message_delete_workout))
                }
            }
        })
    }

    private fun collectObservers() = with(binding) {
        emptyList.show()
        viewModel.safeFetch()
        viewModel.workoutPlanList.observe(viewLifecycleOwner){
            val workoutPlanNameList: ArrayList<String> = arrayListOf()
            for (item in it){
                workoutPlanNameList.add(item.workoutPlan.name)
            }
            if (workoutPlanNameList.isNotEmpty() && homeSelectDropdownText.text.isEmpty()){
                homeSelectDropdownText.setText(it[0].workoutPlan.name)
                viewModel.getCurrentWorkoutPlan(it[0].workoutPlan.name)
            }
            val equipmentAdapter = ArrayAdapter(requireContext(), R.layout.menu_filter_item, workoutPlanNameList)
            homeSelectDropdownText.setAdapter(equipmentAdapter)
            homeSelectDropdownText.setOnItemClickListener { _, _, id, _ ->
                viewModel.getCurrentWorkoutPlan(homeSelectDropdownText.text.toString())
            }
            viewModel.currentWorkoutPlan.observe(viewLifecycleOwner){ currentWorkoutPlan->
                if (currentWorkoutPlan.workoutList.isEmpty()){
                    workoutAdapter.workoutList = emptyList()
                    emptyList.show()
                }else{
                    workoutAdapter.workoutList = currentWorkoutPlan.workoutList.toList()
                    emptyList.gone()
                }
            }
        }
    }

    private fun setupOptionBtn() = with(binding) {
        optionBtn.setOnClickListener {
            showMenu(it, R.menu.popup_home_page_menu)
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)
        popup.show()
        popup.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.option_1 -> {
                    setupWorkoutPlanDialog()
                    true
                }
                R.id.option_2 -> {
                    setupWorkoutDialog()
                    true
                }
                R.id.option_3 -> {
                    true
                }
                R.id.option_4 -> {
                    true
                }
                R.id.option_5 -> {
                    deleteWorkoutPlan()
                    true
                }
                else -> super.onContextItemSelected(item)
            }
        }
    }

    private fun deleteWorkoutPlan() {
        viewModel.deleteWorkoutPlan()
        findNavController().navigate(R.id.homeFragment)
        collectObservers()
    }

    private fun checkUser(){
        viewModel.user.observe(viewLifecycleOwner){ user ->
            val navController = findNavController()
            val navBar = activity?.findViewById<BottomNavigationView>(R.id.bottomAppBar)
            if (user != null) {
                navBar?.show()
            } else {
                navBar?.gone()
                navController.navigate(R.id.signInFragment2)
            }
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    //DIALOGS
    private fun setupWorkoutPlanDialog(){
        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.ThemeOverlay_App_MaterialAlertDialog)
        val dialogLayout = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val editText = dialogLayout.findViewById<TextInputEditText>(R.id.nameInputLayoutText)

        with(builder){
            setTitle(R.string.create_workout_plan)
            setPositiveButton("Ok"){dialog, which ->
                if (editText.text.toString().isEmpty()){
                    toast(getString(R.string.empty_text))
                }else{
                    binding.homeSelectDropdownText.text = editText.text
                    viewModel.createWorkoutPlan(editText.text.toString())
                    viewModel.getCurrentWorkoutPlan(editText.text.toString())
                }
                collectObservers()
            }
            setNegativeButton("Cancel"){_, _ -> }
            setView(dialogLayout)
            show()
        }
    }

    private fun setupWorkoutDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext(),R.style.ThemeOverlay_App_MaterialAlertDialog)
        val dialogLayout = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val editText = dialogLayout.findViewById<TextInputEditText>(R.id.nameInputLayoutText)
        with(builder){
            setTitle(R.string.create_exercise_list)
            setPositiveButton("Ok"){_, _ ->
                if (editText.text.toString().isEmpty()){
                    toast(getString(R.string.empty_text))
                }else if(binding.homeSelectDropdownText.text.isEmpty()){
                    toast(getString(R.string.empty_workout_plan_list))
                } else{
                    viewModel.createWorkout(editText.text.toString(), viewModel.currentWorkoutPlan.value!!.workoutPlan.id)
                }
                collectObservers()
            }
            setNegativeButton("Cancel"){_, _ -> }
            setView(dialogLayout)
            show()
        }
    }

    private fun setupRenameWorkoutDialog(workout: WorkoutModel) {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
        val dialogLayout = layoutInflater.inflate(R.layout.custom_dialog_layout, null)
        val editText = dialogLayout.findViewById<TextInputEditText>(R.id.nameInputLayoutText)
        with(builder){
            setTitle(R.string.edit_workout)
            setPositiveButton("Ok"){_, _ ->
                if (editText.text!!.isEmpty()){
                    toast(getString(R.string.empty_text))
                }else{
                    viewModel.editWorkout(
                        WorkoutModel(
                            workout.id,
                            editText.text.toString(),
                            viewModel.currentWorkoutPlan.value!!.workoutPlan.id,
                        )
                    )
                }
                collectObservers()
            }
            setNegativeButton("Cancel"){_, _ -> }
            setView(dialogLayout)
            show()
        }
    }
}
