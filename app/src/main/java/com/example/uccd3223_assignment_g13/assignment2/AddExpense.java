package com.example.assignment2;


import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.room.Room;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.widget.TextView;

import com.example.assignment2.databinding.ActivityAddExpenseBinding;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

public class AddExpense extends AppCompatActivity {

    ActivityAddExpenseBinding binding;
    private String type;
    private ExpenseModel expenseModel;
    private List<String> categoryList;
    private EditText editTextDate;
    private AppDatabase db;
    private Calendar calendar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_add_expense);

        calendar = Calendar.getInstance();

        db = AppDatabase.getDatabase(getApplicationContext());

        // Initialize category list
        categoryList = new ArrayList<>();
        categoryList.add("Student Bill");
        categoryList.add("Collision");
        categoryList.add("Food");
        categoryList.add("Electricity");
        categoryList.add("Water");
        categoryList.add("Entertainment");
        categoryList.add("Grocery");
        categoryList.add("Injure");
        categoryList.add("Learning");
        categoryList.add("Pet");
        categoryList.add("Sport");
        categoryList.add("Transportation");
        categoryList.add("Girlfriend");
        categoryList.add("Other");

        // Get intent extras
        type = getIntent().getStringExtra("type");
        expenseModel = (ExpenseModel) getIntent().getSerializableExtra("model");

        // Initialize UI elements
        binding.category.setOnClickListener(v -> showCategoryListDialog());

        if (type == null && expenseModel != null) {
            binding.amount.setText(String.valueOf(expenseModel.getAmount()));
            binding.category.setText(expenseModel.getCategory());
            binding.note.setText(expenseModel.getNote());
            // Initialize date field if available
            binding.editTextDate.setText(expenseModel.getDate());
        }

        // Initialize the Date EditText
        editTextDate = binding.editTextDate;

        // Set up the DatePickerDialog
        editTextDate.setOnClickListener(v -> showDatePickerDialog());

        // Set click listener for the Save button
        TextView saveExpenseTextView = findViewById(R.id.saveExpense);
        saveExpenseTextView.setOnClickListener(v -> {
            int id = v.getId();

            if (id == R.id.saveExpense) {
                if (type != null) {
                    createExpense();
                } else {
                    updateExpense();
                }
            }
        });

        // Set click listener for the Delete button
        TextView deleteExpenseTextView = findViewById(R.id.deleteExpense);
        deleteExpenseTextView.setOnClickListener(v -> {
            int id = v.getId();
            if (id == R.id.deleteExpense) {
                deleteExpense();
            }
        });

    }

    private void showCategoryListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Category");

        String[] categories = categoryList.toArray(new String[0]);

        builder.setItems(categories, (dialog, which) -> {
            String selectedCategory = categoryList.get(which);
            binding.category.setText(selectedCategory);
            dialog.dismiss();
        });

        builder.show();
    }

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddExpense.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedMonth += 1; // Months are indexed from 0
                    String date = selectedYear + "-" +
                            (selectedMonth < 10 ? "0" + selectedMonth : selectedMonth) + "-" +
                            (selectedDay < 10 ? "0" + selectedDay : selectedDay);
                    editTextDate.setText(date);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void deleteExpense() {
        if (expenseModel == null || expenseModel.getExpenseID() == null) {
            showToast("Expense data is missing");
            return;
        }

        String expenseID = expenseModel.getExpenseID();

        // Delete from Firestore
        FirebaseFirestore
                .getInstance()
                .collection("expense")
                .document(expenseID)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete from Room
                    new Thread(() -> {
                        Expense expense = db.expenseDao().getExpenseById(expenseID);
                        if (expense != null) {
                            db.expenseDao().deleteExpense(expense);
                        }
                    }).start();

                    updateTotalExpensesAndUI();
                    updateRemainingBudget();
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to delete expense: " + e.getMessage()));
    }

    private void updateRemainingBudget() {

    }

    private void showToast(String expenseDataIsMissing) {
    }

    private void createExpense() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = prefs.getString("userName", "");
        String expenseID = UUID.randomUUID().toString();
        String note = binding.note.getText().toString();
        String categoryName = binding.category.getText().toString();
        String date = binding.editTextDate.getText().toString();
        double amount;

        try {
            amount = Double.parseDouble(binding.amount.getText().toString());
        } catch (NumberFormatException e) {
            binding.amount.setError("Invalid amount");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String yearMonth = sdf.format(new Date());

        if (amount <= 0) {
            binding.amount.setError("Invalid amount");
            return;
        }
        if (categoryName.isEmpty()) {
            showToast("Please select a category");
            return;
        }
        if (date.isEmpty()) {
            showToast("Please select a date");
            return;
        }

        // Create a map to represent the expense data for Firestore
        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("username", username);
        expenseData.put("expenseID", expenseID);
        expenseData.put("note", note);
        expenseData.put("category", categoryName);
        expenseData.put("amount", amount);
        expenseData.put("date", date);
        expenseData.put("time", yearMonth);
        expenseData.put("uid", FirebaseAuth.getInstance().getUid());

        // Add the expense data to the "expense" collection in Firestore
        FirebaseFirestore.getInstance()
                .collection("expense")
                .document(expenseID)
                .set(expenseData)
                .addOnSuccessListener(aVoid -> {
                    // Save to Room in a separate thread
                    new Thread(() -> {
                        Expense expense = new Expense(
                                expenseID,
                                username,
                                note,
                                categoryName,
                                amount,
                                date,
                                yearMonth,
                                FirebaseAuth.getInstance().getUid()
                        );
                        db.expenseDao().insertExpense(expense);
                    }).start();

                    updateTotalExpensesAndUI();
                    updateRemainingBudget();
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to save expense: " + e.getMessage()));
    }

    private void updateExpense() {
        if (expenseModel == null || expenseModel.getExpenseID() == null) {
            showToast("Expense data is missing");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = prefs.getString("userName", "");
        String expenseID = expenseModel.getExpenseID();
        String note = binding.note.getText().toString();
        String categoryName = binding.category.getText().toString();
        String date = binding.editTextDate.getText().toString();
        double amount;

        try {
            amount = Double.parseDouble(binding.amount.getText().toString());
        } catch (NumberFormatException e) {
            binding.amount.setError("Invalid amount");
            return;
        }

        if (amount <= 0) {
            binding.amount.setError("Invalid amount");
            return;
        }
        if (categoryName.isEmpty()) {
            showToast("Please select a category");
            return;
        }
        if (date.isEmpty()) {
            showToast("Please select a date");
            return;
        }

        // Create a map to represent the updated expense data for Firestore
        Map<String, Object> updatedExpenseData = new HashMap<>();
        updatedExpenseData.put("username", username);
        updatedExpenseData.put("expenseID", expenseID);
        updatedExpenseData.put("note", note);
        updatedExpenseData.put("category", categoryName);
        updatedExpenseData.put("amount", amount);
        updatedExpenseData.put("date", date);
        updatedExpenseData.put("time", expenseModel.getTime());
        updatedExpenseData.put("uid", FirebaseAuth.getInstance().getUid());

        // Update the expense data in the "expense" collection in Firestore
        FirebaseFirestore.getInstance()
                .collection("expense")
                .document(expenseID)
                .set(updatedExpenseData)
                .addOnSuccessListener(aVoid -> {
                    // Update Room in a separate thread
                    new Thread(() -> {
                        Expense expense = db.expenseDao().getExpenseById(expenseID);
                        if (expense != null) {
                            expense.setNote(note);
                            expense.setCategory(categoryName);
                            expense.setAmount(amount);
                            expense.setDate(date);
                            // time and uid remain unchanged
                            db.expenseDao().updateExpense(expense);
                        }
                    }).start();

                    updateTotalExpensesAndUI();
                    updateRemainingBudget();
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to update expense: " + e.getMessage()));
    }

    private void updateTotalExpensesAndUI() {
        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
        String username = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("userName", "");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String yearMonth = sdf.format(new Date());

        // Initialize a map to store category-wise expenses
        Map<String, Double> categoryExpenses = new HashMap<>();

        // Get the total expenses for the current month from Firestore
        dbFirestore.collection("expense")
                .whereEqualTo("username", username)
                .whereEqualTo("time", yearMonth)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Process each expense document
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        ExpenseModel expenseModel = documentSnapshot.toObject(ExpenseModel.class);

                        // Sum up expenses for each category
                        String category = expenseModel.getCategory();
                        double expenseAmount = expenseModel.getAmount();

                        categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0D) + expenseAmount);
                    }

                    // Save the total expenses to the "Total expenses" collection
                    saveTotalExpensesToFirestore(categoryExpenses);
                })
                .addOnFailureListener(e -> {
                    // Handle failure to retrieve expenses from Firestore
                    showToast("Failed to retrieve expenses: " + e.getMessage());
                });
    }

    private void saveTotalExpensesToFirestore(Map<String, Double> categoryExpenses) {
        // Calculate total expenses by summing up all category expenses
        double totalExpenses = 0.0;
        for (double expense : categoryExpenses.values()) {
            totalExpenses += expense;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String username = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("userName", "");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String yearMonth = sdf.format(new Date());

        // Create a document ID for the "Total expenses" collection
        String documentId = username + "_" + yearMonth;

        // Create a map to represent the data to be saved
        Map<String, Object> dataToSave = new HashMap<>();
        dataToSave.put("username", username);
        dataToSave.put("totalExpenses", totalExpenses);
        dataToSave.putAll(categoryExpenses); // Add category-wise expenses to the data

        // Save the total and category-wise expenses to the "Total expenses" collection
        db.collection("Total expenses")
                .document(documentId)
                .set(dataToSave)
                .addOnSuccessListener(aVoid -> showToast("Total and category-wise expenses saved successfully"))
                .addOnFailureListener(e -> showToast("Failed to save total and category-wise expenses: " + e.getMessage()));
    }

}