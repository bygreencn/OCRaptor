package mj.ocraptor.rmi_server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import mj.ocraptor.database.dao.FileEntry;
import mj.ocraptor.rmi_client.RMIClient;

public interface RMIServer extends Remote {

  // ------------------------------------------------ //

  void addClient(final RMIClient client) throws RemoteException;

  void removeClient(final RMIClient client) throws RemoteException;

  void transmitResult(final RMIClient client, final FileEntry result) throws RemoteException;

  void ping(final RMIClient client) throws RemoteException;

  void incrementImageCount(final RMIClient client) throws RemoteException;

  // ------------------------------------------------ //
  // *INDENT-OFF*

  void sendDebugInfo  (final RMIClient client, final String msg, final Throwable e, final boolean consoleOnly) throws RemoteException;

  void sendDebugError (final RMIClient client, final String msg, final Throwable e, final boolean consoleOnly) throws RemoteException;

  // *INDENT-ON*
  // ------------------------------------------------ //

}
