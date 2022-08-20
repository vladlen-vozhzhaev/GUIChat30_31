package com.example.chatgui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;

import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class HelloController {
    DataOutputStream out;
    DataInputStream is;
    Socket socket;
    @FXML
    TextField textField;
    @FXML
    TextArea textArea;
    @FXML
    VBox usersListVBox;
    String login = null;
    String pass = null;
    boolean isAuth = false;
    int toUser = 0; // 0 - расссылка всем пользователям

    @FXML
    protected void auth() throws IOException {
        String token = "";
        try {
            FileInputStream fis = new FileInputStream("C://java/token.txt");
            int i = -1;
            while ((i = fis.read()) != -1){
                //token += ((char) i);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (token.equals("")){
            if(login == null)
                textArea.appendText("Введите логин\n");
            else if(pass == null)
                textArea.appendText("Введите пароль\n");
        }else{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("token", token);
            jsonObject.put("login", "");
            jsonObject.put("pass", "");
            out.writeUTF(jsonObject.toJSONString());
        }
    }
    @FXML
    protected void handlerSend() throws IOException {
        String text = textField.getText();
        if(text.equals("")) return;
        textArea.appendText("Вы:"+text+"\n");
        textField.clear();
        textField.requestFocus();
        if(isAuth) {
            JSONObject request = new JSONObject();
            request.put("msg", text);
            request.put("to_user", toUser);
            out.writeUTF(request.toJSONString());
        }else{
            if (login == null){
                login = text;
                auth();
            }else if(pass == null){
                pass = text;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("login", login);
                jsonObject.put("pass", pass);
                jsonObject.put("token", "");
                out.writeUTF(jsonObject.toJSONString());
                login = null;
                pass = null;
            }
        }
    }
    @FXML
    public void connect(Scene scene){
        try {
            this.socket = new Socket("127.0.0.1", 9743);
            this.out = new DataOutputStream(this.socket.getOutputStream());
            this.is = new DataInputStream(this.socket.getInputStream());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int currentId =0;
                    while (true){
                        try {
                            if(!isAuth) auth();
                            String response = is.readUTF();
                            JSONParser jsonParser = new JSONParser();
                            JSONObject jsonResponse = (JSONObject) jsonParser.parse(response);
                            System.out.println(jsonResponse.get("authResult"));
                            if(jsonResponse.get("authResult")!= null){
                                if(jsonResponse.get("authResult").equals("error"))
                                    textArea.appendText("Неправильный логин или пароль\n");
                                else if(jsonResponse.get("authResult").equals("success")) {
                                    isAuth = true;
                                    String token = jsonResponse.get("token").toString();
                                    currentId = Integer.parseInt(jsonResponse.get("id").toString());
                                    FileOutputStream fos = new FileOutputStream("C://java/token.txt");
                                    byte[] buffer = token.getBytes();
                                    fos.write(buffer);
                                    fos.close();
                                }
                            } else if(jsonResponse.get("users") != null){
                                JSONArray jsonArray = (JSONArray) jsonParser.parse(jsonResponse.get("users").toString());
                                usersListVBox.getChildren().removeAll();
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        usersListVBox.getChildren().clear();
                                        Button commonChatBtn = new Button();
                                        commonChatBtn.setText("Общий чат");
                                        commonChatBtn.setOnAction(e->{
                                            textArea.setText("");
                                            toUser = 0;
                                            JSONObject jsonObject = new JSONObject();
                                            jsonObject.put("getHistoryMessage", 1);
                                            try {
                                                out.writeUTF(jsonObject.toJSONString());
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                            }
                                        });
                                        usersListVBox.getChildren().add(commonChatBtn);
                                    }
                                });
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    Button userBtn = new Button();
                                    JSONObject userInfo = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                                    if(Integer.parseInt(userInfo.get("user_id").toString()) == currentId) continue;
                                    userBtn.setText(userInfo.get("name").toString());
                                    userBtn.setId("user_"+userInfo.get("user_id").toString());
                                    userBtn.setOnAction(e->{
                                        Thread threadPrivateMessage = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Platform.runLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        textArea.setText("");
                                                    }
                                                });
                                                // добавляем список сообщений на textArea
                                                toUser = Integer.parseInt(userInfo.get("user_id").toString());
                                                System.out.println(toUser);
                                                JSONObject jsonObject = new JSONObject();
                                                jsonObject.put("getMessageToUser", toUser);
                                                try {
                                                    out.writeUTF(jsonObject.toJSONString());
                                                } catch (Exception ex) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        });
                                        threadPrivateMessage.start();
                                    });
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            usersListVBox.getChildren().add(userBtn);
                                        }
                                    });

                                    //usersTextArea.appendText(jsonArray.get(i).toString()+"\n");
                                }
                            }else if (jsonResponse.get("messages") != null){
                              JSONArray jsonArray = (JSONArray) jsonParser.parse(jsonResponse.get("messages").toString());
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JSONObject message = (JSONObject) jsonArray.get(i);
                                    String text = message.get("name").toString()+": "+message.get("text").toString()+"\n";
                                    textArea.appendText(text);
                                }
                            }else if(jsonResponse.get("privateMessages") != null){
                                JSONArray privateMessages = (JSONArray) jsonParser.parse(jsonResponse.get("privateMessages").toString());
                                for (int i = 0; i < privateMessages.size(); i++) {
                                    JSONObject message = (JSONObject) privateMessages.get(i);
                                    String text = message.get("from_id")+"_"+message.get("to_id")+": "+message.get("text");
                                    textArea.appendText(text+"\n");
                                }
                            }else{
                                if(toUser == Integer.parseInt(jsonResponse.get("from_id").toString()))
                                    textArea.appendText(jsonResponse.get("msg").toString()+"\n");
                                else{
                                    String fromId = jsonResponse.get("from_id").toString();
                                    Button button = (Button) scene.lookup("#user_"+fromId);
                                    button.setStyle("-fx-base: coral;");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}