package com.example.chatgui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class HelloController {
    DataOutputStream out;
    @FXML
    TextField textField;
    @FXML
    TextArea textArea;
    @FXML
    TextArea usersTextArea;
    String login = null;
    String pass = null;
    boolean isAuth = false;
    public HelloController() {

    }
    @FXML
    protected void auth(){
        if(login == null)
            textArea.appendText("Введите логин\n");
        else if(pass == null)
            textArea.appendText("Введите пароль\n");
    }
    @FXML
    protected void handlerSand() throws IOException {
        String text = textField.getText();
        textArea.appendText("Вы:"+text+"\n");
        textField.clear();
        textField.requestFocus();
        if(isAuth)
            out.writeUTF(text);
        else{
            if (login == null){
                login = text;
                auth();
            }else if(pass == null){
                pass = text;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("login", login);
                jsonObject.put("pass", pass);
                out.writeUTF(jsonObject.toJSONString());
                login = null;
                pass = null;
            }
        }
    }
    @FXML
    public void connect(){
        try {
            Socket socket = new Socket("127.0.0.1", 9743);
            this.out = new DataOutputStream(socket.getOutputStream());
            DataInputStream is = new DataInputStream(socket.getInputStream());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            if(!isAuth) auth();
                            String response = is.readUTF();
                            JSONParser jsonParser = new JSONParser();
                            JSONObject jsonResponse = (JSONObject) jsonParser.parse(response);
                            if(jsonResponse.get("authResult")!= null){
                                if(jsonResponse.get("authResult").equals("error"))
                                    textArea.appendText("Неправильный логин или пароль\n");
                                else if(jsonResponse.get("authResult").equals("success")) isAuth = true;
                            } else if(jsonResponse.get("users") != null){
                                JSONArray jsonArray = (JSONArray) jsonParser.parse(jsonResponse.get("users").toString());
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    usersTextArea.appendText(jsonArray.get(i).toString()+"\n");
                                }
                            }else{
                                textArea.appendText(jsonResponse.get("msg").toString());
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