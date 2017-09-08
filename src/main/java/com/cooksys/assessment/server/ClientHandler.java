package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
		
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				//create new map to hold user names and sockets
				Map<String, Socket> names = new HashMap<>();
				
				
				switch (message.getCommand()) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						//adds socket and the user name to the map
						names.put(message.getUsername(), socket);
						//create new message object to hold 'user is connected' message
						Message isConnect = new Message();
						//set contents of the message
						isConnect.setContents(message.getUsername() + " is connected");
						//iterates through the map of username - socket pairs
						for (Map.Entry<String, Socket> pair : names.entrySet()){
							//creates output stream to every socket in the map
							PrintWriter conWriter = new PrintWriter(new OutputStreamWriter(pair.getValue().getOutputStream()));
							//assign string to be sent to the client
							String conAlert = mapper.writeValueAsString(isConnect);
							//send message that the user is connected
							conWriter.write(conAlert);
							conWriter.flush();
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						//create the message containing alert that the user is disconnected
						Message isDisconnect = new Message();
						//set contents of the message
						isDisconnect.setContents(message.getUsername() + " is disconnected");
						//assign string to be sent to the client
						String disconAlert = mapper.writeValueAsString(isDisconnect);
						//iterates through the map of username - socket pairs
						for (Map.Entry<String, Socket> pair : names.entrySet()){
							//creates output stream to every socket in the map
							PrintWriter disconWriter = new PrintWriter(new OutputStreamWriter(pair.getValue().getOutputStream()));
							//send message that the user is disconnected
							disconWriter.write(disconAlert);
							disconWriter.flush();
						}
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
						//implements broadcast command
					case "broadcast":
						log.info("user <{}> broadcasting  <{}>", message.getUsername(), message.getContents());
						//iterates through the map of username - socket pairs
						for (Map.Entry<String, Socket> pair : names.entrySet()){
							//do not send to this user
							if(!message.getUsername().equals(pair.getKey())){
								//creates output stream to every socket in the map
								PrintWriter brWriter = new PrintWriter(new OutputStreamWriter(pair.getValue().getOutputStream()));
								//serialize data
								String broadMess = mapper.writeValueAsString(message);
								brWriter.write(broadMess);
								brWriter.flush();
							}
						}
						break;
						//implements username command
					case "@username":
						log.info("user <{}> sending message  <{}>", message.getUsername(), message.getContents());
						//implements users command
					case "users":
						log.info("user <{}> displays list of connected users", message.getUsername());
						//create the message containing alert that the user is disconnected
						Message unListObj = new Message();
						//list of user names to display when requested
						String userNameList = "";
						//iterates through the list of connected users
						for (Map.Entry<String, Socket> pair : names.entrySet()){
							userNameList += pair.getKey() + "  ";
						}
						//set contents of the message
						unListObj.setContents(userNameList);
						//assign string to be sent to the client
						String usNames = mapper.writeValueAsString(unListObj);
						//send the list of usernames string to the client
						writer.write(usNames);
						writer.flush();
						break;
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
