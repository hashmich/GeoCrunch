package geoCrunch;

import java.io.File;

public class CommandLine {
	
	private static File inputDirectory;
	private static File outputDirectory;
	
	
	
	public static void main(String[] args) throws Exception {
		Core core = null;
		if(args.length == 0) {
			System.out.println("no arguments given, proceeding with config settings");
			core = new Core();
		}else{
			if(args[0] == ".") {
				inputDirectory = new File(System.getProperty("user.dir"));
			}else{
				inputDirectory = new File(args[0]);
			}
			if(args.length == 1)
				core = new Core(inputDirectory);
			
			if(args.length == 2) {
				if(args[1] == ".") {
					core = new Core(inputDirectory);
				}else{
					outputDirectory = new File(args[1]);
					core = new Core(inputDirectory, outputDirectory);
				}
			}
		}
		
		core.main();
		
		System.out.println("Open a webbrowser and browse to:");
		System.out.println(new File(System.getProperty("user.dir"), "webapp/index.html").toURI());
		System.out.println("to view the results.");
	}
}
