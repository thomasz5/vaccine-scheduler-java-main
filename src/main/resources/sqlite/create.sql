CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

-- CREATE TABLE Availabilities (
--     Time date,
--     Username varchar(255) REFERENCES Caregivers,
--     PRIMARY KEY (Time, Username)
-- );

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE IF Patients (
                                        username TEXT PRIMARY KEY,
                                        salt BLOB NOT NULL,
                                        password_hash BLOB NOT NULL
);
CREATE TABLE Reservations (
                                            reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            patient_username TEXT NOT NULL,
                                            caregiver_username TEXT NOT NULL,
                                            vaccine_name TEXT NOT NULL,
                                            appointment_date TEXT NOT NULL,
                                            FOREIGN KEY (patient_username) REFERENCES Patients(username),
    FOREIGN KEY (caregiver_username) REFERENCES Caregivers(username),
    FOREIGN KEY (vaccine_name) REFERENCES Vaccines(vaccine_name)
    );

CREATE TABLE Availabilities (
    caregiver_username TEXT NOT NULL,
    available_date TEXT NOT NULL,
    PRIMARY KEY (caregiver_username, available_date),
    FOREIGN KEY (caregiver_username) REFERENCES Caregivers(username)
    );