package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Controller class
 */
public class Controller {
    public Label label1;
    public TableView ordersTable;
    public ComboBox workerNames;
    public Button setWorker;
    public Button cancelOrder;
    private String orderID;

    /**
     * Filling of the orders table with clicking on the label
     * @param event
     */
    public void fillTable(Event event) {
        ordersTable.setMinHeight(350);
        ordersTable.setMinWidth(450);
        DisplayDatabase.buildData(ordersTable);
        fillCB();
    }

    /**
     * Parsing of the selected table row for the following actions (assigning the worker or changing the order status)
     * @param event
     */
    public void addWorker(Event event) {
        String r = ordersTable.getSelectionModel().getSelectedItem().toString();
        r = r.substring(1, r.length() - 1);
        String[] row = r.split(", ");
        orderID = row[0];
        if (!row[3].equals("None")) {         // работник назначен - кнопка неактивна
            setWorker.setDisable(true);
        }
        else if (row[3].equals("None") & row[2].equals("payed")) {
            setWorker.setDisable(false);
        }
    }

    /**
     * Filling of the ComboBox with worker names
     */
    private void fillCB() {
        workerNames.getItems().clear();
        try {
            Connection connect = DBConnection.connect();
            ResultSet rs = connect.createStatement().executeQuery("select name from emps where post = 'Worker'");
            while(rs.next()){
                workerNames.getItems().add(rs.getString(1));
            }
            connect.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Setting worker for the selected order with clicking on the button. Sending the POST-request.
     * @param actionEvent
     */
    public void setWorker(ActionEvent actionEvent)  {
        if (!orderID.equals("") & workerNames.getSelectionModel().getSelectedItem() != null) {
            String sql = "select id from emps where name = '" +
                    workerNames.getSelectionModel().getSelectedItem().toString() + "'";
            String wID = "";
            try {
                Connection connect = DBConnection.connect();
                ResultSet rs = connect.createStatement().executeQuery(sql);
                while (rs.next())
                    wID = rs.getString(1);
                try {
                    SendPost("assigned", wID);
                }
                catch (IOException ie) {
                    showMessageBox("Error: check your connection");
                }
                connect.close();
                fillTable(actionEvent);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        else
            showMessageBox("Please, choose the order and select a worker for it from list under the table");
    }

    /**
     * Shows message box
     * @param message - The message to show
     */
    private void showMessageBox(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Sending the POST-request. Cancelling the order
     * @param actionEvent
     * @throws IOException
     */
    public void cancelOrder(ActionEvent actionEvent) throws IOException {
        if (!orderID.equals("")) {
            SendPost("canceled", "1");
            fillTable(actionEvent);
        }
        else
            showMessageBox("You should choose the order at first");
    }

    /**
     * Building the JSON-file and sending it via POST-request
     * @param status - Order status
     * @param wID - Worker id
     * @throws IOException
     */
    private void SendPost(String status, String wID) throws IOException {
        JSONObject json = new JSONObject();
        json.put("order_id", orderID);
        json.put("status", status);
        json.put("worker_id", wID);

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://localhost:8080/changedorder");
        StringEntity input = new StringEntity(json.toString());
        input.setContentType("application/json");
        post.setEntity(input);
        HttpResponse response = client.execute(post);
        /*BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }*/
    }
}
