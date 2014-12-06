package mj.file_handler.executer;

public abstract class CommandExEventHandler {
	public abstract void standardOutput(String line);
	public abstract void errorOutput(String line);
	public abstract void executionStarted();
	public abstract void executionStopped(); 
	public abstract void processKilled(); 
}
