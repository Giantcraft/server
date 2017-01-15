import json.simple.JSONObject;
import json.simple.parser.JSONParser;
import json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by caspe on 10-12-2016.
 */
public class Server{

    static List<String> allowedAddress = new ArrayList<>();
    static Map<String,Socket> spigotServers = new HashMap<>();

    public static void main(String[] args){
        loadServer();
    }

    public static void loadServer(){
        allowedAddress.add("localhost");
        allowedAddress.add("127.0.0.1");
        allowedAddress.add("83.92.126.142");
        allowedAddress.add("151.80.230.197");
        try {
            int port = 25505;
            ServerSocket serverSocket = new ServerSocket(port,0);
            System.out.println("Port: " + serverSocket.getLocalPort());
            while (true){
                Socket socket = serverSocket.accept();
                String ip = socket.getInetAddress().getHostAddress();
                if (allowedAddress.contains(ip)) {

                    System.out.println("IP connected: " + ip);

                    List<Byte> byteDataList = new ArrayList<>();
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();

                    for (int i = 0; i < 256; i++){
                        try {
                            byte oneByte = (byte) inputStream.read();
                            if (oneByte == 0 || oneByte == -1 || oneByte == 127) {
                                break;
                            } else {
                                byteDataList.add(oneByte);
                            }
                        }catch (SocketException e){
                            socket.close();
                            break;
                        }
                    }
                    if (byteDataList.size() > 0) {
                        byte[] bytes = new byte[byteDataList.size()];
                        for (int i = 0; i < byteDataList.size(); i++) {
                            bytes[i] = byteDataList.get(i);
                        }

                        String jsonString = toString(bytes);
                        JSONObject jsonObject = stringToJson(jsonString);
                        if(jsonObject!=null) {
                            if (jsonObject.get("m") != null) {
                                String key = (String) jsonObject.get("m");
                                String message = getURLData("http://pack.giantcraft.dk/app/Socket.php?p=dSjQ7KVNB2jDq8Wf&k=" + key);
                                System.out.println(message);
                                jsonObject = stringToJson(message);
                                if (jsonObject != null) {
                                    String packetType = (String) jsonObject.get("type");
                                    if (packetType.equals("spigot")) {
                                        String serverName = (String) jsonObject.get("name");
                                        if (!spigotServers.containsKey(serverName)) {
                                            spigotServers.put(jsonObject.get("name").toString(), socket);
                                            System.out.println("Added: " + jsonObject.get("name"));
                                        }
                                        getURLData("http://pack.giantcraft.dk/app/Socket.php?p=dSjQ7KVNB2jDq8Wf&r=" + key);
                                    }else if (packetType.equals("spigotClose")) {
                                        String serverName = (String) jsonObject.get("s");
                                        if (spigotServers.containsKey(serverName)) {
                                            spigotServers.get(serverName).close();
                                            spigotServers.remove(serverName);
                                            System.out.println("Removed: " + serverName);
                                        }
                                        getURLData("http://pack.giantcraft.dk/app/Socket.php?p=dSjQ7KVNB2jDq8Wf&r=" + key);
                                    }else if (packetType.equals("sendCmd")) {
                                        String server = jsonObject.get("s").toString();
                                        if (spigotServers.containsKey(server)) {
                                            Socket spigotSocket = spigotServers.get(server);
                                            try {
                                                System.out.println("Socket is open: " + server);
                                                OutputStream spigotOut = spigotSocket.getOutputStream();
                                                byte[] data = toBytes(jsonString);
                                                spigotOut.write(data, 0, data.length);
                                            } catch (SocketException e) {
                                                spigotServers.remove(server);
                                                System.out.println("Socket is closed: " + server);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                }else{
                    System.out.println(ip + " tried to connect, but was not allowed");
                    socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getURLData(String sUrl){

        URL url;
        try {
            url = new URL(sUrl);
            Scanner s = new Scanner(url.openStream());
            StringBuilder stringBuilder = new StringBuilder();
            while (s.hasNext()){
                stringBuilder.append(s.next() + " ");
            }
            return stringBuilder.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static JSONObject stringToJson(String string){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) parser.parse(string);
        } catch (ParseException e) {
            System.out.println("Kunne ikke laves til JSON: " + string);
        }
        return jsonObject;
    }

    public static String toString(byte[] bytes){
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] toBytes(String string){
        byte[] bytes = new byte[256];

        for (int i = 0; i < string.getBytes().length; i++){
            bytes[i] = string.getBytes()[i];
        }
        bytes[string.getBytes().length] = 127;

        return bytes;
    }

}
