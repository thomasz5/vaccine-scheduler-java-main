package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {

        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // Validate the number of tokens
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            currentPatient = patient;
            System.out.println("Logged in as " + username);
        }

    }

    private static void loginCaregiver(String[] tokens) {

        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];  // date in format YYYY-MM-DD

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Query available caregivers for the date.
            String queryCaregivers = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
            PreparedStatement psCaregivers = con.prepareStatement(queryCaregivers);
            psCaregivers.setString(1, date);
            ResultSet rsCaregivers = psCaregivers.executeQuery();

            System.out.println("Caregivers:");
            boolean caregiverFound = false;
            while (rsCaregivers.next()) {
                caregiverFound = true;
                System.out.println(rsCaregivers.getString("Username"));
            }
            if (!caregiverFound) {
                System.out.println("No caregivers available");
            }

            // Query all vaccines and their doses.
            String queryVaccines = "SELECT Name, Doses FROM Vaccines";
            PreparedStatement psVaccines = con.prepareStatement(queryVaccines);
            ResultSet rsVaccines = psVaccines.executeQuery();

            System.out.println("Vaccines:");
            boolean vaccineFound = false;
            while (rsVaccines.next()) {
                vaccineFound = true;
                System.out.println(rsVaccines.getString("Name") + " " + rsVaccines.getInt("Doses"));
            }
            if (!vaccineFound) {
                System.out.println("No vaccines available");
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        // Only patients can reserve.
        if (currentPatient == null) {
            System.out.println("Please login as a patient");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Get the available caregiver for the date (alphabetical order).
            String queryCaregiver = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
            PreparedStatement psCaregiver = con.prepareStatement(queryCaregiver);
            psCaregiver.setString(1, date);
            ResultSet rsCaregiver = psCaregiver.executeQuery();
            String caregiverUsername = null;
            if (rsCaregiver.next()) {
                caregiverUsername = rsCaregiver.getString("Username");
            }
            if (caregiverUsername == null) {
                System.out.println("No caregiver is available");
                return;
            }

            // Check if the vaccine exists and has at least one dose.
            String queryVaccine = "SELECT Doses FROM Vaccines WHERE Name = ?";
            PreparedStatement psVaccine = con.prepareStatement(queryVaccine);
            psVaccine.setString(1, vaccineName);
            ResultSet rsVaccine = psVaccine.executeQuery();
            int doses = 0;
            if (rsVaccine.next()) {
                doses = rsVaccine.getInt("Doses");
            } else {
                // Vaccine does not exist.
                System.out.println("Not enough available doses");
                return;
            }
            if (doses < 1) {
                System.out.println("Not enough available doses");
                return;
            }

            // Insert the reservation.
            String insertReservation = "INSERT INTO Reservations (patient_username, caregiver_username, vaccine_name, appointment_date) VALUES (?, ?, ?, ?)";
            PreparedStatement psInsert = con.prepareStatement(insertReservation, Statement.RETURN_GENERATED_KEYS);
            psInsert.setString(1, currentPatient.getUsername());
            psInsert.setString(2, caregiverUsername);
            psInsert.setString(3, vaccineName);
            psInsert.setString(4, date);
            int affectedRows = psInsert.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Please try again");
                return;
            }
            ResultSet rsKey = psInsert.getGeneratedKeys();
            int reservationID = -1;
            if (rsKey.next()) {
                reservationID = rsKey.getInt(1);
            }

            // Remove the caregiver's availability for that date.
            String deleteAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
            PreparedStatement psDelete = con.prepareStatement(deleteAvailability);
            psDelete.setString(1, date);
            psDelete.setString(2, caregiverUsername);
            psDelete.executeUpdate();

            // Decrement the vaccine doses.
            String updateVaccine = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
            PreparedStatement psUpdate = con.prepareStatement(updateVaccine);
            psUpdate.setString(1, vaccineName);
            psUpdate.executeUpdate();

            System.out.println("Appointment ID " + reservationID + ", Caregiver username " + caregiverUsername);
        } catch (SQLException e) {
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {

        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
        }
    }

    private static void cancel(String[] tokens) {
        // Check if a user is logged in.
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        // Validate token length.
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        int appointmentID;
        try {
            appointmentID = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            System.out.println("Please try again");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Retrieve the appointment details.
            String query = "SELECT appointment_date, vaccine_name, caregiver_username FROM Reservations WHERE reservation_id = ?";
            PreparedStatement psQuery = con.prepareStatement(query);
            psQuery.setInt(1, appointmentID);
            ResultSet rs = psQuery.executeQuery();
            if (!rs.next()) {
                System.out.println("Appointment ID " + appointmentID + " does not exist");
                return;
            }
            String appointmentDate = rs.getString("appointment_date");
            String vaccineName = rs.getString("vaccine_name");
            String caregiverUsername = rs.getString("caregiver_username");

            // Begin transaction.
            con.setAutoCommit(false);

            // Delete the appointment.
            String delete = "DELETE FROM Reservations WHERE reservation_id = ?";
            PreparedStatement psDelete = con.prepareStatement(delete);
            psDelete.setInt(1, appointmentID);
            psDelete.executeUpdate();

            String updateVaccine = "UPDATE Vaccines SET Doses = Doses + 1 WHERE Name = ?";
            PreparedStatement psUpdate = con.prepareStatement(updateVaccine);
            psUpdate.setString(1, vaccineName);
            psUpdate.executeUpdate();


            String insertAvailability = "INSERT OR IGNORE INTO Availabilities (Time, Username) VALUES (?, ?)";
            PreparedStatement psInsert = con.prepareStatement(insertAvailability);
            psInsert.setString(1, appointmentDate);
            psInsert.setString(2, caregiverUsername);
            psInsert.executeUpdate();

            con.commit();
            System.out.println("Appointment ID " + appointmentID + " has been successfully canceled");
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                // Ignore rollback exception.
            }
            System.out.println("Please try again");
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                // Ignore.
            }
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
        }

        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        } else {
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            String query;
            PreparedStatement ps;
            if (currentPatient != null) {
                // For patients: show appointment id, vaccine name, date, and caregiver username.
                query = "SELECT reservation_id, vaccine_name, appointment_date, caregiver_username FROM Reservations WHERE patient_username = ? ORDER BY reservation_id ASC";
                ps = con.prepareStatement(query);
                ps.setString(1, currentPatient.getUsername());
            } else {
                // For caregivers: show appointment id, vaccine name, date, and patient username.
                query = "SELECT reservation_id, vaccine_name, appointment_date, patient_username FROM Reservations WHERE caregiver_username = ? ORDER BY reservation_id ASC";
                ps = con.prepareStatement(query);
                ps.setString(1, currentCaregiver.getUsername());
            }
            ResultSet rs = ps.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                int id = rs.getInt("reservation_id");
                String vaccine = rs.getString("vaccine_name");
                String appointmentDate = rs.getString("appointment_date");
                String otherUser;
                if (currentPatient != null) {
                    otherUser = rs.getString("caregiver_username");
                } else {
                    otherUser = rs.getString("patient_username");
                }
                System.out.println(id + " " + vaccine + " " + appointmentDate + " " + otherUser);
            }
            if (!found) {
                System.out.println("No appointments scheduled");
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }

    }

    private static void logout(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Successfully logged out");
    }

    private static boolean isStrongPassword(String password) {
        // At least 8 characters, at least one lowercase, one uppercase, one digit,
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#?]).{8,}$");
    }
}
