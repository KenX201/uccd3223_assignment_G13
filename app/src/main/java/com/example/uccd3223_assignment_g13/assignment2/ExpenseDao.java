package com.example.assignment2;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import java.util.List;

public interface ExpenseDao {

    @Insert
    void insertExpense(Expense expense);

    @Update
    void updateExpense(Expense expense);

    @Delete
    void deleteExpense(Expense expense);

    @Query("SELECT * FROM expenses WHERE expenseID = :expenseID LIMIT 1")
    Expense getExpenseById(String expenseID);

    @Query("SELECT * FROM expenses WHERE username = :username AND time = :yearMonth")
    List<Expense> getExpensesByUserAndMonth(String username, String yearMonth);

    @Query("SELECT * FROM expenses")
    List<Expense> getAllExpenses();

}
