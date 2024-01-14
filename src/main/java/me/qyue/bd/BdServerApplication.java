package me.qyue.bd;

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
    public ResponseEntity<String> getBirthday(@RequestParam(value = "first_name") String first_name,
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
            return ResponseEntity.ok().body(res);
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
            ArrayList<HashMap<String, String>> list = new ArrayList<>();
            if (!resultSet.next()) return ResponseEntity.ok().body(list);
            do {
                HashMap<String, String> h = new HashMap<>();
                h.put(resultSet.getString(1) + " " + resultSet.getString(2), resultSet.getString(3));
                list.add(h);
            } while (resultSet.next());
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

        LocalDate nowDate = LocalDate.now();
        LocalDate afterMonth = nowDate.plusMonths(1);

        System.out.println("Performing Query...");
        ResultSet resultSet;
        try {
            String query = "SELECT * FROM birthdays";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            resultSet = preparedStatement.executeQuery();
            System.out.println("Done!");
            ArrayList<HashMap<String, String>> list = new ArrayList<>();
            if (!resultSet.next()) return ResponseEntity.ok().body(list);
            do {
                String bd = resultSet.getString(3);
                LocalDate birthday = LocalDate.parse(bd, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                birthday = birthday.withYear(nowDate.getYear());
                if(birthday.isAfter(nowDate) && afterMonth.isAfter(birthday)) {
                    HashMap<String, String> h = new HashMap<>();
                    h.put(resultSet.getString(1) + " " + resultSet.getString(2), resultSet.getString(3));
                    list.add(h);
                }
            } while (resultSet.next());

            list.sort((o1, o2) -> {
                // DateFormat: "dd-MM-yyyy"
                String[] first = o1.values().toArray(new String[0]);
                first = first[0].split("-");
                String[] second = o2.values().toArray(new String[0]);
                second = second[0].split("-");
                //comparing if the month is equal
                if (Objects.equals(Integer.parseInt(first[1]), Integer.parseInt(second[1]))) {
                    //comparing if day is equal
                    if (Integer.parseInt(first[0]) == Integer.parseInt(second[0])) return 0;
                    return Integer.parseInt(first[0]) < Integer.parseInt(second[0]) ? -1 : 1;
                } else {
                    return Integer.parseInt(first[1]) < Integer.parseInt(second[1]) ? -1 : 1;
                }
            });

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
            int res = preparedStatement.executeUpdate();
            String msg =  res > 0 ? "Successfully inserted!" : "Insert failed!";
            return ResponseEntity.ok().body(msg);
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
