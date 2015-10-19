package mj.ocraptor.rmi_client;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

import mj.ocraptor.configuration.Config;

public interface RMIClient extends Remote {
  public String getID() throws RemoteException;
  void init(Config config) throws RemoteException;
  void handleFile(File file) throws RemoteException;
  void ping() throws RemoteException;
  void shutdown() throws RemoteException;
  void shutdownDelayed(int delay) throws RemoteException;
  boolean isBusy() throws RemoteException;
}
