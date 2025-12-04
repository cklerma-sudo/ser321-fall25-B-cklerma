package example.grpcclient;

import io.grpc.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.grpc.netty.shaded.io.netty.internal.tcnative.Library;
import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final ConverterGrpc.ConverterBlockingStub blockingStub5;
  private final LibraryGrpc.LibraryBlockingStub blockingStub6;
  private final TriangleGrpc.TriangleBlockingStub blockingStub7;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    blockingStub5 = ConverterGrpc.newBlockingStub(channel);
    blockingStub6 = LibraryGrpc.newBlockingStub(channel);
    blockingStub7 = TriangleGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    blockingStub5 = ConverterGrpc.newBlockingStub(channel);
    blockingStub6 = LibraryGrpc.newBlockingStub(channel);
    blockingStub7 = TriangleGrpc.newBlockingStub(channel);
  }

  public void askServerToCalcArea(int size) {
    calcRequest req = calcRequest.newBuilder().setSize(size).build();
    calcResponse rep = null;
    try{
      rep = blockingStub7.calcArea(req);
    }catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    }catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.println("It would take " + rep.getArea() + " characters to make a right triangle with " + size + " rows.");

  }

  public void askServerToPrintTriangle(String ch, int height, String style) {
    printRequest req = printRequest.newBuilder()
            .setCh(ch)
            .setHeight(height)
            .setStyle(style)
            .build();
    printResponse rep = null;
    try{
      rep = blockingStub7.printTriangle(req);
    }catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    }catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.println("Here is your triangle: \n" + rep.getTriangle());
  }

  public void askServerToListBooks(){
    Empty empt = Empty.newBuilder().build();
    BookListResponse rep = null;
    try{
      rep = blockingStub6.listBooks(empt);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    }catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }

    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.println(rep.getBooksList());
  }

  public void askServerToSearchBooks(String query){
    BookSearchRequest req = BookSearchRequest.newBuilder().setQuery(query).build();
    BookListResponse rep = null;
    try{
      rep = blockingStub6.searchBooks(req);
    }catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      } else {
        throw e;
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }

    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.println(rep.getBooksList());
  }

  public void askServerToBorrowBook(String isbn, String name, String date){
    BorrowRequest req = BorrowRequest.newBuilder()
            .setIsbn(isbn)
            .setBorrowDate(date)
            .setBorrowerName(name)
            .build();
    BorrowResponse rep = null;
    try{
      rep = blockingStub6.borrowBook(req);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }

    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.println(rep.getMessage());
  }

  public void askServerToReturnBook(String isbn, String date){
    ReturnRequest req = ReturnRequest.newBuilder()
            .setIsbn(isbn)
            .setReturnDate(date)
            .build();
    ReturnResponse rep = null;
    try{
      rep = blockingStub6.returnBook(req);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    }catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }

    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.println(rep.getMessage());
  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response = null;
    try {
      response = blockingStub.parrot(request);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForConverstion(String from, String to, double value) {
    ConversionRequest req = ConversionRequest.newBuilder()
            .setFromUnit(from)
            .setToUnit(to)
            .setValue(value)
            .build();
    ConversionResponse rep = null;
    try {
      rep = blockingStub5.convert(req);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }

    if (!rep.getIsSuccess()){
      System.out.println(rep.getError());
      return;
    }
    System.out.printf("The answer is %.2f %s units.\n", rep.getResult(), to);
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response = null;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response = null;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
        System.out.println("Node has shut down. Shutting down...");
        System.exit(0);
      }
      else {
        throw e;
      }
    }catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create a communication channel to the server (Node), known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      // ##############################################################################
      // ## Assume we know the port here from the service node it is basically set through Gradle
      // here.
      // In your version you should first contact the registry to check which services
      // are available and what the port
      // etc is.

      /**
       * Your client should start off with 
       * 1. contacting the Registry to check for the available services
       * 2. List the services in the terminal and the client can
       *    choose one (preferably through numbering) 
       * 3. Based on what the client chooses
       *    the terminal should ask for input, eg. a new sentence, a sorting array or
       *    whatever the request needs 
       * 4. The request should be sent to one of the
       *    available services (client should call the registry again and ask for a
       *    Server providing the chosen service) should send the request to this service and
       *    return the response in a good way to the client
       * 
       * You should make sure your client does not crash in case the service node
       * crashes or went offline.
       */

      // create client
      Client client = new Client(channel, regChannel);
      if (args[5].equals("true")) {
        // Comment these last Service calls while in Activity 1 Task 1, they are not needed and wil throw issues without the Registry running
        // get thread's services
        client.getServices(); // get all registered services

        // get parrot
        client.findServer("services.Echo/parrot"); // get ONE server that provides the parrot service

        // get all setJoke
        client.findServers("services.Joke/setJoke"); // get ALL servers that provide the setJoke service

        // get getJoke
        client.findServer("services.Joke/getJoke"); // get ALL servers that provide the getJoke service

        // get Converter
        client.findServers("services.Converter/convert");

        // does not exist
        //client.findServer("random"); // shows the output if the server does not find a given service
      }

      else {
        System.out.println("Services on the connected node. (without registry)");
        client.getNodeServices(); // get all registered services
      }

      // call the parrot service on the server
      client.askServerToParrot(message);

      // ask the user for input how many jokes the user wants
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      boolean loop = true;
      while (loop) {
        System.out.print("What would you like to do today?\n 1 - Echo a message back\n 2 - Hear some jokes\n 3 - Add a joke\n 4 - Convert one measurement unit to another\n 5 - Go to Library Menu\n" +
                " 6 - Print a Triangle\n 7 - Calculate how many characters for a right triangle\n  q - quit\n");
        String choice = reader.readLine();
        switch (choice) {
          case "1":
            System.out.println("What would you like echoed back?");
            String echo = reader.readLine();
            client.askServerToParrot(echo);
            break;
          case "2":
            System.out.println("How many jokes would you like?");
            String num = reader.readLine();
            try {
              client.askForJokes(Integer.parseInt(num));
            } catch (Exception e) {
              System.err.println("Number of jokes must be an integer");
              break;
            }
            break;
          case "3":
            System.out.println("What is your joke?");
            String joke = reader.readLine();
            client.setJoke(joke);
            break;
          case "4":
            System.out.println("What unit are you converting from? (Supported units \"KILOMETER\", \"MILE\", \"YARD\", \"FOOT\", \"KILOGRAM\", \"POUND\", \"CELSIUS\", \"FAHRENHEIT\")");
            String from = reader.readLine();
            System.out.println("What unit are you converting to?");
            String to = reader.readLine();
            System.out.println("How many " + from + "s?");
            String value = reader.readLine();
            try{
              client.askForConverstion(from, to, Double.parseDouble(value));
            } catch (Exception e) {
              System.err.println("Conversion number must be an double");
              break;
            }
            break;
          case "5":
            libraryMenu(client, reader);
            break;
          case "6":
            System.out.println("Enter how many rows in the triangle");
            String row = reader.readLine();
            int height;
            try {
              height = Integer.parseInt(row);
            } catch (NumberFormatException e) {
              System.err.println("Number of rows must be an integer");
              break;
            }
            System.out.println("What character would you like it print as? Note: it can only be one");
            String character = reader.readLine();
            System.out.println("What style of triangle? Supported styles: \"RIGHT\", \"LEFT\", \"CENTER\", \"HOLLOW\"");
            String style = reader.readLine();
            client.askServerToPrintTriangle(character, height, style);
            break;
          case "7":
            System.out.println("Enter how many rows in the triangle");
            String rows = reader.readLine();
            int size;
            try {
              size = Integer.parseInt(rows);
            } catch (NumberFormatException e) {
              System.err.println("Number of rows must be an integer");
              break;
            }
            client.askServerToCalcArea(size);
            break;
          case "q":
            System.out.println("Goodbye!");
            loop = false;
            break;
          default:
            System.out.println("Invalid choice");
            break;
        }
      }


    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      if (args[5].equals("true")) { 
        regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }

  private static void libraryMenu(Client client, BufferedReader reader) {
    try {
      boolean loop = true;
      while (loop) {
        System.out.print("Select what Library Operation you would like:\n 1 - List all books\n 2 - Search by title/author\n 3 - Borrow a book (Uses ISBN)\n 4 - Return a book (Uses ISBN)\n" +
                " q - Return to main menu\n");
        String choice = reader.readLine();
        switch (choice) {
          case "1":
            client.askServerToListBooks();
            break;
          case "2":
            System.out.println("What is the title/author you are looking for?");
            String query = reader.readLine();
            client.askServerToSearchBooks(query);
            break;
          case "3":
            System.out.println("What is the ISBN of the book you are looking for?");
            String isbn = reader.readLine();
            System.out.println("What is your name?");
            String name = reader.readLine();
            System.out.println("What is the date of borrowing (YYYY-MM-DD)?");
            String borrowDate = reader.readLine();
            client.askServerToBorrowBook(isbn, name, borrowDate);
            break;
          case "4":
            System.out.println("What is the ISBN of the book you are looking for?");
            String isbn2 = reader.readLine();
            System.out.println("What is the date of returning?");
            String returnDate = reader.readLine();
            client.askServerToReturnBook(isbn2, returnDate);
            break;
          case "q":
            System.out.println("Returning to Main menu");
            loop = false;
            break;
          default:
            System.out.println("Invalid choice");
            break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
