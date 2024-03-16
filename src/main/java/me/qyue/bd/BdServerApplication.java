package me.qyue.bd;

import me.qyue.bd.utils.DateComparator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import me.qyue.bd.model.Data;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@RestController
public class BdServerApplication {

    static final String USERNAME = "qyue";
    static String PASSWORD = null;
    static final String DATABASE = "qyuepi";
    static final String URL = "jdbc:mariadb://127.0.0.1:3306/" + DATABASE;
    static Connection connection;
    static String ERROR_MSG = "Something went wrong!";


    public static void main(String[] args) {
        try {
            ensureConnection();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            System.out.println("Connection unsuccessful - aborting...");
//            e.printStackTrace();
            return;
        }
        SpringApplication.run(BdServerApplication.class, args);
    }

    public static void ensureConnection() throws ClassNotFoundException, IOException, SQLException {
        try {
            connection.prepareStatement("SELECT 1").executeQuery();
        } catch (SQLException | NullPointerException e) {
            System.out.println("Database connection is closed. Trying to connect...");
            Class.forName ("org.mariadb.jdbc.Driver");
            if (PASSWORD == null) {
                System.out.println("opening file ...");
                BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                        BdServerApplication.class.getClassLoader().getResourceAsStream("database.pw"))));
                PASSWORD = br.readLine();
                br.close();
                System.out.println("closed file");
            }
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connection successful!");
        }
    }



    //
    // REST Mappings
    //

    @GetMapping("/api/birthday")
    public ResponseEntity getBirthday(@RequestParam(value = "first_name") String first_name,
                                      @RequestParam(value = "last_name") String last_name) {
        try {
            ensureConnection();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            System.out.println("Could not connect to database! Aborting...");
            return ResponseEntity.internalServerError().body(ERROR_MSG);
        }
        System.out.println("Performing Query...");
        ResultSet resultSet;
        try {
            String query = "SELECT birthday FROM birthdays WHERE first_name=? AND last_name=?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, first_name.toLowerCase());
            preparedStatement.setString(2, last_name.toLowerCase());
            resultSet = preparedStatement.executeQuery();
            System.out.println("Done!");
            if(!resultSet.next()) {
                return ResponseEntity.badRequest().body("No Data found! :(");
            }
            String res = resultSet.getString(1);
            return ResponseEntity.ok().body(new Data(first_name, last_name, res));
        } catch (SQLException e) {
//            e.printStackTrace();
            return ResponseEntity.internalServerError().body(ERROR_MSG);
        }
    }

    @GetMapping("/api/birthdays")
    public ResponseEntity<ArrayList> getBirthdays() {
        try {
            ensureConnection();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            System.out.println("Could not connect to database! Aborting...");
            ArrayList<String> l = new ArrayList<>();
            l.add(ERROR_MSG);
            return ResponseEntity.internalServerError().body(l);
        }
        System.out.println("Performing Query...");
        ResultSet resultSet;
        try {
            String query = "SELECT * FROM birthdays";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            resultSet = preparedStatement.executeQuery();
            System.out.println("Done!");
            ArrayList<Data> list = new ArrayList<>();
            while (resultSet.next()){
                list.add(new Data(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)));
            }
            list.sort(new DateComparator());
            return ResponseEntity.ok().body(list);
        } catch (SQLException e) {
            ArrayList<String> l = new ArrayList<>();
            l.add(ERROR_MSG);
            return ResponseEntity.internalServerError().body(l);
        }
    }

    @GetMapping("/api/todaysBirthdays")
    public ResponseEntity<ArrayList> getTodaysBirthdays() {
        try {
            ensureConnection();
        } catch(SQLException | IOException | ClassNotFoundException e) {
            System.out.println("Could not connect to database! Aborting...");
            ArrayList<String> l = new ArrayList<>();
            l.add(ERROR_MSG);
            return ResponseEntity.internalServerError().body(l);
        }

        System.out.println("Performing Query...");
        ResultSet resultSet;
        try {
            String query = "SELECT * FROM birthdays WHERE DATE_FORMAT(STR_TO_DATE(birthday, '%d-%m-%Y'), '%m-%d') = DATE_FORMAT(CURDATE(), '%m-%d')";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            resultSet = preparedStatement.executeQuery();
            System.out.println("Done!");
            ArrayList<Data> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(new Data(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)));
            }
            list.sort(new DateComparator());
            return ResponseEntity.ok().body(list);
        } catch (SQLException e) {
            ArrayList<String> l = new ArrayList<>();
            l.add(ERROR_MSG);
            return ResponseEntity.internalServerError().body(l);
        }
    }

    @GetMapping("/api/upcomingBirthdays")
    public ResponseEntity<ArrayList> getUpcomingBirthdays() {

        try {
            ensureConnection();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            System.out.println("Could not connect to database! Aborting...");
            ArrayList<String> l = new ArrayList<>();
            l.add(ERROR_MSG);
            return ResponseEntity.internalServerError().body(l);
        }


        System.out.println("Performing Query...");
        ResultSet resultSet;
        try {
            String query = "SELECT * FROM birthdays WHERE DATE_FORMAT(STR_TO_DATE(birthday, '%d-%m-%Y'), '%m-%d') BETWEEN DATE_FORMAT(CURDATE(), '%m-%d') and DATE_FORMAT(CURDATE() + INTERVAL 1 MONTH, '%m-%d')"; // order by MONTH(STR_TO_DATE(birthday, '%d-%m-%Y')), DAY(STR_TO_DATE(birthday, '%d-%m-%Y'))";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            resultSet = preparedStatement.executeQuery();
            System.out.println("Done!");
            ArrayList<Data> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(new Data(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)));
            }

            list.sort(new DateComparator());
            return ResponseEntity.ok().body(list);
        } catch (SQLException e) {
            ArrayList<String> l = new ArrayList<>();
            l.add(ERROR_MSG);
            return ResponseEntity.internalServerError().body(l);
        }
    }

    @PostMapping("/api/addBirthday")
    public ResponseEntity<String> addBirthday(@RequestBody Data data) {
        try {
            LocalDate.parse(data.birthday(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch(DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid Date!");
        }
        try {
            ensureConnection();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            System.out.println("Could not connect to database! Aborting...");
            return ResponseEntity.internalServerError().body(ERROR_MSG);
        }
        try {
            String update = "INSERT INTO birthdays (first_name, last_name, birthday) VALUES (?, ?, ?);";
            PreparedStatement preparedStatement = connection.prepareStatement(update);
            preparedStatement.setString(1, data.first_name().toLowerCase());
            preparedStatement.setString(2, data.last_name().toLowerCase());
            preparedStatement.setString(3, data.birthday().toLowerCase());
            System.out.println("Inserting entry:\tfirst_name: '" + data.first_name() + "', last_name: '" + data.last_name() + "', birthday: '" + data.birthday() + "'");
            int res = preparedStatement.executeUpdate();
            String msg;
            //TODO: check validity
            if (res > 0) {
                msg = "Successfully inserted!";
                System.out.println(msg);
                return ResponseEntity.ok().body(msg);
            } else {
                msg = "Insert failed!";
                System.out.println(msg);
                return ResponseEntity.badRequest().body(msg);
            }
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(ERROR_MSG);
        }
    }

    public static void printResults(ResultSet resultSet, String... columns) throws SQLException {
        if(!resultSet.next()) {
            System.out.println("Result is Empty!");
            return;
        }
        System.out.println("---------------------------------------------");
        do {
            System.out.print("|\t");
            for (String s : columns) {
                System.out.print(resultSet.getString(s) + "\t|\t");
            }
            System.out.println();
        } while(resultSet.next());
        System.out.println("---------------------------------------------");
    }

}
