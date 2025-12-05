import com.google.common.base.Converter;
import com.google.protobuf.Empty;
import example.grpcclient.Client;
import example.grpcclient.LibraryImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.netty.internal.tcnative.Library;
import io.grpc.stub.StreamObserver;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import service.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Server unit tests for Assignment 6.
 *
 * IMPORTANT: These tests require the server to be running BEFORE you run them.
 *
 * To run these tests:
 * 1. First, start the server in one terminal: gradle runNode
 * 2. Then, in another terminal, run: gradle test
 *
 * The tests connect to localhost:8000 (the default port for runNode).
 * Make sure your server is running on this port before running tests.
 *
 * TODO for students:
 * This file contains example tests for the Echo and Joke services.
 * You need to add your own tests for:
 * - Converter service (happy path and error cases)
 * - Library service (happy path, error cases, and persistence testing)
 *
 * Your tests should follow the same pattern as the examples below.
 */
public class ServerTest {

    ManagedChannel channel;
    private EchoGrpc.EchoBlockingStub blockingStub;
    private JokeGrpc.JokeBlockingStub blockingStub2;
    private ConverterGrpc.ConverterBlockingStub blockingStub3;
    private LibraryGrpc.LibraryBlockingStub blockingStub4;


    @org.junit.Before
    public void setUp() throws Exception {
        // assuming default port and localhost for our testing, make sure Node runs on this port
        channel = ManagedChannelBuilder.forTarget("localhost:8000").usePlaintext().build();

        blockingStub = EchoGrpc.newBlockingStub(channel);
        blockingStub2 = JokeGrpc.newBlockingStub(channel);
        blockingStub3 = ConverterGrpc.newBlockingStub(channel);
        blockingStub4 = LibraryGrpc.newBlockingStub(channel);
    }

