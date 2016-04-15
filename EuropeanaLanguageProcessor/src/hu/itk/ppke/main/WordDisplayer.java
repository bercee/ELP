package hu.itk.ppke.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import hu.itk.ppke.main.WordCollection.Word;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class WordDisplayer extends Application {

	TextField lemmaField = new TextField();
	ChoiceBox<String> lemmaBox = new ChoiceBox<>();
	ChoiceBox<String> lexFormBox = new ChoiceBox<>();
	TextArea area = new TextArea();
	ScrollPane scroll = new ScrollPane(area);
	FileChooser fc = new FileChooser();
	
	MenuBar mb = new MenuBar();
	Menu file = new Menu("File");
	MenuItem read = new MenuItem("Read word collection");
	
	Stage primaryStage;
	
	WordCollection wc = null;
	
	int c = 0;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		scroll.setFitToHeight(true);
		scroll.setFitToWidth(true);
		VBox vbox = new VBox(lemmaField, lemmaBox, lexFormBox);
		
		
		HBox hbox = new HBox(vbox, scroll);
		VBox root = new VBox(mb, hbox);
		primaryStage.setScene(new Scene(root, 500, 600));
		
		file.getItems().add(read);
		mb.getMenus().add(file);
		read.setOnAction(e -> readFile());
		lemmaField.setOnAction(e -> searchLemma());
		lexFormBox.setOnAction(e -> displayLexForm());
		
		
		primaryStage.show();
		
	}
	
	private void displayLexForm(){
		area.setText("");
		String lexForm = lexFormBox.getValue();
		String lemma = lemmaField.getText();
		if (lexForm.equals("ALL")){
			//ALL
			wc.map.get(lemma).formsTable.forEach((k,v)->{
				c = 0;
				v.forEach((kk,vv) -> c+=vv);
				area.appendText(k+" - "+c+"\n");
			});
			return;
			
		}
		wc.map.get(lemma).formsTable.get(lexForm).forEach((k,v) -> area.appendText(k+" - "+v+"\n"));
	}
	
	private void searchLemma(){
		String s = lemmaField.getText();
		if (wc.map.containsKey(s)){
			Word w = wc.map.get(s);
			ObservableList<String> l = FXCollections.observableArrayList(w.formsTable.keySet().toArray(new String[0]));
			l.add(0, "ALL");
			lexFormBox.setItems(l);
			primaryStage.sizeToScene();
		}else {
			//SEarch for neares option
		}
			
		
	}
	
	private void readFile(){
		
		File f = fc.showOpenDialog(primaryStage);
		if (f == null)
			return;
		Task<Void> t = new Task<Void>(){
			
			Label l = new Label("Please wait.");
			Group g = new Group(l);
			Stage s = new Stage();
			{
				s.setScene(new Scene(g));
				s.setOnCloseRequest(e -> {});
			}

			
			@Override
			protected Void call()  throws Exception{
				
				
				
				Platform.runLater(() -> s.show());
				
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
				Object o = ois.readObject();
				ois.close();
				if (o instanceof WordCollection) {
					wc = (WordCollection) o;
				}else {
					throw new Exception("Selected file is invalid.");
				}
				ObservableList<String> l = FXCollections.observableArrayList(wc.map.keySet());
				System.out.println(l);
				Platform.runLater(() -> {
	//				lemmaBox.setItems(l);
					primaryStage.sizeToScene();
					s.close();
					});
				return null;
			}
			
		};
		
		Thread th = new Thread(t);
		th.setDaemon(true);
		th.start();
		
	}

}
