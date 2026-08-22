package org.example.CapstoneProject.repository.firebase;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.example.CapstoneProject.model.User;
import org.example.CapstoneProject.repository.UserRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// -------------------------------------------------------------------------
// Firebase implementation of UserRepository.
//
// This class contains Firebase-specific database access code.
// The service layer will depend on UserRepository and will not need to know
// how Firebase performs the query.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
@Repository
public class FirebaseUserRepository implements UserRepository {

    // Hold a reference to the Users node in Firebase.
    private final DatabaseReference usersRef;

    // ---------------------------------------------------------------------
    // Builds the repository using the Users database reference created
    // inside FirebaseConfiguration.
    // ---------------------------------------------------------------------
    public FirebaseUserRepository(DatabaseReference usersReference) {
        // Store the injected reference.
        this.usersRef = usersReference;
    }

    // ---------------------------------------------------------------------
    // Finds a user by username.
    //
    // Returns the first matching user.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<User> findByUsername(String username) {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<User>();

        // Query the Users node by the userName field.
        usersRef.orderByChild("userName").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        // Loop over matching users.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Convert the Firebase snapshot
                            // into a User object.
                            User user = child.getValue(User.class);
                            // Complete the future with the user.
                            future.complete(user);
                            // Stop after the first match.
                            return;
                        }

                        // Complete with null when no user was found.
                        future.complete(null);
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete the future exceptionally.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Returns all users stored in Firebase.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<List<User>> findAll() {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<List<User>>();

        // Read all users from Firebase once.
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {

            // Handle the returned Firebase data.
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                // Create the result list.
                List<User> users = new ArrayList<>();

                // Loop over all users stored under the Users node.
                for (DataSnapshot child : snapshot.getChildren()) {
                    // Convert the Firebase snapshot into a User object.
                    User user = child.getValue(User.class);

                    // Add only valid user objects to the result list.
                    if (user != null) { users.add(user); }
                }

                // Complete the future with the complete user list.
                future.complete(users);
            }

            // Handle Firebase read failure.
            @Override
            public void onCancelled(DatabaseError error) {
                // Complete the future exceptionally.
                future.completeExceptionally(error.toException());
            }
        });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Checks whether a user exists by username.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> existsByUsername(String username) {

        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<Boolean>();

        // Query the Users node by the userName field.
        usersRef.orderByChild("userName").equalTo(username).addListenerForSingleValueEvent(
                new ValueEventListener() {

                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        // Loop over matching users.
                        for (DataSnapshot ignored : snapshot.getChildren()) {
                            // A matching user exists.
                            future.complete(true);

                            // Stop after the first match.
                            return;
                        }

                        // No matching user exists.
                        future.complete(false);
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete the future exceptionally.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Deletes a user by username.
    //
    // Returns true when the user was found and deleted.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> deleteByUsername(String username) {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<Boolean>();

        // Query the Users node by the userName field.
        usersRef.orderByChild("userName")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matching users.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Remove the matching user from Firebase.
                            child.getRef().removeValue((error, reference) -> {
                                // Complete with true when the deletion succeeded.
                                future.complete(error == null);
                            });

                            // Stop after the first match.
                            return;
                        }

                        // Complete with false when no user was found.
                        future.complete(false);
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete the future exceptionally.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Updates the editable fields of an existing user by username while
    // preserving the user's existing health and water-related data.
    //
    // Returns the updated user when the operation succeeds.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<User> updateByUsername(
            String username,
            User updatedUser) {

        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<User>();

        // Query the Users node by the userName field.
        usersRef.orderByChild("userName")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        // Loop over matching users.
                        for (DataSnapshot child : snapshot.getChildren()) {

                            // Read the complete existing user from Firebase.
                            var existingUser = child.getValue(User.class);

                            // If Firebase could not convert the stored data
                            // into a User object, treat the operation as failed.
                            if (existingUser == null) {
                                future.complete(null);
                                return;
                            }

                            // Keep the original username unchanged.
                            existingUser.setUserName(username);

                            // Update only the editable fields received
                            // from the PUT request.
                            existingUser.setPassword(updatedUser.getPassword());
                            existingUser.setFullName(updatedUser.getFullName());
                            existingUser.setAge(updatedUser.getAge());

                            // Save the complete user object back to Firebase.
                            //
                            // Existing values such as BMI, calories, water log,
                            // and daily goal remain unchanged.
                            child.getRef().setValue(
                                    existingUser,
                                    (error, reference) -> {

                                        // Complete exceptionally if Firebase
                                        // reports an error while saving.
                                        if (error != null) {
                                            future.completeExceptionally(
                                                    error.toException()
                                            );
                                            return;
                                        }

                                        // Return the complete updated user.
                                        future.complete(existingUser);
                                    }
                            );

                            // Stop after the first matching user.
                            return;
                        }

                        // Complete with null when no matching user was found.
                        future.complete(null);
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {

                        // Complete the future exceptionally.
                        future.completeExceptionally(
                                error.toException()
                        );
                    }
                });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Partially updates an existing user by username.
    //
    // Returns the updated user when the user was found.
    // Returns null when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<User> patchByUsername(String username, Map<String, Object> updates) {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<User>();

        // Query the Users node by the userName field.
        usersRef.orderByChild("userName")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matching users.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Keep a reference to the matched user node.
                            var userRef = child.getRef();

                            // Copy the incoming updates.
                            var safeUpdates = new HashMap<>(updates);

                            // Prevent username changes.
                            safeUpdates.remove("userName");

                            // If no fields remain to update, return the current user.
                            if (safeUpdates.isEmpty()) {
                                future.complete(child.getValue(User.class));
                                return;
                            }

                            // Apply the partial update.
                            userRef.updateChildren(safeUpdates, (error, reference) -> {
                                // Check whether the update failed.
                                if (error != null) {
                                    future.completeExceptionally(error.toException());
                                    return;
                                }

                                // Read the updated user again.
                                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot updatedSnapshot) {
                                        // Complete with the updated user object.
                                        future.complete(updatedSnapshot.getValue(User.class));
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        // Complete the future exceptionally.
                                        future.completeExceptionally(error.toException());
                                    }
                                });
                            });

                            // Stop after the first match.
                            return;
                        }

                        // Complete with null when no user was found.
                        future.complete(null);
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete the future exceptionally.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future immediately.
        return future;
    }


    // ---------------------------------------------------------------------
    // Creates a new user.
    //
    // Returns true when the user was created successfully.
    // Returns false when the username is invalid or already exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> create(User user) {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<Boolean>();

        // Extract the username safely.
        var username = user != null ? user.getUserName() : null;

        // Reject an invalid username.
        if (username == null || username.isBlank()) {
            future.complete(false);
            return future;
        }

        // Query Firebase to check whether the username already exists.
        usersRef.orderByChild("userName")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matching users.
                        for (DataSnapshot ignored : snapshot.getChildren()) {
                            // A matching username already exists.
                            future.complete(false);
                            return;
                        }

                        // Insert the new user into Firebase.
                        usersRef.push().setValue(user, (error, reference) -> {
                            // Complete with true when the write succeeded.
                            future.complete(error == null);
                        });
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete the future exceptionally.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future immediately.
        return future;
    }


    // ---------------------------------------------------------------------
    // Inserts a new user into Firebase.
    //
    // Returns the same result messages used by the original FirebaseService.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<String> insert(User user) {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<String>();

        // Insert a new user node.
        usersRef.push().setValue(user, (error, ref) -> {
            // Check if the write succeeded.
            if (error == null) {
                // Complete with a success message.
                future.complete("User created successfully");
            } else {
                // Complete with the Firebase error message.
                future.complete("Error: " + error.getMessage());
            }
        });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Returns all users that match the provided username.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<List<User>> findAllByUsername(String username) {
        // Create the future that will hold the asynchronous result.
        var future = new CompletableFuture<List<User>>();

        // Query the Users node by the userName field.
        usersRef.orderByChild("userName")
                .equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle the returned Firebase data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Create the result list.
                        List<User> users = new ArrayList<>();

                        // Loop over all matching users.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Convert the snapshot into a User object.
                            User user = child.getValue(User.class);

                            // Add only valid users to the result list.
                            if (user != null) {
                                users.add(user);
                            }
                        }

                        // Complete the future with all matching users.
                        future.complete(users);
                    }

                    // Handle Firebase query failure.
                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Complete the future exceptionally.
                        future.completeExceptionally(error.toException());
                    }
                });

        // Return the future immediately.
        return future;
    }

    // ---------------------------------------------------------------------
    // Updates the BMI value of a user.
    //
    // Returns true when the update succeeded.
    // Returns false when no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> updateBmi(String username, double bmi) {
        // Create the future to return.
        var future = new CompletableFuture<Boolean>();

        // Query by username.
        usersRef.orderByChild("userName").equalTo(username)
                // Read one time.
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    // Handle returned data.
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // Loop over matches.
                        for (DataSnapshot child : snapshot.getChildren()) {
                            // Update the BMI value.
                            child.getRef().child("bmi").setValue(bmi, (error, ref) -> future.complete(error == null));
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
    // Returns the calories value of a user.
    //
    // Returns zero when no value or matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Integer> getCalories(String username) {
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
                            // Read the calories value.
                            var cals = child.child("calories").getValue(Integer.class);
                            // Complete with the calories value or zero.
                            fut.complete(cals != null ? cals : 0);
                            // Stop after the first match.
                            return;
                        }
                        // Complete with zero when not found.
                        fut.complete(0);
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

    // ---------------------------------------------------------------------
    // Updates the calories value of a user.
    //
    // Returns true when the update succeeded.
    // Returns false when the value is invalid or no matching user exists.
    // ---------------------------------------------------------------------
    @Override
    public CompletableFuture<Boolean> updateCalories(String username, int calories) {
        // Create the future to return.
        var fut = new CompletableFuture<Boolean>();

        // Reject invalid calorie values.
        if (calories < 0 || calories > 20000) {
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
                            // Update the calories field.
                            child.getRef().child("calories").setValue(calories, (err, ref) -> fut.complete(err == null));
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

    // ---------------------------------------------------------------------
    // Returns the global BMI distribution for all users.
    //
    // Reads all users from Firebase and counts how many users belong
    // to each BMI category.
    // ---------------------------------------------------------------------
    // Build the global BMI distribution.
    public CompletableFuture<Map<String, Integer>> getBmiDistribution() {
        // Create the future to return.
        var future = new CompletableFuture<Map<String, Integer>>();

        // Read all users once.
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            // Handle returned data.
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Count underweight users.
                var underweight = 0;
                // Count normal users.
                var normal = 0;
                // Count overweight users.
                var overweight = 0;
                // Count obese users.
                var obese = 0;

                // Loop over all users.
                for (var child : snapshot.getChildren()) {
                    // Read the bmi value.
                    var bmi = child.child("bmi").getValue(Double.class);
                    // Skip users with no bmi.
                    if (bmi == null) {
                        continue;
                    }

                    // Convert the BMI to a primitive double.
                    @SuppressWarnings("UnnecessaryUnboxing") var value = bmi.doubleValue();
                    // Count the user in the correct bucket.
                    if (value < 18.5) {
                        underweight++;
                    } else if (value < 25.0) {
                        normal++;
                    } else if (value < 30.0) {
                        overweight++;
                    } else {
                        obese++;
                    }
                }

                // Create the output map.
                Map<String, Integer> distribution = new LinkedHashMap<>();
                // Store the underweight count.
                distribution.put("Underweight", underweight);
                // Store the normal count.
                distribution.put("Normal", normal);
                // Store the overweight count.
                distribution.put("Overweight", overweight);
                // Store the obese count.
                distribution.put("Obese", obese);
                // Complete with the distribution map.
                future.complete(distribution);
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
}