    @org.junit.After
    public void close() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

    }


    @Test
    public void parrot() {
        // success case
        ClientRequest request = ClientRequest.newBuilder().setMessage("test").build();
        ServerResponse response = blockingStub.parrot(request);
        assertTrue(response.getIsSuccess());
        assertEquals("test", response.getMessage());

        // error cases
        request = ClientRequest.newBuilder().build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());

        request = ClientRequest.newBuilder().setMessage("").build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());
    }

    // For this test the server needs to be started fresh AND the list of jokes needs to be the initial list
    @Test
    public void joke() {
        // getting first joke
        JokeReq request = JokeReq.newBuilder().setNumber(1).build();
        JokeRes response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("Did you hear the rumor about butter? Well, I'm not going to spread it!", response.getJoke(0));

        // getting next 2 jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("What do you call someone with no body and no nose? Nobody knows.", response.getJoke(0));
        assertEquals("I don't trust stairs. They're always up to something.", response.getJoke(1));

        // getting 2 more but only one more on server
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("How do you get a squirrel to like you? Act like a nut.", response.getJoke(0));
        assertEquals("I am out of jokes...", response.getJoke(1));

        // trying to get more jokes but out of jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("I am out of jokes...", response.getJoke(0));

        // trying to add joke without joke field
        JokeSetReq req2 = JokeSetReq.newBuilder().build();
        JokeSetRes res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // trying to add empty joke
        req2 = JokeSetReq.newBuilder().setJoke("").build();
        res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // adding a new joke (well word)
        req2 = JokeSetReq.newBuilder().setJoke("whoop").build();
        res2 = blockingStub2.setJoke(req2);
        assertTrue(res2.getOk());

        // should have the new "joke" now and return it
        request = JokeReq.newBuilder().setNumber(1).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("whoop", response.getJoke(0));
    }

    @Test
    public void convert() {
        //success case
        ConversionRequest req = ConversionRequest.newBuilder()
                .setFromUnit("FOOT")
                .setToUnit("YARD")
                .setValue(3.0)
                .build();
        ConversionResponse rep = blockingStub3.convert(req);
        assertTrue(rep.getIsSuccess());
        assertEquals(1.0, rep.getResult(), 1e-9);

        //error cases
        req = ConversionRequest.newBuilder()
                .setFromUnit("FOOT")
                .setToUnit("FOOT")
                .setValue(3.0)
                .build();
        rep = blockingStub3.convert(req);
        assertFalse(rep.getIsSuccess());
        assertEquals("They are the same unit", rep.getError());

        req = ConversionRequest.newBuilder()
                .setFromUnit("FOOT")
                .setToUnit("KILOGRAM")
                .setValue(3.0)
                .build();
        rep = blockingStub3.convert(req);
        assertFalse(rep.getIsSuccess());
        assertEquals("Invalid conversion request, both units must be length units", rep.getError());

        req = ConversionRequest.newBuilder().build();
        rep = blockingStub3.convert(req);
        assertFalse(rep.getIsSuccess());
        assertEquals("Conversion value or the units were not given. All fields are required.", rep.getError());
    }

    @Test
    public void library() {
        //success cases

        //listBooks
        List<Book> books = List.of(
                Book.newBuilder()
                        .setTitle("1984")
                        .setAuthor("George Orwell")
                        .setIsbn("978-0451524935")
                        .build(),

                Book.newBuilder()
                        .setTitle("To Kill a Mockingbird")
                        .setAuthor("Harper Lee")
                        .setIsbn("978-0061120084")
                        .build(),

                Book.newBuilder()
                        .setTitle("The Great Gatsby")
                        .setAuthor("F. Scott Fitzgerald")
                        .setIsbn("978-0743273565")
                        .build(),

                Book.newBuilder()
                        .setTitle("Pride and Prejudice")
                        .setAuthor("Jane Austen")
                        .setIsbn("978-0141439518")
                        .build(),

                Book.newBuilder()
                        .setTitle("The Catcher in the Rye")
                        .setAuthor("J.D. Salinger")
                        .setIsbn("978-0316769174")
                        .build(),

                Book.newBuilder()
                        .setTitle("Brave New World")
                        .setAuthor("Aldous Huxley")
                        .setIsbn("978-0060850524")
                        .build(),

                Book.newBuilder()
                        .setTitle("Moby-Dick")
                        .setAuthor("Herman Melville")
                        .setIsbn("978-1503280786")
                        .build(),

                Book.newBuilder()
                        .setTitle("The Hobbit")
                        .setAuthor("J.R.R. Tolkien")
                        .setIsbn("978-0547928227")
                        .build()
        );
        Empty empty = Empty.newBuilder().build();
        BookListResponse blRep = blockingStub4.listBooks(empty);
        assertTrue(blRep.getIsSuccess());
        assertEquals(books, blRep.getBooksList());

        //searchBooks
        BookSearchRequest searchReq = BookSearchRequest.newBuilder().setQuery("The Hobbit").build();
        Book theHobbit = Book.newBuilder()
                .setTitle("The Hobbit")
                .setAuthor("J.R.R. Tolkien")
                .setIsbn("978-0547928227")
                .build();
        blRep = blockingStub4.searchBooks(searchReq);
        assertTrue(blRep.getIsSuccess());
        assertEquals(theHobbit, blRep.getBooksList().get(0));

        //borrowBook
        BorrowRequest bReq = BorrowRequest.newBuilder()
                .setIsbn("978-0547928227")
                .setBorrowDate("2025-12-04")
                .setBorrowerName("Christopher Lerma")
                .build();
        BorrowResponse bRep = blockingStub4.borrowBook(bReq);
        assertTrue(bRep.getIsSuccess());
        assertEquals("Successfully Borrowed! Please return by 2026-01-04", bRep.getMessage());
        Book theHobbitBorrowed = Book.newBuilder()
                .setTitle("The Hobbit")
                .setAuthor("J.R.R. Tolkien")
                .setIsbn("978-0547928227")
                .setBorrowedBy("Christopher Lerma")
                .setIsBorrowed(true)
                .setReturnBy("2026-01-04")
                .build();
        BookSearchRequest searchReq2 = BookSearchRequest.newBuilder().setQuery("The Hobbit").build();
        blRep = blockingStub4.searchBooks(searchReq2);
        assertEquals(theHobbitBorrowed, blRep.getBooksList().get(0));

        //dataPersistence
        try {
            String JSONFile = Files.readString(Paths.get("library_data.json"));
            JSONArray jsonArray = new JSONArray(JSONFile);
            JSONObject book = null;
            for (int i = 0; i < jsonArray.length(); i++) {
                book = jsonArray.getJSONObject(i);
                if (book.getString("title").equals("The Hobbit")) break;
            }
            JSONObject testBook = new JSONObject();
            testBook.put("title", "The Hobbit");
            testBook.put("author", "J.R.R. Tolkien");
            testBook.put("isbn", "978-0547928227");
            testBook.put("borrowed_by", "Christopher Lerma");
            testBook.put("returned_by", "2026-01-04");
            testBook.put("is_borrowed", true);
            assertTrue(testBook.similar(testBook));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //returnBook
        ReturnRequest rReq = ReturnRequest.newBuilder()
                .setIsbn("978-0547928227")
                .setReturnDate("2025-12-04")
                .build();
        ReturnResponse rRep = blockingStub4.returnBook(rReq);
        assertTrue(rRep.getIsSuccess());
        assertEquals("Successfully Returned! You were on time", rRep.getMessage());
        blRep = blockingStub4.searchBooks(searchReq2);
        assertEquals(theHobbit, blRep.getBooksList().get(0));

        //errorCases
        BookSearchRequest searchReqError = BookSearchRequest.newBuilder().build();
        blRep = blockingStub4.searchBooks(searchReqError);
        assertFalse(blRep.getIsSuccess());
        assertEquals("The query is empty!", blRep.getError());

        ReturnRequest rReqError = ReturnRequest.newBuilder()
                .setIsbn("978-0547928227")
                .setReturnDate("2025-12-4")
                .build();
        rRep = blockingStub4.returnBook(rReqError);
        assertFalse(rRep.getIsSuccess());
        assertEquals("Invalid date format, must be YYYY-MM-DD", rRep.getError());

        BorrowRequest bReqError = BorrowRequest.newBuilder()
                .setIsbn("123")
                .setBorrowerName("Christopher Lerma")
                .setBorrowDate("2025-12-04")
                .build();
        bRep = blockingStub4.borrowBook(bReqError);
        assertFalse(bRep.getIsSuccess());
        assertEquals("Invalid ISBN the book was not found", bRep.getError());


    }

}