package mj.file_handler.executer.handler_impl;

import mj.file_handler.executer.CommandExEventHandler;

public class SuppressOutput extends CommandExEventHandler {

	@Override
	public void standardOutput(String line) {
		System.out.println(line); 
	}

	@Override
	public void errorOutput(String line) {
		System.err.println(line); 
	}

	@Override
	public void executionStarted() {

	}

	@Override
	public void executionStopped() {

	}

	@Override
	public void processKilled() {
		System.out.println("process killed"); 
	}
}
