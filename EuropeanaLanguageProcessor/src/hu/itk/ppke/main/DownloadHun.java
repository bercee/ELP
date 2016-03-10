package hu.itk.ppke.main;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DownloadHun {

	public static void main(String[] args) {
		try {
			String[] s = new String[1];
			s[0] = "qf=LANGUAGE:hu";
			Downloader d = new Downloader("qYEuct4rp", "*", s, new File("output_folder"), Downloader.OutputFormat.JSON_SeparateFiles);
			d.execute();
			d.get();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}

}
