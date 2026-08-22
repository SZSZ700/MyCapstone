package org.example.CapstoneProject.repository.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import org.example.CapstoneProject.repository.WaterRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Firebase implementation of WaterRepository.
//
// This class contains Firebase-specific database access code for
// water-related operations.
// -------------------------------------------------------------------------
@SuppressWarnings({"unused", "ExtractMethodRecommender"})
@Repository
public class FirebaseWaterRepository implements WaterRepository {

    // Hold a reference to the Users node in Firebase.
    private final DatabaseReference usersRef;

    // ---------------------------------------------------------------------
    // Builds the repository using the Users database reference created
    // inside FirebaseConfiguration.
    // ---------------------------------------------------------------------
    public FirebaseWaterRepository(DatabaseReference usersReference) {
        // Store the injected reference.
        this.usersRef = usersReference;
    }

    // ---------------------------------------------------------------------
    // Adds a water amount to the user's water log for today.
    //
    // Returns true when the update succeeded.
    // Returns false when the user was not found or the update failed.
    // ---------------------------------------------------------------------
    // Update the user's water log for today.
    @Override
    public CompletableFuture<Boolean> updateWater(String username, int waterAmount) {
        // Create the future to return.
        var future = new CompletableFuture<Boolean>();

        // Reject invalid water amounts.
        if (waterAmount <= 0) {
            // Complete with false.
            future.complete(false);
            // Return immediately.
            return future;
        }

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Build the date key for today.
                            var dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                            // Point to today's water log node.
                            var todayRef = child.getRef().child("waterLog").child(dayKey);

                            // Run a transaction on today's list.
                            todayRef.runTransaction(new Transaction.Handler() {
                                // Update the list atomically.
                                @Override
                                public Transaction.Result doTransaction(MutableData currentData) {
                                    // Read the existing list.
                                    List<Long> dayList = currentData.getValue(new GenericTypeIndicator<>() {
                                    });
                                    // Create a new list if missing.
                                    if (dayList == null) {
                                        dayList = new ArrayList<>();
                                    }
                                    // Add the total slot if the list is empty.
                                    if (dayList.isEmpty()) {
                                        dayList.add(0L);
                                    } else if (dayList.getFirst() == null) {
                                        // Repair the total slot if it is null.
                                        dayList.set(0, 0L);
                                    }

                                    // Read the current total.
                                    @SuppressWarnings("DataFlowIssue") long currentSum = dayList.getFirst();
                                    // Update the total at index 0.
                                    dayList.set(0, currentSum + waterAmount);
                                    // Append the new drink amount.
                                    dayList.add((long) waterAmount);
                                    // Save the updated list.
                                    currentData.setValue(dayList);
                                    // Commit the transaction.
                                    return Transaction.success(currentData);
                                }

                                // Handle transaction completion.
                                @Override
                                public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                                    // Complete with true only on committed success.
                                    future.complete(error == null && committed);
                                }
                            });
                            // Stop after the first match.
                            return;
                        }
                        // Complete with false when not found.
                        future.complete(false);
                    }

                    // Handle Firebase cancellation.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete with the Firebase exception.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future.
        return future;
    }

    // ---------------------------------------------------------------------
    // Returns today's and yesterday's water totals for a user.
    //
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<JSONObject> getWater(String username) {
        // Create the future to return.
        var future = new CompletableFuture<JSONObject>();

        // Build today's date key.
        var todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        // Create a calendar instance.
        var cal = Calendar.getInstance();
        // Move the calendar back one day.
        cal.add(Calendar.DAY_OF_YEAR, -1);
        // Build yesterday's date key.
        var yesterdayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Read today's total.
                            var todayAmt = child.child("waterLog").child(todayKey).child("0").getValue(Long.class);
                            // Read yesterday's total.
                            var yesterdayAmt = child.child("waterLog").child(yesterdayKey).child("0").getValue(Long.class);
                            // Create the JSON response.
                            var obj = new JSONObject();
                            try {
                                // Put today's water in the response.
                                obj.put("todayWater", todayAmt == null ? 0 : todayAmt);
                                // Put yesterday's water in the response.
                                obj.put("yesterdayWater", yesterdayAmt == null ? 0 : yesterdayAmt);
                            } catch (Exception e) {
                                // Complete with null on JSON failure.
                                future.complete(null);
                                // Stop immediately.
                                return;
                            }
                            // Complete with the JSON object.
                            future.complete(obj);
                            // Stop after the first match.
                            return;
                        }
                        // Complete with null when not found.
                        future.complete(null);
                    }

                    // Handle Firebase cancellation.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete with the Firebase exception.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future.
        return future;
    }

    // Get water history for the last N days.
    @Override
    public CompletableFuture<Map<String, Long>> getWaterHistoryMap(String username, int days) {
        // Create the future to return.
        var future = new CompletableFuture<Map<String, Long>>();

        // Create a list of date keys.
        List<String> keys = new ArrayList<>();
        // Create the formatter for day keys.
        var sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        // Create a calendar instance.
        var cal = Calendar.getInstance();
        // Build the list of requested days.
        for (var i = 0; i < days; i++) {
            // Add the current day key.
            keys.add(sdf.format(cal.getTime()));
            // Move one day back.
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Create the result map.
                            Map<String, Long> result = new LinkedHashMap<>();
                            try {
                                // Loop over all requested keys.
                                for (String key : keys) {
                                    // Read the amount for that day.
                                    var amt = child.child("waterLog").child(key).child("0").getValue(Long.class);
                                    // Put the amount into the result map.
                                    result.put(key, amt == null ? 0 : amt);
                                }
                                // Complete with the result map.
                                future.complete(result);
                            } catch (Exception e) {
                                // Complete with null on failure.
                                future.complete(null);
                            }
                            // Stop after the first match.
                            return;
                        }
                        // Complete with null when not found.
                        future.complete(null);
                    }

                    // Handle Firebase cancellation.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete with the Firebase exception.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future.
        return future;
    }

    // Get weekly water averages for four weeks.
    @Override
    public CompletableFuture<Map<String, Integer>> getWeeklyAverages(String username) {
        // Create the future to return.
        var future = new CompletableFuture<Map<String, Integer>>();

        // Create the formatter for day keys.
        var sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        // Create a calendar instance.
        var cal = Calendar.getInstance();
        // Hold the last 28 day keys.
        List<String> last28 = new ArrayList<>(28);
        // Build the last 28 keys.
        for (var i = 0; i < 28; i++) {
            // Add the current key.
            last28.add(sdf.format(cal.getTime()));
            // Move one day back.
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                // Hold totals per week.
                                var sums = new long[4];
                                // Hold counts per week.
                                var counts = new int[4];
                                // Loop over all 28 days.
                                for (int i = 0; i < 28; i++) {
                                    // Read the date key at this index.
                                    var dateKey = last28.get(i);
                                    // Map the day to its week bucket.
                                    var weekIdx = i / 7;
                                    // Read the water total for the day.
                                    var amt = child.child("waterLog").child(dateKey).child("0").getValue(Long.class);
                                    // Add the amount if it exists.
                                    if (amt != null) {
                                        sums[weekIdx] += amt;
                                        counts[weekIdx] += 1;
                                    }
                                }

                                // Create the output map.
                                Map<String, Integer> out = new LinkedHashMap<>();
                                // Build four weekly averages.
                                for (int w = 0; w < 4; w++) {
                                    // Compute the average for this week.
                                    var avg = (counts[w] > 0) ? (int) (sums[w] / counts[w]) : 0;
                                    // Store the week label and value.
                                    out.put("Week " + (4 - w), avg);
                                }
                                // Complete with the output map.
                                future.complete(out);
                            } catch (Exception e) {
                                // Complete with an empty map on failure.
                                future.complete(Collections.emptyMap());
                            }
                            // Stop after the first match.
                            return;
                        }
                        // Complete with an empty map when not found.
                        future.complete(Collections.emptyMap());
                    }

                    // Handle Firebase cancellation.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete with the Firebase exception.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future.
        return future;
    }

    // Read the user's daily drink goal.
    @Override
    public CompletableFuture<Integer> getGoalMl(String username) {
        // Create the future to return.
        var fut = new CompletableFuture<Integer>();

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Read the goal value.
                            var goal = child.child("goalMl").getValue(Integer.class);
                            // Complete with the goal or the default.
                            fut.complete(goal != null ? goal : 3000);
                            // Stop after the first match.
                            return;
                        }
                        // Complete with the default when not found.
                        fut.complete(3000);
                    }

                    // Handle Firebase cancellation.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete with the Firebase exception.
                        fut.completeExceptionally(error.toException());
                    }
                });

        // Return the future.
        return fut;
    }

    // Update the user's daily drink goal.
    @Override
    public CompletableFuture<Boolean> updateGoalMl(String username, int goalMl) {
        // Create the future to return.
        var fut = new CompletableFuture<Boolean>();

        // Reject invalid goal values.
        if (goalMl < 500 || goalMl > 10000) {
            // Complete with false.
            fut.complete(false);
            // Return immediately.
            return fut;
        }

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Update the goal field.
                            child.getRef().child("goalMl").setValue(goalMl, (err, ref) -> fut.complete(err == null));
                            // Stop after the first match.
                            return;
                        }
                        // Complete with false when not found.
                        fut.complete(false);
                    }

                    // Handle Firebase cancellation.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete with the Firebase exception.
                        fut.completeExceptionally(error.toException());
                    }
                });

        // Return the future.
        return fut;
    }


}